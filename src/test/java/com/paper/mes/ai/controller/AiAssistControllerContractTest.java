package com.paper.mes.ai.controller;

import com.paper.mes.ai.config.AiDataMode;
import com.paper.mes.ai.dto.AiAssistResponse;
import com.paper.mes.ai.dto.AiAssistRequest;
import com.paper.mes.ai.dto.AiStatusResponse;
import com.paper.mes.ai.service.AiAssistService;
import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiAssistControllerContractTest {

    private static final String TOKEN = "ai-contract-token";

    private AuthService authService;
    private AiAssistService assistService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        assistService = mock(AiAssistService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AiAssistController(assistService))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void statusWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mvc.perform(get("/api/ai/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(assistService, never()).status();
    }

    @Test
    void viewerCannotUseAi() throws Exception {
        authorizeAs("viewer");

        mvc.perform(get("/api/ai/status").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(assistService, never()).status();
    }

    @Test
    void authorizedRoleCanReadStatus() throws Exception {
        authorizeAs("operator");
        when(assistService.status()).thenReturn(new AiStatusResponse(
                true, AiDataMode.FAQ_ONLY, "rules-v1.0.0", true,
                com.paper.mes.ai.config.AiProvider.LOCAL_RULES));

        mvc.perform(get("/api/ai/status").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.dataMode").value("FAQ_ONLY"))
                .andExpect(jsonPath("$.data.provider").value("LOCAL_RULES"));
    }

    @Test
    void invalidAssistPayloadReturnsBadRequest() throws Exception {
        authorizeAs("operator");

        mvc.perform(post("/api/ai/assist")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\",\"pageTemplate\":\"BAD_TEMPLATE!\",\"contextEpoch\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(assistService, never()).assist(any());
    }

    @Test
    void authorizedRoleCanSubmitAllowlistedRequest() throws Exception {
        authorizeAs("operator");
        when(assistService.assist(any())).thenReturn(new AiAssistResponse(
                "request-id", "CLARIFY", "LOW", "请补充错误码。", List.of(), List.of(),
                AiDataMode.FAQ_ONLY, "LOCAL_RULES"));

        mvc.perform(post("/api/ai/assist")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"为什么不能操作\",\"pageTemplate\":\"process-orders\","
                                + "\"contextEpoch\":\"opaque-key\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision").value("CLARIFY"))
                .andExpect(jsonPath("$.data.provider").value("LOCAL_RULES"));

        verify(assistService).assist(any(AiAssistRequest.class));
    }

    private void authorizeAs(String roleCode) {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("ai-user")
                .username("tester")
                .realName("测试员")
                .roleCode(roleCode)
                .build());
    }
}
