package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.machine.service.MachineAssignmentPolicy;
import com.paper.mes.processorder.dto.ProcessCatalogUnitVO;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessCatalogStepValidatorTest {

    private final ProcessCatalogService catalogService = mock(ProcessCatalogService.class);
    private final MachineAssignmentPolicy machinePolicy = mock(MachineAssignmentPolicy.class);
    private final ProcessCatalogStepValidator validator = new ProcessCatalogStepValidator(catalogService, machinePolicy);

    @Test
    void validate_withSupportedServiceCapabilities_normalizesUnit() {
        ProcessStep step = step(3, 0, 1, "piece");
        when(catalogService.requireActive(3)).thenReturn(catalog(false, true, List.of(1, 3, 4)));

        ProcessCatalogVO result = validator.validate(step);

        assertEquals("SERVICE_QUANTITY", result.pricingStrategy());
        assertEquals("PIECE", step.getBillingBasis());
    }

    @Test
    void validate_whenProcessCannotBeMain_rejectsStep() {
        ProcessStep step = step(3, 1, 1, "PIECE");
        when(catalogService.requireActive(3)).thenReturn(catalog(false, true, List.of(1, 3, 4)));

        BusinessException error = assertThrows(BusinessException.class, () -> validator.validate(step));

        assertEquals("剥损整理不能作为主工艺", error.getMessage());
    }

    @Test
    void validate_withUnsupportedBillingMode_rejectsStep() {
        ProcessStep step = step(3, 0, 2, "PIECE");
        when(catalogService.requireActive(3)).thenReturn(catalog(false, true, List.of(1, 3, 4)));

        BusinessException error = assertThrows(BusinessException.class, () -> validator.validate(step));

        assertEquals("剥损整理不支持当前计费模式", error.getMessage());
    }

    @Test
    void validate_whenLossIsNotAllowed_rejectsRecordedLoss() {
        ProcessStep step = step(4, 0, 1, "PIECE");
        step.setLossWeight(new BigDecimal("0.100"));
        when(catalogService.requireActive(4)).thenReturn(repackCatalog());

        BusinessException error = assertThrows(BusinessException.class, () -> validator.validate(step));

        assertEquals("重新包装不允许回录损耗", error.getMessage());
    }

    @Test
    void validateMainProcesses_withRepeatedTypes_loadsCatalogOnce() {
        when(catalogService.listActive()).thenReturn(List.of(
                productionCatalog(1, "锯纸"),
                productionCatalog(2, "复卷")
        ));

        validator.validateMainProcesses(List.of(1, 2, 1));

        verify(catalogService).listActive();
    }

    private ProcessStep step(int type, int main, int billingMode, String unit) {
        ProcessStep step = new ProcessStep();
        step.setStepType(type);
        step.setIsMain(main);
        step.setBillingMode(billingMode);
        step.setBillingBasis(unit);
        return step;
    }

    private ProcessCatalogVO catalog(boolean main, boolean loss, List<Integer> modes) {
        return new ProcessCatalogVO("catalog", 3, "STRIP_SORT", "剥损整理", "SERVICE",
                "SERVICE_QUANTITY", false, loss, main,
                List.of(new ProcessCatalogUnitVO("PIECE", "件", true)), modes);
    }

    private ProcessCatalogVO repackCatalog() {
        return new ProcessCatalogVO("catalog-repack", 4, "REPACK", "重新包装", "PACKAGING",
                "SERVICE_QUANTITY", false, false, false,
                List.of(new ProcessCatalogUnitVO("PIECE", "件", true)), List.of(1, 3, 4));
    }

    private ProcessCatalogVO productionCatalog(int type, String name) {
        return new ProcessCatalogVO("catalog-" + type, type, "TYPE_" + type, name, "PROCESS",
                "STANDARD", true, true, true, List.of(), List.of(1));
    }
}
