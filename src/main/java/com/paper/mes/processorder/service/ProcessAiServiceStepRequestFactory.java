package com.paper.mes.processorder.service;

import com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.springframework.stereotype.Component;

/** Converts a confirmed AI service intent to the same request shape used by manual service editing. */
@Component
class ProcessAiServiceStepRequestFactory {

    ProcessStepDTO create(ProcessAiPackagingCandidate candidate, OriginalRoll roll) {
        ProcessStepDTO request = new ProcessStepDTO();
        request.setOriginalUuid(roll.getUuid());
        request.setStepType(candidate.stepType());
        request.setStepName(candidate.stepName());
        request.setIsMain(0);
        request.setBillingBasis(candidate.billingBasis());
        request.setBillingMode(candidate.billingMode());
        request.setUnitPrice(candidate.unitPrice());
        request.setBillingAmount(candidate.billingAmount());
        request.setRemark(candidate.remark());
        if (candidate.billingMode() == ProcessStepPricingPolicy.QUANTITY_OVERRIDE) {
            request.setServiceQuantity(ServiceStepQuantityResolver.resolve(candidate.billingBasis(), roll));
        }
        return request;
    }
}
