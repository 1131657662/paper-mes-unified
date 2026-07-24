package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.processorder.controller.ProcessCatalogController;
import com.paper.mes.processorder.service.ProcessCatalogService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProcessCatalogControllerContractTest {

    private AuthService authService;
    private ProcessCatalogService catalogService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        catalogService = mock(ProcessCatalogService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProcessCatalogController(catalogService))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listActive_withoutToken_isRejected() throws Exception {
        mvc.perform(get("/api/process-catalog"))
                .andExpect(status().isUnauthorized());

        verify(catalogService, never()).listActive();
    }

    @Test
    void listActive_withViewerRole_isAuthorized() throws Exception {
        authorizeViewer();
        when(catalogService.listActive()).thenReturn(List.of());

        mvc.perform(get("/api/process-catalog")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(catalogService).listActive();
    }

    private void authorizeViewer() {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn("token");
        when(authService.currentUser("token")).thenReturn(CurrentUser.builder()
                .uuid("viewer").username("viewer").roleCode("viewer").build());
    }
}
