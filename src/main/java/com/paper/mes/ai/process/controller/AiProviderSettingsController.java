package com.paper.mes.ai.process.controller;

import com.paper.mes.ai.process.credential.AiProviderSettingsService;
import com.paper.mes.ai.process.credential.dto.AiProviderKeyUpdateRequest;
import com.paper.mes.ai.process.credential.dto.AiProviderSettingsResponse;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/provider-settings/{provider}")
@RequirePermission(Permissions.SYSTEM_CONFIG)
@RequiredArgsConstructor
public class AiProviderSettingsController {

    private final AiProviderSettingsService settingsService;

    @GetMapping
    public R<AiProviderSettingsResponse> get(@PathVariable String provider) {
        return R.success(settingsService.get(provider));
    }

    @PutMapping
    public R<AiProviderSettingsResponse> update(
            @PathVariable String provider,
            @Valid @RequestBody AiProviderKeyUpdateRequest request) {
        return R.success(settingsService.update(provider, request));
    }

    @DeleteMapping
    public R<AiProviderSettingsResponse> delete(@PathVariable String provider) {
        return R.success(settingsService.delete(provider));
    }
}
