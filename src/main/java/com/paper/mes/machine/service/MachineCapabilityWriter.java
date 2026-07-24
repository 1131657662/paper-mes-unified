package com.paper.mes.machine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.machine.dto.MachineCapabilitySaveDTO;
import com.paper.mes.machine.entity.MachineCapability;
import com.paper.mes.machine.mapper.MachineCapabilityMapper;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.service.ProcessCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MachineCapabilityWriter {

    private static final int STATUS_ENABLED = 1;

    private final MachineCapabilityMapper capabilityMapper;
    private final ProcessCatalogService catalogService;

    public List<MachineCapabilitySaveDTO> normalize(
            List<MachineCapabilitySaveDTO> requested, Integer legacyType) {
        List<MachineCapabilitySaveDTO> values = requested == null || requested.isEmpty()
                ? fromLegacyType(legacyType) : requested;
        if (values.isEmpty()) {
            throw new BusinessException(ErrorCode.E003, "请至少配置一项工艺能力");
        }
        validate(values);
        return values;
    }

    public List<MachineCapabilitySaveDTO> normalizeForUpdate(
            String machineUuid, List<MachineCapabilitySaveDTO> requested, Integer legacyType) {
        if (requested != null) return normalize(requested, legacyType);
        List<MachineCapabilitySaveDTO> existing = currentCapabilities(machineUuid);
        return existing.isEmpty() ? normalize(null, legacyType) : existing;
    }

    public void replace(String machineUuid, Integer machineStatus,
                        List<MachineCapabilitySaveDTO> capabilities) {
        remove(machineUuid);
        for (MachineCapabilitySaveDTO capability : capabilities) {
            boolean defaultCapability = machineStatus != null && machineStatus == STATUS_ENABLED
                    && Integer.valueOf(1).equals(capability.getIsDefault());
            if (defaultCapability) clearOtherDefault(machineUuid, capability.getCatalogUuid());
            capabilityMapper.insert(toEntity(machineUuid, capability, defaultCapability));
        }
    }

    public void remove(String machineUuid) {
        capabilityMapper.delete(new LambdaQueryWrapper<MachineCapability>()
                .eq(MachineCapability::getMachineUuid, machineUuid));
    }

    public Integer legacyType(List<MachineCapabilitySaveDTO> capabilities) {
        Map<String, ProcessCatalogVO> catalogs = catalogIndex();
        Set<Integer> types = capabilities.stream()
                .map(item -> catalogs.get(item.getCatalogUuid()))
                .filter(catalog -> catalog != null && (catalog.stepType() == 1 || catalog.stepType() == 2))
                .map(ProcessCatalogVO::stepType).collect(Collectors.toSet());
        if (types.containsAll(Set.of(1, 2))) return 3;
        if (types.contains(1)) return 1;
        if (types.contains(2)) return 2;
        return null;
    }

    private void validate(List<MachineCapabilitySaveDTO> capabilities) {
        Map<String, ProcessCatalogVO> catalogs = catalogIndex();
        Set<String> seen = new HashSet<>();
        for (MachineCapabilitySaveDTO capability : capabilities) {
            if (!catalogs.containsKey(capability.getCatalogUuid())) {
                throw new BusinessException(ErrorCode.E003, "工艺能力未启用或不存在");
            }
            if (!seen.add(capability.getCatalogUuid())) {
                throw new BusinessException(ErrorCode.E003, "同一工艺能力不能重复配置");
            }
            validateRange(capability);
        }
    }

    private void validateRange(MachineCapabilitySaveDTO capability) {
        Integer min = capability.getMinWidth();
        Integer max = capability.getMaxWidth();
        if (min != null && max != null && min > max) {
            throw new BusinessException(ErrorCode.E003, "最小门幅不能大于最大门幅");
        }
    }

    private List<MachineCapabilitySaveDTO> fromLegacyType(Integer legacyType) {
        if (legacyType == null) return List.of();
        Set<Integer> types = legacyType == 3 ? Set.of(1, 2) : Set.of(legacyType);
        return catalogService.listActive().stream().filter(item -> types.contains(item.stepType()))
                .map(this::legacyCapability).toList();
    }

    private List<MachineCapabilitySaveDTO> currentCapabilities(String machineUuid) {
        return capabilityMapper.selectList(new LambdaQueryWrapper<MachineCapability>()
                        .eq(MachineCapability::getMachineUuid, machineUuid)
                        .orderByAsc(MachineCapability::getPriority))
                .stream().map(this::toSaveDto).toList();
    }

    private MachineCapabilitySaveDTO toSaveDto(MachineCapability source) {
        MachineCapabilitySaveDTO target = new MachineCapabilitySaveDTO();
        target.setCatalogUuid(source.getCatalogUuid());
        target.setIsDefault(source.getIsDefault());
        target.setPriority(source.getPriority());
        target.setMinWidth(source.getMinWidth());
        target.setMaxWidth(source.getMaxWidth());
        target.setMaxRollWeight(source.getMaxRollWeight());
        target.setMaxDiameter(source.getMaxDiameter());
        target.setRemark(source.getRemark());
        return target;
    }

    private MachineCapabilitySaveDTO legacyCapability(ProcessCatalogVO catalog) {
        MachineCapabilitySaveDTO capability = new MachineCapabilitySaveDTO();
        capability.setCatalogUuid(catalog.uuid());
        capability.setPriority(100);
        capability.setIsDefault(0);
        return capability;
    }

    private void clearOtherDefault(String machineUuid, String catalogUuid) {
        capabilityMapper.update(null, new LambdaUpdateWrapper<MachineCapability>()
                .eq(MachineCapability::getCatalogUuid, catalogUuid)
                .eq(MachineCapability::getIsDefault, 1)
                .ne(MachineCapability::getMachineUuid, machineUuid)
                .set(MachineCapability::getIsDefault, 0));
    }

    private MachineCapability toEntity(String machineUuid, MachineCapabilitySaveDTO source,
                                       boolean defaultCapability) {
        MachineCapability target = new MachineCapability();
        target.setMachineUuid(machineUuid);
        target.setCatalogUuid(source.getCatalogUuid());
        target.setIsDefault(defaultCapability ? 1 : 0);
        target.setPriority(source.getPriority() == null ? 100 : source.getPriority());
        target.setMinWidth(source.getMinWidth());
        target.setMaxWidth(source.getMaxWidth());
        target.setMaxRollWeight(source.getMaxRollWeight());
        target.setMaxDiameter(source.getMaxDiameter());
        target.setRemark(source.getRemark());
        return target;
    }

    private Map<String, ProcessCatalogVO> catalogIndex() {
        return catalogService.listActive().stream()
                .collect(Collectors.toMap(ProcessCatalogVO::uuid, Function.identity()));
    }
}
