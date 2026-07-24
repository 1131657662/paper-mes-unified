package com.paper.mes.machine.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.entity.MachineCapability;
import com.paper.mes.machine.mapper.MachineCapabilityMapper;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.service.ProcessCatalogService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MachineAssignmentPolicyTest {

    private final MachineMapper machineMapper = mock(MachineMapper.class);
    private final MachineCapabilityMapper capabilityMapper = mock(MachineCapabilityMapper.class);
    private final ProcessCatalogService catalogService = mock(ProcessCatalogService.class);
    private final MachineAssignmentPolicy policy = new MachineAssignmentPolicy(
            machineMapper, capabilityMapper, catalogService);

    @Test
    void requireCompatible_withEnabledSupportedMachine_returnsMachine() {
        Machine machine = machine(1);
        when(machineMapper.selectBatchIds(any())).thenReturn(List.of(machine));
        when(catalogService.listActive()).thenReturn(List.of(catalog()));
        when(capabilityMapper.selectList(any())).thenReturn(List.of(capability()));

        assertEquals(machine, policy.requireCompatible("machine", 1));
    }

    @Test
    void requireCompatible_withDisabledMachine_rejectsAssignment() {
        when(machineMapper.selectBatchIds(any())).thenReturn(List.of(machine(2)));
        when(catalogService.listActive()).thenReturn(List.of(catalog()));
        when(capabilityMapper.selectList(any())).thenReturn(List.of(capability()));

        BusinessException error = assertThrows(BusinessException.class,
                () -> policy.requireCompatible("machine", 1));

        assertEquals("所选机台或工位未启用", error.getMessage());
    }

    @Test
    void requireCompatible_withUnsupportedProcess_rejectsAssignment() {
        when(machineMapper.selectBatchIds(any())).thenReturn(List.of(machine(1)));
        when(catalogService.listActive()).thenReturn(List.of(catalog()));
        when(capabilityMapper.selectList(any())).thenReturn(List.of());

        BusinessException error = assertThrows(BusinessException.class,
                () -> policy.requireCompatible("machine", 1));

        assertEquals("一号机不支持锯纸工艺", error.getMessage());
    }

    @Test
    void requireCompatible_withWidthBelowMinimum_rejectsAssignment() {
        arrangeEnabledMachine(capabilityWithLimits());

        assertThrows(BusinessException.class, () -> policy.requireCompatible(
                "machine", 1, new MachineAssignmentPolicy.PhysicalContext(799, new BigDecimal("800"), 1200)));
    }

    @Test
    void requireCompatible_withOverweightRoll_rejectsAssignment() {
        arrangeEnabledMachine(capabilityWithLimits());

        assertThrows(BusinessException.class, () -> policy.requireCompatible(
                "machine", 1, new MachineAssignmentPolicy.PhysicalContext(1000, new BigDecimal("1000.001"), 1200)));
    }

    @Test
    void requireCompatible_withOversizeDiameter_rejectsAssignment() {
        arrangeEnabledMachine(capabilityWithLimits());

        assertThrows(BusinessException.class, () -> policy.requireCompatible(
                "machine", 1, new MachineAssignmentPolicy.PhysicalContext(1000, new BigDecimal("800"), 1501)));
    }

    private void arrangeEnabledMachine(MachineCapability capability) {
        when(machineMapper.selectBatchIds(any())).thenReturn(List.of(machine(1)));
        when(catalogService.listActive()).thenReturn(List.of(catalog()));
        when(capabilityMapper.selectList(any())).thenReturn(List.of(capability));
    }

    private Machine machine(int status) {
        Machine machine = new Machine();
        machine.setUuid("machine");
        machine.setMachineName("一号机");
        machine.setStatus(status);
        return machine;
    }

    private ProcessCatalogVO catalog() {
        return new ProcessCatalogVO("saw", 1, "SAW", "锯纸", "PRODUCTION", "STANDARD",
                true, true, true, List.of(), List.of(1));
    }

    private MachineCapability capability() {
        MachineCapability capability = new MachineCapability();
        capability.setMachineUuid("machine");
        capability.setCatalogUuid("saw");
        return capability;
    }

    private MachineCapability capabilityWithLimits() {
        MachineCapability capability = capability();
        capability.setMinWidth(800);
        capability.setMaxWidth(1600);
        capability.setMaxRollWeight(new BigDecimal("1000"));
        capability.setMaxDiameter(1500);
        return capability;
    }
}
