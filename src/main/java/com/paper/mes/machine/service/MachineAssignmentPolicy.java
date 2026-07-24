package com.paper.mes.machine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.entity.MachineCapability;
import com.paper.mes.machine.mapper.MachineCapabilityMapper;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.service.ProcessCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MachineAssignmentPolicy {

    private static final int STATUS_ENABLED = 1;

    private final MachineMapper machineMapper;
    private final MachineCapabilityMapper capabilityMapper;
    private final ProcessCatalogService catalogService;

    public Machine requireCompatible(String machineUuid, Integer stepType) {
        return requireCompatible(machineUuid, stepType, null);
    }

    public Machine requireCompatible(String machineUuid, Integer stepType, PhysicalContext physical) {
        if (!StringUtils.hasText(machineUuid)) return null;
        if (stepType == null) {
            throw new BusinessException(ErrorCode.E003, "工序类型不能为空");
        }
        return requireCompatibleAssignments(Map.of(machineUuid, Set.of(stepType)), physical).get(machineUuid);
    }

    public Map<String, Machine> requireCompatibleAssignments(Map<String, Set<Integer>> assignments) {
        return requireCompatibleAssignments(assignments, null);
    }

    public Map<String, Machine> requireCompatibleAssignments(
            Map<String, Set<Integer>> assignments, PhysicalContext physical) {
        if (assignments.isEmpty()) return Map.of();
        Map<String, Machine> machines = machineMapper.selectBatchIds(assignments.keySet()).stream()
                .collect(Collectors.toMap(Machine::getUuid, Function.identity()));
        Map<Integer, ProcessCatalogVO> catalogs = catalogService.listActive().stream()
                .collect(Collectors.toMap(ProcessCatalogVO::stepType, Function.identity()));
        Map<String, MachineCapability> capabilities = capabilities(assignments.keySet());
        AssignmentContext context = new AssignmentContext(machines, catalogs, capabilities);
        assignments.forEach((machineUuid, types) -> validateAssignment(machineUuid, types, context, physical));
        return machines;
    }

    private Map<String, MachineCapability> capabilities(Collection<String> machineUuids) {
        List<MachineCapability> rows = capabilityMapper.selectList(
                new LambdaQueryWrapper<MachineCapability>()
                        .in(MachineCapability::getMachineUuid, machineUuids));
        return rows.stream().collect(Collectors.toMap(
                row -> key(row.getMachineUuid(), row.getCatalogUuid()), Function.identity(), (left, right) -> left));
    }

    private void validateAssignment(String machineUuid, Set<Integer> stepTypes,
                                     AssignmentContext context, PhysicalContext physical) {
        Machine machine = context.machines().get(machineUuid);
        if (machine == null || !Integer.valueOf(STATUS_ENABLED).equals(machine.getStatus())) {
            throw new BusinessException(ErrorCode.E003, "所选机台或工位未启用");
        }
        for (Integer stepType : stepTypes) {
            ProcessCatalogVO catalog = context.catalogs().get(stepType);
            if (catalog == null) throw new BusinessException(ErrorCode.E003, "工序类型未启用或不存在");
            MachineCapability capability = context.capabilities().get(key(machineUuid, catalog.uuid()));
            if (capability == null) {
                throw new BusinessException(ErrorCode.E003,
                        machine.getMachineName() + "不支持" + catalog.name() + "工艺");
            }
            validatePhysical(machine, capability, physical);
        }
    }

    private void validatePhysical(Machine machine, MachineCapability capability, PhysicalContext physical) {
        if (physical == null) return;
        if (physical.width() != null && capability.getMinWidth() != null
                && physical.width() < capability.getMinWidth()) {
            throw new BusinessException(machine.getMachineName() + "最小门幅为" + capability.getMinWidth() + "mm");
        }
        if (physical.width() != null && capability.getMaxWidth() != null
                && physical.width() > capability.getMaxWidth()) {
            throw new BusinessException(machine.getMachineName() + "最大门幅为" + capability.getMaxWidth() + "mm");
        }
        if (physical.weight() != null && capability.getMaxRollWeight() != null
                && physical.weight().compareTo(capability.getMaxRollWeight()) > 0) {
            throw new BusinessException(machine.getMachineName() + "最大单卷重量为" + capability.getMaxRollWeight() + "kg");
        }
        if (physical.diameter() != null && capability.getMaxDiameter() != null
                && physical.diameter() > capability.getMaxDiameter()) {
            throw new BusinessException(machine.getMachineName() + "最大卷径为" + capability.getMaxDiameter() + "mm");
        }
    }

    private String key(String machineUuid, String catalogUuid) {
        return machineUuid + ':' + catalogUuid;
    }

    private record AssignmentContext(
            Map<String, Machine> machines,
            Map<Integer, ProcessCatalogVO> catalogs,
            Map<String, MachineCapability> capabilities) { }

    public record PhysicalContext(Integer width, BigDecimal weight, Integer diameter) { }
}
