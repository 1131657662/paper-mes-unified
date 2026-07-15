package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;

import java.util.HashSet;
import java.util.Set;

import static com.paper.mes.processorder.service.impl.LegacySnapshotValues.dateTime;
import static com.paper.mes.processorder.service.impl.LegacySnapshotValues.decimal;
import static com.paper.mes.processorder.service.impl.LegacySnapshotValues.integer;
import static com.paper.mes.processorder.service.impl.LegacySnapshotValues.text;

/** 旧版 1.x 快照覆盖到受保护的当前详情结构。 */
final class LegacyProcessOrderSnapshotOverlay {

    private LegacyProcessOrderSnapshotOverlay() {
    }

    static ProcessOrderDetailVO issued(ProcessOrderDetailVO live, JsonNode root,
                                       ObjectMapper objectMapper) {
        ProcessOrderDetailVO detail = cleanCopy(live, objectMapper);
        applyOrder(detail.getOrder(), root, "print_time", "print_user");
        applyIssuedOriginals(detail, root.path("original_rolls"));
        applyIssuedFinishes(detail, root.path("finish_rolls"));
        clearActualValues(detail);
        return detail;
    }

    static ProcessOrderDetailVO finished(ProcessOrderDetailVO live, JsonNode root,
                                         ObjectMapper objectMapper) {
        ProcessOrderDetailVO detail = cleanCopy(live, objectMapper);
        applyOrder(detail.getOrder(), root, "back_record_time", "back_record_user");
        applyFinishedOriginals(detail, root.path("original_rolls"));
        applyFinishedFinishes(detail, root.path("finish_rolls"));
        return detail;
    }

    private static ProcessOrderDetailVO cleanCopy(ProcessOrderDetailVO live, ObjectMapper objectMapper) {
        ProcessOrderDetailVO detail = ProcessOrderSnapshotDetailCodec.copy(live, objectMapper);
        detail.getOrder().setSnapPrint(null);
        detail.getOrder().setSnapFinish(null);
        return detail;
    }

    private static void applyOrder(ProcessOrder order, JsonNode root, String timeKey, String userKey) {
        order.setOrderNo(text(root, "order_no", order.getOrderNo()));
        order.setCustomerName(text(root, "customer_name", order.getCustomerName()));
        if ("print_time".equals(timeKey)) {
            order.setLastPrintTime(dateTime(root.get(timeKey), order.getLastPrintTime()));
            order.setLastPrintUser(text(root, userKey, order.getLastPrintUser()));
            order.setPrintCount(integer(root.get("print_count"), order.getPrintCount()));
        } else {
            order.setBackRecordTime(dateTime(root.get(timeKey), order.getBackRecordTime()));
            order.setBackRecordUser(text(root, userKey, order.getBackRecordUser()));
        }
    }

    private static void applyIssuedOriginals(ProcessOrderDetailVO detail, JsonNode nodes) {
        if (!nodes.isArray()) return;
        for (JsonNode node : nodes) {
            String uuid = text(node, "uuid", null);
            detail.getOriginalRolls().stream().filter(item -> item.getUuid().equals(uuid))
                    .findFirst().ifPresent(item -> applyIssuedOriginal(item, node));
            detail.getRollProductions().stream().filter(item -> item.getOriginalUuid().equals(uuid))
                    .findFirst().ifPresent(item -> applyIssuedProduction(item, node));
        }
    }

    private static void applyIssuedOriginal(OriginalRoll roll, JsonNode node) {
        roll.setRollNo(text(node, "roll_no", roll.getRollNo()));
        roll.setPaperName(text(node, "paper_name", roll.getPaperName()));
        roll.setGramWeight(integer(node.get("gram_weight"), roll.getGramWeight()));
        roll.setOriginalWidth(integer(node.get("original_width"), roll.getOriginalWidth()));
        roll.setOriginalDiameter(integer(node.get("original_diameter"), roll.getOriginalDiameter()));
        roll.setCoreDiameter(integer(node.get("core_diameter"), roll.getCoreDiameter()));
        roll.setRollWeight(decimal(node.get("roll_weight"), roll.getRollWeight()));
        roll.setPieceNum(integer(node.get("piece_num"), roll.getPieceNum()));
        roll.setProcessMode(integer(node.get("process_mode"), roll.getProcessMode()));
        roll.setMainStepType(integer(node.get("main_step_type"), roll.getMainStepType()));
    }

