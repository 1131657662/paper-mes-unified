package com.paper.mes.settle.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 结算金额读取规则：历史结算单以快照金额为准，旧数据无快照时才按明细兜底计算。
 */
@Slf4j
final class SettleAmountSnapshotReader {

    private static final int MONEY_SCALE = 2;

    private SettleAmountSnapshotReader() {
    }

    static Amounts resolve(SettleOrder settle, List<SettleDetail> details, ObjectMapper objectMapper) {
        Amounts detailAmounts = amountsFromDetails(details);
        JsonNode root = snapshotRoot(settle.getSnapBill(), objectMapper);
        if (root == null) {
            return detailAmounts;
        }

        BigDecimal noTax = firstNonNull(decimalValue(root, "amount_no_tax", "amountNoTax"), settle.getAmountNoTax());
        BigDecimal tax = firstNonNull(decimalValue(root, "tax_amount", "taxAmount"), settle.getTaxAmount());
        BigDecimal total = firstNonNull(decimalValue(root, "total_amount", "totalAmount"), settle.getTotalAmount());
        return completeAmounts(noTax, tax, total, detailAmounts);
    }

    private static Amounts amountsFromDetails(List<SettleDetail> details) {
        BigDecimal noTax = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (SettleDetail detail : details == null ? List.<SettleDetail>of() : details) {
            noTax = noTax.add(detailBaseAmount(detail));
            total = total.add(nz(detail.getOrderAmount()));
        }
        return new Amounts(money(noTax), money(total.subtract(noTax)), money(total));
    }

    private static Amounts completeAmounts(BigDecimal noTax, BigDecimal tax, BigDecimal total, Amounts fallback) {
        if (total == null && noTax != null && tax != null) {
            total = noTax.add(tax);
        }
        if (tax == null && total != null && noTax != null) {
            tax = total.subtract(noTax);
        }
        if (noTax == null && total != null && tax != null) {
            noTax = total.subtract(tax);
        }
        return new Amounts(
                money(firstNonNull(noTax, fallback.noTax())),
                money(firstNonNull(tax, fallback.tax())),
                money(firstNonNull(total, fallback.total())));
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
        } catch (Exception ex) {
            log.warn("结算金额快照解析失败，将按结算明细兜底计算：{}", ex.getMessage());
            return null;
        }
    }

    private static BigDecimal decimalValue(JsonNode root, String snakeName, String camelName) {
        JsonNode node = root.get(snakeName);
        if (node == null || node.isNull()) {
            node = root.get(camelName);
        }
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return node.isNumber() ? node.decimalValue() : new BigDecimal(node.asText());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BigDecimal firstNonNull(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
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
