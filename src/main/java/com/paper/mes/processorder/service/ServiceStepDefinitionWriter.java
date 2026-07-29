package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;

import java.time.LocalDateTime;

/** Persists editable service-step fields, including deliberate null values. */
public final class ServiceStepDefinitionWriter {

    private ServiceStepDefinitionWriter() {
    }

    public static void update(ProcessStepMapper mapper, ProcessStep step) {
        LocalDateTime now = LocalDateTime.now();
        String operator = currentOperator();
        LambdaUpdateWrapper<ProcessStep> update = definitionUpdate(step)
                .set(ProcessStep::getUpdateBy, operator)
                .set(ProcessStep::getUpdateTime, now)
                .setSql("version = version + 1");
        ConcurrencyGuard.requireRowUpdated(mapper.update(null, update));
    }

    private static String currentOperator() {
        String operator = AuthContextHolder.currentDisplayName();
        return operator == null || operator.isBlank() ? "system" : operator;
    }

    private static LambdaUpdateWrapper<ProcessStep> definitionUpdate(ProcessStep step) {
        return new LambdaUpdateWrapper<ProcessStep>()
                .eq(ProcessStep::getUuid, step.getUuid())
                .eq(ProcessStep::getVersion, step.getVersion())
                .eq(ProcessStep::getIsDeleted, 0)
                .set(ProcessStep::getStepType, step.getStepType())
                .set(ProcessStep::getStepName, step.getStepName())
                .set(ProcessStep::getMachineUuid, step.getMachineUuid())
                .set(ProcessStep::getMachineNameSnap, step.getMachineNameSnap())
                .set(ProcessStep::getBillingMode, step.getBillingMode())
                .set(ProcessStep::getBillingBasis, step.getBillingBasis())
                .set(ProcessStep::getServiceQuantity, step.getServiceQuantity())
                .set(ProcessStep::getUnitPrice, step.getUnitPrice())
                .set(ProcessStep::getBillingUnitPrice, step.getBillingUnitPrice())
                .set(ProcessStep::getBillingQuantity, step.getBillingQuantity())
                .set(ProcessStep::getBillingAmount, step.getBillingAmount())
                .set(ProcessStep::getPricingAdjustmentReason, step.getPricingAdjustmentReason())
                .set(ProcessStep::getPricingAdjustedBy, step.getPricingAdjustedBy())
                .set(ProcessStep::getPricingAdjustedAt, step.getPricingAdjustedAt())
                .set(ProcessStep::getPricingAdjustmentBatchId, step.getPricingAdjustmentBatchId())
                .set(ProcessStep::getRemark, step.getRemark());
    }
}
