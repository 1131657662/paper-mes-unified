package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.inventory.service.InventoryLedgerBusinessRecorder;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackRecordReopenServiceTest {

    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initialize(configuration, OriginalRoll.class);
        initialize(configuration, FinishRoll.class);
        initialize(configuration, FinishOriginalRel.class);
    }

    @Test
    void reopenWithdrawsInventoryAndPreservesRecordedValues() {
        Fixture fixture = fixture(0L, producedFinish());

        int reopened = fixture.service.reopen("order-1", List.of("roll-1"), "tester", 7);

        assertThat(reopened).isEqualTo(1);
        verify(fixture.inventoryLedgerRecorder).reverseReceipt(
                eq(fixture.finish), eq("order-1"), eq("7"), any());
        ArgumentCaptor<LambdaUpdateWrapper<FinishRoll>> finishUpdate = finishUpdateCaptor();
        verify(fixture.finishMapper).update(isNull(), finishUpdate.capture());
        assertThat(finishUpdate.getValue().getSqlSet())
                .contains("finish_status", "stock_in_time", "version = COALESCE(version, 0) + 1")
                .doesNotContain("actual_weight =");
        ArgumentCaptor<LambdaUpdateWrapper<OriginalRoll>> rollUpdate = rollUpdateCaptor();
        verify(fixture.rollMapper).update(isNull(), rollUpdate.capture());
        assertThat(rollUpdate.getValue().getSqlSet())
                .contains("is_checked", "roll_status", "check_user", "check_time")
                .doesNotContain("actual_weight", "actual_width", "actual_gram_weight");
    }

    @Test
    void reopenBlocksWhenFinishHasEnteredDeliveryFlow() {
        Fixture fixture = fixture(1L, producedFinish());

        assertThrows(BusinessException.class,
                () -> fixture.service.reopen("order-1", List.of("roll-1"), "tester", 7));

        verify(fixture.finishMapper, never()).update(isNull(), any());
        verify(fixture.rollMapper, never()).update(isNull(), any());
    }

    @Test
    void reopenChecksBlockingDeliveryActivityInOneBatch() {
        Fixture fixture = fixture(0L, producedFinish());

        fixture.service.reopen("order-1", List.of("roll-1"), "tester", 7);

        verify(fixture.deliveryMapper).countBlockingDeliveryActivity(List.of("finish-1"));
    }

    @Test
    void rollbackCleanupReversesInStockReceiptsBeforeClearingFinishFacts() {
        Fixture fixture = fixture(0L, producedFinish());

        int reversed = fixture.service.reverseStockInReceipts("order-1", 7);

        assertThat(reversed).isEqualTo(1);
        verify(fixture.inventoryLedgerRecorder).reverseReceipt(
                eq(fixture.finish), eq("order-1"), eq("7"), any());
        verify(fixture.deliveryMapper).countBlockingDeliveryActivity(List.of("finish-1"));
    }

    @Test
    void reopenRestoresAPlannedFinishThatWasMarkedNotProduced() {
        FinishRoll finish = producedFinish();
        finish.setFinishStatus(4);
        finish.setRollNoStatus(3);
        finish.setProductionResult(3);
        Fixture fixture = fixture(0L, finish);

        fixture.service.reopen("order-1", List.of("roll-1"), "tester", 7);

        ArgumentCaptor<LambdaUpdateWrapper<FinishRoll>> update = finishUpdateCaptor();
        verify(fixture.finishMapper).update(isNull(), update.capture());
        assertThat(update.getValue().getSqlSet()).contains(
                "roll_no_status", "production_result", "actual_weight",
                "remaining_weight", "production_adjustment_reason");
    }

    @Test
    void reopenKeepsVoidedActualAdditionOutOfInventory() {
        FinishRoll finish = producedFinish();
        finish.setFinishStatus(4);
        finish.setRollNoStatus(3);
        finish.setProductionResult(4);
        finish.setActualWeight(null);
        Fixture fixture = fixture(0L, finish);

        fixture.service.reopen("order-1", List.of("roll-1"), "tester", 7);

        verify(fixture.finishMapper, never()).update(isNull(), any());
        verify(fixture.rollMapper).update(isNull(), any());
    }

    @Test
    void reopenRejectsDisposedRollEvenWhenItIsMarkedChecked() {
        Fixture fixture = fixture(0L, producedFinish());
        fixture.roll.setDispositionAction(
                com.paper.mes.processorder.model.ProcessRollDispositionAction.DIRECT_SHIP);
        fixture.roll.setRollStatus(4);

        assertThrows(BusinessException.class,
                () -> fixture.service.reopen("order-1", List.of("roll-1"), "tester", 7));

        verify(fixture.finishMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(fixture.rollMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void reopenWithDisposedDirectShipRollRestoresOnlyActiveProcessFinishes() {
        OriginalRoll active = originalRoll("roll-1", 1, null, 2);
        OriginalRoll directShipSource = originalRoll("roll-2", 1,
                com.paper.mes.processorder.model.ProcessRollDispositionAction.DIRECT_SHIP, 4);
        FinishRoll linked = producedFinish();
        FinishRoll directShip = producedFinish();
        directShip.setUuid("finish-direct");
        directShip.setSourceType(2);
        FinishRoll legacy = producedFinish();
        legacy.setUuid("finish-legacy");
        FinishOriginalRel relation = relation("roll-1", "finish-1");
        Fixture fixture = fixture(List.of(active, directShipSource), List.of(relation),
                List.of(linked, directShip, legacy), List.of(linked, legacy), 0L);

        fixture.service.reopen("order-1", List.of("roll-1"), "tester", 7);

        verify(fixture.inventoryLedgerRecorder, times(2)).reverseReceipt(
                any(FinishRoll.class), eq("order-1"), eq("7"), any());
        verify(fixture.inventoryLedgerRecorder, never()).reverseReceipt(
                eq(directShip), eq("order-1"), eq("7"), any());
        ArgumentCaptor<LambdaQueryWrapper<FinishRoll>> queryCaptor = queryCaptor();
        verify(fixture.finishMapper, times(2)).selectList(queryCaptor.capture());
        assertThat(queryCaptor.getAllValues().get(0).getSqlSegment())
                .contains("source_type", "IS NULL", "<>");
    }

    private Fixture fixture(long deliveryCount, FinishRoll finish) {
        OriginalRoll roll = originalRoll("roll-1", 1, null, null);
        FinishOriginalRel relation = relation("roll-1", "finish-1");
        return fixture(List.of(roll), List.of(relation), List.of(finish), List.of(finish), deliveryCount);
    }

    private Fixture fixture(List<OriginalRoll> rolls, List<FinishOriginalRel> relations,
                            List<FinishRoll> allFinishes, List<FinishRoll> lockedFinishes,
                            long deliveryCount) {
        OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        FinishOriginalRelMapper relationMapper = mock(FinishOriginalRelMapper.class);
        DeliveryDetailMapper deliveryMapper = mock(DeliveryDetailMapper.class);
        BusinessLockService lockService = mock(BusinessLockService.class);
        InventoryLedgerBusinessRecorder inventoryLedgerRecorder = mock(InventoryLedgerBusinessRecorder.class);
        when(rollMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(rolls);
        when(relationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(finishMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(allFinishes, lockedFinishes);
        when(deliveryMapper.countBlockingDeliveryActivity(any())).thenReturn(deliveryCount);
        when(finishMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(rollMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        return new Fixture(new BackRecordReopenService(
                rollMapper, finishMapper, relationMapper, deliveryMapper, lockService, inventoryLedgerRecorder),
                rollMapper, finishMapper, deliveryMapper, inventoryLedgerRecorder, allFinishes.get(0), rolls.get(0));
    }

    private OriginalRoll originalRoll(String uuid, int checked,
                                      com.paper.mes.processorder.model.ProcessRollDispositionAction disposition,
                                      Integer status) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setOrderUuid("order-1");
        roll.setIsChecked(checked);
        roll.setDispositionAction(disposition);
        roll.setRollStatus(status);
        return roll;
    }

    private FinishOriginalRel relation(String originalUuid, String finishUuid) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setOrderUuid("order-1");
        relation.setOriginalUuid(originalUuid);
        relation.setFinishUuid(finishUuid);
        return relation;
    }

    private FinishRoll producedFinish() {
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setOrderUuid("order-1");
        finish.setFinishStatus(2);
        finish.setRollNoStatus(2);
        finish.setProductionResult(2);
        finish.setActualWeight(new BigDecimal("100.000"));
        finish.setVersion(1);
        return finish;
    }

    private static void initialize(MybatisConfiguration configuration, Class<?> type) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<LambdaUpdateWrapper<OriginalRoll>> rollUpdateCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<LambdaUpdateWrapper<FinishRoll>> finishUpdateCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<LambdaQueryWrapper<FinishRoll>> queryCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(LambdaQueryWrapper.class);
    }

    private record Fixture(BackRecordReopenService service, OriginalRollMapper rollMapper,
                           FinishRollMapper finishMapper, DeliveryDetailMapper deliveryMapper,
                           InventoryLedgerBusinessRecorder inventoryLedgerRecorder, FinishRoll finish,
                           OriginalRoll roll) {
    }
}
