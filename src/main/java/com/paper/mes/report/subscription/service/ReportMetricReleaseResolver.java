package com.paper.mes.report.subscription.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.report.subscription.entity.ReportSubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportMetricReleaseResolver {

    private final JdbcTemplate jdbcTemplate;

    public String resolve(ReportSubscription subscription) {
        if (Integer.valueOf(2).equals(subscription.getReleasePolicy())) {
            return requirePinned(subscription.getPinnedReleaseUuid());
        }
        List<String> releases = jdbcTemplate.queryForList(
                "SELECT uuid FROM rpt_metric_release WHERE is_deleted = 0 AND release_status = 2 "
                        + "ORDER BY COALESCE(published_at, create_time) DESC, uuid DESC LIMIT 1",
                String.class);
        if (releases.isEmpty()) throw unavailable();
        return releases.getFirst();
    }

    public String requirePinned(String uuid) {
        if (uuid == null) throw unavailable();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rpt_metric_release "
                + "WHERE uuid = ? AND is_deleted = 0 AND release_status IN (2, 3)", Integer.class, uuid);
        if (count == null || count == 0) throw unavailable();
        return uuid;
    }

    private BusinessException unavailable() {
        return new BusinessException(ResultCode.ERROR, "报表指标发布包不可用");
    }
}
