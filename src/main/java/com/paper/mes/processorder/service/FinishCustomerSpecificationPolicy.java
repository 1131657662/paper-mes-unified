package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.entity.FinishRoll;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

public final class FinishCustomerSpecificationPolicy {

    private FinishCustomerSpecificationPolicy() {
    }

    public static void apply(FinishRoll finish, FinishConfigSpecDTO spec, String operator) {
        finish.setCustomerPaperName(trimToNull(spec.getCustomerPaperName()));
        finish.setCustomerGramWeight(spec.getCustomerGramWeight());
        finish.setCustomerFinishWidth(spec.getCustomerFinishWidth());
        requireOverrideReason(
                finish.getPaperName(), finish.getGramWeight(), finish.getFinishWidth(),
                finish.getCustomerPaperName(), finish.getCustomerGramWeight(),
                finish.getCustomerFinishWidth(), spec.getCustomerSpecOverrideReason());
        if (!isOverride(finish)) {
            return;
        }
        String reason = trimToNull(spec.getCustomerSpecOverrideReason());
        if (reason == null) {
            throw new BusinessException("客户销售规格与物理规格不同时必须填写改写原因");
        }
        finish.setCustomerSpecOverrideReason(reason);
        finish.setCustomerSpecOverrideBy(operator);
        finish.setCustomerSpecOverrideAt(LocalDateTime.now());
    }

    public static void requireOverrideReason(String physicalPaperName,
                                             Integer physicalGramWeight,
                                             Integer physicalWidth,
                                             String customerPaperName,
                                             Integer customerGramWeight,
                                             Integer customerWidth,
                                             String reason) {
        boolean override = differs(trimToNull(customerPaperName), physicalPaperName)
                || differs(customerGramWeight, physicalGramWeight)
                || differs(customerWidth, physicalWidth);
        if (override && trimToNull(reason) == null) {
            throw new BusinessException("客户销售规格与物理规格不同时必须填写改写原因");
        }
    }

    private static boolean isOverride(FinishRoll finish) {
        return differs(finish.getCustomerPaperName(), finish.getPaperName())
                || differs(finish.getCustomerGramWeight(), finish.getGramWeight())
                || differs(finish.getCustomerFinishWidth(), finish.getFinishWidth());
    }

    private static boolean differs(Object customerValue, Object physicalValue) {
        return customerValue != null && !Objects.equals(customerValue, physicalValue);
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
