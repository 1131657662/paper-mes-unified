package com.paper.mes.system.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.PageResult;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.config.dto.DictItemQuery;
import com.paper.mes.system.config.dto.DictItemSaveDTO;
import com.paper.mes.system.config.entity.SysDictItem;
import com.paper.mes.system.config.mapper.SysDictItemMapper;
import com.paper.mes.system.config.service.SystemDictService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SystemDictServiceImpl extends ServiceImpl<SysDictItemMapper, SysDictItem> implements SystemDictService {

    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    private final OperationLogService operationLogService;

    public SystemDictServiceImpl(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    public PageResult<SysDictItem> page(DictItemQuery query) {
        Page<SysDictItem> page = page(Page.of(query.getCurrent(), query.getSize()), buildWrapper(query));
        return PageResult.of(page);
    }

    @Override
    public List<SysDictItem> enabledByTypes(List<String> dictTypes) {
        List<String> types = dictTypes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (types.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<SysDictItem>()
                .in(SysDictItem::getDictType, types)
                .eq(SysDictItem::getStatus, STATUS_ENABLED)
                .orderByAsc(SysDictItem::getDictType)
                .orderByAsc(SysDictItem::getSortNo)
                .orderByAsc(SysDictItem::getItemValue));
    }

    @Override
    public SysDictItem getByUuid(String uuid) {
        SysDictItem item = getById(uuid);
        if (item == null) {
            throw new BusinessException("字典项不存在");
        }
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(DictItemSaveDTO dto) {
        ensureStatus(dto.getStatus());
        ensureUnique(dto.getDictType(), dto.getItemCode(), null);
        SysDictItem item = new SysDictItem();
        applyDto(item, dto);
        item.setBuiltIn(0);
        save(item);
        record(item, "新增字典", "新增字典项：" + item.getDictName() + "/" + item.getItemName());
        return item.getUuid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String uuid, DictItemSaveDTO dto) {
        ensureStatus(dto.getStatus());
        SysDictItem item = getByUuid(uuid);
        ensureUnique(dto.getDictType(), dto.getItemCode(), uuid);
        Integer version = item.getVersion();
        Integer builtIn = item.getBuiltIn();
        applyDto(item, dto);
        item.setUuid(uuid);
        item.setVersion(version);
        item.setBuiltIn(builtIn);
        updateById(item);
        record(item, "编辑字典", "编辑字典项：" + item.getDictName() + "/" + item.getItemName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String uuid) {
        SysDictItem item = getByUuid(uuid);
        if (Integer.valueOf(1).equals(item.getBuiltIn())) {
            throw new BusinessException("内置字典项不允许删除，可停用或调整排序");
        }
        removeById(uuid);
        record(item, "删除字典", "删除字典项：" + item.getDictName() + "/" + item.getItemName());
    }

    private LambdaQueryWrapper<SysDictItem> buildWrapper(DictItemQuery query) {
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysDictItem::getDictName, kw)
                    .or().like(SysDictItem::getItemCode, kw)
                    .or().like(SysDictItem::getItemName, kw));
        }
        if (StringUtils.hasText(query.getDictType())) {
            wrapper.eq(SysDictItem::getDictType, query.getDictType().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysDictItem::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysDictItem::getDictType)
                .orderByAsc(SysDictItem::getSortNo)
                .orderByDesc(SysDictItem::getCreateTime);
        return wrapper;
    }

    private void applyDto(SysDictItem item, DictItemSaveDTO dto) {
        item.setDictType(dto.getDictType().trim());
        item.setDictName(dto.getDictName().trim());
        item.setItemCode(dto.getItemCode().trim());
        item.setItemName(dto.getItemName().trim());
        item.setItemValue(dto.getItemValue());
        item.setSortNo(dto.getSortNo() == null ? 100 : dto.getSortNo());
        item.setStatus(dto.getStatus());
        item.setRemark(dto.getRemark());
    }

    private void ensureUnique(String dictType, String itemCode, String excludeUuid) {
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictType, dictType.trim())
                .eq(SysDictItem::getItemCode, itemCode.trim());
        if (StringUtils.hasText(excludeUuid)) {
            wrapper.ne(SysDictItem::getUuid, excludeUuid);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("同一字典分类下编码已存在：" + itemCode);
        }
    }

    private void ensureStatus(Integer status) {
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new BusinessException("状态不正确");
        }
    }

    private void record(SysDictItem item, String action, String remark) {
        operationLogService.record(OperationLogService.BIZ_TYPE_SYSTEM_CONFIG, item.getUuid(), item.getItemCode(), action, null, remark);
    }
}
