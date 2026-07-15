package com.paper.mes.performance;

import com.paper.mes.processorder.dto.ProcessOrderQuery;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "app.schema-bootstrap.enabled=false",
        "app.data-health.initial-delay-ms=86400000",
        "app.backup.enabled=false"
})
@EnabledIfSystemProperty(named = "paper.mes.performance.enabled", matches = "true")
class ProductionScalePerformanceIT {

    private static final String PERF_CUSTOMER_UUID = "b68d1e20b8893a5045297d6146eba796";
    private static final long MEBIBYTE = 1024L * 1024L;

    @Autowired private ProcessOrderService processOrderService;
    @Autowired private ReportService reportService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void requireIsolatedPerformanceDatabase() {
        String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_process_order WHERE customer_uuid = ? AND is_deleted = 0",
                Long.class, PERF_CUSTOMER_UUID);
        assertThat(database).contains("_perf_");
        assertThat(count).isEqualTo(100_000L);
    }

    @Test
    void processOrderFirstPage_meetsInteractiveLatencyBaseline() {
        ProcessOrderQuery query = pageQuery(1);
        long started = System.nanoTime();

        var result = processOrderService.pageOrders(query);
        long elapsedMs = elapsedMs(started);

        assertThat(result.getRecords()).hasSize(20);
        assertThat(elapsedMs).isLessThan(1_000L);
        print("加工单首屏", elapsedMs, result.getTotal());
    }

    @Test
    void processOrderDeepPage_staysWithinAdministrativeLatencyBaseline() {
        ProcessOrderQuery query = pageQuery(5_000);
        long started = System.nanoTime();

        var result = processOrderService.pageOrders(query);
        long elapsedMs = elapsedMs(started);

        assertThat(result.getRecords()).hasSize(20);
        assertThat(elapsedMs).isLessThan(2_000L);
        print("加工单第5000页", elapsedMs, result.getTotal());
    }

    @Test
    void reportDetails_withOneHundredThousandRows_meetsDisplayBaseline() {
        long started = System.nanoTime();

        var result = reportService.detailRows(reportQuery());
        long elapsedMs = elapsedMs(started);

        assertThat(result.getTotal()).isEqualTo(100_000L);
        assertThat(result.getRows()).hasSize(1_000);
        assertThat(result.isTruncated()).isTrue();
        assertThat(elapsedMs).isLessThan(10_000L);
        print("十万级报表明细", elapsedMs, result.getTotal());
    }

    @Test
    void reportExport_withOneHundredThousandRows_staysWithinTimeAndHeapBaseline() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        resetHeapPeaks();
        long started = System.nanoTime();

        reportService.exportWorkbook(reportQuery(), response);
        long elapsedMs = elapsedMs(started);
        long peakHeapMb = peakHeapBytes() / MEBIBYTE;

        assertThat(response.getContentAsByteArray()).isNotEmpty();
        assertThat(elapsedMs).isLessThan(90_000L);
        assertThat(peakHeapMb).isLessThan(768L);
        System.out.printf("PERF 十万行报表导出 elapsedMs=%d peakHeapMb=%d bytes=%d%n",
                elapsedMs, peakHeapMb, response.getContentAsByteArray().length);
    }

    @Test
    void twoConcurrentReportExports_stayWithinTimeAndHeapBaseline() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        resetHeapPeaks();
        try {
            var first = executor.submit(() -> exportBytes(ready, start));
            var second = executor.submit(() -> exportBytes(ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            long started = System.nanoTime();
            start.countDown();

            assertThat(first.get(120, TimeUnit.SECONDS)).isPositive();
            assertThat(second.get(120, TimeUnit.SECONDS)).isPositive();
            long elapsedMs = elapsedMs(started);
            long peakHeapMb = peakHeapBytes() / MEBIBYTE;

            assertThat(elapsedMs).isLessThan(120_000L);
            assertThat(peakHeapMb).isLessThan(1_024L);
            System.out.printf("PERF 双并发十万行导出 elapsedMs=%d peakHeapMb=%d%n", elapsedMs, peakHeapMb);
        } finally {
            executor.shutdownNow();
        }
    }

    private ProcessOrderQuery pageQuery(long current) {
        ProcessOrderQuery query = new ProcessOrderQuery();
        query.setCustomerUuid(PERF_CUSTOMER_UUID);
        query.setCurrent(current);
        query.setSize(20);
        return query;
    }

    private ReportQuery reportQuery() {
        ReportQuery query = new ReportQuery();
        query.setCustomerUuid(PERF_CUSTOMER_UUID);
        query.setDimension("month");
        return query;
    }

    private long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }

    private void resetHeapPeaks() {
        ManagementFactory.getMemoryPoolMXBeans().forEach(pool -> pool.resetPeakUsage());
    }

    private long peakHeapBytes() {
        return ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == java.lang.management.MemoryType.HEAP)
                .mapToLong(pool -> pool.getPeakUsage().getUsed())
                .sum();
    }

    private int exportBytes(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
        MockHttpServletResponse response = new MockHttpServletResponse();
        reportService.exportWorkbook(reportQuery(), response);
        return response.getContentAsByteArray().length;
    }

    private void print(String name, long elapsedMs, long rows) {
        System.out.printf("PERF %s elapsedMs=%d rows=%d%n", name, elapsedMs, rows);
    }
}
