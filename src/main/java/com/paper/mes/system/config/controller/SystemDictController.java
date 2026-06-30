package com.paper.mes.system.config.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.system.config.dto.DictItemQuery;
import com.paper.mes.system.config.dto.DictItemSaveDTO;
import com.paper.mes.system.config.entity.SysDictItem;
import com.paper.mes.system.config.service.SystemDictService;
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
@RequestMapping("/api/system/dict-items")
@RequirePermission(Permissions.USER_MANAGE)
@RequiredArgsConstructor
public class SystemDictController {

    private final SystemDictService systemDictService;

    @GetMapping
    public R<PageResult<SysDictItem>> page(DictItemQuery query) {
        return R.success(systemDictService.page(query));
    }

    @GetMapping("/{uuid}")
    public R<SysDictItem> detail(@PathVariable String uuid) {
        return R.success(systemDictService.getByUuid(uuid));
    }

    @PostMapping
    public R<String> create(@Valid @RequestBody DictItemSaveDTO dto) {
        return R.success(systemDictService.create(dto));
    }

    @PutMapping("/{uuid}")
    public R<Void> update(@PathVariable String uuid, @Valid @RequestBody DictItemSaveDTO dto) {
        systemDictService.update(uuid, dto);
        return R.success();
    }

    @DeleteMapping("/{uuid}")
    public R<Void> delete(@PathVariable String uuid) {
        systemDictService.delete(uuid);
        return R.success();
    }
}
