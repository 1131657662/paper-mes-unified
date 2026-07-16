package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.paper.mes.delivery.dto.DeliveryListSummaryVO;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliveryListSummaryService {

    private final DeliveryOrderMapper deliveryOrderMapper;

    public DeliveryListSummaryVO summarize(DeliveryQuery query) {
        DeliverySummaryAccumulator summary = new DeliverySummaryAccumulator();
        for (Map<String, Object> row : deliveryOrderMapper.selectMaps(summaryQuery(query))) {
            summary.add(row);
        }
        return summary.toView();
    }

    private QueryWrapper<DeliveryOrder> summaryQuery(DeliveryQuery query) {
        QueryWrapper<DeliveryOrder> wrapper = new QueryWrapper<>();
        wrapper.select("delivery_status AS status", "COUNT(*) AS document_count",
                "COALESCE(SUM(total_count), 0) AS roll_count",
                "COALESCE(SUM(total_weight), 0) AS weight");
        applyFilters(wrapper, query);
        return wrapper.groupBy("delivery_status");
    }

    private void applyFilters(QueryWrapper<DeliveryOrder> wrapper, DeliveryQuery query) {
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like("delivery_no", keyword).or().like("customer_name", keyword));
        }
        wrapper.eq(StringUtils.hasText(query.getCustomerUuid()), "customer_uuid", query.getCustomerUuid());
        wrapper.ge(query.getDateFrom() != null, "delivery_date", query.getDateFrom());
        wrapper.le(query.getDateTo() != null, "delivery_date", query.getDateTo());
    }

    private static final class DeliverySummaryAccumulator {
        private long totalDocuments;
        private long pendingDocuments;
        private long deliveredDocuments;
        private long voidDocuments;
        private long activeRolls;
        private BigDecimal activeWeight = BigDecimal.ZERO;
        private BigDecimal pendingWeight = BigDecimal.ZERO;
        private BigDecimal deliveredWeight = BigDecimal.ZERO;

        void add(Map<String, Object> row) {
            int status = intValue(row, "status");
            long documents = longValue(row, "document_count");
            BigDecimal weight = decimalValue(row, "weight");
            totalDocuments += documents;
            if (status == 1) addPending(row, documents, weight);
            if (status == 2) addDelivered(row, documents, weight);
            if (status == 3) voidDocuments += documents;
        }

        private void addPending(Map<String, Object> row, long documents, BigDecimal weight) {
            pendingDocuments += documents;
            activeRolls += longValue(row, "roll_count");
            pendingWeight = pendingWeight.add(weight);
            activeWeight = activeWeight.add(weight);
        }

        private void addDelivered(Map<String, Object> row, long documents, BigDecimal weight) {
            deliveredDocuments += documents;
            activeRolls += longValue(row, "roll_count");
            deliveredWeight = deliveredWeight.add(weight);
            activeWeight = activeWeight.add(weight);
        }

        DeliveryListSummaryVO toView() {
            return new DeliveryListSummaryVO(totalDocuments, pendingDocuments, deliveredDocuments, voidDocuments,
                    activeRolls, activeWeight, pendingWeight, deliveredWeight);
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
