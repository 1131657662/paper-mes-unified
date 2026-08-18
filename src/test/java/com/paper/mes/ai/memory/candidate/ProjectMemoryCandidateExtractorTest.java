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

class ProjectMemoryCandidateExtractorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProjectMemoryCandidateExtractor extractor = new ProjectMemoryCandidateExtractor(
            mapper, new ProjectMemoryContextSelector(), new ProcessTextRedactor());

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
                List.of("/assignments/R1/sawIntent/type"), memory("different"));

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.candidateType()).isEqualTo("EXAMPLE");
            assertThat(candidate.document().path("input").asText()).isEqualTo("1000的9件切900");
            assertThat(candidate.document().path("expected").path("intent").asText())
                    .isEqualTo("SAW_CUTS");
        });
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
