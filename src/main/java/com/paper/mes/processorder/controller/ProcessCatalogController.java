package com.paper.mes.processorder.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.service.ProcessCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/process-catalog")
@RequirePermission(Permissions.ORDER_VIEW)
@RequiredArgsConstructor
public class ProcessCatalogController {

    private final ProcessCatalogService catalogService;

    @GetMapping
    public R<List<ProcessCatalogVO>> listActive() {
        return R.success(catalogService.listActive());
    }
}
