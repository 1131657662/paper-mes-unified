package com.paper.mes.machine.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.machine.dto.MachineCapabilitySaveDTO;
import com.paper.mes.machine.entity.MachineCapability;
import com.paper.mes.machine.mapper.MachineCapabilityMapper;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.service.ProcessCatalogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MachineCapabilityWriterTest {

    private final MachineCapabilityMapper mapper = mock(MachineCapabilityMapper.class);
    private final ProcessCatalogService catalogService = mock(ProcessCatalogService.class);
    private final MachineCapabilityWriter writer = new MachineCapabilityWriter(mapper, catalogService);

    @Test
    void normalize_withLegacyGeneralMachine_preservesSawAndRewindCapabilities() {
        when(catalogService.listActive()).thenReturn(catalogs());

        List<MachineCapabilitySaveDTO> result = writer.normalize(null, 3);

        assertEquals(List.of("saw", "rewind"), result.stream()
                .map(MachineCapabilitySaveDTO::getCatalogUuid).toList());
        assertEquals(3, writer.legacyType(result));
    }

    @Test
    void normalize_withDuplicateCatalog_rejectsCapabilities() {
        when(catalogService.listActive()).thenReturn(catalogs());
        List<MachineCapabilitySaveDTO> duplicated = List.of(capability("saw"), capability("saw"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> writer.normalize(duplicated, null));

        assertEquals("同一工艺能力不能重复配置", error.getMessage());
    }

    @Test
    void normalize_withInvalidWidthRange_rejectsCapabilities() {
        when(catalogService.listActive()).thenReturn(catalogs());
        MachineCapabilitySaveDTO capability = capability("saw");
        capability.setMinWidth(2000);
        capability.setMaxWidth(1000);

        BusinessException error = assertThrows(BusinessException.class,
                () -> writer.normalize(List.of(capability), null));

        assertEquals("最小门幅不能大于最大门幅", error.getMessage());
    }

    @Test
    void replace_whenMachineDisabled_doesNotPersistDefaultCapability() {
        MachineCapabilitySaveDTO source = capability("saw");
        source.setIsDefault(1);
        ArgumentCaptor<MachineCapability> inserted = ArgumentCaptor.forClass(MachineCapability.class);

        writer.replace("machine", 2, List.of(source));

        verify(mapper).insert(inserted.capture());
        assertEquals(0, inserted.getValue().getIsDefault());
        assertFalse(inserted.getValue().getPriority() < 1);
    }

    @Test
    void normalizeForUpdate_whenCapabilitiesOmitted_preservesStoredCapabilities() {
        MachineCapability stored = new MachineCapability();
        stored.setCatalogUuid("strip");
        stored.setPriority(20);
        stored.setIsDefault(1);
        when(mapper.selectList(any())).thenReturn(List.of(stored));

        List<MachineCapabilitySaveDTO> result = writer.normalizeForUpdate(
                "machine", null, null);

        assertEquals("strip", result.getFirst().getCatalogUuid());
        assertEquals(1, result.getFirst().getIsDefault());
    }

    private MachineCapabilitySaveDTO capability(String catalogUuid) {
        MachineCapabilitySaveDTO value = new MachineCapabilitySaveDTO();
        value.setCatalogUuid(catalogUuid);
        value.setPriority(100);
        value.setIsDefault(0);
        return value;
    }

    private List<ProcessCatalogVO> catalogs() {
        return List.of(catalog("saw", 1, "SAW"), catalog("rewind", 2, "REWIND"));
    }

    private ProcessCatalogVO catalog(String uuid, int type, String code) {
        return new ProcessCatalogVO(uuid, type, code, code, "PRODUCTION", "STANDARD",
                true, true, true, List.of(), List.of(1));
    }
}
