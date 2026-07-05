package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import com.paper.mes.processorder.dto.RewindSourcePlanDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProcessPlanMapper {

    public FinishConfigSaveDTO toSaveDto(ProcessPlanDTO plan) {
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        dto.setProcessMode(plan.getProcessMode());
        dto.setMainStepType(plan.getMainStepType());
        dto.setMachineUuid(plan.getMachineUuid());
        dto.setSpareCount(plan.getSpareCount());
        dto.setRewindMode(plan.getRewindMode());
        dto.setKnifeCount(plan.getKnifeCount());
        dto.setUnitPrice(plan.getUnitPrice());
        dto.setFinishSpecs(plan.getFinishSpecs());
        dto.setRewindSegments(toPreviewSegments(plan.getSegments()));
        return dto;
    }

    public ProcessPlanDTO fromSaveDto(FinishConfigSaveDTO dto) {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(dto.getProcessMode());
        plan.setMainStepType(dto.getMainStepType());
        plan.setMachineUuid(dto.getMachineUuid());
        plan.setSpareCount(dto.getSpareCount());
        plan.setRewindMode(dto.getRewindMode());
        plan.setKnifeCount(dto.getKnifeCount());
        plan.setUnitPrice(dto.getUnitPrice());
        plan.setFinishSpecs(dto.getFinishSpecs());
        plan.setSegments(fromPreviewSegments(dto.getRewindSegments()));
        return plan;
    }

    public RewindPlanPreviewDTO toPreviewDto(ProcessPlanDTO plan) {
        RewindPlanPreviewDTO dto = new RewindPlanPreviewDTO();
        dto.setRewindMode(plan.getRewindMode());
        dto.setSpareCount(plan.getSpareCount());
        dto.setSegments(toPreviewSegments(plan.getSegments()));
        return dto;
    }

    public PlanPreviewVO toPlanPreview(ProcessPlanDTO plan, String originalUuid, FinishPreviewVO preview) {
        PlanPreviewVO vo = shell(plan, originalUuid);
        vo.setFinishCount(preview.getFinishCount());
        vo.setTrimCount(preview.getTrimCount());
        vo.setTotalEstimateWeight(preview.getTotalEstimateWeight());
        vo.setTotalTrimWeight(preview.getTotalTrimWeight());
        vo.setSegments(nullToEmpty(preview.getSegments()));
        vo.setFinishes(nullToEmpty(preview.getFinishes()));
        vo.setReady(true);
        vo.setSummary(summary(vo));
        return vo;
    }

    public PlanPreviewVO directPreview(ProcessPlanDTO plan, String originalUuid) {
        PlanPreviewVO vo = shell(plan, originalUuid);
        vo.setFinishCount(0);
        vo.setTrimCount(0);
        vo.setReady(true);
        vo.setSummary("直发卷无需工艺配置，提交后回录阶段沿用母卷号");
        return vo;
    }

    private PlanPreviewVO shell(ProcessPlanDTO plan, String originalUuid) {
        PlanPreviewVO vo = new PlanPreviewVO();
        vo.setOriginalUuid(originalUuid);
        vo.setProcessMode(plan.getProcessMode());
        vo.setMainStepType(plan.getMainStepType());
        vo.setRewindMode(plan.getRewindMode());
        vo.setSpareCount(plan.getSpareCount() == null ? 0 : plan.getSpareCount());
        return vo;
    }

    private List<RewindPlanPreviewDTO.RewindSegmentDTO> toPreviewSegments(List<RewindSegmentPlanDTO> segments) {
        if (CollectionUtils.isEmpty(segments)) {
            return List.of();
        }
        List<RewindPlanPreviewDTO.RewindSegmentDTO> result = new ArrayList<>();
        for (RewindSegmentPlanDTO segment : segments) {
            RewindPlanPreviewDTO.RewindSegmentDTO dto = new RewindPlanPreviewDTO.RewindSegmentDTO();
            dto.setSegmentSort(segment.getSegmentSort());
            dto.setSegmentRatio(segment.getSegmentRatio());
            dto.setTargetDiameter(segment.getTargetDiameter());
            dto.setFinishCoreDiameter(segment.getFinishCoreDiameter());
            dto.setRepeatCount(segment.getRepeatCount());
            dto.setSources(toFinishSources(segment.getSources()));
            dto.setLayoutItems(toLayoutItems(segment.getLayoutItems()));
            result.add(dto);
        }
        return result;
    }

    private List<RewindSegmentPlanDTO> fromPreviewSegments(List<RewindPlanPreviewDTO.RewindSegmentDTO> segments) {
        if (CollectionUtils.isEmpty(segments)) {
            return List.of();
        }
        List<RewindSegmentPlanDTO> result = new ArrayList<>();
        for (RewindPlanPreviewDTO.RewindSegmentDTO segment : segments) {
            RewindSegmentPlanDTO dto = new RewindSegmentPlanDTO();
            dto.setSegmentSort(segment.getSegmentSort());
            dto.setSegmentRatio(segment.getSegmentRatio());
            dto.setTargetDiameter(segment.getTargetDiameter());
            dto.setFinishCoreDiameter(segment.getFinishCoreDiameter());
            dto.setRepeatCount(segment.getRepeatCount());
            dto.setSources(fromFinishSources(segment.getSources()));
            dto.setLayoutItems(fromLayoutItems(segment.getLayoutItems()));
            result.add(dto);
        }
        return result;
    }

    private List<FinishConfigSpecDTO.FinishSourceDTO> toFinishSources(List<RewindSourcePlanDTO> sources) {
        if (CollectionUtils.isEmpty(sources)) {
            return List.of();
        }
        List<FinishConfigSpecDTO.FinishSourceDTO> result = new ArrayList<>();
        for (RewindSourcePlanDTO source : sources) {
            FinishConfigSpecDTO.FinishSourceDTO dto = new FinishConfigSpecDTO.FinishSourceDTO();
            dto.setOriginalUuid(source.getOriginalUuid());
            dto.setShareRatio(source.getShareRatio());
            dto.setConsumeRatio(source.getConsumeRatio());
            result.add(dto);
        }
        return result;
    }

    private List<RewindSourcePlanDTO> fromFinishSources(List<FinishConfigSpecDTO.FinishSourceDTO> sources) {
        if (CollectionUtils.isEmpty(sources)) {
            return List.of();
        }
        List<RewindSourcePlanDTO> result = new ArrayList<>();
        int sort = 1;
        for (FinishConfigSpecDTO.FinishSourceDTO source : sources) {
            RewindSourcePlanDTO dto = new RewindSourcePlanDTO();
            dto.setOriginalUuid(source.getOriginalUuid());
            dto.setShareRatio(source.getShareRatio());
            dto.setConsumeRatio(source.getConsumeRatio());
            dto.setSourceSort(sort++);
            result.add(dto);
        }
        return result;
    }

    private List<RewindPlanPreviewDTO.RewindLayoutItemDTO> toLayoutItems(List<RewindLayoutItemPlanDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return List.of();
        }
        List<RewindPlanPreviewDTO.RewindLayoutItemDTO> result = new ArrayList<>();
        for (RewindLayoutItemPlanDTO item : items) {
            RewindPlanPreviewDTO.RewindLayoutItemDTO dto = new RewindPlanPreviewDTO.RewindLayoutItemDTO();
            dto.setWidth(item.getWidth());
            dto.setQuantity(item.getQuantity());
            dto.setItemType(item.getItemType());
            dto.setLayers(item.getLayers());
            result.add(dto);
        }
        return result;
    }

    private List<RewindLayoutItemPlanDTO> fromLayoutItems(List<RewindPlanPreviewDTO.RewindLayoutItemDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return List.of();
        }
        List<RewindLayoutItemPlanDTO> result = new ArrayList<>();
        for (RewindPlanPreviewDTO.RewindLayoutItemDTO item : items) {
            RewindLayoutItemPlanDTO dto = new RewindLayoutItemPlanDTO();
            dto.setWidth(item.getWidth());
            dto.setQuantity(item.getQuantity());
            dto.setItemType(item.getItemType());
            dto.setLayers(item.getLayers());
            result.add(dto);
        }
        return result;
    }

    private <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private String summary(PlanPreviewVO vo) {
        return "预计生成 " + vo.getFinishCount() + " 个正式号，"
                + vo.getSpareCount() + " 个备用号，修边 "
                + (vo.getTotalTrimWeight() == null ? BigDecimal.ZERO : vo.getTotalTrimWeight()) + " kg";
    }
}
