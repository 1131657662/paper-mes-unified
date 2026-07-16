package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.paper.mes.settle.dto.SettleListSummaryVO;
import com.paper.mes.settle.dto.SettleQuery;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettleListSummaryService {

    private final SettleOrderMapper settleOrderMapper;

    public SettleListSummaryVO summarize(SettleQuery query) {
        SettleSummaryAccumulator summary = new SettleSummaryAccumulator();
        for (Map<String, Object> row : settleOrderMapper.selectMaps(summaryQuery(query))) {
            summary.add(row);
        }
        return summary.toView();
    }

    private QueryWrapper<SettleOrder> summaryQuery(SettleQuery query) {
        QueryWrapper<SettleOrder> wrapper = new QueryWrapper<>();
        wrapper.select("settle_status AS status", "COUNT(*) AS document_count",
                "COALESCE(SUM(total_amount), 0) AS total_amount",
                "COALESCE(SUM(received_amount), 0) AS received_amount",
                "COALESCE(SUM(unreceived_amount), 0) AS unreceived_amount",
                "COALESCE(SUM(discount_amount), 0) AS discount_amount");
        applyFilters(wrapper, query);
        return wrapper.groupBy("settle_status");
    }

    private void applyFilters(QueryWrapper<SettleOrder> wrapper, SettleQuery query) {
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like("settle_no", keyword).or().like("customer_name", keyword));
        }
        wrapper.eq(StringUtils.hasText(query.getCustomerUuid()), "customer_uuid", query.getCustomerUuid());
        wrapper.eq(query.getSettleType() != null, "settle_type", query.getSettleType());
        wrapper.ge(query.getDateFrom() != null, "settle_date", query.getDateFrom());
        wrapper.le(query.getDateTo() != null, "settle_date", query.getDateTo());
    }

    private static final class SettleSummaryAccumulator {
        private long totalDocuments;
        private long pendingDocuments;
        private long partialDocuments;
        private long paidDocuments;
        private long voidDocuments;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private BigDecimal receivedAmount = BigDecimal.ZERO;
        private BigDecimal unreceivedAmount = BigDecimal.ZERO;
        private BigDecimal discountAmount = BigDecimal.ZERO;

        void add(Map<String, Object> row) {
            int status = intValue(row, "status");
            long documents = longValue(row, "document_count");
            totalDocuments += documents;
            if (status == 1) pendingDocuments += documents;
            if (status == 2) partialDocuments += documents;
            if (status == 3) paidDocuments += documents;
            if (status == 4) voidDocuments += documents;
            if (status >= 1 && status <= 3) addActiveAmounts(row);
        }

        private void addActiveAmounts(Map<String, Object> row) {
            totalAmount = totalAmount.add(decimalValue(row, "total_amount"));
            receivedAmount = receivedAmount.add(decimalValue(row, "received_amount"));
            unreceivedAmount = unreceivedAmount.add(decimalValue(row, "unreceived_amount"));
            discountAmount = discountAmount.add(decimalValue(row, "discount_amount"));
        }

        SettleListSummaryVO toView() {
            return new SettleListSummaryVO(totalDocuments, pendingDocuments, partialDocuments, paidDocuments,
                    voidDocuments, totalAmount, receivedAmount, unreceivedAmount, discountAmount);
        }
    }

    private static int intValue(Map<String, Object> row, String key) {
        return ((Number) row.getOrDefault(key, 0)).intValue();
    }

    private static long longValue(Map<String, Object> row, String key) {
        return ((Number) row.getOrDefault(key, 0)).longValue();
    }

    private static BigDecimal decimalValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
