package com.paper.mes.safety;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.paper.mes.processorder.dto.DraftOrderBaseDTO;
import com.paper.mes.processorder.dto.ProcessOrderCreateDTO;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderTeamGroupWriteContractTest {

    private static final Path CREATE_DTO = Path.of(
            "src/main/java/com/paper/mes/processorder/dto/ProcessOrderCreateDTO.java");
    private static final Path DRAFT_DTO = Path.of(
            "src/main/java/com/paper/mes/processorder/dto/DraftOrderBaseDTO.java");

    @Test
    void writeDtos_doNotExposeHistoricalTeamGroupProperty() throws IOException {
        assertThat(hasField(ProcessOrderCreateDTO.class, "teamGroup")).isFalse();
        assertThat(hasField(DraftOrderBaseDTO.class, "teamGroup")).isFalse();
        assertThat(hasField(ProcessOrder.class, "teamGroup")).isTrue();
        assertThat(Files.readString(CREATE_DTO, StandardCharsets.UTF_8)).doesNotContain("teamGroup");
        assertThat(Files.readString(DRAFT_DTO, StandardCharsets.UTF_8)).doesNotContain("teamGroup");
    }

    @Test
    void legacyJsonField_isNotBoundToEitherWriteDto() throws IOException {
        ObjectMapper mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        ProcessOrderCreateDTO create = mapper.readValue("{\"team_group\":\"legacy\"}",
                ProcessOrderCreateDTO.class);
        DraftOrderBaseDTO draft = mapper.readValue("{\"team_group\":\"legacy\"}",
                DraftOrderBaseDTO.class);

        assertThat(hasField(create.getClass(), "teamGroup")).isFalse();
        assertThat(hasField(draft.getClass(), "teamGroup")).isFalse();
    }

    private boolean hasField(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field != null;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
    }
}
