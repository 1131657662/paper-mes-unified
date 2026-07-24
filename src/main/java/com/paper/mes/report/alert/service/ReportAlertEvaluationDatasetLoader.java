package com.paper.mes.report.alert.service;

import com.paper.mes.paper.entity.Paper;
import com.paper.mes.paper.mapper.PaperMapper;
import com.paper.mes.report.alert.entity.ReportAlertRule;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportAlertEvaluationDatasetLoader {
    private final ReportService reportService;
    private final PaperMapper paperMapper;

    public ReportAlertEvaluationDataset load(ReportQuery baseQuery, List<ReportAlertRule> rules) {
        Map<String, ReportAlertMetricSnapshot> snapshots = new HashMap<>();
        Map<String, String> labels = new HashMap<>();
        LoadState state = new LoadState(baseQuery, rules, snapshots, labels);
        state.snapshots().put("GLOBAL", ReportAlertMetricSnapshot.from(reportService.overview(baseQuery)));
        state.labels().put("GLOBAL", "全局");
        loadSimpleDimension(state, new DimensionSpec(2, "customer", "CUSTOMER:"));
        loadSimpleDimension(state, new DimensionSpec(4, "process", "PROCESS:"));
        loadPaperDimension(state);
        return new ReportAlertEvaluationDataset(Map.copyOf(snapshots), Map.copyOf(labels));
    }

    private void loadSimpleDimension(LoadState state, DimensionSpec spec) {
        if (state.rules().stream().noneMatch(rule -> rule.getScopeType() == spec.scopeType())) return;
        for (ReportDimensionVO row : reportService.dimensionSummary(
                dimensionQuery(state.query(), spec.dimension()))) {
            String key = spec.prefix() + normalizeDimensionKey(spec.dimension(), row.getDimensionKey());
            state.snapshots().put(key, ReportAlertMetricSnapshot.from(row));
            state.labels().put(key, row.getDimensionName());
        }
    }

    private void loadPaperDimension(LoadState state) {
        Set<String> paperUuids = state.rules().stream().filter(rule -> rule.getScopeType() == 3)
                .map(ReportAlertRule::getPaperUuid).collect(Collectors.toSet());
        if (paperUuids.isEmpty()) return;
        Map<String, ReportDimensionVO> rows = reportService.dimensionSummary(
                        dimensionQuery(state.query(), "paper"))
                .stream().collect(Collectors.toMap(ReportDimensionVO::getDimensionKey, row -> row));
        for (Paper paper : paperMapper.selectByIds(paperUuids)) {
            addPaper(state, paper, rows.get(paper.getPaperName()));
        }
    }

    private void addPaper(LoadState state, Paper paper, ReportDimensionVO row) {
        if (row == null) return;
        String key = "PAPER:" + paper.getUuid();
        state.snapshots().put(key, ReportAlertMetricSnapshot.from(row));
        state.labels().put(key, paper.getPaperName());
    }

    private String normalizeDimensionKey(String dimension, String key) {
        if (!"process".equals(dimension)) return key;
        return switch (key) {
            case "saw" -> "1";
            case "rewind" -> "2";
            default -> key;
        };
    }

    private ReportQuery dimensionQuery(ReportQuery source, String dimension) {
        ReportQuery query = new ReportQuery();
        query.setMetricReleaseUuid(source.getMetricReleaseUuid());
        query.setDateFrom(source.getDateFrom());
        query.setDateTo(source.getDateTo());
        query.setDimension(dimension);
        return query;
    }

    private record DimensionSpec(int scopeType, String dimension, String prefix) {
    }

    private record LoadState(
            ReportQuery query,
            List<ReportAlertRule> rules,
            Map<String, ReportAlertMetricSnapshot> snapshots,
            Map<String, String> labels
    ) {
    }
}
