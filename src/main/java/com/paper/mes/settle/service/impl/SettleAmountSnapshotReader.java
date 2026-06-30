package com.paper.mes.settle.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 结算金额读取规则：历史结算单以快照金额为准，旧数据无快照时才按明细兜底计算。
 */
final class SettleAmountSnapshotReader {

    private static final int MONEY_SCALE = 2;

    private SettleAmountSnapshotReader() {
    }

    static Amounts resolve(SettleOrder settle, List<SettleDetail> details, ObjectMapper objectMapper) {
        JsonNode root = snapshotRoot(settle.getSnapBill(), objectMapper);
        if (root != null) {
            return new Amounts(
                    money(decimalValue(root, "amount_no_tax", "amountNoTax", settle.getAmountNoTax())),
                    money(decimalValue(root, "tax_amount", "taxAmount", settle.getTaxAmount())),
                    money(decimalValue(root, "total_amount", "totalAmount", settle.getTotalAmount())));
        }
        BigDecimal noTax = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (SettleDetail detail : details) {
            noTax = noTax.add(detailBaseAmount(detail));
            total = total.add(nz(detail.getOrderAmount()));
        }
        return new Amounts(money(noTax), money(total.subtract(noTax)), money(total));
    }

    private static BigDecimal detailBaseAmount(SettleDetail detail) {
        return nz(detail.getSawAmount())
                .add(nz(detail.getRewindAmount()))
                .add(nz(detail.getExtraAmount()))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static JsonNode snapshotRoot(String json, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BigDecimal decimalValue(JsonNode root, String snakeName, String camelName, BigDecimal fallback) {
        JsonNode node = root.get(snakeName);
        if (node == null || node.isNull()) {
            node = root.get(camelName);
        }
        if (node == null || node.isNull()) {
            return fallback;
        }
        try {
            return node.isNumber() ? node.decimalValue() : new BigDecimal(node.asText());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static BigDecimal money(BigDecimal amount) {
        return nz(amount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    record Amounts(BigDecimal noTax, BigDecimal tax, BigDecimal total) {
    }
}
