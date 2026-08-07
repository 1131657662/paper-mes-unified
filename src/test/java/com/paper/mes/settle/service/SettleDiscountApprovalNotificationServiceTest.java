package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.notification.entity.SystemNotification;
import com.paper.mes.notification.mapper.SystemNotificationMapper;
import com.paper.mes.settle.entity.SettleDiscountApproval;
import com.paper.mes.settle.entity.SettleOrder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettleDiscountApprovalNotificationServiceTest {
    private SystemNotificationMapper notificationMapper;
    private SysUserMapper userMapper;
    private SettleDiscountApprovalNotificationService service;

    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SysUser.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SystemNotification.class);
    }

    @BeforeEach
    void setUp() {
        notificationMapper = mock(SystemNotificationMapper.class);
        userMapper = mock(SysUserMapper.class);
        service = new SettleDiscountApprovalNotificationService(notificationMapper, userMapper);
        when(notificationMapper.selectOne(any())).thenReturn(null);
    }

    @Test
    void publishRequested_excludesRequesterAndNotifiesEligibleAccounts() {
        when(userMapper.selectList(any())).thenReturn(List.of(
                user("requester"), user("finance-approver"), user("admin-approver")));

        service.publishRequested(approval("requester", "FINANCE"), settlement());

        ArgumentCaptor<SystemNotification> captor = ArgumentCaptor.forClass(SystemNotification.class);
        verify(notificationMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(SystemNotification::getRecipientUuid)
                .containsExactlyInAnyOrder("finance-approver", "admin-approver");
        assertThat(captor.getAllValues()).allMatch(item ->
                "SETTLE_DISCOUNT_REQUESTED".equals(item.getNotificationType()));
    }

    @Test
    void publishDecision_notifiesTheOriginalRequester() {
        SettleDiscountApproval approval = approval("requester", "ADMIN");

        service.publishDecision(approval, settlement(), SettleDiscountApprovalStatus.APPROVED);

        ArgumentCaptor<SystemNotification> captor = ArgumentCaptor.forClass(SystemNotification.class);
        verify(notificationMapper).insert(captor.capture());
        assertThat(captor.getValue().getRecipientUuid()).isEqualTo("requester");
        assertThat(captor.getValue().getNotificationType()).isEqualTo("SETTLE_DISCOUNT_APPROVED");
        assertThat(captor.getValue().getContent()).contains("JS202608070001", "¥5.00");
    }

    @Test
    void publishRequested_withoutIndependentAdmin_rejectsTheRequest() {
        when(userMapper.selectList(any())).thenReturn(List.of(user("requester")));

        assertThatThrownBy(() -> service.publishRequested(approval("requester", "ADMIN"), settlement()))
                .isInstanceOf(com.paper.mes.common.BusinessException.class)
                .hasMessageContaining("其他可审批的管理员账号");
    }

    private SysUser user(String uuid) {
        SysUser user = new SysUser();
        user.setUuid(uuid);
        user.setStatus(1);
        return user;
    }

    private SettleDiscountApproval approval(String requester, String level) {
        SettleDiscountApproval approval = new SettleDiscountApproval();
        approval.setUuid("approval-1");
        approval.setRequestBy(requester);
        approval.setRequiredLevel(level);
        approval.setDiscountAmount(new BigDecimal("5.00"));
        return approval;
    }

    private SettleOrder settlement() {
        SettleOrder settle = new SettleOrder();
        settle.setSettleNo("JS202608070001");
        settle.setCustomerName("测试客户");
        return settle;
    }
}
