package com.paper.mes.backup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.mapper.SysConfigItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
public class BackupAutoSettingService {

    public static final String ENABLED_KEY = "backup.autoEnabled";
    public static final String TIME_KEY = "backup.autoTime";
    private static final LocalTime DEFAULT_TIME = LocalTime.of(2, 35);

    private final SysConfigItemMapper configMapper;
    private final OperationLogService operationLogService;

    public BackupAutoSettingService(SysConfigItemMapper configMapper,
                                    OperationLogService operationLogService) {
        this.configMapper = configMapper;
        this.operationLogService = operationLogService;
    }

    public BackupAutoSetting setting() {
        SysConfigItem enabledItem = find(ENABLED_KEY);
        SysConfigItem timeItem = find(TIME_KEY);
        boolean enabled = parseEnabled(enabledItem);
        return new BackupAutoSetting(enabled, parseTime(timeItem));
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(boolean enabled, String executionTime) {
        LocalTime time = parseTime(executionTime);
        SysConfigItem enabledItem = require(ENABLED_KEY);
        SysConfigItem timeItem = require(TIME_KEY);
        updateValue(enabledItem, Boolean.toString(enabled));
        updateValue(timeItem, time.toString());
        operationLogService.record(OperationLogService.BIZ_TYPE_BACKUP, enabledItem.getUuid(),
                ENABLED_KEY, OperationLogService.ACTION_FIELD_MODIFY, null,
                "自动备份已" + (enabled ? "启用，执行时间 " : "停用，保留时间 ") + time);
    }

    private LocalTime parseTime(SysConfigItem item) {
        if (item == null) return DEFAULT_TIME;
        try {
            return parseTime(item.getConfigValue());
        } catch (BusinessException ex) {
            log.warn("Invalid automatic backup time, using {}: {}", DEFAULT_TIME, item.getConfigValue());
            return DEFAULT_TIME;
        }
    }

    private boolean parseEnabled(SysConfigItem item) {
        if (item == null) return true;
        String value = item.getConfigValue();
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        log.warn("Invalid automatic backup switch, using enabled default: {}", value);
        return true;
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new BusinessException("自动备份时间格式必须为HH:mm");
        }
    }

    private SysConfigItem require(String key) {
        SysConfigItem item = find(key);
        if (item == null) throw new BusinessException("自动备份配置尚未初始化，请重启服务后重试");
        return item;
    }

    private SysConfigItem find(String key) {
        return configMapper.selectOne(new LambdaQueryWrapper<SysConfigItem>()
                .eq(SysConfigItem::getConfigKey, key)
                .eq(SysConfigItem::getIsDeleted, 0).last("LIMIT 1"));
    }

    private void updateValue(SysConfigItem item, String value) {
        item.setConfigValue(value);
        ConcurrencyGuard.requireRowUpdated(configMapper.updateById(item));
    }
}
