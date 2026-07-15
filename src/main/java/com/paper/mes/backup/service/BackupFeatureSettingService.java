package com.paper.mes.backup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.backup.config.BackupProperties;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.mapper.SysConfigItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackupFeatureSettingService {

    public static final String CONFIG_KEY = "backup.managementEnabled";

    private final BackupProperties properties;
    private final SysConfigItemMapper configItemMapper;
    private final OperationLogService operationLogService;

    public BackupFeatureSettingService(BackupProperties properties, SysConfigItemMapper configItemMapper,
                                       OperationLogService operationLogService) {
        this.properties = properties;
        this.configItemMapper = configItemMapper;
        this.operationLogService = operationLogService;
    }

    public boolean isEnabled() {
        SysConfigItem item = findSetting();
        return item == null ? properties.isEnabled() : Boolean.parseBoolean(item.getConfigValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateEnabled(boolean enabled) {
        SysConfigItem item = findSetting();
        if (item == null) {
            throw new BusinessException("备份开关尚未初始化，请重启服务后重试");
        }
        item.setConfigValue(Boolean.toString(enabled));
        ConcurrencyGuard.requireRowUpdated(configItemMapper.updateById(item));
        operationLogService.record(OperationLogService.BIZ_TYPE_BACKUP, item.getUuid(), CONFIG_KEY,
                OperationLogService.ACTION_FIELD_MODIFY, null, enabled ? "启用管理端备份" : "停用管理端备份");
    }

    private SysConfigItem findSetting() {
        return configItemMapper.selectOne(new LambdaQueryWrapper<SysConfigItem>()
                .eq(SysConfigItem::getConfigKey, CONFIG_KEY)
                .eq(SysConfigItem::getIsDeleted, 0)
                .last("LIMIT 1"));
    }
}
