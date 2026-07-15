package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordTrimDTO;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackRecordOnSiteTrimRecorderTest {

    @Mock private FinishRollMapper finishRollMapper;
    @Mock private FinishOriginalRelMapper relationMapper;
    @Mock private RollNoSequenceService rollNoSequenceService;

    private BackRecordOnSiteTrimRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new BackRecordOnSiteTrimRecorder(
                finishRollMapper, relationMapper, rollNoSequenceService);
    }

    @Test
    void record_validOnSiteTrim_createsRemainInventoryRowAndRelation() {
        ProcessOrder order = order();
        OriginalRoll source = source(2, 1200);
        BackRecordTrimDTO dto = trim(120, "20.000");
        when(rollNoSequenceService.nextFinishRollNo()).thenReturn("A000123");
        when(finishRollMapper.insert(any(FinishRoll.class))).thenAnswer(invocation -> {
            invocation.<FinishRoll>getArgument(0).setUuid("trim-1");
            return 1;
        });

        BackRecordOnSiteTrimRecorder.Result result = recorder.record(
                List.of(dto), context(order, source));

        FinishRoll finish = result.finishes().get(0);
        assertThat(finish.getFinishRollNo()).isEqualTo("A000123");
        assertThat(finish.getIsRemain()).isEqualTo(1);
        assertThat(finish.getFinishWidth()).isEqualTo(120);
        assertThat(finish.getActualWeight()).isEqualByComparingTo("20.000");
        assertThat(result.relations().get(0).getOriginalUuid()).isEqualTo("roll-1");
        verify(relationMapper).insert(any(FinishOriginalRel.class));
    }

    @Test
    void record_standardSource_rejectsDynamicTrim() {
        assertThatThrownBy(() -> recorder.record(
                List.of(trim(120, "20.000")), context(order(), source(1, 1200))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有现场定尺母卷");

        verify(finishRollMapper, never()).insert(any(FinishRoll.class));
    }

    @Test
    void record_trimWidthsExceedSource_rejectsWholeBatch() {
        assertThatThrownBy(() -> recorder.record(
                List.of(trim(700, "10.000"), trim(600, "10.000")),
                context(order(), source(2, 1200))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("切边宽度合计不能超过来源母卷门幅 1200mm");

        verify(finishRollMapper, never()).insert(any(FinishRoll.class));
    }

    private BackRecordOnSiteTrimRecorder.Context context(ProcessOrder order, OriginalRoll source) {
        return new BackRecordOnSiteTrimRecorder.Context(order, List.of(source), List.of());
    }

    private ProcessOrder order() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setWarehouseUuid("warehouse-1");
        return order;
    }

    private OriginalRoll source(int processMode, int width) {
        OriginalRoll source = new OriginalRoll();
        source.setUuid("roll-1");
        source.setRollNo("M001");
        source.setPaperName("测试纸");
        source.setGramWeight(80);
        source.setOriginalWidth(width);
        source.setProcessMode(processMode);
        return source;
    }

    private BackRecordTrimDTO trim(int width, String weight) {
        BackRecordTrimDTO dto = new BackRecordTrimDTO();
        dto.setOriginalUuid("roll-1");
        dto.setFinishWidth(width);
        dto.setActualWeight(new BigDecimal(weight));
        return dto;
    }
}
