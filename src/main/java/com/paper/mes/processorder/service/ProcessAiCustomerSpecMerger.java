package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;

import java.util.List;
import java.util.Set;

/** Applies only explicitly confirmed AI customer sales fields to a physical process plan. */
final class ProcessAiCustomerSpecMerger {

    List<FinishConfigSpecDTO> copyFinishSpecsWithoutCustomerFields(ProcessPlanDTO source) {
        ProcessPlanDTO copy = ProcessPlanCopies.copy(source);
        clearFinishSpecs(copy.getFinishSpecs());
        return copy.getFinishSpecs();
    }

    List<RewindLayoutItemPlanDTO> copyLayoutItemsWithoutCustomerFields(RewindSegmentPlanDTO source) {
        RewindSegmentPlanDTO copy = ProcessPlanCopies.copySegment(source);
        clearLayoutItems(copy.getLayoutItems());
        return copy.getLayoutItems();
    }

    void apply(ProcessPlanDTO target, ProcessPlanDTO source, Set<String> accepted, String base) {
        String prefix = base + "/customerSpecs/";
        if (accepted.stream().noneMatch(path -> path.startsWith(prefix))) return;
        applyToFinishSpecs(target.getFinishSpecs(), source.getFinishSpecs(), accepted, prefix);
        applyToRewindLayouts(target.getSegments(), source.getSegments(), accepted, prefix);
    }

    private void applyToFinishSpecs(List<FinishConfigSpecDTO> target, List<FinishConfigSpecDTO> source,
                                    Set<String> accepted, String prefix) {
        if (target == null || source == null) return;
        for (int index = 0; index < Math.min(target.size(), source.size()); index++) {
            applyFields(target.get(index), source.get(index), accepted, prefix + index + "/");
        }
    }

    private void applyToRewindLayouts(List<RewindSegmentPlanDTO> target,
                                      List<RewindSegmentPlanDTO> source, Set<String> accepted, String prefix) {
        if (target == null || source == null) return;
        for (int index = 0; index < Math.min(target.size(), source.size()); index++) {
            applyLayoutItems(target.get(index).getLayoutItems(), source.get(index).getLayoutItems(), accepted, prefix);
        }
    }

    private void applyLayoutItems(List<RewindLayoutItemPlanDTO> target,
                                  List<RewindLayoutItemPlanDTO> source, Set<String> accepted, String prefix) {
        if (target == null || source == null) return;
        for (int index = 0; index < Math.min(target.size(), source.size()); index++) {
            applyFields(target.get(index), source.get(index), accepted, prefix + index + "/");
        }
    }

    private void applyFields(FinishConfigSpecDTO target, FinishConfigSpecDTO source,
                             Set<String> accepted, String prefix) {
        if (accepted.contains(prefix + "paperName")) target.setCustomerPaperName(source.getCustomerPaperName());
        if (accepted.contains(prefix + "gramWeight")) target.setCustomerGramWeight(source.getCustomerGramWeight());
        if (accepted.contains(prefix + "finishWidth")) target.setCustomerFinishWidth(source.getCustomerFinishWidth());
        if (accepted.contains(prefix + "overrideReason")) target.setCustomerSpecOverrideReason(source.getCustomerSpecOverrideReason());
    }

    private void applyFields(RewindLayoutItemPlanDTO target, RewindLayoutItemPlanDTO source,
                             Set<String> accepted, String prefix) {
        if (accepted.contains(prefix + "paperName")) target.setCustomerPaperName(source.getCustomerPaperName());
        if (accepted.contains(prefix + "gramWeight")) target.setCustomerGramWeight(source.getCustomerGramWeight());
        if (accepted.contains(prefix + "finishWidth")) target.setCustomerFinishWidth(source.getCustomerFinishWidth());
        if (accepted.contains(prefix + "overrideReason")) target.setCustomerSpecOverrideReason(source.getCustomerSpecOverrideReason());
    }

    private void clearFinishSpecs(List<FinishConfigSpecDTO> specs) {
        if (specs != null) specs.forEach(this::clearFields);
    }

    private void clearLayoutItems(List<RewindLayoutItemPlanDTO> items) {
        if (items != null) items.forEach(this::clearFields);
    }

    private void clearFields(FinishConfigSpecDTO item) {
        item.setCustomerPaperName(null);
        item.setCustomerGramWeight(null);
        item.setCustomerFinishWidth(null);
        item.setCustomerSpecOverrideReason(null);
    }

    private void clearFields(RewindLayoutItemPlanDTO item) {
        item.setCustomerPaperName(null);
        item.setCustomerGramWeight(null);
        item.setCustomerFinishWidth(null);
        item.setCustomerSpecOverrideReason(null);
    }
}
