package com.paper.mes.processorder.service;

import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.processorder.dto.FeeResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessStepPricingApprovalPolicyTest {

    @Test
    void requireForOrder_aggregatesSmallDiscountsBeforeCheckingApproval() {
        ProcessStepPricingSettings settings = mock(ProcessStepPricingSettings.class);
        PermissionChecker checker = mock(PermissionChecker.class);
        when(settings.autoApproveLimit()).thenReturn(new BigDecimal("100"));
        FeeResultVO result = resultWithAdjustments("-60", "-60");

        new ProcessStepPricingApprovalPolicy(settings, checker).requireForOrder(result);

        verify(checker).require(Permissions.ORDER_PRICING_APPROVE);
    }

    @Test
    void requireForOrder_belowLimit_doesNotRequireApproval() {
        ProcessStepPricingSettings settings = mock(ProcessStepPricingSettings.class);
        PermissionChecker checker = mock(PermissionChecker.class);
        when(settings.autoApproveLimit()).thenReturn(new BigDecimal("100"));

        new ProcessStepPricingApprovalPolicy(settings, checker).requireForOrder(resultWithAdjustments("-40", "-50"));

        verify(checker, never()).require(Permissions.ORDER_PRICING_APPROVE);
    }

    private FeeResultVO resultWithAdjustments(String... amounts) {
        FeeResultVO result = new FeeResultVO();
        result.setStepFees(java.util.Arrays.stream(amounts).map(amount -> {
            FeeResultVO.StepFee fee = new FeeResultVO.StepFee();
            fee.setPricingAdjustmentAmount(new BigDecimal(amount));
            return fee;
        }).toList());
        return result;
    }
}
