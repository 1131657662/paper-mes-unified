package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessDraftSafetyContractTest {

    private static final String DRAFT_SERVICE =
            "src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderDraftServiceImpl.java";
    private static final String PLAN_DRAFT_MANAGER =
            "src/main/java/com/paper/mes/processorder/service/ProcessPlanDraftManager.java";
    private static final String ROUTE_DRAFT_MANAGER =
            "src/main/java/com/paper/mes/processorder/service/ProcessRouteDraftManager.java";

    @Test
    void draftPreviewSaveAndSubmit_useFormalProcessPlanEntryPoints() throws IOException {
        String draftService = source(DRAFT_SERVICE);
        String planManager = source(PLAN_DRAFT_MANAGER);
        String routeManager = source(ROUTE_DRAFT_MANAGER);

        assertContainsAll(slice(draftService, "public void saveProcessConfig", "public PlanPreviewVO previewProcessPlan"),
                "saveProcessPlan(orderUuid, rollUuid, processPlanMapper.fromSaveDto(dto), expectedVersion);");
        assertContainsAll(slice(draftService, "public void saveBaseInfo", "public void saveDraftProgress"),
                "versionGuard.assertExpected(order, dto.getExpectedVersion());",
                "ConcurrencyGuard.requireRowUpdated(processOrderMapper.updateById(order));");
        assertContainsAll(slice(planManager, "private PlanPreviewVO previewOnly", "private PlanPreviewVO errorPreview"),
                "orderService.previewRewindPlan(orderUuid, roll.getUuid(), planMapper.toPreviewDto(plan))");
        assertContainsAll(slice(draftService, "private ProcessOrderSubmitVO generateFinishConfigs",
                        "private Set<String> coveredByMultiSourceDrafts"),
                "processOrderService.saveFinishConfig(",
                "routeDraftManager.submit(order, roll, draft)",
                "readConfig(draft)");
        assertContainsAll(draftService,
                "routeDraftManager.isRouteDraft(draft)",
                "businessLockService.lockProcessOrders(List.of(orderUuid));",
                "ConcurrencyGuard.requireRowUpdated(processOrderMapper.updateById(order));");
        assertContainsAll(slice(draftService,
                        "public ProcessOrderSubmitVO submit(String orderUuid, Integer expectedVersion)",
                        "private void copyBaseFields"),
                "versionGuard.assertExpected(order, expectedVersion);");
        assertContainsAll(slice(draftService, "private void validateSubmit", "private ProcessOrderSubmitVO generateFinishConfigs"),
                "ProcessModePolicy.isServiceOnly(roll.getProcessMode())",
                "continue;");
        assertContainsAll(slice(draftService, "private ProcessOrderSubmitVO generateFinishConfigs", "private Set<String> coveredByMultiSourceDrafts"),
                "serviceOnlyConfig()",
                "processOrderService.saveFinishConfig(order.getUuid(), roll.getUuid(), serviceOnlyConfig());");
        assertContainsAll(planManager,
                "businessLockService.lockProcessOrders(List.of(orderUuid));",
                "versionGuard.assertExpected(order, expectedVersion);",
                "versionGuard.advance(orderUuid, expectedVersion);",
                "upsertDraft(orderUuid, roll.getUuid(), plan, preview)");
        assertContainsAll(slice(routeManager,
                "public ProcessRoutePreviewVO save(String orderUuid, ProcessRoutePreviewDTO dto)",
                        "public boolean isRouteDraft"),
                "businessLockService.lockProcessOrders(List.of(orderUuid));",
                "upsertDraft(order.getUuid(), roll.getUuid(), dto, preview)");

        assertTrue(!draftService.contains("private String previewJson("),
                "draft service must not keep a second preview JSON path");
        assertTrue(!draftService.contains("private void upsertDraft(String orderUuid, String rollUuid, FinishConfigSaveDTO"),
                "draft service must store plan drafts through ProcessPlanDraftManager");
    }

    private String source(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }

    private String slice(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        assertTrue(startIndex >= 0, "Missing start marker: " + start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue(endIndex >= 0, "Missing end marker: " + end);
        return source.substring(startIndex, endIndex);
    }

    private void assertContainsAll(String text, String... snippets) {
        for (String snippet : snippets) {
            assertTrue(text.contains(snippet), "Missing snippet: " + snippet);
        }
    }
}
