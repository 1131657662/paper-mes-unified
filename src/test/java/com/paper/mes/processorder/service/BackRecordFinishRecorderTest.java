package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackRecordFinishRecorderTest {

    @Mock private FinishRollMapper finishRollMapper;
    @Mock private FinishRollSourceBinder sourceBinder;

    private BackRecordFinishRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new BackRecordFinishRecorder(finishRollMapper, sourceBinder);
    }

    @Test
    void record_onSiteFinishWithValidWidth_persistsActualSpecification() {
        FinishRoll finish = finish(false, 0);
        OriginalRoll source = source(2, 1200);
        BackRecordFinishDTO dto = dto(1000, "88.500");
        when(finishRollMapper.updateById(finish)).thenReturn(1);

        recorder.record(List.of(dto), context(finish, source, true));

        assertThat(finish.getFinishWidth()).isEqualTo(1000);
        assertThat(finish.getActualWeight()).isEqualByComparingTo("88.500");
        assertThat(finish.getRemainingWeight()).isEqualByComparingTo("88.500");
    }

    @Test
    void record_onSiteFinishWithoutWidth_rejectsSubmission() {
        FinishRoll finish = finish(false, 0);

        assertThatThrownBy(() -> recorder.record(
                List.of(dto(null, "88.500")), context(finish, source(2, 1200), true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("现场定尺成品门幅必须大于0");
        verify(finishRollMapper, never()).updateById(finish);
    }

    @Test
    void record_onSiteFinishWiderThanSource_rejectsSubmission() {
        FinishRoll finish = finish(false, 0);

        assertThatThrownBy(() -> recorder.record(
                List.of(dto(1300, "88.500")), context(finish, source(2, 1200), true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超过来源母卷门幅 1200mm");
    }

    @Test
    void record_standardFinishWithChangedWidth_rejectsSubmission() {
        FinishRoll finish = finish(false, 1000);

        assertThatThrownBy(() -> recorder.record(
                List.of(dto(900, "88.500")), context(finish, source(1, 1200), true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标准加工成品门幅不可在回录中修改");
    }

    @Test
    void record_standardFinishMarkedNotProduced_voidsPlannedRollWithoutWeight() {
        FinishRoll finish = finish(false, 1000);
        BackRecordFinishDTO dto = dto(null, null);
        dto.setProductionAction("NOT_PRODUCED");
        dto.setProductionAdjustmentReason("现场实际未产出");
        when(finishRollMapper.update(isNull(), any())).thenReturn(1);

        recorder.record(List.of(dto), context(finish, source(1, 1200), true));

        assertThat(finish.getRollNoStatus()).isEqualTo(3);
        assertThat(finish.getFinishStatus()).isEqualTo(4);
        assertThat(finish.getProductionResult()).isEqualTo(3);
        assertThat(finish.getActualWeight()).isNull();
        verify(finishRollMapper).update(isNull(), any());
    }

    @Test
    void record_unusedSpareWithoutSpecification_leavesItUnchanged() {
        FinishRoll spare = finish(true, 0);

        recorder.record(List.of(dto(null, null)), context(spare, source(2, 1200), true));

        verify(finishRollMapper, never()).updateById(spare);
    }

    @Test
    void record_usedSpareWithoutSource_rejectsSubmission() {
        FinishRoll spare = finish(true, 0);

        assertThatThrownBy(() -> recorder.record(
                List.of(dto(800, "50.000")), context(spare, source(2, 1200), false)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("缺少来源母卷");
    }

    @Test
    void record_usedSpare_marksItAsActualAddedOutput() {
        FinishRoll spare = finish(true, 0);
        when(finishRollMapper.updateById(spare)).thenReturn(1);

        recorder.record(List.of(dto(800, "50.000")), context(spare, source(2, 1200), true));

        assertThat(spare.getProductionResult()).isEqualTo(4);
        assertThat(spare.getProductionAdjustmentReason()).isEqualTo("备用卷号实际启用");
    }

    @Test
    void record_existingAddedFinish_preservesAdjustmentClassification() {
        FinishRoll added = finish(false, 800);
        added.setProductionResult(4);
        added.setProductionAdjustmentReason("现场实际多产出");
        when(finishRollMapper.updateById(added)).thenReturn(1);

        recorder.record(List.of(dto(800, "55.000")), context(added, source(1, 1200), true));

        assertThat(added.getProductionResult()).isEqualTo(4);
        assertThat(added.getProductionAdjustmentReason()).isEqualTo("现场实际多产出");
    }

    @Test
    void record_existingAddedFinishMarkedNotProduced_keepsAddedClassification() {
        FinishRoll added = finish(false, 800);
        added.setProductionResult(4);
        BackRecordFinishDTO dto = dto(null, null);
        dto.setProductionAction("NOT_PRODUCED");
        dto.setProductionAdjustmentReason("复核后删除临时新增产出");
        when(finishRollMapper.update(isNull(), any())).thenReturn(1);

        recorder.record(List.of(dto), context(added, source(1, 1200), true));

        assertThat(added.getProductionResult()).isEqualTo(4);
        assertThat(added.getRollNoStatus()).isEqualTo(3);
        assertThat(added.getActualWeight()).isNull();
    }

    @Test
    void record_unlinkedOnSiteFinishWithSelectedSource_bindsAndPersists() {
        FinishRoll finish = finish(false, 0);
        finish.setOrderUuid("order-1");
        BackRecordFinishDTO dto = dto(800, "50.000");
        dto.setOriginalUuid("roll-1");
        when(finishRollMapper.updateById(finish)).thenReturn(1);

        recorder.record(List.of(dto), context(finish, source(2, 1200), false));

        verify(sourceBinder).bind(any(FinishRollSourceBinder.BindRequest.class));
        assertThat(finish.getFinishWidth()).isEqualTo(800);
    }

    @Test
    void record_scrappedFinishWithoutDto_skipsBackRecord() {
        FinishRoll finish = finish(false, 1000);
        finish.setFinishStatus(4);

        recorder.record(List.of(), context(finish, source(1, 1200), true));

        verify(finishRollMapper, never()).updateById(finish);
    }

    @Test
    void record_scrappedFinishWithDto_rejectsInvalidSubmission() {
        FinishRoll finish = finish(false, 1000);
        finish.setFinishStatus(4);

        assertThatThrownBy(() -> recorder.record(
                List.of(dto(1000, "88.500")), context(finish, source(1, 1200), true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无效记录");
        verify(finishRollMapper, never()).updateById(finish);
    }

    private BackRecordFinishRecorder.Context context(FinishRoll finish, OriginalRoll source,
                                                      boolean withRelation) {
        List<FinishOriginalRel> relations = withRelation ? List.of(relation(finish, source)) : List.of();
        return new BackRecordFinishRecorder.Context(List.of(finish), List.of(source), relations);
    }

    private FinishRoll finish(boolean spare, int width) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setFinishRollNo("A000001");
        finish.setRollNoStatus(1);
        finish.setSourceType(1);
        finish.setIsSpare(spare ? 1 : 0);
        finish.setFinishWidth(width);
        return finish;
    }

    private OriginalRoll source(int processMode, int width) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setProcessMode(processMode);
        roll.setOriginalWidth(width);
        return roll;
    }

    private FinishOriginalRel relation(FinishRoll finish, OriginalRoll source) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid(finish.getUuid());
        relation.setOriginalUuid(source.getUuid());
        return relation;
    }

    private BackRecordFinishDTO dto(Integer width, String weight) {
        BackRecordFinishDTO dto = new BackRecordFinishDTO();
        dto.setUuid("finish-1");
        dto.setFinishWidth(width);
        dto.setActualWeight(weight == null ? null : new BigDecimal(weight));
        dto.setIsRemain(0);
        dto.setIsAbnormal(0);
        return dto;
    }
}
