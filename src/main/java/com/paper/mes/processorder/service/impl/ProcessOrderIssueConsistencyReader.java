package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.processorder.dto.PrintViewVersion;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.dto.ProcessOrderIssueConsistencyVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Calculates the semantic gap without treating back-record facts as a print-version mismatch. */
final class ProcessOrderIssueConsistencyReader {

    private ProcessOrderIssueConsistencyReader() {
    }

    static ProcessOrderIssueConsistencyVO read(ProcessOrderDetailVO live, ObjectMapper objectMapper) {
        ProcessOrderIssueConsistencyVO result = new ProcessOrderIssueConsistencyVO();
        Integer status = live.getOrder().getOrderStatus();
        if (status == null || status < 2 || status > 5) {
            result.setStatus("NOT_APPLICABLE");
            result.setChangedGroups(List.of());
            return result;
        }
        if (live.getOrder().getSnapPrint() == null || live.getOrder().getSnapPrint().isBlank()) {
            result.setStatus("PENDING_REISSUE");
            result.setChangedGroups(List.of());
            result.setBlockingReason("当前加工单没有可用的下发快照，请先完成下发或联系管理员处理");
            return result;
        }
        ProcessOrderDetailVO issued = ProcessOrderPrintViewReader.read(
                live, PrintViewVersion.ISSUED, objectMapper).getDetail();
        List<String> groups = changedGroups(live, issued);
        result.setChangedGroups(groups);
        result.setStatus(groups.isEmpty() ? "IN_SYNC" : "REISSUE_REQUIRED");
        if (!groups.isEmpty()) {
            result.setBlockingReason("当前数据已不同于下发版本；旧版打印仅用于追溯，不得作为当前生产指令");
        }
        return result;
    }

    private static List<String> changedGroups(ProcessOrderDetailVO live, ProcessOrderDetailVO issued) {
        List<String> groups = new ArrayList<>();
        if (!Objects.equals(live.getOrder().getRemark(), issued.getOrder().getRemark())
                || !Objects.equals(live.getOrder().getRemarkLong(), issued.getOrder().getRemarkLong())) {
            groups.add("生产备注");
        }
        if (printedCustomerSpecificationChanged(live.getFinishRolls(), issued.getFinishRolls())) {
            groups.add("客户品名/克重/门幅");
        }
        if (rollDispositionChanged(live.getOriginalRolls(), issued.getOriginalRolls())) {
            groups.add("母卷处置");
        }
        return List.copyOf(groups);
    }

    private static boolean rollDispositionChanged(List<OriginalRoll> live, List<OriginalRoll> issued) {
        Map<String, OriginalRoll> issuedByUuid = issued.stream().collect(Collectors.toMap(
                OriginalRoll::getUuid, Function.identity(), (left, right) -> left));
        for (OriginalRoll current : live) {
            OriginalRoll frozen = issuedByUuid.get(current.getUuid());
            if (frozen == null) {
                if (current.getDispositionAction() != null) return true;
                continue;
            }
            if (!Objects.equals(current.getDispositionAction(), frozen.getDispositionAction())) return true;
        }
        return false;
    }

    private static boolean printedCustomerSpecificationChanged(
            List<FinishRoll> live, List<FinishRoll> issued) {
        Map<String, FinishRoll> issuedByUuid = issued.stream().collect(Collectors.toMap(
                FinishRoll::getUuid, Function.identity(), (left, right) -> left));
        for (FinishRoll current : live) {
            FinishRoll frozen = issuedByUuid.get(current.getUuid());
            if (frozen != null && !samePrintedCustomerSpecification(current, frozen)) return true;
        }
        return false;
    }

    private static boolean samePrintedCustomerSpecification(FinishRoll left, FinishRoll right) {
        return Objects.equals(left.getCustomerPaperName(), right.getCustomerPaperName())
                && Objects.equals(left.getCustomerGramWeight(), right.getCustomerGramWeight())
                && Objects.equals(left.getCustomerFinishWidth(), right.getCustomerFinishWidth());
    }
}
