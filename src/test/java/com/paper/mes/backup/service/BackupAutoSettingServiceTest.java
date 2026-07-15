package com.paper.mes.backup.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.mapper.SysConfigItemMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackupAutoSettingServiceTest {

    @Test
    void setting_withoutPersistedValues_returnsEnabledDefault() {
        BackupAutoSettingService service = service(mock(SysConfigItemMapper.class));

        BackupAutoSetting setting = service.setting();

        assertTrue(setting.enabled());
        assertEquals(LocalTime.of(2, 35), setting.executionTime());
    }

    @Test
    void setting_withInvalidTime_returnsDefaultTime() {
        SysConfigItemMapper mapper = mock(SysConfigItemMapper.class);
        when(mapper.selectOne(any())).thenReturn(setting("broken"), setting("25:00"));
        BackupAutoSettingService service = service(mapper);

        BackupAutoSetting setting = service.setting();

        assertEquals(LocalTime.of(2, 35), setting.executionTime());
    }

    @Test
    void setting_withInvalidSwitch_returnsEnabledDefault() {
        SysConfigItemMapper mapper = mock(SysConfigItemMapper.class);
        when(mapper.selectOne(any())).thenReturn(setting("broken"), setting("02:35"));
        BackupAutoSettingService service = service(mapper);

        assertTrue(service.setting().enabled());
    }

    @Test
    void update_withValidValues_persistsBothSettings() {
        SysConfigItemMapper mapper = mock(SysConfigItemMapper.class);
        SysConfigItem enabled = setting("true");
        SysConfigItem time = setting("02:35");
        when(mapper.selectOne(any())).thenReturn(enabled, time);
        when(mapper.updateById(any(SysConfigItem.class))).thenReturn(1);
        BackupAutoSettingService service = service(mapper);

        service.update(false, "04:20");

        assertEquals("false", enabled.getConfigValue());
        assertEquals("04:20", time.getConfigValue());
        verify(mapper).updateById(enabled);
        verify(mapper).updateById(time);
    }

    @Test
    void update_withInvalidTime_rejectsChange() {
        BackupAutoSettingService service = service(mock(SysConfigItemMapper.class));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.update(true, "24:00"));

        assertEquals("自动备份时间格式必须为HH:mm", error.getMessage());
    }

    private BackupAutoSettingService service(SysConfigItemMapper mapper) {
        return new BackupAutoSettingService(mapper, mock(OperationLogService.class));
    }

    private SysConfigItem setting(String value) {
        SysConfigItem item = new SysConfigItem();
        item.setUuid("setting-" + value);
        item.setConfigValue(value);
        return item;
    }
}
