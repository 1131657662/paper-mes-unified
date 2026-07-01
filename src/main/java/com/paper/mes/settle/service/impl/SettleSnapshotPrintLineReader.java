package com.paper.mes.settle.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads frozen settle print-line snapshots from current camelCase fields and legacy snake_case fields.
 */
final class SettleSnapshotPrintLineReader {

    private SettleSnapshotPrintLineReader() {
    }

    static List<SettlePrintLineVO> read(String snapBill, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(snapBill)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(snapBill);
            for (String name : List.of("print_line_items", "printLineItems", "print_lines", "printLines")) {
                List<SettlePrintLineVO> lines = readNode(root.get(name), objectMapper);
                if (hasUsableSnapshotLines(lines)) {
                    return lines;
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<SettlePrintLineVO> readNode(JsonNode node, ObjectMapper objectMapper) {
        if (node == null || !node.isArray()) {
            return null;
        }
        List<SettlePrintLineVO> lines = readCamelCaseLines(node, objectMapper);
        if (hasUsableSnapshotLines(lines)) {
            return lines;
        }
        return readSnakeCaseLines(node);
    }

    private static List<SettlePrintLineVO> readCamelCaseLines(JsonNode node, ObjectMapper objectMapper) {
        try {
            return objectMapper.convertValue(node, new TypeReference<List<SettlePrintLineVO>>() {
            });
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static List<SettlePrintLineVO> readSnakeCaseLines(JsonNode node) {
        List<SettlePrintLineVO> lines = new ArrayList<>(node.size());
        for (JsonNode itemNode : node) {
            SettlePrintLineVO line = new SettlePrintLineVO();
            line.setSettleUuid(text(itemNode, "settle_uuid"));
            line.setOrderUuid(text(itemNode, "order_uuid"));
            line.setOrderNo(text(itemNode, "order_no"));
            line.setOrderDate(date(itemNode, "order_date"));
            line.setOriginalUuid(text(itemNode, "original_uuid"));
            line.setOriginalLabel(text(itemNode, "original_label"));
            line.setOriginalRollNo(text(itemNode, "original_roll_no"));
            line.setOriginalExtraNo(text(itemNode, "original_extra_no"));
            line.setPaperName(text(itemNode, "paper_name"));
            line.setGramWeight(integer(itemNode, "gram_weight"));
            line.setActualGramWeight(integer(itemNode, "actual_gram_weight"));
            line.setOriginalWidth(integer(itemNode, "original_width"));
            line.setActualWidth(integer(itemNode, "actual_width"));
            line.setOriginalDiameter(integer(itemNode, "original_diameter"));
            line.setCoreDiameter(integer(itemNode, "core_diameter"));
            line.setOriginalLength(integer(itemNode, "original_length"));
            line.setOriginalWeight(decimal(itemNode, "original_weight"));
            line.setProcessMode(integer(itemNode, "process_mode"));
            line.setMainStepType(integer(itemNode, "main_step_type"));
            line.setMachineUuid(text(itemNode, "machine_uuid"));
            line.setMachineName(text(itemNode, "machine_name"));
            line.setProcessText(text(itemNode, "process_text"));
            line.setProcessStepSummary(text(itemNode, "process_step_summary"));
            line.setFinishSummary(text(itemNode, "finish_summary"));
            line.setFinishDetailSummary(text(itemNode, "finish_detail_summary"));
            line.setFinishCount(integer(itemNode, "finish_count"));
            line.setFinishWeight(decimal(itemNode, "finish_weight"));
            line.setTrimWeight(decimal(itemNode, "trim_weight"));
            line.setTrimSummary(text(itemNode, "trim_summary"));
            line.setSawWeight(decimal(itemNode, "saw_weight"));
            line.setRewindWeight(decimal(itemNode, "rewind_weight"));
            line.setSawUnitPrice(decimal(itemNode, "saw_unit_price"));
            line.setSawInvoiceUnitPrice(decimal(itemNode, "saw_invoice_unit_price"));
            line.setRewindUnitPrice(decimal(itemNode, "rewind_unit_price"));
            line.setRewindInvoiceUnitPrice(decimal(itemNode, "rewind_invoice_unit_price"));
            line.setSawAmount(decimal(itemNode, "saw_amount"));
            line.setRewindAmount(decimal(itemNode, "rewind_amount"));
            line.setProcessAmount(decimal(itemNode, "process_amount"));
            line.setExtraAmount(decimal(itemNode, "extra_amount"));
            line.setExtraFeeSummary(text(itemNode, "extra_fee_summary"));
            line.setTaxAmount(decimal(itemNode, "tax_amount"));
            line.setTaxRate(decimal(itemNode, "tax_rate"));
            line.setLineAmount(decimal(itemNode, "line_amount"));
            line.setIsInvoice(integer(itemNode, "is_invoice"));
            line.setRemark(text(itemNode, "remark"));
            lines.add(line);
        }
        return lines;
    }

    private static boolean hasUsableSnapshotLines(List<SettlePrintLineVO> lines) {
        return lines != null && !lines.isEmpty()
                && lines.stream().anyMatch(line -> StringUtils.hasText(line.getOrderUuid())
                || StringUtils.hasText(line.getOrderNo()));
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Integer integer(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return value.isNumber() ? value.asInt() : Integer.valueOf(value.asText());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static BigDecimal decimal(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asText());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static LocalDate date(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return value.isArray() && value.size() >= 3
                    ? LocalDate.of(value.get(0).asInt(), value.get(1).asInt(), value.get(2).asInt())
                    : LocalDate.parse(value.asText());
        } catch (Exception ignored) {
            return null;
        }
    }
}
