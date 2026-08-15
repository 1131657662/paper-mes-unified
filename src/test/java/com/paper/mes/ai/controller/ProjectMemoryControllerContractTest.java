package com.paper.mes.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.memory.ProjectMemoryManagementService;
import com.paper.mes.ai.memory.dto.ProjectMemoryResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectMemoryControllerContractTest {

    private static final String TOKEN = "memory-contract-token";
    private AuthService authService;
    private ProjectMemoryManagementService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        service = mock(ProjectMemoryManagementService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProjectMemoryController(service))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void operatorCannotPatchMemory() throws Exception {
        authorizeAs("operator");

        mvc.perform(post("/api/ai/project-memory/patch")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedMemoryVersion\":\"1.0.0\",\"operations\":[],"
                                + "\"idempotencyKey\":\"k\",\"reason\":\"test\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void adminCanPatchMemory() throws Exception {
        authorizeAs("admin");
        when(service.patch(any())).thenReturn(new ProjectMemoryResponse("1.0.1", "1.0",
                "sha256:" + "0".repeat(64), "READY", new ObjectMapper().createObjectNode()));

        mvc.perform(post("/api/ai/project-memory/patch")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedMemoryVersion\":\"1.0.0\",\"operations\":[{"
                                + "\"op\":\"replace\",\"path\":\"/rules/r1/status\",\"value\":\"ACTIVE\"}],"
                                + "\"idempotencyKey\":\"k\",\"reason\":\"test\"}"))
                .andExpect(status().isOk());

        verify(service).patch(any());
    }

    private void authorizeAs(String roleCode) {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("memory-user").username("tester").realName("test").roleCode(roleCode).build());
    }
}
