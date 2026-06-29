package com.paper.mes.warehouse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.paper.mes.common.PageResult;
import com.paper.mes.warehouse.dto.WarehouseQuery;
import com.paper.mes.warehouse.dto.WarehouseSaveDTO;
import com.paper.mes.warehouse.entity.Warehouse;

public interface WarehouseService extends IService<Warehouse> {

    PageResult<Warehouse> pageWarehouses(WarehouseQuery query);

    Warehouse getByUuid(String uuid);

    String create(WarehouseSaveDTO dto);

    void update(String uuid, WarehouseSaveDTO dto);

    void delete(String uuid);
}