    private static void applyIssuedProduction(ProcessOrderDetailVO.RollProductionVO item, JsonNode node) {
        item.setRollNo(text(node, "roll_no", item.getRollNo()));
        item.setPaperName(text(node, "paper_name", item.getPaperName()));
        item.setGramWeight(integer(node.get("gram_weight"), item.getGramWeight()));
        item.setOriginalWidth(integer(node.get("original_width"), item.getOriginalWidth()));
        item.setRollWeight(decimal(node.get("roll_weight"), item.getRollWeight()));
        item.setPieceNum(integer(node.get("piece_num"), item.getPieceNum()));
        item.setProcessMode(integer(node.get("process_mode"), item.getProcessMode()));
        item.setMainStepType(integer(node.get("main_step_type"), item.getMainStepType()));
    }

    private static void applyIssuedFinishes(ProcessOrderDetailVO detail, JsonNode nodes) {
        if (!nodes.isArray()) return;
        Set<String> ids = new HashSet<>();
        nodes.forEach(node -> ids.add(text(node, "uuid", "")));
        detail.setFinishRolls(detail.getFinishRolls().stream()
                .filter(item -> ids.contains(item.getUuid())).toList());
        for (ProcessOrderDetailVO.RollProductionVO production : detail.getRollProductions()) {
            production.setFinishes(production.getFinishes().stream()
                    .filter(item -> ids.contains(item.getUuid())).toList());
        }
        for (JsonNode node : nodes) {
            String uuid = text(node, "uuid", null);
            detail.getFinishRolls().stream().filter(item -> item.getUuid().equals(uuid))
                    .findFirst().ifPresent(item -> applyIssuedFinish(item, node));
            detail.getRollProductions().stream().flatMap(item -> item.getFinishes().stream())
                    .filter(item -> item.getUuid().equals(uuid)).forEach(item -> applyIssuedProductionFinish(item, node));
        }
    }

    private static void applyIssuedFinish(FinishRoll finish, JsonNode node) {
        finish.setFinishRollNo(text(node, "finish_roll_no", finish.getFinishRollNo()));
        finish.setPaperName(text(node, "paper_name", finish.getPaperName()));
        finish.setFinishWidth(integer(node.get("finish_width"), finish.getFinishWidth()));
        finish.setFinishDiameter(integer(node.get("finish_diameter"), finish.getFinishDiameter()));
        finish.setFinishCoreDiameter(integer(node.get("finish_core_diameter"), finish.getFinishCoreDiameter()));
        finish.setEstimateWeight(decimal(node.get("estimate_weight"), finish.getEstimateWeight()));
        finish.setIsSpare(integer(node.get("is_spare"), finish.getIsSpare()));
        finish.setIsRemain(integer(node.get("is_remain"), finish.getIsRemain()));
    }

    private static void applyIssuedProductionFinish(ProcessOrderDetailVO.FinishProductionVO finish,
                                                     JsonNode node) {
        finish.setFinishRollNo(text(node, "finish_roll_no", finish.getFinishRollNo()));
        finish.setPaperName(text(node, "paper_name", finish.getPaperName()));
        finish.setFinishWidth(integer(node.get("finish_width"), finish.getFinishWidth()));
        finish.setFinishDiameter(integer(node.get("finish_diameter"), finish.getFinishDiameter()));
        finish.setFinishCoreDiameter(integer(node.get("finish_core_diameter"), finish.getFinishCoreDiameter()));
        finish.setEstimateWeight(decimal(node.get("estimate_weight"), finish.getEstimateWeight()));
        finish.setIsSpare(integer(node.get("is_spare"), finish.getIsSpare()));
        finish.setIsRemain(integer(node.get("is_remain"), finish.getIsRemain()));
    }

