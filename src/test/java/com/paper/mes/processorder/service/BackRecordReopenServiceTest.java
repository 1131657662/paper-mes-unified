package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

        int reopened = fixture.service.reopen("order-1", List.of("roll-1"), "tester");

        assertThat(reopened).isEqualTo(1);
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
                () -> fixture.service.reopen("order-1", List.of("roll-1"), "tester"));

        verify(fixture.finishMapper, never()).update(isNull(), any());
        verify(fixture.rollMapper, never()).update(isNull(), any());
    }

    @Test
    void reopenChecksBlockingDeliveryActivityInOneBatch() {
        Fixture fixture = fixture(0L, producedFinish());

        fixture.service.reopen("order-1", List.of("roll-1"), "tester");

        verify(fixture.deliveryMapper).countBlockingDeliveryActivity(List.of("finish-1"));
    }

    @Test
    void reopenRestoresAPlannedFinishThatWasMarkedNotProduced() {
        FinishRoll finish = producedFinish();
        finish.setFinishStatus(4);
        finish.setRollNoStatus(3);
        finish.setProductionResult(3);
        Fixture fixture = fixture(0L, finish);

        fixture.service.reopen("order-1", List.of("roll-1"), "tester");

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

        fixture.service.reopen("order-1", List.of("roll-1"), "tester");

        verify(fixture.finishMapper, never()).update(isNull(), any());
        verify(fixture.rollMapper).update(isNull(), any());
    }

    private Fixture fixture(long deliveryCount, FinishRoll finish) {
        OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        FinishOriginalRelMapper relationMapper = mock(FinishOriginalRelMapper.class);
        DeliveryDetailMapper deliveryMapper = mock(DeliveryDetailMapper.class);
        BusinessLockService lockService = mock(BusinessLockService.class);
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setOrderUuid("order-1");
        roll.setIsChecked(1);
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setOrderUuid("order-1");
        relation.setOriginalUuid("roll-1");
        relation.setFinishUuid("finish-1");
        when(rollMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(roll));
        when(relationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(relation));
        when(finishMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(finish));
        when(deliveryMapper.countBlockingDeliveryActivity(any())).thenReturn(deliveryCount);
        when(finishMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(rollMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        return new Fixture(new BackRecordReopenService(
                rollMapper, finishMapper, relationMapper, deliveryMapper, lockService),
                rollMapper, finishMapper, deliveryMapper);
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

    private record Fixture(BackRecordReopenService service, OriginalRollMapper rollMapper,
                           FinishRollMapper finishMapper, DeliveryDetailMapper deliveryMapper) {
    }
}
