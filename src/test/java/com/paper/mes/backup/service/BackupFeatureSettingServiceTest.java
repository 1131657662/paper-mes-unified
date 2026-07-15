package com.paper.mes.backup.service;

import com.paper.mes.backup.config.BackupProperties;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.mapper.SysConfigItemMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackupFeatureSettingServiceTest {

    @Test
    void isEnabled_withoutPersistedSetting_usesEnabledDefault() {
        BackupProperties properties = new BackupProperties();
        properties.setEnabled(true);
        SysConfigItemMapper mapper = mock(SysConfigItemMapper.class);
        BackupFeatureSettingService service = service(properties, mapper);

        assertTrue(service.isEnabled());
    }

    @Test
    void isEnabled_withPersistedFalse_returnsFalse() {
        BackupProperties properties = new BackupProperties();
        properties.setEnabled(true);
        SysConfigItemMapper mapper = mock(SysConfigItemMapper.class);
        when(mapper.selectOne(any())).thenReturn(setting("false"));
        BackupFeatureSettingService service = service(properties, mapper);

        assertFalse(service.isEnabled());
    }

    @Test
    void updateEnabled_withExistingSetting_persistsTrue() {
        BackupProperties properties = new BackupProperties();
        SysConfigItemMapper mapper = mock(SysConfigItemMapper.class);
        SysConfigItem item = setting("false");
        when(mapper.selectOne(any())).thenReturn(item);
        when(mapper.updateById(item)).thenReturn(1);
        BackupFeatureSettingService service = service(properties, mapper);

        service.updateEnabled(true);

        assertEquals("true", item.getConfigValue());
        verify(mapper).updateById(item);
    }

    private BackupFeatureSettingService service(BackupProperties properties, SysConfigItemMapper mapper) {
        return new BackupFeatureSettingService(properties, mapper, mock(OperationLogService.class));
    }

    private SysConfigItem setting(String value) {
        SysConfigItem item = new SysConfigItem();
        item.setUuid("cfg-backup-management-enabled");
        item.setConfigKey(BackupFeatureSettingService.CONFIG_KEY);
        item.setConfigValue(value);
        return item;
    }

}
