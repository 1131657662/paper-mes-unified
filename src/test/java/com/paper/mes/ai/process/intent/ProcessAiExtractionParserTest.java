package com.paper.mes.ai.process.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.model.ProcessAiProviderException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@ExtendWith(OutputCaptureExtension.class)
class ProcessAiExtractionParserTest {

    private ProcessAiExtractionParser parser;

    @BeforeEach
    void setUp() {
        parser = new ProcessAiExtractionParser(new ObjectMapper(),
                Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Test
    void parseAcceptsTheStrictVersionOneContract() {
        ProcessAiExtractionResult result = parser.parse(validJson());

        assertThat(result.schemaVersion()).isEqualTo("1.0");
        assertThat(result.assignments()).hasSize(1);
        assertThat(result.assignments().getFirst().ownerRollRef()).isEqualTo("R1");
    }

    @Test
    void parseRejectsUnknownFieldsWithoutLoggingModelContent(CapturedOutput output) {
        String content = validJson().replace("\"schemaVersion\":\"1.0\",",
                "\"schemaVersion\":\"1.0\",\"unexpected\":\"customer-secret-13800138000\",");

        ProcessAiProviderException error = catchThrowableOfType(
                () -> parser.parse(content), ProcessAiProviderException.class);

        assertThat(error.failureCode()).isEqualTo("AI_MODEL_RESULT_INVALID");
        assertThat(output).contains("category=UNKNOWN_FIELD", "field=unexpected")
                .doesNotContain("customer-secret", "13800138000");
    }

    @Test
    void parseRejectsResultsWithoutEvidenceAndLogsValidationPath(CapturedOutput output) {
        String content = validJson().replace(
                "\"evidence\":[{\"field\":\"diameterRule\",\"text\":\"直径一分为二\"}]",
                "\"evidence\":[]");

        ProcessAiProviderException error = catchThrowableOfType(
                () -> parser.parse(content), ProcessAiProviderException.class);

        assertThat(error.failureCode()).isEqualTo("AI_MODEL_RESULT_INVALID");
        assertThat(output).contains("category=BEAN_VALIDATION", "assignments[0].evidence:Size");
    }

    @Test
    void parseTreatsAnAllNullOptionalDiameterAsUnspecified() {
        String content = validJson().replace(
                "{\"value\":1200,\"unit\":\"mm\",\"source\":\"DEFAULT\"}",
                "{\"value\":null,\"unit\":null,\"source\":null}");

        ProcessAiExtractionResult result = parser.parse(content);

        assertThat(result.assignments().getFirst().rewindIntent()
                .diameterRule().targetDiameter()).isNull();
    }

    @Test
    void parseStillRejectsAPartiallySpecifiedDiameter() {
        String content = validJson().replace(
                "{\"value\":1200,\"unit\":\"mm\",\"source\":\"DEFAULT\"}",
                "{\"value\":1200,\"unit\":null,\"source\":\"DEFAULT\"}");

        ProcessAiProviderException error = catchThrowableOfType(
                () -> parser.parse(content), ProcessAiProviderException.class);

        assertThat(error.failureCode()).isEqualTo("AI_MODEL_RESULT_INVALID");
    }

    private String validJson() {
        return """
                {
                  "parseId":"parse-1",
                  "schemaVersion":"1.0",
                  "assignments":[{
                    "sourceRollRefs":["R1"],
                    "ownerRollRef":"R1",
                    "coveredRollRefs":[],
                    "processType":"REWIND",
                    "rewindIntent":{
                      "modeIntent":"CHANGE_DIAMETER",
                      "diameterRule":{"type":"WEIGHT_SPLIT","parts":2,"ratios":[50,50],
                        "targetDiameter":{"value":1200,"unit":"mm","source":"DEFAULT"}},
                      "core":{"value":3,"unit":"inch","source":"DEFAULT"},
                      "widthRule":null
                    },
                    "sawIntent":null,
                    "ancillaryRequirements":null,
                    "evidence":[{"field":"diameterRule","text":"直径一分为二"}]
                  }],
                  "unmappedText":[],
                  "conflicts":[],
                  "needsClarification":false,
                  "clarificationQuestions":[]
                }
                """;
    }
}
