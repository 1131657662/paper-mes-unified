package com.paper.mes.processorder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderAppendJsonTest {

    private final ProcessOrderAppendJson json = new ProcessOrderAppendJson(new ObjectMapper());

    @Test
    void referencesAnyOriginalUuid_whenNestedSourceMatches_returnsTrue() {
        String config = """
                {"segments":[{"sources":[{"originalUuid":"source-2"}]}]}
                """;

        boolean referenced = json.referencesAnyOriginalUuid(config, Set.of("source-2"));

        assertThat(referenced).isTrue();
    }

    @Test
    void referencesAnyOriginalUuid_whenOnlyDifferentSourceExists_returnsFalse() {
        String config = """
                {"segments":[{"sources":[{"originalUuid":"source-1"}]}]}
                """;

        boolean referenced = json.referencesAnyOriginalUuid(config, Set.of("source-2"));

        assertThat(referenced).isFalse();
    }

    @Test
    void readServiceSteps_whenJsonContainsServiceDefinitions_returnsTypedSteps() {
        List<com.paper.mes.processorder.dto.ProcessStepDTO> steps = json.readServiceSteps("""
                [{"stepType":3,"originalUuid":"client-roll","isMain":0}]
                """);

        assertThat(steps).singleElement()
                .extracting(com.paper.mes.processorder.dto.ProcessStepDTO::getStepType)
                .isEqualTo(3);
    }
}
