package com.paper.mes.remain.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.remain.entity.RemainRegistrationLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

final class RemainApplicationAllocation {

    private RemainApplicationAllocation() {
    }

    static AllocationResult allocate(List<RemainRegistrationLine> lines,
                                     BigDecimal targetAmount, BigDecimal targetWeight) {
        BigDecimal amountTotal = lines.stream().map(RemainApplicationAllocation::remainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weightTotal = lines.stream().map(RemainApplicationAllocation::remainingWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amountTotal.signum() <= 0 || weightTotal.signum() <= 0) {
            throw new BusinessException("登记单没有可分配的金额或重量");
        }
        BigDecimal allocatedAmount = BigDecimal.ZERO;
        BigDecimal allocatedWeight = BigDecimal.ZERO;
        List<LineAllocation> result = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            RemainRegistrationLine line = lines.get(index);
            BigDecimal lineAmount = remainingAmount(line);
            BigDecimal lineWeight = remainingWeight(line);
            if (lineAmount.signum() == 0 && lineWeight.signum() == 0) {
                continue;
            }
            boolean last = index == lastEligibleIndex(lines);
            BigDecimal amount = last ? targetAmount.subtract(allocatedAmount)
                    : targetAmount.multiply(lineAmount).divide(amountTotal, 0, RoundingMode.HALF_UP);
            BigDecimal weight = last ? targetWeight.subtract(allocatedWeight)
                    : targetWeight.multiply(lineWeight).divide(weightTotal, 3, RoundingMode.HALF_UP);
            line.setAppliedAmount(nz(line.getAppliedAmount()).add(amount));
            line.setAppliedWeight(nz(line.getAppliedWeight()).add(weight));
            allocatedAmount = allocatedAmount.add(amount);
            allocatedWeight = allocatedWeight.add(weight);
            result.add(new LineAllocation(line, amount, weight));
        }
        return new AllocationResult(result, allocatedAmount, allocatedWeight);
    }

    static BigDecimal remainingAmount(RemainRegistrationLine line) {
        return nz(line.getAmount()).subtract(nz(line.getAppliedAmount())).max(BigDecimal.ZERO);
    }

    static BigDecimal remainingWeight(RemainRegistrationLine line) {
        return nz(line.getTransferredSystemWeight())
                .subtract(nz(line.getRolledBackSystemWeight()))
                .subtract(nz(line.getProcessedSystemWeight()))
                .subtract(nz(line.getAppliedWeight())).max(BigDecimal.ZERO);
    }

    private static int lastEligibleIndex(List<RemainRegistrationLine> lines) {
        for (int index = lines.size() - 1; index >= 0; index--) {
            if (remainingAmount(lines.get(index)).signum() > 0 || remainingWeight(lines.get(index)).signum() > 0) {
                return index;
            }
        }
        return -1;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    record LineAllocation(RemainRegistrationLine line, BigDecimal amount, BigDecimal weight) {
    }

    record AllocationResult(List<LineAllocation> lines, BigDecimal amount, BigDecimal weight) {
    }
}
