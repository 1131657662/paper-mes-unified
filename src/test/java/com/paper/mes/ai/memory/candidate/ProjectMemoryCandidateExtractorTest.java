package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.memory.ProjectMemoryContextSelector;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;
import com.paper.mes.ai.process.intent.ProcessAiEvidence;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiSawIntent;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProjectMemoryCandidateExtractorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProjectMemoryCandidateExtractor extractor = new ProjectMemoryCandidateExtractor(
            mapper, new ProjectMemoryContextSelector(), new ProcessTextRedactor());
    private final ProjectMemoryCandidateDocumentValidator documentValidator =
            new ProjectMemoryCandidateDocumentValidator();

    @Test
    void extractCreatesATermOnlyForUnknownConfirmedEvidence() {
        var result = extractor.extract(extraction("clean pass through"),
                List.of("/assignments/R1/sawIntent/type"), memory("cut twice"));

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.candidateType()).isEqualTo("TERM");
            assertThat(candidate.scope()).isEqualTo("SAW");
            assertThat(candidate.intent()).isEqualTo("SAW_CUTS");
            assertThat(candidate.document().path("status").asText()).isEqualTo("ACTIVE");
        });
        assertThatCode(() -> documentValidator.validate(result.getFirst().candidateType(),
                result.getFirst().document())).doesNotThrowAnyException();
    }

    @Test
    void extractSkipsKnownOrSensitiveEvidence() {
        assertThat(extractor.extract(extraction("cut twice"),
                List.of("/assignments/R1/sawIntent/type"), memory("cut twice"))).isEmpty();
        assertThat(extractor.extract(extraction("contact: Alice, cut twice"),
                List.of("/assignments/R1/sawIntent/type"), memory("different"))).isEmpty();
    }

    @Test
    void extractClassifiesOrderSpecificNumericPhraseAsExample() {
        var result = extractor.extract(extraction("1000的9件切900"),
                List.of("/assignments/R1/sawIntent/knifeCount"), memory("different"));

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.document().path("source").asText())
                    .isEqualTo("confirmed-ai-candidate");
            assertThat(candidate.document().toString())
                    .doesNotContain("1000的9件切900");
            assertThat(candidate.document().path("expected").path("field").asText())
                    .isEqualTo("sawIntent/knifeCount");
            assertThat(candidate.document().path("expected").path("processType").asText())
                    .isEqualTo("SAW");
        });
        assertThatCode(() -> documentValidator.validate(result.getFirst().candidateType(),
                result.getFirst().document())).doesNotThrowAnyException();
    }

    @Test
    void extractOnlyUsesAcceptedFieldSubtree() {
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "REWIND",
                new com.paper.mes.ai.process.intent.ProcessAiRewindIntent(
                        "CHANGE_WIDTH",
                        new com.paper.mes.ai.process.intent.ProcessAiDiameterRule(
                                "EXPLICIT", 1, List.of(java.math.BigDecimal.ONE),
                                new com.paper.mes.ai.process.intent.ProcessAiMeasurement(
                                        java.math.BigDecimal.valueOf(1300), "mm", "EXPLICIT")),
                        null,
                        new com.paper.mes.ai.process.intent.ProcessAiWidthRule(
                                "EXPLICIT", List.of(500, 500), "mm", null)),
                null, null, List.of(new ProcessAiEvidence("diameterRule", "目标直径1300mm")));
        ProcessAiExtractionResult extraction = new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment), List.of(), List.of(), false, List.of());

        assertThat(extractor.extract(extraction,
                List.of("/assignments/R1/rewindIntent/widthRule/values"), memory("different")))
                .allMatch(candidate -> !candidate.document().toString().contains("diameter"));
    }

    private ProcessAiExtractionResult extraction(String phrase) {
        ProcessAiAssignment assignment = new ProcessAiAssignment(
                List.of("R1"), "R1", List.of(), "SAW", null,
                new ProcessAiSawIntent("CUTS", 2, null, "mm"), null,
                List.of(new ProcessAiEvidence("knifeCount", phrase)));
        return new ProcessAiExtractionResult(
                "parse-1", "1.0", List.of(assignment), List.of(), List.of(), false, List.of());
    }

    private ProjectMemorySnapshot memory(String knownPhrase) {
        var document = mapper.createObjectNode();
        document.set("rules", mapper.createObjectNode());
        var terms = mapper.createObjectNode();
        var term = mapper.createObjectNode();
        term.put("status", "ACTIVE");
        term.put("phrase", knownPhrase);
        term.put("intent", "SAW_CUTS");
        terms.set("known", term);
        document.set("terms", terms);
        document.set("examples", mapper.createObjectNode());
        document.set("disabled", mapper.createObjectNode());
        return new ProjectMemorySnapshot(
                "1.0.0", "1.0", "sha256:" + "a".repeat(64), document, Instant.now());
    }
}
