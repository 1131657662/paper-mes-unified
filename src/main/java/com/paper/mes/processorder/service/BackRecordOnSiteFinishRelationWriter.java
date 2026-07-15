package com.paper.mes.processorder.service;

import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
class BackRecordOnSiteFinishRelationWriter {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private final FinishOriginalRelMapper relationMapper;

    FinishOriginalRel create(ProcessOrder order, OriginalRoll source,
                             FinishRoll finish, BigDecimal actualWeight) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setOrderUuid(order.getUuid());
        relation.setFinishUuid(finish.getUuid());
        relation.setOriginalUuid(source.getUuid());
        relation.setShareRatio(HUNDRED.setScale(2));
        relation.setShareWeight(actualWeight);
        relation.setRemark("现场定尺回录实际产出");
        relationMapper.insert(relation);
        return relation;
    }

    void updateWeights(List<FinishOriginalRel> relations, BigDecimal actualWeight) {
        for (FinishOriginalRel relation : relations) {
            BigDecimal ratio = relation.getShareRatio() == null ? HUNDRED : relation.getShareRatio();
            relation.setShareWeight(actualWeight.multiply(ratio)
                    .divide(HUNDRED, 3, RoundingMode.HALF_UP));
            ConcurrencyGuard.requireRowUpdated(relationMapper.updateById(relation));
        }
    }
}
