package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.machine.service.MachineAssignmentPolicy;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.entity.OriginalRoll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProcessCatalogStepValidator {

    private final ProcessCatalogService catalogService;
    private final MachineAssignmentPolicy machineAssignmentPolicy;

    public void validateMainProcess(Integer stepType) {
        validateMainProcesses(Collections.singleton(stepType));
    }

    public void validateMainProcesses(Collection<Integer> stepTypes) {
        if (stepTypes == null || stepTypes.isEmpty()) {
            return;
        }
        List<ProcessCatalogVO> activeCatalog = catalogService.listActive();
        for (Integer stepType : new LinkedHashSet<>(stepTypes)) {
            ProcessCatalogVO catalog = findActive(activeCatalog, stepType);
            ProcessStep step = new ProcessStep();
            step.setStepType(stepType);
            step.setIsMain(1);
            validateWithCatalog(step, catalog);
        }
    }

    public ProcessCatalogVO validate(ProcessStep step) {
        return validate(step, (OriginalRoll) null);
    }

    public ProcessCatalogVO validate(ProcessStep step, OriginalRoll roll) {
        ProcessCatalogVO catalog = catalogService.requireActive(step.getStepType());
        machineAssignmentPolicy.requireCompatible(step.getMachineUuid(), step.getStepType(), physicalContext(roll));
        return validateCatalog(step, catalog);
    }

    private ProcessCatalogVO validateWithCatalog(ProcessStep step, ProcessCatalogVO catalog) {
        machineAssignmentPolicy.requireCompatible(step.getMachineUuid(), step.getStepType());
        return validateCatalog(step, catalog);
    }

    private ProcessCatalogVO validateCatalog(ProcessStep step, ProcessCatalogVO catalog) {
        validateMainCapability(step, catalog);
        validateBillingMode(step, catalog);
        validateUnit(step, catalog);
        validateLossCapability(step, catalog);
        return catalog;
    }

    private MachineAssignmentPolicy.PhysicalContext physicalContext(OriginalRoll roll) {
        if (roll == null) return null;
        return new MachineAssignmentPolicy.PhysicalContext(
                roll.getOriginalWidth(), roll.getRollWeight(), roll.getOriginalDiameter());
    }

    private ProcessCatalogVO findActive(List<ProcessCatalogVO> catalog, Integer stepType) {
        return catalog.stream()
                .filter(entry -> Integer.valueOf(entry.stepType()).equals(stepType))
                .findFirst()
                .orElseThrow(() -> new BusinessException("工序类型未启用或不存在"));
    }

    private void validateMainCapability(ProcessStep step, ProcessCatalogVO catalog) {
        if (Integer.valueOf(1).equals(step.getIsMain()) && !catalog.allowsMainProcess()) {
            throw new BusinessException(catalog.name() + "不能作为主工艺");
        }
    }

    private void validateBillingMode(ProcessStep step, ProcessCatalogVO catalog) {
        int billingMode = step.getBillingMode() == null ? ProcessStepPricingPolicy.STANDARD
                : step.getBillingMode();
        if (!catalog.supportsBillingMode(billingMode)) {
            throw new BusinessException(catalog.name() + "不支持当前计费模式");
        }
    }

    private void validateUnit(ProcessStep step, ProcessCatalogVO catalog) {
        if (!StringUtils.hasText(step.getBillingBasis())) {
            return;
        }
        String unit = step.getBillingBasis().trim().toUpperCase();
        step.setBillingBasis(unit);
        if (!catalog.supportsUnit(unit)) {
            throw new BusinessException(catalog.name() + "不支持计费单位 " + unit);
        }
    }

    private void validateLossCapability(ProcessStep step, ProcessCatalogVO catalog) {
        if (catalog.allowsLossRecording() || !hasRecordedLoss(step)) {
            return;
        }
        throw new BusinessException(catalog.name() + "不允许回录损耗");
    }

    private boolean hasRecordedLoss(ProcessStep step) {
        return positive(step.getLossWeight()) || positive(step.getPlannedLossWeight())
                || step.getPlannedLossWidth() != null && step.getPlannedLossWidth() > 0;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
