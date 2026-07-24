package com.paper.mes.report.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportMetricContextVO;
import com.paper.mes.report.dto.ReportMetricReleaseDetailVO;
import com.paper.mes.report.dto.ReportMetricVersionAuditVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.dto.ReportQueryExecutionMetaVO;
import com.paper.mes.report.service.ReportMetricCatalogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ReportQueryCoordinator {

    private final ReportMetricCatalogService catalogService;
    private final JdbcTemplate jdbcTemplate;
    private final ReportLiveMetricRegistry liveMetricRegistry;
    private final ConcurrentMap<String, ReportMetricReleaseDetailVO> releaseCache = new ConcurrentHashMap<>();

    public ReportQueryCoordinator(ReportMetricCatalogService catalogService, JdbcTemplate jdbcTemplate,
                                  ReportLiveMetricRegistry liveMetricRegistry) {
        this.catalogService = catalogService;
        this.jdbcTemplate = jdbcTemplate;
        this.liveMetricRegistry = liveMetricRegistry;
    }

    public ReportQueryExecutionMetaVO prepare(ReportQuery query) {
        ReportMetricReleaseDetailVO release = resolveRelease(query);
        validateRelease(release);
        LocalDateTime watermark = jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP", LocalDateTime.class);
        if (watermark == null) throw new BusinessException("无法读取报表数据时点");
        return new ReportQueryExecutionMetaVO(UUID.randomUUID().toString(), hash(query, release),
                release.release().releaseUuid(), versionMap(release.metrics()), watermark, watermark,
                "LIVE_DB_READ", "LIVE_ONLY", warnings(release), sectionStatuses());
    }

    public void requireExecutable(ReportQuery query) {
        validateRelease(resolveRelease(query));
    }

    private ReportMetricReleaseDetailVO resolveRelease(ReportQuery query) {
        String releaseUuid = query == null ? null : query.getMetricReleaseUuid();
        if (StringUtils.hasText(releaseUuid)) return release(releaseUuid);
        ReportMetricContextVO active = catalogService.activeContext();
        return release(active.releaseUuid());
    }

    private ReportMetricReleaseDetailVO release(String releaseUuid) {
        ReportMetricReleaseDetailVO cached = releaseCache.get(releaseUuid);
        if (cached != null) return cached;
        ReportMetricReleaseDetailVO loaded = catalogService.releaseDetail(releaseUuid);
        if (loaded.release().releaseStatus() != 1) releaseCache.putIfAbsent(releaseUuid, loaded);
        return loaded;
    }

    private void validateRelease(ReportMetricReleaseDetailVO release) {
        if (release.release().releaseStatus() == 1) {
            throw new BusinessException("指标发布包尚未发布，不能用于报表查询");
        }
        if (release.metrics().isEmpty()) throw new BusinessException("指标发布包不包含可执行指标");
        liveMetricRegistry.requireExecutable(release.metrics());
    }

    private Map<String, String> versionMap(List<ReportMetricVersionAuditVO> metrics) {
        Map<String, String> result = new LinkedHashMap<>();
        for (ReportMetricVersionAuditVO metric : metrics) {
            if (result.put(metric.metricCode(), metric.metricVersionUuid()) != null) {
                throw new BusinessException("指标发布包包含重复指标: " + metric.metricCode());
            }
        }
        return result;
    }

    private List<String> warnings(ReportMetricReleaseDetailVO release) {
        List<String> warnings = new ArrayList<>();
        if (release.release().releaseStatus() == 3) warnings.add("当前使用的是已停用发布包，适用于历史追溯");
        warnings.add("当前查询使用实时 SQL 适配器，未命中物化快照");
        return warnings;
    }

    private Map<String, String> sectionStatuses() {
        return Map.of("overview", "READY", "dimensions", "READY", "details", "READY");
    }

    private String hash(ReportQuery query, ReportMetricReleaseDetailVO release) {
        String value = String.join("|", release.release().releaseUuid(),
                value(query == null ? null : query.getDateFrom()), value(query == null ? null : query.getDateTo()),
                value(query == null ? null : query.getCustomerUuid()), value(query == null ? null : query.getPaperName()),
                value(query == null ? null : query.getMainStepType()), value(query == null ? null : query.getProcessStepType()),
                value(query == null ? null : query.getProcessMode()),
                value(query == null ? null : query.getMachineUuid()), value(query == null ? null : query.getSettleType()),
                value(query == null ? null : query.getIsInvoice()), value(query == null ? null : query.getOrderStatus()),
                value(query == null ? null : query.getDimension()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
