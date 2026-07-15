package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackRecordOnSiteFinishRecorderTest {

    @Mock private FinishRollMapper finishRollMapper;
    @Mock private FinishOriginalRelMapper relationMapper;
    @Mock private RollNoSequenceService rollNoSequenceService;

    private BackRecordOnSiteFinishRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new BackRecordOnSiteFinishRecorder(
                finishRollMapper, new BackRecordOnSiteFinishRelationWriter(relationMapper), rollNoSequenceService);
    }

    @Test
    void record_newOnSiteFinish_createsInventoryRowAndSourceRelation() {
        when(rollNoSequenceService.nextFinishRollNo()).thenReturn("A000321");
        when(finishRollMapper.insert(any(FinishRoll.class))).thenAnswer(invocation -> {
            invocation.<FinishRoll>getArgument(0).setUuid("finish-new");
            return 1;
        });

        BackRecordOnSiteFinishRecorder.Result result = recorder.record(
                List.of(newFinish()), context(List.of(), List.of()));

        FinishRoll finish = result.finishes().getFirst();
        assertThat(finish.getFinishRollNo()).isEqualTo("A000321");
        assertThat(finish.getFinishWidth()).isEqualTo(900);
        assertThat(finish.getActualWeight()).isEqualByComparingTo("80.000");
        assertThat(result.relations().getFirst().getOriginalUuid()).isEqualTo("roll-1");
    }

    @Test
    void record_existingOnSiteFinish_updatesActualSpecification() {
        FinishRoll existing = finish("finish-old");
        FinishOriginalRel relation = relation(existing);
        when(finishRollMapper.updateById(existing)).thenReturn(1);
        when(relationMapper.updateById(relation)).thenReturn(1);
        BackRecordFinishDTO dto = newFinish();
        dto.setUuid(existing.getUuid());

        BackRecordOnSiteFinishRecorder.Result result = recorder.record(
                List.of(dto), context(List.of(existing), List.of(relation)));

        assertThat(existing.getFinishWidth()).isEqualTo(900);
        assertThat(relation.getShareWeight()).isEqualByComparingTo("80.000");
        assertThat(result.finishes()).isEmpty();
        verify(finishRollMapper).updateById(existing);
        verify(relationMapper).updateById(relation);
    }

    @Test
    void record_omittedLegacyPlaceholder_voidsUnusedRollNo() {
        FinishRoll existing = finish("finish-old");
        when(finishRollMapper.updateById(existing)).thenReturn(1);

        recorder.record(List.of(), context(List.of(existing), List.of(relation(existing))));

        assertThat(existing.getRollNoStatus()).isEqualTo(3);
        assertThat(existing.getActualRemark()).contains("自动作废");
    }

    @Test
    void record_existingOnSiteFinishWithChangedSource_rejectsSubmission() {
        FinishRoll existing = finish("finish-old");
        BackRecordFinishDTO dto = newFinish();
        dto.setUuid(existing.getUuid());
        dto.setOriginalUuid("roll-2");

        assertThatThrownBy(() -> recorder.record(
                List.of(dto), context(List.of(existing), List.of(relation(existing)))))
                .hasMessageContaining("来源与原记录不一致");
    }

    @Test
    void record_standardFinishWithForgedOnSiteSource_ignoresOnSiteRecorder() {
        FinishRoll existing = finish("finish-standard");
        BackRecordFinishDTO dto = newFinish();
        dto.setUuid(existing.getUuid());
        FinishOriginalRel standardRelation = relation(existing);
        standardRelation.setOriginalUuid("roll-standard");

        BackRecordOnSiteFinishRecorder.Result result = recorder.record(
                List.of(dto), context(List.of(existing), List.of(standardRelation)));

        assertThat(result.managedExistingUuids()).isEmpty();
    }

    @Test
    void record_newFinishWithoutSource_rejectsSubmission() {
        BackRecordFinishDTO dto = newFinish();
        dto.setOriginalUuid(null);

        assertThatThrownBy(() -> recorder.record(
                List.of(dto), context(List.of(), List.of())))
                .hasMessageContaining("必须选择来源母卷");
    }

    @Test
    void record_newFinishWithStandardSource_rejectsSubmission() {
        BackRecordFinishDTO dto = newFinish();
        dto.setOriginalUuid("roll-standard");

        assertThatThrownBy(() -> recorder.record(
                List.of(dto), context(List.of(), List.of())))
                .hasMessageContaining("必须选择来源母卷");
    }

    @Test
    void record_duplicateExistingFinish_rejectsSubmission() {
        FinishRoll existing = finish("finish-old");
        BackRecordFinishDTO first = newFinish();
        first.setUuid(existing.getUuid());
        BackRecordFinishDTO duplicate = newFinish();
        duplicate.setUuid(existing.getUuid());

        assertThatThrownBy(() -> recorder.record(
                List.of(first, duplicate), context(List.of(existing), List.of(relation(existing)))))
                .hasMessageContaining("回录明细重复");
    }

    private BackRecordOnSiteFinishRecorder.Context context(
            List<FinishRoll> finishes, List<FinishOriginalRel> relations) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setWarehouseUuid("warehouse-1");
        return new BackRecordOnSiteFinishRecorder.Context(
                order, List.of(source()), finishes, relations);
    }

    private BackRecordFinishDTO newFinish() {
        BackRecordFinishDTO dto = new BackRecordFinishDTO();
        dto.setOriginalUuid("roll-1");
        dto.setFinishWidth(900);
        dto.setFinishDiameter(40);
        dto.setFinishCoreDiameter(3);
        dto.setActualWeight(new BigDecimal("80.000"));
        return dto;
    }

    private OriginalRoll source() {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setRollNo("M001");
        roll.setProcessMode(2);
        roll.setPaperName("牛卡纸");
        roll.setGramWeight(120);
        roll.setOriginalWidth(1200);
        return roll;
    }

    private FinishRoll finish(String uuid) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setOrderUuid("order-1");
        finish.setFinishRollNo("A000001");
        finish.setRollNoStatus(1);
        finish.setSourceType(1);
        finish.setIsSpare(0);
        return finish;
    }

    private FinishOriginalRel relation(FinishRoll finish) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid(finish.getUuid());
        relation.setOriginalUuid("roll-1");
        return relation;
    }
}
