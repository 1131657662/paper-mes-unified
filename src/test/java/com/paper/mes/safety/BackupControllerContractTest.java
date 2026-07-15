package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.backup.controller.BackupController;
import com.paper.mes.backup.dto.BackupOperationVO;
import com.paper.mes.backup.service.BackupService;
import com.paper.mes.common.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BackupControllerContractTest {

    private static final String TOKEN = "test-token";
    private AuthService authService;
    private BackupService backupService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        backupService = mock(BackupService.class);
        mvc = MockMvcBuilders.standaloneSetup(new BackupController(backupService))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createBackup_withWarehouseRole_returnsForbidden() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(post("/api/system/backups").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(backupService, never()).startBackup();
    }

    @Test
    void verifyBackup_withAdminRole_startsVerification() throws Exception {
        authorizeAs("admin");
        when(backupService.startVerification("20260713-023000"))
                .thenReturn(new BackupOperationVO(true, "恢复演练已开始"));

        mvc.perform(post("/api/system/backups/20260713-023000/verify")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accepted").value(true));

        verify(backupService).startVerification("20260713-023000");
    }

    @Test
    void updateEnabled_withAdminRole_bindsEnabledState() throws Exception {
        authorizeAs("admin");

        mvc.perform(put("/api/system/backups/enabled")
                        .contentType("application/json")
                        .content("{\"enabled\":false}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(backupService).updateEnabled(false);
    }

    @Test
    void updateEnabled_withoutValue_returnsBadRequest() throws Exception {
        authorizeAs("admin");

        mvc.perform(put("/api/system/backups/enabled")
                        .contentType("application/json")
                        .content("{}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(backupService, never()).updateEnabled(anyBoolean());
    }

    @Test
    void updateRetention_withWarehouseRole_returnsForbidden() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(put("/api/system/backups/retention")
                        .contentType("application/json")
                        .content("{\"retentionDays\":30}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(backupService, never()).updateRetention(anyInt());
    }

    @Test
    void updateRetention_belowMinimum_returnsBadRequest() throws Exception {
        authorizeAs("admin");

        mvc.perform(put("/api/system/backups/retention")
                        .contentType("application/json")
                        .content("{\"retentionDays\":6}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(backupService, never()).updateRetention(anyInt());
    }

    @Test
    void deleteBackup_withWarehouseRole_returnsForbidden() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(delete("/api/system/backups/20260713-023000")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(backupService, never()).delete(any());
    }

    @Test
    void deleteBackup_withAdminRole_bindsBackupId() throws Exception {
        authorizeAs("admin");

        mvc.perform(delete("/api/system/backups/20260713-023000")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(backupService).delete("20260713-023000");
    }

    @Test
    void updateAutomatic_withWarehouseRole_returnsForbidden() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(put("/api/system/backups/automatic")
                        .contentType("application/json")
                        .content("{\"enabled\":true,\"executionTime\":\"02:35\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(backupService, never()).updateAutomatic(anyBoolean(), any());
    }

    @Test
    void updateAutomatic_withInvalidTime_returnsBadRequest() throws Exception {
        authorizeAs("admin");

        mvc.perform(put("/api/system/backups/automatic")
                        .contentType("application/json")
                        .content("{\"enabled\":true,\"executionTime\":\"24:00\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(backupService, never()).updateAutomatic(anyBoolean(), any());
    }

    @Test
    void updateAutomatic_withAdminRole_bindsSetting() throws Exception {
        authorizeAs("admin");

        mvc.perform(put("/api/system/backups/automatic")
                        .contentType("application/json")
                        .content("{\"enabled\":false,\"executionTime\":\"04:20\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(backupService).updateAutomatic(false, "04:20");
    }

    private void authorizeAs(String roleCode) {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("user-uuid").username("tester").realName("tester").roleCode(roleCode).build());
    }
}
