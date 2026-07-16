package com.paper.mes.auth.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthUserVOSerializationTest {

    @Test
    void serialize_whenTokenIsPresent_doesNotExposeToken() throws Exception {
        AuthUserVO user = AuthUserVO.builder()
                .uuid("user-1")
                .username("admin")
                .accessToken("session-secret")
                .permissions(List.of("system:read"))
                .build();

        String json = new ObjectMapper().writeValueAsString(user);

        assertTrue(json.contains("\"username\":\"admin\""));
        assertFalse(json.contains("accessToken"));
        assertFalse(json.contains("session-secret"));
    }
}
