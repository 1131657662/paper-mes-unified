package com.paper.mes.report;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportMapperDimensionSqlContractTest {

    @Test
    void reportSql_whenSummarizingProcessDimension_usesProcessStepsAndFinalOutputs() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");
        String branch = slice(sql, "<when test=\"dimension == 'process'\">", "<when test=\"dimension == 'machine'\">");

        assertTrue(branch.contains("FROM biz_process_step ps"));
        assertTrue(branch.contains("CASE WHEN ps.step_type = 1 THEN COALESCE(ps.step_amount, 0) ELSE 0 END AS sawAmount"));
        assertTrue(branch.contains("CASE WHEN ps.step_type = 2 THEN COALESCE(ps.step_amount, 0) ELSE 0 END AS rewindAmount"));
        assertTrue(branch.contains("s.output_type = 2"));
        assertTrue(branch.contains("s.output_status = 3"));
        assertTrue(branch.contains("COUNT(DISTINCT f.uuid) AS finishRollCount"));
        assertTrue(branch.contains("UNION ALL"));
        assertTrue(branch.contains("FROM biz_process_step legacyStep"));
        assertTrue(normalize(branch).contains("NOT EXISTS ( SELECT 1 FROM biz_process_stage_output existingOutput"));
        assertTrue(branch.contains("COALESCE(settle.billedAmount, o.total_amount, 0) * COALESCE(ps.step_amount, 0) / orderStepBase.orderStepAmount"));
    }

    @Test
    void reportSql_whenSummarizingMachineDimension_usesProcessStepMachineWithOriginalFallback() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");
        String branch = slice(sql, "<when test=\"dimension == 'machine'\">", "<when test=\"dimension == 'paper'\">");

        assertTrue(branch.contains("FROM biz_process_step ps"));
        assertTrue(branch.contains("COALESCE(ps.machine_uuid, r.machine_uuid, 'none') AS dimensionKey"));
        assertTrue(branch.contains("COALESCE(ps.machine_name_snap, stepMachine.machine_name, rollMachine.machine_name, '未分配机台') AS dimensionName"));
        assertTrue(branch.contains("LEFT JOIN sys_machine stepMachine ON stepMachine.uuid = ps.machine_uuid"));
        assertTrue(branch.contains("LEFT JOIN sys_machine rollMachine ON rollMachine.uuid = r.machine_uuid"));
        assertTrue(branch.contains("<include refid=\"StepFilters\"/>"));
        assertTrue(branch.contains("<include refid=\"RollSpecFilters\"/>"));
    }

    @Test
    void reportSql_whenFilteringMachine_usesStepMachineAndLegacyRollFallback() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");
        String orderFilters = slice(sql, "<sql id=\"OrderFilters\">", "<sql id=\"RollSpecFilters\">");
        String rollFilters = slice(sql, "<sql id=\"RollFilters\">", "<sql id=\"StepFilters\">");

        assertTrue(orderFilters.contains("COALESCE(qps.machine_uuid, rf.machine_uuid) = #{q.machineUuid}"));
        assertTrue(orderFilters.contains("NOT EXISTS"));
        assertTrue(rollFilters.contains("COALESCE(r.process_mode, 0) != 3"));
        assertTrue(rollFilters.contains("COALESCE(qps.machine_uuid, r.machine_uuid) = #{q.machineUuid}"));
        assertTrue(rollFilters.contains("qps.step_type = #{q.processStepType}"));
        assertFalse(rollFilters.contains("AND r.machine_uuid = #{q.machineUuid}</if>"));
    }

    @Test
    void reportSql_whenFilteringProcessDimension_appliesExactStepFilter() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");
        String processBranch = slice(sql, "<when test=\"dimension == 'process'\">",
                "<when test=\"dimension == 'machine'\">");

        assertTrue(sql.contains("<sql id=\"StepFilters\">"));
        assertTrue(sql.contains("AND ps.step_type = #{q.processStepType}"));
        assertTrue(processBranch.contains("<include refid=\"StepFilters\"/>"));
    }

    @Test
    void reportSql_whenCountingFinishWeights_excludesScrappedFinishes() throws IOException {
        String sql = resourceText("mapper/report/ReportMapper.xml");

        assertEquals(1, count(sql, "COALESCE(f.finish_status, 0) != 4"));
        assertEquals(0, count(sql, "COALESCE(finish_status, 0) != 4"));
    }

    @Test
    void dashboardSql_whenCountingProcessingStats_usesSameDirectAndScrapFilters() throws IOException {
        String sql = resourceText("mapper/dashboard/DashboardMapper.xml");

        assertEquals(6, count(sql, "COALESCE(process_mode, 0) != 3"));
        assertEquals(1, count(sql, "COALESCE(r.process_mode, 0) != 3"));
        assertEquals(1, count(sql, "COALESCE(f.finish_status, 0) != 4"));
        assertEquals(0, count(sql, "COALESCE(finish_status, 0) != 4"));
        assertFalse(settleAllocationSql("mapper/dashboard/DashboardMapper.xml").contains("process_mode"));
    }

    @Test
    void reportAndDashboard_whenCountingFinishProducts_useFinalStageOutputPredicate() throws IOException {
        String reportSql = resourceText("mapper/report/ReportMapper.xml");
        String dashboardSql = resourceText("mapper/dashboard/DashboardMapper.xml");

        assertFinalFinishPredicate(reportSql);
        assertFinalFinishPredicate(dashboardSql);
    }

    @Test
    void dashboardSql_whenRankingMachines_usesEffectiveProcessSteps() throws IOException {
        String sql = resourceText("mapper/dashboard/DashboardMapper.xml");

        assertTrue(sql.contains("<sql id=\"MachineRankRows\">"));
        assertTrue(sql.contains("FROM biz_process_step ps"));
        assertTrue(sql.contains("COALESCE(ps.machine_uuid, r.machine_uuid, 'none') AS machine_uuid"));
        assertTrue(sql.contains("COALESCE(ps.machine_name_snap, stepMachine.machine_name, rollMachine.machine_name, '未分配机台') AS machine_name"));
        assertTrue(sql.contains("COUNT(DISTINCT order_uuid) AS count"));
        assertTrue(sql.contains("COALESCE(SUM(stepAmount), 0) AS amount"));
        assertFalse(sql.contains("SELECT r.machine_uuid AS id"));
    }

    @Test
    void reportAndDashboard_whenUsingProcessWeight_convertTonsToKilogramsForWeightColumns() throws IOException {
        String reportSql = resourceText("mapper/report/ReportMapper.xml");
        String dashboardSql = resourceText("mapper/dashboard/DashboardMapper.xml");

        assertTrue(reportSql.contains("ps.process_weight * 1000"));
        assertTrue(dashboardSql.contains("ps.process_weight * 1000"));
    }

    private String settleAllocationSql(String resource) throws IOException {
        String sql = resourceText(resource);
        String start = "<sql id=\"SettleAllocationByOrder\">";
        int startIndex = sql.indexOf(start);
        int endIndex = sql.indexOf("</sql>", startIndex);
        return normalize(sql.substring(startIndex + start.length(), endIndex));
    }

    private String resourceText(String resource) throws IOException {
        try (var in = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Missing resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String sourceText(String relativePath) throws IOException {
        return java.nio.file.Files.readString(java.nio.file.Path.of(relativePath), StandardCharsets.UTF_8);
    }

    private String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private long count(String text, String pattern) {
        return (text.length() - text.replace(pattern, "").length()) / pattern.length();
    }

    private void assertFinalFinishPredicate(String sql) {
        assertTrue(sql.contains("<sql id=\"EffectiveFinishRollWhere\">"));
        assertTrue(sql.contains("COALESCE(f.is_spare, 0) = 0"));
        assertTrue(sql.contains("COALESCE(f.roll_no_status, 1) != 3"));
        assertTrue(sql.contains("COALESCE(f.finish_status, 0) != 4"));
        assertTrue(sql.contains("sff.output_type = 2"));
        assertTrue(sql.contains("sff.output_status = 3"));
    }

    private String slice(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        int endIndex = text.indexOf(end, startIndex + start.length());
        assertTrue(startIndex >= 0, "Missing start: " + start);
        assertTrue(endIndex >= 0, "Missing end: " + end);
        return text.substring(startIndex, endIndex);
    }
}
