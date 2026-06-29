package com.paper.mes.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.PageResult;
import com.paper.mes.warehouse.dto.WarehouseQuery;
import com.paper.mes.warehouse.dto.WarehouseSaveDTO;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.mapper.WarehouseMapper;
import com.paper.mes.warehouse.service.WarehouseService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements WarehouseService {

    private static final int STATUS_ENABLED = 1;

    @Override
    public PageResult<Warehouse> pageWarehouses(WarehouseQuery query) {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(Warehouse::getWarehouseCode, kw)
                    .or().like(Warehouse::getWarehouseName, kw));
        }
        if (query.getStatus() != null) {
            wrapper.eq(Warehouse::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Warehouse::getCreateTime);
        Page<Warehouse> page = page(Page.of(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public Warehouse getByUuid(String uuid) {
        Warehouse warehouse = getById(uuid);
        if (warehouse == null) {
            throw new BusinessException("仓库不存在");
        }
        return warehouse;
    }

    @Override
    public String create(WarehouseSaveDTO dto) {
        ensureCodeUnique(dto.getWarehouseCode(), null);
        Warehouse warehouse = new Warehouse();
        BeanUtils.copyProperties(dto, warehouse);
        if (warehouse.getStatus() == null) {
            warehouse.setStatus(STATUS_ENABLED);
        }
        save(warehouse);
        return warehouse.getUuid();
    }

    @Override
    public void update(String uuid, WarehouseSaveDTO dto) {
        Warehouse existing = getByUuid(uuid);
        ensureCodeUnique(dto.getWarehouseCode(), uuid);
        Integer savedVersion = existing.getVersion();
        Integer keepStatus = existing.getStatus();
        BeanUtils.copyProperties(dto, existing);
        existing.setUuid(uuid);
        existing.setVersion(savedVersion);
        if (existing.getStatus() == null) {
            existing.setStatus(keepStatus);
        }
        updateById(existing);
    }

    @Override
    public void delete(String uuid) {
        getByUuid(uuid);
        removeById(uuid);
    }

    private void ensureCodeUnique(String warehouseCode, String excludeUuid) {
        if (!StringUtils.hasText(warehouseCode)) {
            return;
        }
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getWarehouseCode, warehouseCode);
        if (StringUtils.hasText(excludeUuid)) {
            wrapper.ne(Warehouse::getUuid, excludeUuid);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("仓库编码已存在：" + warehouseCode);
        }
    }
}
