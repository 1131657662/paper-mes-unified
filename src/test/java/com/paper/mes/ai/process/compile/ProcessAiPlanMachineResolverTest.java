package com.paper.mes.ai.process.compile;

import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.entity.MachineCapability;
import com.paper.mes.machine.mapper.MachineCapabilityMapper;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.service.ProcessCatalogService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessAiPlanMachineResolverTest {

    @Test
    void resolve_whenAiChangesRewindToSaw_selectsTheCompatibleSawMachine() {
        OriginalRollMapper rolls = mock(OriginalRollMapper.class);
        MachineMapper machines = mock(MachineMapper.class);
        MachineCapabilityMapper capabilities = mock(MachineCapabilityMapper.class);
        ProcessCatalogService catalogs = mock(ProcessCatalogService.class);
        ProcessAiPlanMachineResolver resolver = new ProcessAiPlanMachineResolver(
                rolls, machines, capabilities, catalogs);
        OriginalRoll roll = new OriginalRoll();
        roll.setMachineUuid("rewind-machine");
        when(rolls.selectById("roll-1")).thenReturn(roll);
        when(catalogs.requireActive(1)).thenReturn(catalog("saw-catalog"));
        when(capabilities.selectList(any())).thenReturn(List.of(capability("saw-machine")));
        when(machines.selectBatchIds(any())).thenReturn(List.of(machine("saw-machine")));
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setMainStepType(1);

        resolver.resolve(owner(), plan);

        assertThat(plan.getMachineUuid()).isEqualTo("saw-machine");
    }

    private ProcessAiRollContext owner() {
        return new ProcessAiRollContext(
                "R1", "roll-1", 1, "白卡纸", 80, 1000, 1200, 3,
                new BigDecimal("500"), 1, 1, 2);
    }

    private ProcessCatalogVO catalog(String uuid) {
        return new ProcessCatalogVO(uuid, 1, "SAW", "锯纸", "PRODUCTION",
                "STANDARD", true, true, true, List.of(), List.of());
    }

    private MachineCapability capability(String machineUuid) {
        MachineCapability value = new MachineCapability();
        value.setMachineUuid(machineUuid);
        value.setCatalogUuid("saw-catalog");
        value.setIsDefault(1);
        value.setPriority(1);
        value.setMinWidth(1);
        value.setMaxWidth(2000);
        value.setMaxRollWeight(new BigDecimal("1000"));
        value.setMaxDiameter(2000);
        return value;
    }

    private Machine machine(String uuid) {
        Machine value = new Machine();
        value.setUuid(uuid);
        value.setMachineName("锯纸机");
        value.setStatus(1);
        return value;
    }
}
