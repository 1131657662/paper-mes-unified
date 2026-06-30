package com.paper.mes.system.config.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.system.config.dto.NoRulePreviewVO;
import com.paper.mes.system.config.dto.NoRuleQuery;
import com.paper.mes.system.config.dto.NoRuleSaveDTO;
import com.paper.mes.system.config.entity.SysNoRule;
import com.paper.mes.system.config.service.NoRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/system/no-rules")
@RequirePermission(Permissions.USER_MANAGE)
@RequiredArgsConstructor
public class SystemNoRuleController {

    private final NoRuleService noRuleService;

    @GetMapping
    public R<PageResult<SysNoRule>> page(NoRuleQuery query) {
        return R.success(noRuleService.page(query));
    }

    @GetMapping("/{uuid}")
    public R<SysNoRule> detail(@PathVariable String uuid) {
        return R.success(noRuleService.getByUuid(uuid));
    }

    @PutMapping("/{uuid}")
    public R<Void> update(@PathVariable String uuid, @Valid @RequestBody NoRuleSaveDTO dto) {
        noRuleService.update(uuid, dto);
        return R.success();
    }

    @GetMapping("/{bizType}/preview")
    public R<NoRulePreviewVO> preview(@PathVariable String bizType,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                      LocalDate bizDate) {
        return R.success(noRuleService.preview(bizType, bizDate == null ? LocalDate.now() : bizDate));
    }
}
