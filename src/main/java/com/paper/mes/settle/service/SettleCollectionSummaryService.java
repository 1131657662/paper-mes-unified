package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.paper.mes.settle.dto.SettleCollectionSummaryVO;
import com.paper.mes.settle.dto.SettleQuery;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettleCollectionSummaryService {
    private static final String NOT_REMINDED_TODAY =
            "(last_reminder_time IS NULL OR last_reminder_time < CURRENT_DATE())";
    private final SettleOrderMapper settleOrderMapper;

    public SettleCollectionSummaryVO summarize(SettleQuery query) {
        Map<String, Object> row = settleOrderMapper.selectMaps(summaryQuery(query)).stream()
                .findFirst().orElse(Map.of());
        return new SettleCollectionSummaryVO(
                longValue(row, "due_today_count"), decimalValue(row, "due_today_amount"),
                longValue(row, "overdue_count"), decimalValue(row, "overdue_amount"),
                longValue(row, "upcoming_count"), decimalValue(row, "upcoming_amount"),
                longValue(row, "reminded_today_count"), decimalValue(row, "reminded_today_amount"),
                LocalDateTime.now());
    }

    private QueryWrapper<SettleOrder> summaryQuery(SettleQuery query) {
        QueryWrapper<SettleOrder> wrapper = new QueryWrapper<>();
        wrapper.select(
                countCase(NOT_REMINDED_TODAY + " AND due_date = CURRENT_DATE()", "due_today_count"),
                amountCase(NOT_REMINDED_TODAY + " AND due_date = CURRENT_DATE()", "due_today_amount"),
                countCase(NOT_REMINDED_TODAY + " AND due_date < CURRENT_DATE()", "overdue_count"),
                amountCase(NOT_REMINDED_TODAY + " AND due_date < CURRENT_DATE()", "overdue_amount"),
                countCase(NOT_REMINDED_TODAY + " AND (due_date > CURRENT_DATE() OR due_date IS NULL)", "upcoming_count"),
                amountCase(NOT_REMINDED_TODAY + " AND (due_date > CURRENT_DATE() OR due_date IS NULL)", "upcoming_amount"),
                countCase("last_reminder_time >= CURRENT_DATE()", "reminded_today_count"),
                amountCase("last_reminder_time >= CURRENT_DATE()", "reminded_today_amount"));
        wrapper.in("settle_status", 1, 2).gt("unreceived_amount", BigDecimal.ZERO);
        applyFilters(wrapper, query);
        return wrapper;
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

    private String countCase(String condition, String alias) {
        return "COALESCE(SUM(CASE WHEN " + condition + " THEN 1 ELSE 0 END), 0) AS " + alias;
    }

    private String amountCase(String condition, String alias) {
        return "COALESCE(SUM(CASE WHEN " + condition + " THEN unreceived_amount ELSE 0 END), 0) AS " + alias;
    }

    private long longValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private BigDecimal decimalValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
