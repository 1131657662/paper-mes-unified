package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessStepPricingBatchDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ProcessStepPricingBatchRequestResolver {

    private ProcessStepPricingBatchRequestResolver() {
    }

    static Map<String, Change> resolve(ProcessStepPricingBatchDTO dto) {
        Map<String, Change> result = new LinkedHashMap<>();
        Set<Integer> types = new HashSet<>();
        for (ProcessStepPricingBatchDTO.Group group : dto.getGroups()) {
            if (!types.add(group.getStepType())) {
                throw new BusinessException("同一工序类型只能出现一次");
            }
            List<Change> changes = changesForGroup(group);
            for (int index = 0; index < group.getStepUuids().size(); index++) {
                String uuid = group.getStepUuids().get(index);
                if (result.putIfAbsent(uuid, changes.get(index)) != null) {
                    throw new BusinessException("工序不能重复选择");
                }
            }
        }
        return result;
    }

    private static List<Change> changesForGroup(ProcessStepPricingBatchDTO.Group group) {
        if (group.getStepType() <= 2) {
            validateProductionGroup(group);
            BigDecimal price = Boolean.TRUE.equals(group.getRestoreStandard()) ? null
                    : price(group.getBillingUnitPrice());
            return repeated(group, new Change(group.getStepType(), null, null, price, null));
        }
        return serviceChanges(group);
    }

    private static void validateProductionGroup(ProcessStepPricingBatchDTO.Group group) {
        if (!Boolean.TRUE.equals(group.getRestoreStandard()) && group.getBillingUnitPrice() == null) {
            throw new BusinessException("核定单价不能为空");
        }
        if (group.getBillingMode() != null && group.getBillingMode() != ProcessStepPricingPolicy.STANDARD) {
            throw new BusinessException("切纸和复卷批量核定仅支持调整单价");
        }
    }

    private static List<Change> serviceChanges(ProcessStepPricingBatchDTO.Group group) {
        int mode = group.getBillingMode() == null ? ProcessStepPricingPolicy.STANDARD : group.getBillingMode();
        if (mode == ProcessStepPricingPolicy.QUANTITY_OVERRIDE) {
            throw new BusinessException("附加工艺数量由系统自动计算，不支持指定数量");
        }
        if (mode == ProcessStepPricingPolicy.STANDARD) {
            requireServiceBasis(group.getBillingBasis());
            if (!Boolean.TRUE.equals(group.getRestoreStandard()) && group.getBillingUnitPrice() == null) {
                throw new BusinessException("附加工艺核定单价不能为空");
            }
            BigDecimal price = Boolean.TRUE.equals(group.getRestoreStandard()) ? null
                    : price(group.getBillingUnitPrice());
            return repeated(group, new Change(group.getStepType(), mode, group.getBillingBasis(), price, null));
        }
        if (mode == ProcessStepPricingPolicy.FIXED_AMOUNT) {
            if (group.getBillingAmount() == null || group.getBillingAmount().signum() < 0) {
                throw new BusinessException("请填写不小于0的固定总额");
            }
            return fixedTotalChanges(group);
        }
        if (mode == ProcessStepPricingPolicy.FREE) {
            return repeated(group, new Change(group.getStepType(), mode, null, null, BigDecimal.ZERO));
        }
        throw new BusinessException("附加工艺计价模式不正确");
    }

    private static List<Change> fixedTotalChanges(ProcessStepPricingBatchDTO.Group group) {
        int count = group.getStepUuids().size();
        BigDecimal totalCents = group.getBillingAmount().setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2);
        BigDecimal[] divided = totalCents.divideAndRemainder(BigDecimal.valueOf(count));
        int remainder = divided[1].intValueExact();
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> new Change(group.getStepType(), ProcessStepPricingPolicy.FIXED_AMOUNT,
                        null, null, divided[0].add(index < remainder ? BigDecimal.ONE : BigDecimal.ZERO)
                        .movePointLeft(2).setScale(2)))
                .toList();
    }

    private static List<Change> repeated(ProcessStepPricingBatchDTO.Group group, Change change) {
        return java.util.Collections.nCopies(group.getStepUuids().size(), change);
    }

    private static void requireServiceBasis(String basis) {
        if (!"PIECE".equals(basis) && !"TON".equals(basis)) {
            throw new BusinessException("请选择按件或按吨计费");
        }
    }

    private static BigDecimal price(BigDecimal value) {
        if (value == null || value.signum() <= 0) throw new BusinessException("核定单价必须大于0");
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    record Change(int stepType, Integer billingMode, String billingBasis,
                  BigDecimal billingUnitPrice, BigDecimal billingAmount) {
    }
}
