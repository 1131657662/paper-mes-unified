package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.report.controller.ReportController;
import com.paper.mes.report.dto.ReportMetricContextVO;
import com.paper.mes.report.dto.ReportMetricReleaseSummaryVO;
import com.paper.mes.report.dto.ReportProductionAnalysisVO;
import com.paper.mes.report.service.ReportMetricCatalogService;
import com.paper.mes.report.service.ReportQueryCoordinator;
import com.paper.mes.report.service.ReportQuerySnapshotService;
import com.paper.mes.report.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerContractTest {

    private static final String TOKEN = "report-token";
    private AuthService authService;
    private ReportMetricCatalogService metricCatalogService;
    private ReportService reportService;
    private ReportQuerySnapshotService querySnapshotService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        reportService = mock(ReportService.class);
        metricCatalogService = mock(ReportMetricCatalogService.class);
        querySnapshotService = mock(ReportQuerySnapshotService.class);
        var controller = new ReportController(reportService, metricCatalogService,
                mock(ReportQueryCoordinator.class), querySnapshotService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void metricContext_withoutToken_returnsUnauthorized() throws Exception {
        mvc.perform(get("/api/reports/metric-context"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(metricCatalogService, never()).activeContext();
    }

    @Test
    void metricContext_withViewerRole_returnsPublishedContext() throws Exception {
        authorizeViewer();
        when(metricCatalogService.activeContext()).thenReturn(context());

        mvc.perform(get("/api/reports/metric-context")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.releaseCode").value("REPORT-BASELINE-V1"));
    }

    @Test
    void createQuerySnapshot_withoutToken_returnsUnauthorized() throws Exception {
        mvc.perform(post("/api/reports/query-snapshots")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(querySnapshotService, never()).create(any());
    }

    @Test
    void productionTopic_withoutToken_returnsUnauthorized() throws Exception {
        mvc.perform(post("/api/reports/topics/production/query")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        verify(reportService, never()).productionAnalysis(any());
    }

    @Test
    void productionTopic_withViewerRole_returnsAnalysis() throws Exception {
        authorizeViewer();
        when(reportService.productionAnalysis(any())).thenReturn(
                new ReportProductionAnalysisVO("production", null, List.of(), List.of(), List.of(),
                        LocalDateTime.of(2026, 7, 21, 11, 0), null));

        mvc.perform(post("/api/reports/topics/production/query")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicCode").value("production"))
                .andExpect(jsonPath("$.data.monthlyTrend").isArray());
    }

    @Test
    void metricReleases_withViewerRole_returnsReleaseHistory() throws Exception {
        authorizeViewer();
        when(metricCatalogService.releaseHistory()).thenReturn(List.of(release()));

        mvc.perform(get("/api/reports/metric-releases")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].releaseStatus").value(2))
                .andExpect(jsonPath("$.data[0].metricCount").value(19));
    }

    @Test
    void metricRelease_withMalformedUuid_returnsBadRequest() throws Exception {
        authorizeViewer();

        mvc.perform(get("/api/reports/metric-releases/not-valid")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isBadRequest());

        verify(metricCatalogService, never()).releaseDetail(any());
    }

    private void authorizeViewer() {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("viewer-uuid").username("viewer").realName("viewer")
                .roleCode("viewer").build());
    }

    private ReportMetricContextVO context() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 10, 0);
        return new ReportMetricContextVO("release-uuid", "REPORT-BASELINE-V1",
                "统计报表基线口径 V1", "checksum", now, now, List.of());
    }

    private ReportMetricReleaseSummaryVO release() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 10, 0);
        return new ReportMetricReleaseSummaryVO("release-uuid", "REPORT-BASELINE-V1",
                "统计报表基线口径 V1", 2, "checksum", 19,
                now, "system", null, null, now, now);
    }
}
