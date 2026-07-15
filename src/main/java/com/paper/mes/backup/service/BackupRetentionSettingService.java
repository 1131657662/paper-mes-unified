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

@Service
@Slf4j
public class BackupRetentionSettingService {

    public static final String CONFIG_KEY = "backup.retentionDays";
    private static final int DEFAULT_DAYS = 30;
    private final SysConfigItemMapper configMapper;
    private final OperationLogService operationLogService;

    public BackupRetentionSettingService(SysConfigItemMapper configMapper,
                                         OperationLogService operationLogService) {
        this.configMapper = configMapper;
        this.operationLogService = operationLogService;
    }

    public int retentionDays() {
        SysConfigItem item = findSetting();
        if (item == null) return DEFAULT_DAYS;
        try {
            return validate(Integer.parseInt(item.getConfigValue()));
        } catch (NumberFormatException | BusinessException ex) {
            log.warn("Invalid backup retention setting, using {} days: {}", DEFAULT_DAYS,
                    item.getConfigValue());
            return DEFAULT_DAYS;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(int retentionDays) {
        int validated = validate(retentionDays);
        SysConfigItem item = findSetting();
        if (item == null) throw new BusinessException("备份保留策略尚未初始化，请重启服务后重试");
        item.setConfigValue(String.valueOf(validated));
        ConcurrencyGuard.requireRowUpdated(configMapper.updateById(item));
        operationLogService.record(OperationLogService.BIZ_TYPE_BACKUP, item.getUuid(), CONFIG_KEY,
                OperationLogService.ACTION_FIELD_MODIFY, null, "备份保留天数调整为 " + validated + " 天");
    }

    private int validate(int days) {
        if (days < 7 || days > 3650) throw new BusinessException("备份保留天数必须在7至3650天之间");
        return days;
    }

    private SysConfigItem findSetting() {
        return configMapper.selectOne(new LambdaQueryWrapper<SysConfigItem>()
                .eq(SysConfigItem::getConfigKey, CONFIG_KEY).last("LIMIT 1"));
    }
}
