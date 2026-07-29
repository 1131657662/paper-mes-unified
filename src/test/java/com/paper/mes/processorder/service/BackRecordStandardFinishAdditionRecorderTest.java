package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackRecordStandardFinishAdditionRecorderTest {

    @Mock private FinishRollMapper finishRollMapper;
    @Mock private FinishRollSourceBinder sourceBinder;
    @Mock private RollNoSequenceService rollNoSequenceService;

    private BackRecordStandardFinishAdditionRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new BackRecordStandardFinishAdditionRecorder(finishRollMapper, sourceBinder, rollNoSequenceService);
    }

    @Test
    void record_addedStandardFinish_createsTraceableFinish() {
        OriginalRoll source = source();
        BackRecordFinishDTO dto = new BackRecordFinishDTO();
        dto.setProductionAction("ADDED");
        dto.setOriginalUuid(source.getUuid());
        dto.setFinishWidth(600);
        dto.setActualWeight(new BigDecimal("42.000"));
        dto.setProductionAdjustmentReason("现场实际多产出");
        when(rollNoSequenceService.nextFinishRollNo()).thenReturn("A000099");
        when(finishRollMapper.insert(any(FinishRoll.class))).thenReturn(1);

        var result = recorder.record(List.of(dto), new BackRecordStandardFinishAdditionRecorder.Context(
                "order-1", List.of(source), List.of()));

        FinishRoll created = result.finishes().get(0);
        assertThat(created.getFinishRollNo()).isEqualTo("A000099");
        assertThat(created.getOriginalRollNos()).isEqualTo("M001");
        assertThat(created.getFinishWidth()).isEqualTo(600);
        assertThat(created.getProductionResult()).isEqualTo(4);
        assertThat(created.getActualWeight()).isEqualByComparingTo("42.000");
        verify(sourceBinder).bind(any(FinishRollSourceBinder.BindRequest.class));
    }

    @Test
    void record_addedServiceOnlyFinish_rejectsNonStandardSource() {
        OriginalRoll source = source();
        source.setProcessMode(4);
        BackRecordFinishDTO dto = new BackRecordFinishDTO();
        dto.setProductionAction("ADDED");
        dto.setOriginalUuid(source.getUuid());
        dto.setFinishWidth(600);
        dto.setActualWeight(new BigDecimal("42.000"));
        dto.setProductionAdjustmentReason("实际多产出");

        assertThatThrownBy(() -> recorder.record(List.of(dto),
                new BackRecordStandardFinishAdditionRecorder.Context("order-1", List.of(source), List.of())))
                .isInstanceOf(BusinessException.class);
        verify(finishRollMapper, org.mockito.Mockito.never()).insert(any(FinishRoll.class));
    }

    private OriginalRoll source() {
        OriginalRoll source = new OriginalRoll();
        source.setUuid("roll-1");
        source.setRollNo("M001");
        source.setProcessMode(1);
        source.setOriginalWidth(2400);
        source.setPaperName("原纸");
        source.setGramWeight(120);
        return source;
    }
}
