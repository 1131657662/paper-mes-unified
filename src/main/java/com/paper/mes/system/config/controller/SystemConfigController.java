package com.paper.mes.system.config.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.system.config.dto.ConfigItemQuery;
import com.paper.mes.system.config.dto.ConfigItemSaveDTO;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/config-items")
@RequirePermission(Permissions.USER_MANAGE)
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public R<PageResult<SysConfigItem>> page(ConfigItemQuery query) {
        return R.success(systemConfigService.page(query));
    }

    @GetMapping("/{uuid}")
    public R<SysConfigItem> detail(@PathVariable String uuid) {
        return R.success(systemConfigService.getByUuid(uuid));
    }

    @PostMapping
    public R<String> create(@Valid @RequestBody ConfigItemSaveDTO dto) {
        return R.success(systemConfigService.create(dto));
    }

    @PutMapping("/{uuid}")
    public R<Void> update(@PathVariable String uuid, @Valid @RequestBody ConfigItemSaveDTO dto) {
        systemConfigService.update(uuid, dto);
        return R.success();
    }

    @DeleteMapping("/{uuid}")
    public R<Void> delete(@PathVariable String uuid) {
        systemConfigService.delete(uuid);
        return R.success();
    }
}
