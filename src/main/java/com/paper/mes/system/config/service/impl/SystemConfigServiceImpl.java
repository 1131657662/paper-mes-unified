package com.paper.mes.system.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.PageResult;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.config.dto.ConfigItemQuery;
import com.paper.mes.system.config.dto.ConfigItemSaveDTO;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.mapper.SysConfigItemMapper;
import com.paper.mes.system.config.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class SystemConfigServiceImpl extends ServiceImpl<SysConfigItemMapper, SysConfigItem> implements SystemConfigService {

    private static final Set<String> VALUE_TYPES = Set.of("string", "number", "boolean");
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    private final OperationLogService operationLogService;

    public SystemConfigServiceImpl(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    public PageResult<SysConfigItem> page(ConfigItemQuery query) {
        Page<SysConfigItem> page = page(Page.of(query.getCurrent(), query.getSize()), buildWrapper(query));
        return PageResult.of(page);
    }

    @Override
    public List<SysConfigItem> enabledByKeys(List<String> configKeys) {
        List<String> keys = configKeys.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (keys.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<SysConfigItem>()
                .in(SysConfigItem::getConfigKey, keys)
                .eq(SysConfigItem::getStatus, STATUS_ENABLED)
                .orderByAsc(SysConfigItem::getConfigGroup)
                .orderByAsc(SysConfigItem::getSortNo));
    }

    @Override
    public SysConfigItem getByUuid(String uuid) {
        SysConfigItem item = getById(uuid);
        if (item == null) {
            throw new BusinessException("系统参数不存在");
        }
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ConfigItemSaveDTO dto) {
        ensureStatus(dto.getStatus());
        ensureValueType(dto.getValueType());
        ensureUnique(dto.getConfigKey(), null);
        SysConfigItem item = new SysConfigItem();
        applyDto(item, dto);
        item.setBuiltIn(0);
        save(item);
        record(item, "新增参数", "新增系统参数：" + item.getConfigName());
        return item.getUuid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String uuid, ConfigItemSaveDTO dto) {
        ensureStatus(dto.getStatus());
        ensureValueType(dto.getValueType());
        SysConfigItem item = getByUuid(uuid);
        ensureUnique(dto.getConfigKey(), uuid);
        Integer version = item.getVersion();
        Integer builtIn = item.getBuiltIn();
        applyDto(item, dto);
        item.setUuid(uuid);
        item.setVersion(version);
        item.setBuiltIn(builtIn);
        ConcurrencyGuard.requireUpdated(updateById(item));
        record(item, "编辑参数", "编辑系统参数：" + item.getConfigName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String uuid) {
        SysConfigItem item = getByUuid(uuid);
        if (Integer.valueOf(1).equals(item.getBuiltIn())) {
            throw new BusinessException("内置系统参数不允许删除，可停用或调整参数值");
        }
        ConcurrencyGuard.requireUpdated(removeById(uuid));
        record(item, "删除参数", "删除系统参数：" + item.getConfigName());
    }

    private LambdaQueryWrapper<SysConfigItem> buildWrapper(ConfigItemQuery query) {
        LambdaQueryWrapper<SysConfigItem> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysConfigItem::getConfigKey, kw)
                    .or().like(SysConfigItem::getConfigName, kw)
                    .or().like(SysConfigItem::getRemark, kw));
        }
        if (StringUtils.hasText(query.getConfigGroup())) {
            wrapper.eq(SysConfigItem::getConfigGroup, query.getConfigGroup().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysConfigItem::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysConfigItem::getConfigGroup)
                .orderByAsc(SysConfigItem::getSortNo)
                .orderByDesc(SysConfigItem::getCreateTime);
        return wrapper;
    }

    private void applyDto(SysConfigItem item, ConfigItemSaveDTO dto) {
        item.setConfigGroup(dto.getConfigGroup().trim());
        item.setConfigKey(dto.getConfigKey().trim());
        item.setConfigName(dto.getConfigName().trim());
        item.setConfigValue(dto.getConfigValue().trim());
        item.setValueType(dto.getValueType().trim());
        item.setUnit(dto.getUnit());
        item.setSortNo(dto.getSortNo() == null ? 100 : dto.getSortNo());
        item.setStatus(dto.getStatus());
        item.setRemark(dto.getRemark());
    }

    private void ensureUnique(String configKey, String excludeUuid) {
        LambdaQueryWrapper<SysConfigItem> wrapper = new LambdaQueryWrapper<SysConfigItem>()
                .eq(SysConfigItem::getConfigKey, configKey.trim());
        if (StringUtils.hasText(excludeUuid)) {
            wrapper.ne(SysConfigItem::getUuid, excludeUuid);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("参数键已存在：" + configKey);
        }
    }

    private void ensureStatus(Integer status) {
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new BusinessException("状态不正确");
        }
    }

    private void ensureValueType(String valueType) {
        if (!StringUtils.hasText(valueType) || !VALUE_TYPES.contains(valueType.trim())) {
            throw new BusinessException("参数值类型不正确");
        }
    }

    private void record(SysConfigItem item, String action, String remark) {
        operationLogService.record(OperationLogService.BIZ_TYPE_SYSTEM_CONFIG, item.getUuid(), item.getConfigKey(), action, null, remark);
    }
}
