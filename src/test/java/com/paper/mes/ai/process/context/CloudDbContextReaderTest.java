package com.paper.mes.ai.process.context;

import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudDbContextReaderTest {

    private final ProcessOrderMapper orderMapper = mock(ProcessOrderMapper.class);
    private final OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
    private final PermissionChecker permissionChecker = mock(PermissionChecker.class);
    private final ProcessAiDraftBaselineReader baselineReader = mock(ProcessAiDraftBaselineReader.class);
    private CloudDbContextReader reader;

    @BeforeEach
    void setUp() {
        reader = new CloudDbContextReader(orderMapper, rollMapper, permissionChecker, baselineReader);
        when(baselineReader.read(any(), any())).thenReturn(List.of());
    }

    @Test
    void readReturnsAllowlistedDraftFactsWithStableShortReferences() {
        when(orderMapper.selectById("order-1")).thenReturn(draft(7));
        when(rollMapper.selectList(any())).thenReturn(List.of(roll("roll-2", 2), roll("roll-1", 1)));

        ProcessAiOrderContext context = reader.read("order-1", 7);

        assertThat(context.draftVersion()).isEqualTo(7);
        assertThat(context.remarkLong()).isEqualTo("客户原话");
        assertThat(context.rolls()).extracting(ProcessAiRollContext::shortRef)
                .containsExactly("R1", "R2");
        assertThat(context.baseline().remarkLong()).isEqualTo(context.remarkLong());
        verify(permissionChecker).require(Permissions.ORDER_CREATE);
        verify(permissionChecker).require(Permissions.AI_ASSIST);
    }

    @Test
    void readRejectsAStaleDraftVersion() {
        when(orderMapper.selectById("order-1")).thenReturn(draft(8));

        BusinessException exception = catchThrowableOfType(
                () -> reader.read("order-1", 7), BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo("AI_PROCESS_VERSION_CONFLICT");
        assertThat(exception.getCode()).isEqualTo(409);
    }

    @Test
    void readRejectsMoreThanOneHundredSourcePieces() {
        when(orderMapper.selectById("order-1")).thenReturn(draft(7));
        OriginalRoll oversized = roll("roll-1", 1);
        oversized.setPieceNum(101);
        when(rollMapper.selectList(any())).thenReturn(List.of(oversized));

        BusinessException exception = catchThrowableOfType(
                () -> reader.read("order-1", 7), BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo("AI_PROCESS_ROLL_LIMIT");
    }

    private ProcessOrder draft(int version) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(0);
        order.setIsDeleted(0);
        order.setVersion(version);
        order.setRemarkLong("客户原话");
        order.setCustomerName("不得出域");
        order.setOrderNo("不得出域");
        return order;
    }

    private OriginalRoll roll(String uuid, int rowSort) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setRowSort(rowSort);
        roll.setPaperName("白卡纸");
        roll.setGramWeight(250);
        roll.setOriginalWidth(2000);
        roll.setPieceNum(1);
        return roll;
    }
}
