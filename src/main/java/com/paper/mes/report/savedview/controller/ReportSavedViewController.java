package com.paper.mes.report.savedview.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.report.savedview.dto.ReportSavedViewSaveDTO;
import com.paper.mes.report.savedview.dto.ReportSavedViewVO;
import com.paper.mes.report.savedview.service.ReportSavedViewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/report-saved-views")
@RequirePermission(Permissions.REPORT_VIEW)
@RequiredArgsConstructor
public class ReportSavedViewController {
    private static final String UUID_PATTERN = "^[0-9a-fA-F-]{32,36}$";
    private final ReportSavedViewService service;

    @GetMapping
    public R<List<ReportSavedViewVO>> list() { return R.success(service.listMine()); }

    @PostMapping
    public R<String> create(@Valid @RequestBody ReportSavedViewSaveDTO dto) { return R.success(service.create(dto)); }

    @PutMapping("/{uuid}")
    public R<Void> update(@PathVariable @Pattern(regexp = UUID_PATTERN) String uuid,
                          @Valid @RequestBody ReportSavedViewSaveDTO dto) {
        service.update(uuid, dto);
        return R.success();
    }

    @DeleteMapping("/{uuid}")
    public R<Void> delete(@PathVariable @Pattern(regexp = UUID_PATTERN) String uuid,
                          @RequestParam @Min(1) int version) {
        service.delete(uuid, version);
        return R.success();
    }
}
