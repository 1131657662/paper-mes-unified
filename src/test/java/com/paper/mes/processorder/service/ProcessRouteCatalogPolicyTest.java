package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessRouteCatalogPolicyTest {

    private final ProcessCatalogService catalogService = mock(ProcessCatalogService.class);
    private final ProcessRouteCatalogPolicy policy = new ProcessRouteCatalogPolicy(catalogService);

    @Test
    void validate_whenSawAndRewindAreActive_acceptsRouteWithSingleCatalogLoad() {
        when(catalogService.listActive()).thenReturn(List.of(
                catalog(1, "锯纸", true, true),
                catalog(2, "复卷", true, true)
        ));

        assertDoesNotThrow(() -> policy.validate(List.of(stage(1, 1), stage(2, 2))));

        verify(catalogService).listActive();
    }

    @Test
    void validate_whenProcessIsDisabled_rejectsRoute() {
        when(catalogService.listActive()).thenReturn(List.of(catalog(1, "锯纸", true, true)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> policy.validate(List.of(stage(1, 2))));

        assertEquals("工序类型未启用或不存在", error.getMessage());
    }

    @Test
    void validate_whenServiceStepIsUsed_rejectsInventoryRoute() {
        when(catalogService.listActive()).thenReturn(List.of(catalog(3, "剥损整理", false, false)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> policy.validate(List.of(stage(2, 3))));

        assertEquals("链式工艺仅支持锯纸和复卷，服务工序请单独追加", error.getMessage());
    }

    @Test
    void validate_whenFutureOutputProcessIsUnknownToEngine_rejectsRoute() {
        when(catalogService.listActive()).thenReturn(List.of(catalog(5, "分切", true, true)));

        assertThrows(BusinessException.class, () -> policy.validate(List.of(stage(1, 5))));
    }

    private ProcessRoutePreviewDTO.RouteStageDTO stage(int level, int type) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = new ProcessRoutePreviewDTO.RouteStageDTO();
        stage.setStageLevel(level);
        stage.setStepType(type);
        return stage;
    }

    private ProcessCatalogVO catalog(int type, String name, boolean output, boolean main) {
        return new ProcessCatalogVO("catalog-" + type, type, "TYPE_" + type, name, "PROCESS",
                type == FeeCalculator.STEP_TYPE_SAW ? "SAW_KNIFE" : "REWIND_WEIGHT",
                output, true, main, List.of(), List.of(ProcessStepPricingPolicy.STANDARD));
    }
}
