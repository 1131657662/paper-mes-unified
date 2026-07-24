package com.paper.mes.machine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.common.PageResult;
import com.paper.mes.machine.dto.MachineCapabilityVO;
import com.paper.mes.machine.dto.MachineVO;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.entity.MachineCapability;
import com.paper.mes.machine.mapper.MachineCapabilityMapper;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.service.ProcessCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MachineCapabilityReader {

    private final MachineCapabilityMapper capabilityMapper;
    private final ProcessCatalogService catalogService;

    public PageResult<MachineVO> toPage(Page<Machine> page) {
        PageResult<MachineVO> result = new PageResult<>();
        result.setRecords(toViews(page.getRecords()));
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    public MachineVO toView(Machine machine) {
        return toViews(List.of(machine)).getFirst();
    }

    private List<MachineVO> toViews(List<Machine> machines) {
        Map<String, List<MachineCapabilityVO>> capabilities = loadCapabilities(machineIds(machines));
        return machines.stream().map(machine -> toView(machine,
                capabilities.getOrDefault(machine.getUuid(), List.of()))).toList();
    }

    private Map<String, List<MachineCapabilityVO>> loadCapabilities(Collection<String> machineIds) {
        if (machineIds.isEmpty()) return Map.of();
        Map<String, ProcessCatalogVO> catalogs = catalogService.listActive().stream()
                .collect(Collectors.toMap(ProcessCatalogVO::uuid, Function.identity()));
        return capabilityMapper.selectList(new LambdaQueryWrapper<MachineCapability>()
                        .in(MachineCapability::getMachineUuid, machineIds)
                        .orderByAsc(MachineCapability::getPriority))
                .stream().filter(row -> catalogs.containsKey(row.getCatalogUuid()))
                .collect(Collectors.groupingBy(MachineCapability::getMachineUuid,
                        Collectors.mapping(row -> toView(row, catalogs.get(row.getCatalogUuid())),
                                Collectors.toList())));
    }

    private MachineCapabilityVO toView(MachineCapability row, ProcessCatalogVO catalog) {
        return new MachineCapabilityVO(catalog.uuid(), catalog.stepType(), catalog.code(), catalog.name(),
                catalog.category(), Integer.valueOf(1).equals(row.getIsDefault()),
                row.getPriority(), row.getMinWidth(), row.getMaxWidth(), row.getMaxRollWeight(),
                row.getMaxDiameter(), row.getRemark());
    }

    private MachineVO toView(Machine machine, List<MachineCapabilityVO> capabilities) {
        return new MachineVO(machine.getUuid(), machine.getVersion(), machine.getMachineCode(),
                machine.getMachineName(), machine.getMachineType(), machine.getResourceKind(),
                machine.getStatus(), machine.getRemark(), machine.getCreateTime(),
                machine.getUpdateTime(), capabilities);
    }

    private List<String> machineIds(List<Machine> machines) {
        return machines.stream().map(Machine::getUuid).toList();
    }
}
