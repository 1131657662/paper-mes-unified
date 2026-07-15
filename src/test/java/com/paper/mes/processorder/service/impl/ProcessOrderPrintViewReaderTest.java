package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.PrintViewVersion;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessOrderPrintViewReaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void issuedVersion_withCompleteSnapshot_usesFrozenDetail() throws Exception {
        ProcessOrderDetailVO frozen = detail(2, "frozen-paper", List.of("finish-1"));
        Map<String, Object> root = snapshotRoot("2.0", "print_time", "2026-07-15T00:10:00");
        ProcessOrderSnapshotDetailCodec.append(root, frozen, objectMapper);
        ProcessOrderDetailVO live = detail(4, "changed-paper", List.of("finish-1", "finish-2"));
        live.getOrder().setSnapPrint(objectMapper.writeValueAsString(root));

        var view = ProcessOrderPrintViewReader.read(live, PrintViewVersion.ISSUED, objectMapper);

        assertThat(view.getSource()).isEqualTo("SNAPSHOT");
        assertThat(view.getSchemaVersion()).isEqualTo("2.0");
        assertThat(view.getDetail().getOriginalRolls().getFirst().getPaperName()).isEqualTo("frozen-paper");
        assertThat(view.getDetail().getRollProductions().getFirst().getStageOutputs()).hasSize(1);
        assertThat(view.getDetail().getOrder().getSnapPrint()).isNull();
    }

    @Test
    void issuedVersion_withLegacySnapshot_filtersLaterFinishesAndClearsActuals() throws Exception {
        ProcessOrderDetailVO live = detail(4, "changed-paper", List.of("finish-1", "finish-2"));
        Map<String, Object> root = snapshotRoot("1.1", "print_time", "2026-07-14T10:00:00");
        root.put("original_rolls", List.of(Map.of(
                "uuid", "roll-1", "paper_name", "issued-paper", "gram_weight", 80,
                "original_width", 1200, "roll_weight", new BigDecimal("1000"))));
        root.put("finish_rolls", List.of(Map.of(
                "uuid", "finish-1", "finish_roll_no", "A001", "finish_width", 600,
                "estimate_weight", new BigDecimal("500"))));
        live.getOrder().setSnapPrint(objectMapper.writeValueAsString(root));

        var view = ProcessOrderPrintViewReader.read(live, PrintViewVersion.ISSUED, objectMapper);

        assertThat(view.getSource()).isEqualTo("LEGACY_FALLBACK");
        assertThat(view.getWarning()).contains("完整打印详情上线前");
        assertThat(view.getDetail().getFinishRolls()).extracting(FinishRoll::getUuid)
                .containsExactly("finish-1");
        assertThat(view.getDetail().getOriginalRolls().getFirst().getPaperName()).isEqualTo("issued-paper");
        assertThat(view.getDetail().getFinishRolls().getFirst().getActualWeight()).isNull();
    }

    @Test
    void issuedVersion_beforeFirstPrint_returnsLivePreviewWithoutRawSnapshots() {
        ProcessOrderDetailVO live = detail(1, "preview-paper", List.of("finish-1"));

        var view = ProcessOrderPrintViewReader.read(live, PrintViewVersion.ISSUED, objectMapper);

        assertThat(view.getSource()).isEqualTo("LIVE_PREVIEW");
        assertThat(view.getAvailableVersions()).containsExactly(PrintViewVersion.ISSUED);
        assertThat(view.getDetail().getOrder().getSnapPrint()).isNull();
    }

    @Test
    void finishedVersion_withoutSnapshot_rejectsRequest() {
        ProcessOrderDetailVO live = detail(3, "paper", List.of("finish-1"));

        assertThatThrownBy(() -> ProcessOrderPrintViewReader.read(
                live, PrintViewVersion.FINISHED, objectMapper))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("完成快照尚未生成");
    }

    @Test
    void issuedVersion_withCorruptedSnapshot_rejectsLiveFallback() {
        ProcessOrderDetailVO live = detail(4, "paper", List.of("finish-1"));
        live.getOrder().setSnapPrint("{}");

        assertThatThrownBy(() -> ProcessOrderPrintViewReader.read(
                live, PrintViewVersion.ISSUED, objectMapper))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "E008");
    }

    private ProcessOrderDetailVO detail(int status, String paperName, List<String> finishIds) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderNo("JG-1");
        order.setCustomerName("测试客户");
        order.setOrderStatus(status);
        OriginalRoll roll = original(paperName);
        List<FinishRoll> finishes = finishIds.stream().map(this::finish).toList();
        ProcessOrderDetailVO detail = new ProcessOrderDetailVO();
        detail.setOrder(order);
        detail.setOriginalRolls(List.of(roll));
        detail.setFinishRolls(finishes);
        detail.setSteps(List.of());
        detail.setRollProductions(List.of(production(roll, finishes)));
        return detail;
    }

    private OriginalRoll original(String paperName) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setPaperName(paperName);
        roll.setGramWeight(100);
        roll.setOriginalWidth(1300);
        roll.setRollWeight(new BigDecimal("1000"));
        roll.setActualWeight(new BigDecimal("990"));
        return roll;
    }

    private FinishRoll finish(String uuid) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setFinishRollNo(uuid);
        finish.setFinishWidth(650);
        finish.setEstimateWeight(new BigDecimal("500"));
        finish.setActualWeight(new BigDecimal("495"));
        return finish;
    }

    private ProcessOrderDetailVO.RollProductionVO production(OriginalRoll roll, List<FinishRoll> finishes) {
        ProcessOrderDetailVO.RollProductionVO production = new ProcessOrderDetailVO.RollProductionVO();
        production.setOriginalUuid(roll.getUuid());
        production.setPaperName(roll.getPaperName());
        production.setGramWeight(roll.getGramWeight());
        production.setOriginalWidth(roll.getOriginalWidth());
        production.setRollWeight(roll.getRollWeight());
        production.setActualWeight(roll.getActualWeight());
        production.setSteps(List.of());
        ProcessOrderDetailVO.StageOutputVO output = new ProcessOrderDetailVO.StageOutputVO();
        output.setUuid("output-1");
        output.setActualWeight(new BigDecimal("495"));
        production.setStageOutputs(List.of(output));
        production.setRewindParams(List.of());
        production.setFinishes(finishes.stream().map(this::productionFinish).toList());
        return production;
    }

    private ProcessOrderDetailVO.FinishProductionVO productionFinish(FinishRoll finish) {
        ProcessOrderDetailVO.FinishProductionVO item = new ProcessOrderDetailVO.FinishProductionVO();
        item.setUuid(finish.getUuid());
        item.setFinishRollNo(finish.getFinishRollNo());
        item.setFinishWidth(finish.getFinishWidth());
        item.setEstimateWeight(finish.getEstimateWeight());
        item.setActualWeight(finish.getActualWeight());
        item.setSources(List.of());
        return item;
    }

    private Map<String, Object> snapshotRoot(String schema, String timeKey, String time) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", schema);
        root.put(timeKey, time);
        root.put("print_user", "tester");
        root.put("back_record_user", "tester");
        root.put("original_rolls", List.of());
        root.put("finish_rolls", List.of());
        return root;
    }
}
