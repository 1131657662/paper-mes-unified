package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.entity.ProcessStep;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

/** Normalizes editable service pricing before either single or batch persistence. */
public final class DraftServiceStepPricingNormalizer {

    private DraftServiceStepPricingNormalizer() {
    }

    public static void apply(ProcessStep step, ProcessStepDTO request) {
        int mode = request.getBillingMode() == null
                ? ProcessStepPricingPolicy.STANDARD : request.getBillingMode();
        boolean quantityPricing = mode == ProcessStepPricingPolicy.STANDARD
                || mode == ProcessStepPricingPolicy.QUANTITY_OVERRIDE;
        step.setBillingMode(mode);
        step.setBillingBasis(quantityPricing && StringUtils.hasText(request.getBillingBasis())
                ? request.getBillingBasis().trim().toUpperCase(Locale.ROOT) : null);
        step.setUnitPrice(quantityPricing ? request.getUnitPrice() : null);
        step.setBillingAmount(resolveAmount(mode, request));
        clearAdjustment(step);
        if (mode == ProcessStepPricingPolicy.QUANTITY_OVERRIDE) {
            step.setBillingQuantity(request.getServiceQuantity());
        }
    }

    private static BigDecimal resolveAmount(int mode, ProcessStepDTO request) {
        if (mode == ProcessStepPricingPolicy.FIXED_AMOUNT) return request.getBillingAmount();
        if (mode == ProcessStepPricingPolicy.FREE) return BigDecimal.ZERO;
        return null;
    }

    private static void clearAdjustment(ProcessStep step) {
        step.setBillingUnitPrice(null);
        step.setBillingQuantity(null);
        step.setPricingAdjustmentReason(null);
        step.setPricingAdjustedBy(null);
        step.setPricingAdjustedAt(null);
        step.setPricingAdjustmentBatchId(null);
    }
}
