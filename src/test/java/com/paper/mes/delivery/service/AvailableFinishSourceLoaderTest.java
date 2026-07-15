package com.paper.mes.delivery.service;

import com.paper.mes.delivery.dto.AvailableFinishVO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailableFinishSourceLoaderTest {

    @Mock private FinishOriginalRelMapper finishOriginalRelMapper;
    @Mock private OriginalRollMapper originalRollMapper;

    @Test
    void load_withMultipleSources_returnsSortedMotherRollsUsingBatchQueries() {
        AvailableFinishSourceLoader loader = loader();
        when(finishOriginalRelMapper.selectList(any())).thenReturn(List.of(
                relation("finish-1", "original-2", "20.00"),
                relation("finish-1", "original-1", "30.00")));
        when(originalRollMapper.selectBatchIds(any())).thenReturn(List.of(
                original("original-2", 2, "R002"),
                original("original-1", 1, "R001")));

        Map<String, List<AvailableFinishVO.SourceMotherRollVO>> result =
                loader.load(List.of(finish("finish-1"), finish("finish-2")));

        List<AvailableFinishVO.SourceMotherRollVO> sources = result.get("finish-1");
        assertEquals(List.of("original-1", "original-2"),
                sources.stream().map(AvailableFinishVO.SourceMotherRollVO::getOriginalUuid).toList());
        assertEquals(new BigDecimal("30.00"), sources.getFirst().getAllocationWeight());
        assertEquals(80, sources.getFirst().getGramWeight());
        verify(finishOriginalRelMapper).selectList(any());
        verify(originalRollMapper).selectBatchIds(any());
    }

    @Test
    void load_withoutFinishes_skipsDatabaseQueries() {
        AvailableFinishSourceLoader loader = loader();

        assertEquals(Map.of(), loader.load(List.of()));

        verify(finishOriginalRelMapper, never()).selectList(any());
        verify(originalRollMapper, never()).selectBatchIds(any());
    }

    private AvailableFinishSourceLoader loader() {
        return new AvailableFinishSourceLoader(finishOriginalRelMapper, originalRollMapper);
    }

    private FinishRoll finish(String uuid) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        return finish;
    }

    private FinishOriginalRel relation(String finishUuid, String originalUuid, String weight) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setFinishUuid(finishUuid);
        relation.setOriginalUuid(originalUuid);
        relation.setShareWeight(new BigDecimal(weight));
        return relation;
    }

    private OriginalRoll original(String uuid, int rowSort, String rollNo) {
        OriginalRoll original = new OriginalRoll();
        original.setUuid(uuid);
        original.setRowSort(rowSort);
        original.setRollNo(rollNo);
        original.setPaperName("测试纸");
        original.setActualGramWeight(80);
        original.setActualWidth(1045);
        original.setActualWeight(new BigDecimal("50.00"));
        return original;
    }
}
