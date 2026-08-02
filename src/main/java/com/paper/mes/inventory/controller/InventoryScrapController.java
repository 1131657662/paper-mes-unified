package com.paper.mes.inventory.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.inventory.dto.InventoryScrapDTO;
import com.paper.mes.inventory.service.FinishedGoodsScrapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/finish-rolls")
@RequirePermission(Permissions.INVENTORY_SCRAP)
@RequiredArgsConstructor
public class InventoryScrapController {

    private final FinishedGoodsScrapService scrapService;

    @PostMapping("/{finishUuid}/scrap")
    public R<Void> scrap(@PathVariable String finishUuid,
                         @Valid @RequestBody InventoryScrapDTO dto) {
        scrapService.scrap(finishUuid, dto);
        return R.success();
    }
}
