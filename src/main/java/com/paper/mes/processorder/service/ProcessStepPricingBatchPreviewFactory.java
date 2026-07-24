package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessStepPricingBatchPreviewVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class ProcessStepPricingBatchPreviewFactory {

    private ProcessStepPricingBatchPreviewFactory() {
    }

    static ProcessStepPricingBatchPreviewVO build(
            ProcessOrder order,
            List<ProcessStep> steps,
            Map<String, ProcessStepPricingBatchRequestResolver.Change> changes,
            Map<String, OriginalRoll> rolls) {
        List<ProcessStepPricingBatchPreviewVO.Row> rows = steps.stream()
                .map(step -> previewStep(step, changes.get(step.getUuid()), rolls))
                .toList();
        ProcessStepPricingBatchPreviewVO vo = new ProcessStepPricingBatchPreviewVO();
        vo.setOrderUuid(order.getUuid());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderVersion(order.getVersion());
        vo.setStepCount(rows.size());
        vo.setRows(rows);
        vo.setStandardAmount(sum(rows, ProcessStepPricingBatchPreviewVO.Row::getStandardAmount));
        vo.setCurrentAmount(sum(rows, ProcessStepPricingBatchPreviewVO.Row::getCurrentAmount));
        vo.setFinalAmount(sum(rows, ProcessStepPricingBatchPreviewVO.Row::getFinalAmount));
        vo.setAdjustmentAmount(vo.getFinalAmount().subtract(vo.getStandardAmount()).setScale(2));
        return vo;
    }

    private static ProcessStepPricingBatchPreviewVO.Row previewStep(
            ProcessStep step,
            ProcessStepPricingBatchRequestResolver.Change change,
            Map<String, OriginalRoll> rolls) {
        BigDecimal quantity = step.getBillingQuantity() == null
                ? step.getStandardQuantity() : step.getBillingQuantity();
        if (Integer.valueOf(ProcessStepPricingPolicy.STANDARD).equals(change.billingMode())) {
            quantity = ServiceStepQuantityResolver.resolve(change.billingBasis(), rolls.get(step.getOriginalUuid()));
        }
        Integer mode = change.billingMode() == null ? step.getBillingMode() : change.billingMode();
        BigDecimal amount = change.billingMode() == null ? step.getBillingAmount() : change.billingAmount();
        ProcessStepPricingBatchPreviewVO.Row row = ProcessStepPricingBatchCalculator.preview(
                step, change.billingUnitPrice(), mode, quantity, amount);
        row.setBillingBasis(change.billingBasis() == null ? step.getBillingBasis() : change.billingBasis());
        return row;
    }

    private static BigDecimal sum(List<ProcessStepPricingBatchPreviewVO.Row> rows,
                                  Function<ProcessStepPricingBatchPreviewVO.Row, BigDecimal> getter) {
        return rows.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }
}