    private static void applyFinishedOriginals(ProcessOrderDetailVO detail, JsonNode nodes) {
        if (!nodes.isArray()) return;
        for (JsonNode node : nodes) {
            String uuid = text(node, "uuid", null);
            detail.getOriginalRolls().stream().filter(item -> item.getUuid().equals(uuid)).findFirst()
                    .ifPresent(item -> {
                        item.setActualGramWeight(integer(node.get("actual_gram_weight"), item.getActualGramWeight()));
                        item.setActualWidth(integer(node.get("actual_width"), item.getActualWidth()));
                        item.setActualWeight(decimal(node.get("actual_weight"), item.getActualWeight()));
                    });
            detail.getRollProductions().stream().filter(item -> item.getOriginalUuid().equals(uuid)).findFirst()
                    .ifPresent(item -> {
                        item.setActualGramWeight(integer(node.get("actual_gram_weight"), item.getActualGramWeight()));
                        item.setActualWidth(integer(node.get("actual_width"), item.getActualWidth()));
                        item.setActualWeight(decimal(node.get("actual_weight"), item.getActualWeight()));
                    });
        }
    }

    private static void applyFinishedFinishes(ProcessOrderDetailVO detail, JsonNode nodes) {
        if (!nodes.isArray()) return;
        for (JsonNode node : nodes) {
            String uuid = text(node, "uuid", null);
            detail.getFinishRolls().stream().filter(item -> item.getUuid().equals(uuid)).findFirst()
                    .ifPresent(item -> applyFinishedFinish(item, node));
            detail.getRollProductions().stream().flatMap(item -> item.getFinishes().stream())
                    .filter(item -> item.getUuid().equals(uuid)).forEach(item -> applyFinishedProductionFinish(item, node));
        }
    }

    private static void applyFinishedFinish(FinishRoll finish, JsonNode node) {
        finish.setFinishWidth(integer(node.get("finish_width"), finish.getFinishWidth()));
        finish.setFinishDiameter(integer(node.get("finish_diameter"), finish.getFinishDiameter()));
        finish.setFinishCoreDiameter(integer(node.get("finish_core_diameter"), finish.getFinishCoreDiameter()));
        finish.setActualWeight(decimal(node.get("actual_weight"), finish.getActualWeight()));
        finish.setFinishStatus(integer(node.get("finish_status"), finish.getFinishStatus()));
        finish.setRollNoStatus(integer(node.get("roll_no_status"), finish.getRollNoStatus()));
    }

    private static void applyFinishedProductionFinish(ProcessOrderDetailVO.FinishProductionVO finish,
                                                       JsonNode node) {
        finish.setFinishWidth(integer(node.get("finish_width"), finish.getFinishWidth()));
        finish.setFinishDiameter(integer(node.get("finish_diameter"), finish.getFinishDiameter()));
        finish.setFinishCoreDiameter(integer(node.get("finish_core_diameter"), finish.getFinishCoreDiameter()));
        finish.setActualWeight(decimal(node.get("actual_weight"), finish.getActualWeight()));
        finish.setFinishStatus(integer(node.get("finish_status"), finish.getFinishStatus()));
        finish.setRollNoStatus(integer(node.get("roll_no_status"), finish.getRollNoStatus()));
    }

    private static void clearActualValues(ProcessOrderDetailVO detail) {
        detail.getOriginalRolls().forEach(item -> {
            item.setActualGramWeight(null);
            item.setActualWidth(null);
            item.setActualWeight(null);
        });
        detail.getRollProductions().forEach(item -> {
            item.setActualGramWeight(null);
            item.setActualWidth(null);
            item.setActualWeight(null);
            item.getFinishes().forEach(finish -> finish.setActualWeight(null));
            item.getStageOutputs().forEach(output -> output.setActualWeight(null));
        });
        detail.getFinishRolls().forEach(item -> item.setActualWeight(null));
    }

}
