package com.paper.mes.backup.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.mapper.SysConfigItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackupRetentionSettingServiceTest {

    @Test
    void retentionDays_withoutPersistedSetting_returnsThirtyDays() {
        SysConfigItemMapper mapper = mock(SysConfigItemMapper.class);
        BackupRetentionSettingService service = service(mapper);

        assertEquals(30, service.retentionDays());
    }

    @Test
    void retentionDays_withOutOfRangePersistedSetting_returnsThirtyDays() {
        SysConfigItemMapper mapper = mock(SysConfigItemMapper.class);
        when(mapper.selectOne(any())).thenReturn(setting("1"));
        BackupRetentionSettingService service = service(mapper);

        assertEquals(30, service.retentionDays());
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 3650})
    void update_withBoundaryValue_persistsSetting(int days) {
        SysConfigItemMapper mapper = mock(SysConfigItemMapper.class);
        SysConfigItem item = setting("30");
        when(mapper.selectOne(any())).thenReturn(item);
        when(mapper.updateById(item)).thenReturn(1);
        BackupRetentionSettingService service = service(mapper);

        service.update(days);

        assertEquals(String.valueOf(days), item.getConfigValue());
        verify(mapper).updateById(item);
    }

    @ParameterizedTest
    @ValueSource(ints = {6, 3651})
    void update_withOutOfRangeValue_rejectsSetting(int days) {
        SysConfigItemMapper mapper = mock(SysConfigItemMapper.class);
        BackupRetentionSettingService service = service(mapper);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.update(days));

        assertEquals("备份保留天数必须在7至3650天之间", error.getMessage());
        verify(mapper, never()).updateById(any(SysConfigItem.class));
    }

    @Test
    void update_withExistingSetting_persistsNewValue() {
        SysConfigItemMapper mapper = mock(SysConfigItemMapper.class);
        SysConfigItem item = setting("30");
        when(mapper.selectOne(any())).thenReturn(item);
        when(mapper.updateById(item)).thenReturn(1);
        BackupRetentionSettingService service = service(mapper);

        service.update(90);

        assertEquals("90", item.getConfigValue());
        verify(mapper).updateById(item);
    }

    private BackupRetentionSettingService service(SysConfigItemMapper mapper) {
        return new BackupRetentionSettingService(mapper, mock(OperationLogService.class));
    }

    private SysConfigItem setting(String value) {
        SysConfigItem item = new SysConfigItem();
        item.setUuid("cfg-backup-retention-days");
        item.setConfigKey(BackupRetentionSettingService.CONFIG_KEY);
        item.setConfigValue(value);
        return item;
    }
}
