package com.paper.mes.ai.process.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.memory.ProjectMemoryContextSelector;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import com.paper.mes.ai.process.intent.ProcessAiClarificationOption;
import com.paper.mes.ai.process.intent.ProcessAiClarificationQuestion;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import com.paper.mes.ai.process.session.dto.ProcessAiMessageResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiPromptAssemblerTest {

    @Test
    void assembleIncludesOnlyShortRollReferencesAndAllowlistedProcessFacts() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProcessAiPromptAssembler assembler = new ProcessAiPromptAssembler(
                mapper, new ProjectMemoryContextSelector(), new ProcessTextRedactor());

        var bundle = assembler.assemble(new ProcessAiPromptContext(
                "parse-1", order(), memory(mapper), "直径一分为二",
                List.of(new ProcessAiMessageResponse(
                        1, "USER", "FINAL", "直径一分为二", null, LocalDateTime.now()))));
        var prompt = bundle.prompt();

        assertThat(prompt.userContext())
                .contains("\"ref\":\"R1\"", "\"widthMm\":2000", "rule-split")
                .doesNotContain("weightKg", "estimateWeight", "estimateWeightKg")
                .doesNotContain("roll-secret-uuid", "order-1", "CUSTOMER-001", "API_KEY");
        assertThat(prompt.systemInstruction()).contains(
                "不调用工具", "WEIGHT_SPLIT", "\"rewindIntent\"", "\"sawIntent\"",
                "\"ancillaryRequirements\"", "SERVICE_ONLY", "DIRECT_SHIP",
                "\"processMode\"", "REWIND时processMode只能为STANDARD或ON_SITE");
        assertThat(prompt.systemInstruction()).contains(
                "门幅一分二", "widthRule.type=KNIFE_COUNT", "knifeCount=1",
                "widthRule为EXPLICIT时values为完整成品门幅数组",
                "后端会把差额自动补为TRIM", "STRIP_SORT", "REPACKAGE",
                "按件或按吨的服务数量由系统", "本次允许的母卷短代号：R1");
        assertThat(bundle.memoryItemIds()).containsExactly("rule-split");
    }

    @Test
    void assembleIncludesTheServerBoundClarificationQuestionAndRevision() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProcessAiPromptAssembler assembler = new ProcessAiPromptAssembler(
                mapper, new ProjectMemoryContextSelector(), new ProcessTextRedactor());
        ProcessAiClarificationQuestion question = new ProcessAiClarificationQuestion(
                "quantity-scope", "quantityScope", 4, "数量范围？", List.of(
                new ProcessAiClarificationOption("PER_SOURCE", "每条母卷"),
                new ProcessAiClarificationOption("TOTAL", "全单")), true);

        var bundle = assembler.assemble(new ProcessAiPromptContext(
                "parse-1", 4, order(), memory(mapper), "PER_SOURCE", List.of(),
                question, "PER_SOURCE", null));

        assertThat(bundle.prompt().userContext()).contains(
                "\"parseRevision\":4", "\"questionId\":\"quantity-scope\"",
                "\"answerCode\":\"PER_SOURCE\"", "每条母卷", "全单");
    }

    @Test
    void assembleRedactsSensitiveValuesFromConversationHistory() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProcessAiPromptAssembler assembler = new ProcessAiPromptAssembler(
                mapper, new ProjectMemoryContextSelector(), new ProcessTextRedactor());
        var history = List.of(new ProcessAiMessageResponse(
                1, "USER", "FINAL",
                "包膜加20元每件，电话13800138000，api key=sk-1234567890123456",
                null, LocalDateTime.now()));

        var bundle = assembler.assemble(new ProcessAiPromptContext(
                "parse-1", order(), memory(mapper), "补充包装要求", history));

        assertThat(bundle.prompt().userContext())
                .contains("[金额]", "[手机号已隐藏]", "[密钥已隐藏]")
                .doesNotContain("20元", "13800138000", "sk-1234567890123456");
    }

    @Test
    void assembleExcludesFailedAndPartialAssistantMessagesFromModelHistory() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProcessAiPromptAssembler assembler = new ProcessAiPromptAssembler(
                mapper, new ProjectMemoryContextSelector(), new ProcessTextRedactor());
        var history = List.of(
                new ProcessAiMessageResponse(1, "USER", "FINAL", "9件切900", null,
                        LocalDateTime.now()),
                new ProcessAiMessageResponse(2, "ASSISTANT", "FAILED", "raw failed json", null,
                        LocalDateTime.now()),
                new ProcessAiMessageResponse(3, "ASSISTANT", "PARTIAL", "raw partial json", null,
                        LocalDateTime.now()),
                new ProcessAiMessageResponse(4, "ASSISTANT", "FINAL", "请说明母卷", null,
                        LocalDateTime.now()));

        var bundle = assembler.assemble(new ProcessAiPromptContext(
                "parse-1", order(), memory(mapper), "补充说明", history));

        assertThat(bundle.prompt().userContext())
                .contains("9件切900", "请说明母卷")
                .doesNotContain("raw failed json", "raw partial json");
    }

    private ProcessAiOrderContext order() {
        ProcessAiRollContext roll = new ProcessAiRollContext(
                "R1", "roll-secret-uuid", 1, "白卡纸", 250,
                2000, 1500, 3, new BigDecimal("1000"), 1, 1, 2);
        return new ProcessAiOrderContext("order-1", 7, "CUSTOMER-001", List.of(roll));
    }

    private ProjectMemorySnapshot memory(ObjectMapper mapper) throws Exception {
        var document = mapper.readTree("""
                {"rules":{"rule-split":{"status":"ACTIVE","keywords":["直径一分为二"],
                "content":"重量50/50","apiKey":"API_KEY"}}}
                """);
        return new ProjectMemorySnapshot(
                "1.0.0", "1.0", "sha256:test", document, Instant.now());
    }
}
