package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderIssueConsistencyReaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void actualProductionChanges_andLaterOutput_doNotRequireReissue() throws Exception {
        ProcessOrderDetailVO issued = detail("customer-paper", 80, 500, List.of(finish("finish-1")));
        Map<String, Object> root = snapshot(issued);
        ProcessOrderDetailVO live = detail("customer-paper", 80, 500,
                List.of(finish("finish-1"), finish("finish-added")));
        live.getFinishRolls().getFirst().setActualWeight(new java.math.BigDecimal("99"));
        live.getOrder().setSnapPrint(objectMapper.writeValueAsString(root));

        var result = ProcessOrderIssueConsistencyReader.read(live, objectMapper);

        assertThat(result.getStatus()).isEqualTo("IN_SYNC");
        assertThat(result.getChangedGroups()).isEmpty();
    }

    @Test
    void printedCustomerSpecificationChange_requiresReissue() throws Exception {
        ProcessOrderDetailVO issued = detail("customer-paper", 80, 500, List.of(finish("finish-1")));
        ProcessOrderDetailVO live = detail("new-paper", 80, 500, List.of(finish("finish-1")));
        live.getOrder().setSnapPrint(objectMapper.writeValueAsString(snapshot(issued)));

        var result = ProcessOrderIssueConsistencyReader.read(live, objectMapper);

        assertThat(result.getStatus()).isEqualTo("REISSUE_REQUIRED");
        assertThat(result.getChangedGroups()).containsExactly("客户品名/克重/门幅");
    }

    @Test
    void processingOrder_postIssueRollDisposition_requiresReissue() throws Exception {
        OriginalRoll issuedRoll = originalRoll("roll-1");
        ProcessOrderDetailVO issued = detailWithRolls(2, List.of(issuedRoll));
        OriginalRoll liveRoll = originalRoll("roll-1");
        liveRoll.setDispositionAction(com.paper.mes.processorder.model.ProcessRollDispositionAction.CANCEL);
        ProcessOrderDetailVO live = detailWithRolls(2, List.of(liveRoll));
        live.getOrder().setSnapPrint(objectMapper.writeValueAsString(snapshot(issued)));

        var result = ProcessOrderIssueConsistencyReader.read(live, objectMapper);

        assertThat(result.getStatus()).isEqualTo("REISSUE_REQUIRED");
        assertThat(result.getChangedGroups()).containsExactly("母卷处置");
    }

    @Test
    void toRecordOrder_postIssueRollDisposition_isCompletionFact() throws Exception {
        OriginalRoll issuedRoll = originalRoll("roll-1");
        ProcessOrderDetailVO issued = detailWithRolls(3, List.of(issuedRoll));
        ProcessOrderDetailVO live = detailWithRolls(3, List.of(disposedRoll("roll-1")));
        live.getOrder().setSnapPrint(objectMapper.writeValueAsString(snapshot(issued)));

        var result = ProcessOrderIssueConsistencyReader.read(live, objectMapper);

        assertThat(result.getStatus()).isEqualTo("IN_SYNC");
        assertThat(result.getChangedGroups()).isEmpty();
    }

    @Test
    void finishedOrder_legacySnapshotDisposition_isCompletionFact() throws Exception {
        ProcessOrderDetailVO live = detailWithRolls(4, List.of(disposedRoll("roll-1")));
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", "1.1");
        root.put("print_time", "2026-08-12T10:00:00");
        root.put("original_rolls", List.of(Map.of("uuid", "roll-1", "paper_name", "paper")));
        root.put("finish_rolls", List.of());
        live.getOrder().setSnapPrint(objectMapper.writeValueAsString(root));

        var result = ProcessOrderIssueConsistencyReader.read(live, objectMapper);

        assertThat(result.getStatus()).isEqualTo("IN_SYNC");
        assertThat(result.getChangedGroups()).isEmpty();
    }

    private Map<String, Object> snapshot(ProcessOrderDetailVO detail) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", "2.0");
        root.put("print_time", "2026-08-12T10:00:00");
        root.put("print_user", "tester");
        root.put("original_rolls", List.of());
        root.put("finish_rolls", List.of());
        ProcessOrderSnapshotDetailCodec.append(root, detail, objectMapper);
        return root;
    }

    private ProcessOrderDetailVO detail(String name, int gramWeight, int width, List<FinishRoll> finishes) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(4);
        ProcessOrderDetailVO detail = new ProcessOrderDetailVO();
        detail.setOrder(order);
        detail.setOriginalRolls(List.of());
        detail.setFinishRolls(finishes);
        detail.setSteps(List.of());
        detail.setRollProductions(List.of());
        finishes.forEach(finish -> {
            finish.setCustomerPaperName(name);
            finish.setCustomerGramWeight(gramWeight);
            finish.setCustomerFinishWidth(width);
        });
        return detail;
    }

    private ProcessOrderDetailVO detailWithRolls(int status, List<OriginalRoll> rolls) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(status);
        ProcessOrderDetailVO detail = new ProcessOrderDetailVO();
        detail.setOrder(order);
        detail.setOriginalRolls(rolls);
        detail.setFinishRolls(List.of());
        detail.setSteps(List.of());
        detail.setRollProductions(List.of());
        return detail;
    }

    private OriginalRoll originalRoll(String uuid) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        return roll;
    }

    private OriginalRoll disposedRoll(String uuid) {
        OriginalRoll roll = originalRoll(uuid);
        roll.setDispositionAction(com.paper.mes.processorder.model.ProcessRollDispositionAction.CANCEL);
        return roll;
    }

    private FinishRoll finish(String uuid) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setFinishRollNo(uuid);
        return finish;
    }
}
