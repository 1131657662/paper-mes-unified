package com.paper.mes.settle.service;

import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.settle.dto.SettleQuoteLineVO;
import com.paper.mes.settle.dto.SettleQuoteVO;
import com.paper.mes.settle.entity.SettleDetail;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SettlementQuoteFactory {
    public static final String VERSION = "settlement-quote-v1";

    public SettleQuoteVO create(List<ProcessOrder> orders, Integer isInvoice,
                                SettlementAmountCalculator.Calculation calculation) {
        List<SettleQuoteLineVO> lines = quoteLines(orders, calculation.details());
        String hash = hash(orders, isInvoice, lines);
        return new SettleQuoteVO(VERSION, hash, orders.size(), calculation.pendingPriceCount(), isInvoice,
                calculation.saw(), calculation.rewind(), calculation.extra(), calculation.noTax(),
                calculation.tax(), calculation.total(), lines);
    }

    private List<SettleQuoteLineVO> quoteLines(List<ProcessOrder> orders, List<SettleDetail> details) {
        Map<String, SettleDetail> byOrder = new LinkedHashMap<>();
        details.forEach(detail -> byOrder.put(detail.getOrderUuid(), detail));
        return orders.stream().map(order -> quoteLine(order, byOrder.get(order.getUuid()))).toList();
    }

    private SettleQuoteLineVO quoteLine(ProcessOrder order, SettleDetail detail) {
        BigDecimal saw = money(detail.getSawAmount());
        BigDecimal rewind = money(detail.getRewindAmount());
        BigDecimal standardProcess = money(detail.getStandardProcessAmount());
        BigDecimal pricingAdjustment = money(detail.getPricingAdjustmentAmount());
        BigDecimal extra = money(detail.getExtraAmount());
        BigDecimal noTax = order.getTotalAmountNoTax() == null
                ? saw.add(rewind).add(extra) : money(order.getTotalAmountNoTax());
        BigDecimal total = money(detail.getOrderAmount());
        return new SettleQuoteLineVO(order.getUuid(), saw, rewind, standardProcess, pricingAdjustment,
                extra, noTax,
                total.subtract(noTax).setScale(2, RoundingMode.HALF_UP), total);
    }

    private String hash(List<ProcessOrder> orders, Integer isInvoice, List<SettleQuoteLineVO> lines) {
        StringBuilder canonical = new StringBuilder(VERSION).append('|').append(isInvoice);
        Map<String, ProcessOrder> ordersByUuid = new LinkedHashMap<>();
        orders.forEach(order -> ordersByUuid.put(order.getUuid(), order));
        lines.stream().sorted((a, b) -> a.getOrderUuid().compareTo(b.getOrderUuid()))
                .forEach(line -> appendLine(canonical, ordersByUuid.get(line.getOrderUuid()), line));
        return sha256(canonical.toString());
    }

    private void appendLine(StringBuilder value, ProcessOrder order, SettleQuoteLineVO line) {
        value.append('|').append(line.getOrderUuid())
                .append(':').append(order.getVersion())
                .append(':').append(order.getOrderStatus())
                .append(':').append(money(line.getSawAmount()))
                .append(':').append(money(line.getRewindAmount()))
                .append(':').append(money(line.getStandardProcessAmount()))
                .append(':').append(money(line.getPricingAdjustmentAmount()))
                .append(':').append(money(line.getExtraAmount()))
                .append(':').append(money(line.getAmountNoTax()))
                .append(':').append(money(line.getTaxAmount()))
                .append(':').append(money(line.getTotalAmount()));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
