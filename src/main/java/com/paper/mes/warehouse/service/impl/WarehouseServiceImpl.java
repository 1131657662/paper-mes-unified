package com.paper.mes.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.PageResult;
import com.paper.mes.warehouse.dto.WarehouseQuery;
import com.paper.mes.warehouse.dto.WarehouseSaveDTO;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.mapper.WarehouseMapper;
import com.paper.mes.warehouse.service.WarehouseService;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.service.DocumentNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements WarehouseService {

    private static final int STATUS_ENABLED = 1;

    private final DocumentNoService documentNoService;

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
    @Transactional(rollbackFor = Exception.class)
    public String create(WarehouseSaveDTO dto) {
        Warehouse warehouse = new Warehouse();
        BeanUtils.copyProperties(dto, warehouse);
        warehouse.setWarehouseCode(documentNoService.next(NoRuleBizType.WAREHOUSE, LocalDate.now()));
        if (warehouse.getStatus() == null) {
            warehouse.setStatus(STATUS_ENABLED);
        }
        save(warehouse);
        return warehouse.getUuid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String uuid, WarehouseSaveDTO dto) {
        Warehouse existing = getByUuid(uuid);
        Integer savedVersion = existing.getVersion();
        Integer keepStatus = existing.getStatus();
        String savedCode = existing.getWarehouseCode();
        BeanUtils.copyProperties(dto, existing);
        existing.setUuid(uuid);
        existing.setVersion(savedVersion);
        existing.setWarehouseCode(keepCodeOrGenerate(savedCode));
        if (existing.getStatus() == null) {
            existing.setStatus(keepStatus);
        }
        ConcurrencyGuard.requireUpdated(updateById(existing));
    }

    @Override
    public void delete(String uuid) {
        getByUuid(uuid);
        removeById(uuid);
    }

    private String keepCodeOrGenerate(String code) {
        return StringUtils.hasText(code) ? code : documentNoService.next(NoRuleBizType.WAREHOUSE, LocalDate.now());
    }
}
