package com.paper.mes.processorder.service;

import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.ResultCode;
import com.paper.mes.processorder.dto.FeeResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ProcessStepPricingApprovalPolicy {

    private final ProcessStepPricingSettings settings;
    private final PermissionChecker permissionChecker;

    public void requireForOrder(FeeResultVO result) {
        BigDecimal discount = result.getStepFees() == null ? BigDecimal.ZERO
                : result.getStepFees().stream()
                .map(FeeResultVO.StepFee::getPricingAdjustmentAmount)
                .filter(amount -> amount != null && amount.signum() < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (discount.compareTo(settings.autoApproveLimit()) <= 0) return;
        try {
            permissionChecker.require(Permissions.ORDER_PRICING_APPROVE);
        } catch (BusinessException exception) {
            if (exception.getCode() != ResultCode.FORBIDDEN) throw exception;
            throw new BusinessException(ResultCode.FORBIDDEN, ErrorCode.E009.getCode(),
                    ErrorCode.E009.getDefaultMessage());
        }
    }
}
