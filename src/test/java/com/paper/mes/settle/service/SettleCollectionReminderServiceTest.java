package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.settle.dto.SettleCollectionReminderRequestDTO;
import com.paper.mes.settle.entity.SettleCollectionReminder;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleCollectionReminderMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettleCollectionReminderServiceTest {
    private SettleCollectionReminderMapper reminderMapper;
    private SettleOrderMapper orderMapper;
    private PermissionChecker permissionChecker;
    private OperationLogService operationLogService;
    private SettleCollectionReminderService service;

    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SettleOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SettleCollectionReminder.class);
    }

    @BeforeEach
    void setUp() {
        reminderMapper = mock(SettleCollectionReminderMapper.class);
        orderMapper = mock(SettleOrderMapper.class);
        permissionChecker = mock(PermissionChecker.class);
        operationLogService = mock(OperationLogService.class);
        service = new SettleCollectionReminderService(reminderMapper, orderMapper, permissionChecker,
                mock(BusinessLockService.class), operationLogService);
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1").username("collector")
                .realName("催收员").build());
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void record_forReceivableOrder_writesHistoryAndUpdatesQueueSnapshot() {
        when(orderMapper.selectById("settle-1")).thenReturn(receivable());
        when(reminderMapper.insert(any(SettleCollectionReminder.class))).thenAnswer(invocation -> {
            invocation.<SettleCollectionReminder>getArgument(0).setUuid("reminder-1");
            return 1;
        });
        when(orderMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        String uuid = service.record("settle-1", request());

        assertThat(uuid).isEqualTo("reminder-1");
        verify(permissionChecker).require(Permissions.SETTLE_RECEIVE);
        ArgumentCaptor<Wrapper<SettleOrder>> updateCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orderMapper).update(isNull(), updateCaptor.capture());
        assertThat(((LambdaUpdateWrapper<SettleOrder>) updateCaptor.getValue()).getSqlSet())
                .contains("reminder_count = reminder_count + 1").contains("last_reminder_time");
        verify(operationLogService).record(OperationLogService.BIZ_TYPE_SETTLE, "settle-1", "JS-001",
                OperationLogService.ACTION_COLLECTION_REMINDER, "催收员", "客户承诺下周付款");
    }

    @Test
    void record_forPaidOrder_rejectsWithoutWritingHistory() {
        SettleOrder paid = receivable();
        paid.setSettleStatus(3);
        paid.setUnreceivedAmount(BigDecimal.ZERO);
        when(orderMapper.selectById("settle-1")).thenReturn(paid);

        assertThatThrownBy(() -> service.record("settle-1", request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无需催收");

        verify(reminderMapper, never()).insert(any(SettleCollectionReminder.class));
    }

    @Test
    void record_forOlderReminder_keepsLatestSnapshotButIncrementsCount() {
        SettleOrder settle = receivable();
        settle.setLastReminderTime(LocalDateTime.of(2026, 7, 20, 10, 0));
        when(orderMapper.selectById("settle-1")).thenReturn(settle);
        when(reminderMapper.insert(any(SettleCollectionReminder.class))).thenAnswer(invocation -> {
            invocation.<SettleCollectionReminder>getArgument(0).setUuid("reminder-2");
            return 1;
        });
        when(orderMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        SettleCollectionReminderRequestDTO request = request();
        request.setReminderTime(LocalDateTime.of(2026, 7, 10, 10, 0));

        service.record("settle-1", request);

        ArgumentCaptor<Wrapper<SettleOrder>> updateCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orderMapper).update(isNull(), updateCaptor.capture());
        assertThat(((LambdaUpdateWrapper<SettleOrder>) updateCaptor.getValue()).getSqlSet())
                .contains("reminder_count = reminder_count + 1")
                .doesNotContain("last_reminder_time", "last_reminder_by",
                        "last_reminder_result", "next_follow_up_date");
    }

    private SettleOrder receivable() {
        SettleOrder order = new SettleOrder();
        order.setUuid("settle-1");
        order.setSettleNo("JS-001");
        order.setSettleStatus(1);
        order.setUnreceivedAmount(new BigDecimal("100"));
        return order;
    }

    private SettleCollectionReminderRequestDTO request() {
        SettleCollectionReminderRequestDTO dto = new SettleCollectionReminderRequestDTO();
        dto.setRequestId("request-1");
        dto.setReminderChannel(1);
        dto.setReminderResult(3);
        dto.setRemark("客户承诺下周付款");
        return dto;
    }
}
