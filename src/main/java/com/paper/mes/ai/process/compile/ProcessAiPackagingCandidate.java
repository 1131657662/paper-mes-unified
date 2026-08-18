package com.paper.mes.ai.process.compile;

import java.math.BigDecimal;
import java.util.List;

public record ProcessAiPackagingCandidate(
        String ownerRollRef,
        String originalUuid,
        List<String> coveredOriginalUuids,
        int stepType,
        String packagingType,
        String stepName,
        String billingBasis,
        BigDecimal serviceQuantity,
        int billingMode,
        BigDecimal unitPrice,
        BigDecimal billingAmount,
        String remark) {

    public ProcessAiPackagingCandidate {
        coveredOriginalUuids = List.copyOf(coveredOriginalUuids);
    }
}
