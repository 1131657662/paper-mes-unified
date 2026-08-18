package com.paper.mes.ai.process.compile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.entity.MachineCapability;
import com.paper.mes.machine.mapper.MachineCapabilityMapper;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.service.ProcessCatalogService;
import com.paper.mes.processorder.service.ProcessModePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class ProcessAiPlanMachineResolver {

    private static final int ENABLED = 1;

    private final OriginalRollMapper rollMapper;
    private final MachineMapper machineMapper;
    private final MachineCapabilityMapper capabilityMapper;
    private final ProcessCatalogService catalogService;

    void resolve(ProcessAiRollContext owner, ProcessPlanDTO plan) {
        if (!ProcessModePolicy.requiresMainProcess(plan.getProcessMode())) {
            plan.setMachineUuid(null);
            return;
        }
        OriginalRoll roll = rollMapper.selectById(owner.originalUuid());
        ProcessCatalogVO catalog = catalogService.requireActive(plan.getMainStepType());
        List<MachineCandidate> candidates = candidates(owner, catalog.uuid());
        MachineCandidate selected = select(candidates, roll == null ? null : roll.getMachineUuid());
        if (selected == null) throw selectionRequired(owner.shortRef());
        plan.setMachineUuid(selected.machine().getUuid());
    }

    private List<MachineCandidate> candidates(ProcessAiRollContext owner, String catalogUuid) {
        List<MachineCapability> capabilities = capabilityMapper.selectList(
                new LambdaQueryWrapper<MachineCapability>()
                        .eq(MachineCapability::getCatalogUuid, catalogUuid));
        if (capabilities.isEmpty()) return List.of();
        Map<String, Machine> machines = machineMapper.selectBatchIds(capabilities.stream()
                        .map(MachineCapability::getMachineUuid).distinct().toList()).stream()
                .filter(machine -> Integer.valueOf(ENABLED).equals(machine.getStatus()))
                .collect(Collectors.toMap(Machine::getUuid, Function.identity()));
        return capabilities.stream()
                .filter(capability -> machines.containsKey(capability.getMachineUuid()))
                .filter(capability -> supports(capability, owner))
                .map(capability -> new MachineCandidate(
                        machines.get(capability.getMachineUuid()), capability))
                .sorted(candidateOrder())
                .toList();
    }

    private MachineCandidate select(List<MachineCandidate> candidates, String currentMachineUuid) {
        MachineCandidate current = candidates.stream()
                .filter(candidate -> candidate.machine().getUuid().equals(currentMachineUuid))
                .findFirst().orElse(null);
        if (current != null) return current;
        List<MachineCandidate> defaults = candidates.stream()
                .filter(candidate -> Integer.valueOf(1).equals(candidate.capability().getIsDefault()))
                .toList();
        if (defaults.size() == 1) return defaults.getFirst();
        return candidates.size() == 1 ? candidates.getFirst() : null;
    }

    private boolean supports(MachineCapability capability, ProcessAiRollContext owner) {
        if (below(owner.originalWidth(), capability.getMinWidth())) return false;
        if (above(owner.originalWidth(), capability.getMaxWidth())) return false;
        if (owner.rollWeight() != null && capability.getMaxRollWeight() != null
                && owner.rollWeight().compareTo(capability.getMaxRollWeight()) > 0) return false;
        return !above(owner.originalDiameter(), capability.getMaxDiameter());
    }

    private Comparator<MachineCandidate> candidateOrder() {
        return Comparator.comparing((MachineCandidate value) ->
                        !Integer.valueOf(1).equals(value.capability().getIsDefault()))
                .thenComparing(value -> value.capability().getPriority(),
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(value -> value.machine().getMachineName(),
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(value -> value.machine().getUuid());
    }

    private boolean below(Integer value, Integer minimum) {
        return value != null && minimum != null && value < minimum;
    }

    private boolean above(Integer value, Integer maximum) {
        return value != null && maximum != null && value > maximum;
    }

    private BusinessException selectionRequired(String rollRef) {
        return new BusinessException(ResultCode.BAD_REQUEST, "AI_MACHINE_SELECTION_REQUIRED",
                rollRef + "没有唯一兼容机台，请先在加工方式中选择机台");
    }

    private record MachineCandidate(Machine machine, MachineCapability capability) {
    }
}
