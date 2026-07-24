package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.dto.ProcessStepBatchResultVO;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServiceStepBatchUpsertWriter {

    private static final int STANDARD = ProcessStepPricingPolicy.STANDARD;
    private static final int FIXED_AMOUNT = ProcessStepPricingPolicy.FIXED_AMOUNT;
    private static final int FREE = ProcessStepPricingPolicy.FREE;

    private final ProcessStepMapper stepMapper;
    private final MachineMapper machineMapper;
    private final ProcessCatalogStepValidator catalogValidator;

    public ProcessStepBatchResultVO upsert(String orderUuid, List<ProcessStepDTO> requests,
                                           Map<String, OriginalRoll> rolls) {
        requireUniqueTargets(requests);
        List<ProcessStep> orderSteps = loadOrderSteps(orderUuid);
        Map<String, ProcessStep> existing = serviceStepMap(orderSteps);
        Map<String, Integer> nextSort = nextSortByRoll(orderSteps);
        Map<String, String> machineNames = loadMachineNames(requests);
        int created = 0;
        int updated = 0;
        for (ProcessStepDTO request : requests) {
            String key = key(request.getOriginalUuid(), request.getStepType());
            ProcessStep step = existing.get(key);
            OriginalRoll roll = requireRoll(rolls, request.getOriginalUuid());
            if (step == null) {
                step = newStep(orderUuid, request, nextSort);
                applyTemplate(step, request, roll, machineNames);
                ConcurrencyGuard.requireRowUpdated(stepMapper.insert(step));
                created++;
            } else {
                applyTemplate(step, request, roll, machineNames);
                ConcurrencyGuard.requireRowUpdated(stepMapper.updateById(step));
                updated++;
            }
        }
        return new ProcessStepBatchResultVO(requests.size(), created, updated);
    }

    private List<ProcessStep> loadOrderSteps(String orderUuid) {
        return stepMapper.selectList(new LambdaQueryWrapper<ProcessStep>()
                .eq(ProcessStep::getOrderUuid, orderUuid)
                .orderByAsc(ProcessStep::getStepSort));
    }

    private Map<String, ProcessStep> serviceStepMap(List<ProcessStep> steps) {
        Map<String, ProcessStep> result = new LinkedHashMap<>();
        for (ProcessStep step : steps) {
            if (Integer.valueOf(1).equals(step.getIsMain()) || !isServiceStep(step.getStepType())) continue;
            ProcessStep duplicate = result.putIfAbsent(key(step.getOriginalUuid(), step.getStepType()), step);
            if (duplicate != null) throw new BusinessException("母卷存在重复附加工艺，请先清理后重试");
        }
        return result;
    }

    private void requireUniqueTargets(List<ProcessStepDTO> requests) {
        Set<String> keys = new LinkedHashSet<>();
        for (ProcessStepDTO request : requests) {
            if (!keys.add(key(request.getOriginalUuid(), request.getStepType()))) {
                throw new BusinessException("同一母卷不能重复提交相同附加工艺");
            }
        }
    }

    private Map<String, Integer> nextSortByRoll(List<ProcessStep> steps) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (ProcessStep step : steps) {
            int next = (step.getStepSort() == null ? 0 : step.getStepSort()) + 1;
            result.merge(step.getOriginalUuid(), next, Math::max);
        }
        return result;
    }

    private ProcessStep newStep(String orderUuid, ProcessStepDTO request,
                                Map<String, Integer> nextSort) {
        ProcessStep step = new ProcessStep();
        step.setOrderUuid(orderUuid);
        step.setOriginalUuid(request.getOriginalUuid());
        step.setIsMain(0);
        int sort = nextSort.getOrDefault(request.getOriginalUuid(), 1);
        nextSort.put(request.getOriginalUuid(), sort + 1);
        step.setStepSort(sort);
        return step;
    }

    private OriginalRoll requireRoll(Map<String, OriginalRoll> rolls, String originalUuid) {
        OriginalRoll roll = rolls.get(originalUuid);
        if (roll == null) throw new BusinessException("原纸明细不存在");
        return roll;
    }

    private void applyTemplate(ProcessStep step, ProcessStepDTO request, OriginalRoll roll,
                               Map<String, String> machineNames) {
        step.setStepType(request.getStepType());
        step.setIsMain(0);
        step.setStepName(request.getStepName());
        step.setMachineUuid(resolveMachineUuid(request));
        step.setMachineNameSnap(StringUtils.hasText(step.getMachineUuid())
                ? machineNames.get(step.getMachineUuid()) : null);
        step.setBillingMode(request.getBillingMode() == null ? STANDARD : request.getBillingMode());
        step.setBillingBasis(normalizeBasis(request.getBillingBasis()));
        step.setUnitPrice(request.getUnitPrice());
        step.setBillingAmount(request.getBillingAmount());
        step.setRemark(request.getRemark());
        clearIncompatiblePricing(step);
        step.setServiceQuantity(ServiceStepQuantityResolver.resolve(step.getBillingBasis(), roll));
        ProcessCatalogVO catalog = catalogValidator.validate(step, roll);
        if (!StringUtils.hasText(step.getStepName())) step.setStepName(catalog.name());
        validatePricing(step);
    }

    private void clearIncompatiblePricing(ProcessStep step) {
        int mode = step.getBillingMode();
        step.setBillingUnitPrice(null);
        step.setBillingQuantity(null);
        step.setPricingAdjustmentReason(null);
        step.setPricingAdjustedBy(null);
        step.setPricingAdjustedAt(null);
        if (mode != FIXED_AMOUNT) step.setBillingAmount(mode == FREE ? java.math.BigDecimal.ZERO : null);
        if (mode == FIXED_AMOUNT || mode == FREE) {
            step.setBillingBasis(null);
            step.setUnitPrice(null);
        }
    }

    private void validatePricing(ProcessStep step) {
        int mode = step.getBillingMode();
        if (mode == FIXED_AMOUNT && (step.getBillingAmount() == null || step.getBillingAmount().signum() < 0)) {
            throw new BusinessException("固定金额服务必须填写不小于0的金额");
        }
        if (mode == FIXED_AMOUNT || mode == FREE) return;
        if (!"PIECE".equals(step.getBillingBasis()) && !"TON".equals(step.getBillingBasis())) {
            throw new BusinessException("请选择按件或按吨计费");
        }
        if (step.getServiceQuantity() == null || step.getServiceQuantity().signum() <= 0) {
            throw new BusinessException("附加工艺计费数量无效");
        }
        if (step.getUnitPrice() != null && step.getUnitPrice().signum() <= 0) {
            throw new BusinessException("服务单价必须大于0，暂不定价请留空");
        }
    }

    private Map<String, String> loadMachineNames(List<ProcessStepDTO> requests) {
        Set<String> uuids = requests.stream().map(ProcessStepDTO::getMachineUuid)
                .filter(StringUtils::hasText).collect(Collectors.toCollection(LinkedHashSet::new));
        if (uuids.isEmpty()) return Map.of();
        return machineMapper.selectBatchIds(uuids).stream()
                .collect(Collectors.toMap(Machine::getUuid, Machine::getMachineName));
    }

    private String resolveMachineUuid(ProcessStepDTO request) {
        // Service-only steps must not inherit the roll's main-process machine.
        // A rewind/sawing machine may be incompatible with strip sorting or repackaging.
        return StringUtils.hasText(request.getMachineUuid()) ? request.getMachineUuid() : null;
    }

    private String normalizeBasis(String basis) {
        return StringUtils.hasText(basis) ? basis.trim().toUpperCase() : null;
    }

    private boolean isServiceStep(Integer stepType) {
        return Integer.valueOf(3).equals(stepType) || Integer.valueOf(4).equals(stepType);
    }

    private String key(String originalUuid, Integer stepType) {
        return originalUuid + ":" + stepType;
    }
}
