package com.paper.mes.ai.process.parse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.compile.ProcessAiCompiledPlan;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiEvidence;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiSawIntent;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmRequest;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.process.session.crypto.AiMessageCipher;
import com.paper.mes.ai.process.session.crypto.AiStructuredResultCipher;
import com.paper.mes.ai.process.security.ProcessAiIntentCipher;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

final class ProcessAiConfirmationTestFixtures {

    static final String ACCEPTED_PATH = "/assignments/R1/sawIntent/knifeCount";

    private ProcessAiConfirmationTestFixtures() {
    }

    static ProcessAiExtractionResult extraction() {
        ProcessAiSawIntent saw = new ProcessAiSawIntent("CUTS", 2, null, null);
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", null, saw, null,
                List.of(new ProcessAiEvidence("knifeCount", "cut twice")));
        return new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment), List.of(), List.of(), false, List.of());
    }

    static ProcessAiConfirmRequest request(String key, List<String> paths) {
        return new ProcessAiConfirmRequest(
                "conversation-1", "parse-1", 7, key, paths);
    }

    static ProcessAiParseRecord record(ObjectMapper mapper, String status,
                                       ProcessAiParseConfirmation confirmation) {
        return new ProcessAiParseRecord(
                "row-1", "order-1", "conversation-1", "parse-1", 2, 1,
                "request-1", 7, status, "DEEPSEEK", "deepseek-v4-pro", "PRO", "1.0",
                "1.0.0", "sha256:" + "a".repeat(64), "[\"rule-saw\"]",
                json(mapper, extraction()), "b".repeat(64), confirmation,
                LocalDateTime.parse("2026-08-16T10:00:00"));
    }

    static ProcessAiCompilationResult compilation() {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(2);
        plan.setMainStepType(2);
        PlanPreviewVO preview = new PlanPreviewVO();
        preview.setOriginalUuid("original-1");
        preview.setReady(true);
        ProcessAiCompiledPlan compiled = new ProcessAiCompiledPlan(
                "R1", "original-1", List.of(), plan, preview);
        return new ProcessAiCompilationResult(
                true, List.of(compiled), List.of(), List.of(), List.of("review candidate"));
    }

    static ObjectMapper mapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    static AiStructuredResultCipher structuredCipher() {
        AiProperties properties = new AiProperties();
        properties.setMessageEncryptionKey(Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        return new AiStructuredResultCipher(new AiMessageCipher(properties), mapper());
    }

    static ProcessAiIntentCipher intentCipher() {
        return new ProcessAiIntentCipher(structuredCipher());
    }

    private static String json(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
