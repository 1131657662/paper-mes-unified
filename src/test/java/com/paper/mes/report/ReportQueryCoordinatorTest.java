package com.paper.mes.report;

import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportMetricContextVO;
import com.paper.mes.report.dto.ReportMetricReleaseDetailVO;
import com.paper.mes.report.dto.ReportMetricReleaseSummaryVO;
import com.paper.mes.report.dto.ReportMetricVersionAuditVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportMetricCatalogService;
import com.paper.mes.report.service.ReportLiveMetricRegistry;
import com.paper.mes.report.service.ReportQueryCoordinator;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportQueryCoordinatorTest {

    @Test
    void prepare_whenReleaseIsExecutable_returnsVersionedLiveMetadata() {
        ReportMetricCatalogService catalog = mock(ReportMetricCatalogService.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ReportQuery query = new ReportQuery();
        query.setMetricReleaseUuid("release-1");
        when(catalog.releaseDetail("release-1")).thenReturn(release(2, "report.sql.order_count"));
        when(jdbc.queryForObject("SELECT CURRENT_TIMESTAMP", LocalDateTime.class))
                .thenReturn(LocalDateTime.of(2026, 7, 21, 1, 30));

        var metadata = new ReportQueryCoordinator(catalog, jdbc, new ReportLiveMetricRegistry()).prepare(query);

        assertEquals("release-1", metadata.metricReleaseUuid());
        assertEquals("version-order_count", metadata.metricVersionMap().get("order_count"));
        assertEquals("LIVE_DB_READ", metadata.consistencyMode());
        assertEquals(64, metadata.queryHash().length());
    }

    @Test
    void prepare_whenReleaseUsesUnknownImplementation_rejectsInsteadOfUsingLatestLogic() {
        ReportMetricCatalogService catalog = mock(ReportMetricCatalogService.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ReportQuery query = new ReportQuery();
        query.setMetricReleaseUuid("release-1");
        when(catalog.releaseDetail("release-1")).thenReturn(release(2, "unknown.metric"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> new ReportQueryCoordinator(catalog, jdbc, new ReportLiveMetricRegistry()).prepare(query));

        assertTrue(error.getMessage().contains("order_count"));
    }

    @Test
    void prepare_whenRetiredV1BundleIsReplayed_keepsHistoricalExecutionAvailable() {
        ReportMetricCatalogService catalog = mock(ReportMetricCatalogService.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ReportQuery query = new ReportQuery();
        query.setMetricReleaseUuid("release-v1");
        when(catalog.releaseDetail("release-v1")).thenReturn(v1Release());
        when(jdbc.queryForObject("SELECT CURRENT_TIMESTAMP", LocalDateTime.class))
                .thenReturn(LocalDateTime.of(2026, 7, 21, 1, 30));

        var metadata = new ReportQueryCoordinator(catalog, jdbc, new ReportLiveMetricRegistry()).prepare(query);

        assertEquals(19, metadata.metricVersionMap().size());
        assertEquals("version-order_count", metadata.metricVersionMap().get("order_count"));
    }

    private ReportMetricReleaseDetailVO release(int status, String implementationKey) {
        var summary = new ReportMetricReleaseSummaryVO("release-1", "R1", "Release 1", status,
                "checksum", 38, LocalDateTime.now(), "system", null, null,
                LocalDateTime.now(), LocalDateTime.now());
        List<ReportMetricVersionAuditVO> metrics = metricCodes().stream()
                .map(code -> metric(code, "order_count".equals(code) ? implementationKey : "report.sql." + code))
                .toList();
        return new ReportMetricReleaseDetailVO(summary, metrics);
    }

    private ReportMetricReleaseDetailVO v1Release() {
        var summary = new ReportMetricReleaseSummaryVO("release-v1", "REPORT-BASELINE-V1", "V1", 3,
                "checksum", 19, LocalDateTime.now(), "system", LocalDateTime.now(), "system",
                LocalDateTime.now(), LocalDateTime.now());
        var metrics = metricCodes().subList(0, 19).stream()
                .map(code -> metric(code, "report.sql." + code)).toList();
        return new ReportMetricReleaseDetailVO(summary, metrics);
    }

    private ReportMetricVersionAuditVO metric(String code, String implementationKey) {
        return new ReportMetricVersionAuditVO("metric-" + code, code, code, "", "DECIMAL", "COUNT",
                2, 10, "version-" + code, 1, implementationKey, "{}", "checksum", 2,
                LocalDateTime.now(), "system");
    }

    private List<String> metricCodes() {
        return List.of("order_count", "original_roll_count", "finish_roll_count", "original_weight_kg",
                "finish_weight_kg", "loss_weight_kg", "loss_ratio_pct", "knife_count", "saw_amount",
                "rewind_amount", "process_amount", "extra_amount", "total_amount", "settled_amount",
                "pending_settle_amount", "received_amount", "cash_received_amount", "scrap_offset_amount",
                "unreceived_amount", "settlement_document_count", "settlement_pending_count",
                "settlement_partial_count", "overdue_document_count", "overdue_amount",
                "collection_record_count", "discount_amount", "scrap_weight_kg",
                "inventory_roll_count", "inventory_available_count", "inventory_locked_count",
                "inventory_exception_count", "inventory_weight_kg", "inventory_locked_weight_kg",
                "delivery_document_count", "delivery_pending_count", "delivery_completed_count",
                "delivery_pending_weight_kg", "delivery_completed_weight_kg");
    }
}
