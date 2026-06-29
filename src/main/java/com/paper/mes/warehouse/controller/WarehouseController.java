package com.paper.mes.warehouse.controller;

import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.warehouse.dto.WarehouseQuery;
import com.paper.mes.warehouse.dto.WarehouseSaveDTO;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.service.WarehouseService;
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
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public R<PageResult<Warehouse>> page(WarehouseQuery query) {
        return R.success(warehouseService.pageWarehouses(query));
    }

    @GetMapping("/{uuid}")
    public R<Warehouse> detail(@PathVariable String uuid) {
        return R.success(warehouseService.getByUuid(uuid));
    }

    @PostMapping
    public R<String> create(@Valid @RequestBody WarehouseSaveDTO dto) {
        return R.success(warehouseService.create(dto));
    }

    @PutMapping("/{uuid}")
    public R<Void> update(@PathVariable String uuid, @Valid @RequestBody WarehouseSaveDTO dto) {
        warehouseService.update(uuid, dto);
        return R.success();
    }

    @DeleteMapping("/{uuid}")
    public R<Void> delete(@PathVariable String uuid) {
        warehouseService.delete(uuid);
        return R.success();
    }
}
