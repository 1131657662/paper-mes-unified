package com.paper.mes.paper.controller;

import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.paper.dto.PaperQuery;
import com.paper.mes.paper.dto.PaperSaveDTO;
import com.paper.mes.paper.entity.Paper;
import com.paper.mes.paper.service.PaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
@Tag(name = "Papers", description = "Paper master data")
public class PaperController {

    private final PaperService paperService;

    @GetMapping
    @RequirePermission(Permissions.BASE_VIEW)
    @Operation(operationId = "listPapers", summary = "List papers")
    public R<PageResult<Paper>> page(@ParameterObject PaperQuery query) {
        return R.success(paperService.pagePapers(query));
    }

    @GetMapping("/{uuid}")
    @RequirePermission(Permissions.BASE_VIEW)
    @Operation(operationId = "getPaper", summary = "Get a paper profile")
    public R<Paper> detail(@Parameter(description = "Paper UUID", required = true)
                           @PathVariable String uuid) {
        return R.success(paperService.getByUuid(uuid));
    }

    @PostMapping
    @RequirePermission(Permissions.BASE_MANAGE)
    public R<String> create(@Valid @RequestBody PaperSaveDTO dto) {
        return R.success(paperService.create(dto));
    }

    @PutMapping("/{uuid}")
    @RequirePermission(Permissions.BASE_MANAGE)
    public R<Void> update(@PathVariable String uuid, @Valid @RequestBody PaperSaveDTO dto) {
        paperService.update(uuid, dto);
        return R.success();
    }

    @DeleteMapping("/{uuid}")
    @RequirePermission(Permissions.BASE_MANAGE)
    public R<Void> delete(@PathVariable String uuid) {
        paperService.delete(uuid);
        return R.success();
    }
}
