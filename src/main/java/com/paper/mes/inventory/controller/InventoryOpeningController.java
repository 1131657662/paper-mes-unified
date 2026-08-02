package com.paper.mes.inventory.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.inventory.dto.InventoryOpeningReconciliation;
import com.paper.mes.inventory.dto.InventoryOpeningRequest;
import com.paper.mes.inventory.service.InventoryOpeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/ledger")
@RequirePermission(Permissions.DATA_HEALTH)
@RequiredArgsConstructor
public class InventoryOpeningController {

    private final InventoryOpeningService openingService;

    @PostMapping("/opening")
    public R<InventoryOpeningReconciliation> open(@Valid @RequestBody InventoryOpeningRequest request) {
        return R.success(openingService.openCurrentProjection(request));
    }
}
