package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackRecordOnSiteFinishRelationWriterTest {

    @Mock private FinishOriginalRelMapper relationMapper;

    @Test
    void updateWeights_multiSourceRelations_usesEachShareRatio() {
        FinishOriginalRel first = relation("60.00");
        FinishOriginalRel second = relation("40.00");
        when(relationMapper.updateById(any(FinishOriginalRel.class))).thenReturn(1);

        new BackRecordOnSiteFinishRelationWriter(relationMapper)
                .updateWeights(List.of(first, second), new BigDecimal("80.000"));

        assertThat(first.getShareWeight()).isEqualByComparingTo("48.000");
        assertThat(second.getShareWeight()).isEqualByComparingTo("32.000");
    }

    private FinishOriginalRel relation(String ratio) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setShareRatio(new BigDecimal(ratio));
        return relation;
    }
}
