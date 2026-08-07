package com.paper.mes.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RSerializationTest {

    @Test
    void successWithoutPayload_keepsExplicitNullDataField() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        String json = mapper.writeValueAsString(R.<Void>success());

        assertThat(json).contains("\"code\":200");
        assertThat(json).contains("\"data\":null");
    }
}
