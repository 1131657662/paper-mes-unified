package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackRecordDirectShipRecorderTest {

    @Mock private FinishRollMapper finishMapper;
    @Mock private FinishOriginalRelMapper relationMapper;
    @Mock private RollNoSequenceService sequenceService;

    private BackRecordDirectShipRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new BackRecordDirectShipRecorder(finishMapper, relationMapper, sequenceService);
    }

    @Test
    void record_legacyNumberPresent_preservesNumberAndAddsUuidRelation() {
        OriginalRoll source = source("roll-1", "M001", "90.000");
        FinishRoll finish = finish("finish-1", "M001", 2);
        when(finishMapper.selectList(any())).thenReturn(List.of(finish));
        when(relationMapper.selectList(any())).thenReturn(List.of());
        when(finishMapper.updateById(finish)).thenReturn(1);

        BackRecordDirectShipRecorder.Result result = recorder.record(
                order(), List.of(source), List.of(source));

        assertThat(result.generated()).isZero();
        assertThat(finish.getFinishRollNo()).isEqualTo("M001");
        assertThat(finish.getActualWeight()).isEqualByComparingTo("90.000");
        verify(sequenceService, never()).nextFinishRollNo();
        ArgumentCaptor<FinishOriginalRel> captor = ArgumentCaptor.forClass(FinishOriginalRel.class);
        verify(relationMapper).insert(captor.capture());
        assertThat(captor.getValue().getOriginalUuid()).isEqualTo("roll-1");
        assertThat(captor.getValue().getFinishUuid()).isEqualTo("finish-1");
    }

    @Test
    void record_existingUuidRelation_reusesFinishAndRefreshesWeight() {
        OriginalRoll source = source("roll-1", null, "75.000");
        FinishRoll finish = finish("finish-1", "A000123", 2);
        FinishOriginalRel relation = relation(source, finish);
        when(finishMapper.selectList(any())).thenReturn(List.of(finish));
        when(relationMapper.selectList(any())).thenReturn(List.of(relation));
        when(finishMapper.updateById(finish)).thenReturn(1);
        when(relationMapper.updateById(relation)).thenReturn(1);

        BackRecordDirectShipRecorder.Result result = recorder.record(
                order(), List.of(source), List.of(source));

        assertThat(result.generated()).isZero();
        assertThat(result.finishes()).containsExactly(finish);
        assertThat(finish.getFinishRollNo()).isEqualTo("A000123");
        assertThat(relation.getShareWeight()).isEqualByComparingTo("75.000");
    }

    @Test
    void record_onlyVoidedLinkedFinish_createsNewBusinessNumber() {
        OriginalRoll source = source("roll-1", "M001", "80.000");
        FinishRoll voided = finish("finish-old", "A000111", 3);
        FinishOriginalRel oldRelation = relation(source, voided);
        when(finishMapper.selectList(any())).thenReturn(List.of(voided));
        when(relationMapper.selectList(any())).thenReturn(List.of(oldRelation));
        when(sequenceService.nextFinishRollNo()).thenReturn("A000222");
        when(finishMapper.insert(any(FinishRoll.class))).thenAnswer(invocation -> {
            invocation.<FinishRoll>getArgument(0).setUuid("finish-new");
            return 1;
        });

        BackRecordDirectShipRecorder.Result result = recorder.record(
                order(), List.of(source), List.of(source));

        assertThat(result.generated()).isEqualTo(1);
        assertThat(result.finishes().getFirst().getFinishRollNo()).isEqualTo("A000222");
        verify(finishMapper, never()).updateById(voided);
    }

    @Test
    void record_threePhysicalPieces_createsThreeFinishesAndAbsorbsWeightRemainder() {
        OriginalRoll source = source("roll-1", "M001", "100.000");
        source.setPieceNum(3);
        when(finishMapper.selectList(any())).thenReturn(List.of());
        when(relationMapper.selectList(any())).thenReturn(List.of());
        when(sequenceService.nextFinishRollNo()).thenReturn("A000001", "A000002", "A000003");
        AtomicInteger sequence = new AtomicInteger();
        when(finishMapper.insert(any(FinishRoll.class))).thenAnswer(invocation -> {
            invocation.<FinishRoll>getArgument(0).setUuid("finish-" + sequence.incrementAndGet());
            return 1;
        });

        BackRecordDirectShipRecorder.Result result = recorder.record(
                order(), List.of(source), List.of(source));

        assertThat(result.generated()).isEqualTo(3);
        assertThat(result.finishes()).extracting(FinishRoll::getActualWeight)
                .containsExactly(new BigDecimal("33.333"), new BigDecimal("33.333"),
                        new BigDecimal("33.334"));
        ArgumentCaptor<FinishOriginalRel> relationCaptor = ArgumentCaptor.forClass(FinishOriginalRel.class);
        verify(relationMapper, times(3)).insert(relationCaptor.capture());
        assertThat(relationCaptor.getAllValues()).extracting(FinishOriginalRel::getShareWeight)
                .containsExactly(new BigDecimal("33.333"), new BigDecimal("33.333"),
                        new BigDecimal("33.334"));
    }

    private ProcessOrder order() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setWarehouseUuid("warehouse-1");
        return order;
    }

    private OriginalRoll source(String uuid, String rollNo, String weight) {
        OriginalRoll source = new OriginalRoll();
        source.setUuid(uuid);
        source.setOrderUuid("order-1");
        source.setRollNo(rollNo);
        source.setRowSort(1);
        source.setProcessMode(3);
        source.setPaperName("test-paper");
        source.setGramWeight(80);
        source.setOriginalWidth(1200);
        source.setActualWeight(new BigDecimal(weight));
        source.setPieceNum(1);
        return source;
    }

    private FinishRoll finish(String uuid, String rollNo, int status) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setOrderUuid("order-1");
        finish.setFinishRollNo(rollNo);
        finish.setRowSort(1);
        finish.setSourceType(2);
        finish.setRollNoStatus(status);
        return finish;
    }

    private FinishOriginalRel relation(OriginalRoll source, FinishRoll finish) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setUuid("relation-" + finish.getUuid());
        relation.setOrderUuid("order-1");
        relation.setOriginalUuid(source.getUuid());
        relation.setFinishUuid(finish.getUuid());
        return relation;
    }
}
