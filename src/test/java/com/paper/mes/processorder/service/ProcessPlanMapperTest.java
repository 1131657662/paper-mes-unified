package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessPlanMapperTest {

    private final ProcessPlanMapper mapper = new ProcessPlanMapper();

    @Test
    void toSaveDto_withLayeredLayout_preservesLayers() {
        ProcessPlanDTO plan = layeredPlan();

        FinishConfigSaveDTO dto = mapper.toSaveDto(plan);

        List<FinishConfigSpecDTO.FinishLayerDTO> layers = dto.getRewindSegments()
                .getFirst()
                .getLayoutItems()
                .getFirst()
                .getLayers();
        assertEquals(2, layers.size());
        assertEquals(30, layers.getFirst().getOutDiameter());
        assertEquals(18, layers.get(1).getOutDiameter());
    }

    @Test
    void fromSaveDto_withLayeredLayout_preservesLayers() {
        FinishConfigSaveDTO dto = mapper.toSaveDto(layeredPlan());

        ProcessPlanDTO plan = mapper.fromSaveDto(dto);

        List<FinishConfigSpecDTO.FinishLayerDTO> layers = plan.getSegments()
                .getFirst()
                .getLayoutItems()
                .getFirst()
                .getLayers();
        assertEquals(2, layers.size());
        assertEquals(3, layers.getFirst().getCoreDiameter());
        assertEquals(6, layers.get(1).getCoreDiameter());
    }

    @Test
    void serviceOnlyPreview_reportsOneOutputPerSourcePiece() {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(4);

        var preview = mapper.serviceOnlyPreview(plan, "roll-1", 3, true);

        assertTrue(preview.isReady());
        assertEquals(3, preview.getFinishCount());
        assertEquals("仅附加工艺已配置，提交后按母卷件数生成整理成品", preview.getSummary());
    }

    @Test
    void serviceOnlyPreview_withoutServiceStep_isBlocked() {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(4);

        var preview = mapper.serviceOnlyPreview(plan, "roll-1", 1, false);

        assertFalse(preview.isReady());
        assertEquals(1, preview.getErrors().size());
    }

    private ProcessPlanDTO layeredPlan() {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(1);
        plan.setMainStepType(2);
        plan.setRewindMode(4);
        plan.setSegments(List.of(layeredSegment()));
        return plan;
    }

    private RewindSegmentPlanDTO layeredSegment() {
        RewindSegmentPlanDTO segment = new RewindSegmentPlanDTO();
        segment.setSegmentSort(1);
        segment.setLayoutItems(List.of(layeredItem()));
        return segment;
    }

    private RewindLayoutItemPlanDTO layeredItem() {
        RewindLayoutItemPlanDTO item = new RewindLayoutItemPlanDTO();
        item.setWidth(1600);
        item.setQuantity(1);
        item.setItemType("FINISH");
        item.setLayers(List.of(layer(30, 3), layer(18, 6)));
        return item;
    }

    private FinishConfigSpecDTO.FinishLayerDTO layer(int outDiameter, int coreDiameter) {
        FinishConfigSpecDTO.FinishLayerDTO layer = new FinishConfigSpecDTO.FinishLayerDTO();
        layer.setOutDiameter(outDiameter);
        layer.setCoreDiameter(coreDiameter);
        return layer;
    }
}
