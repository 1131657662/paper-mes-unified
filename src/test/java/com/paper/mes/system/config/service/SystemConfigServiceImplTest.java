package com.paper.mes.system.config.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.config.dto.ConfigItemSaveDTO;
import com.paper.mes.system.config.entity.SysConfigItem;
import com.paper.mes.system.config.mapper.SysConfigItemMapper;
import com.paper.mes.system.config.service.impl.SystemConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemConfigServiceImplTest {

    private SysConfigItemMapper mapper;
    private SystemConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysConfigItemMapper.class);
        service = new SystemConfigServiceImpl(mock(OperationLogService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void create_withInvalidNumberValue_rejectsSave() {
        assertThrows(BusinessException.class, () -> service.create(dto("number", "abc")));

        verify(mapper, never()).insert(any(SysConfigItem.class));
    }

    @Test
    void create_withInvalidBooleanValue_rejectsSave() {
        assertThrows(BusinessException.class, () -> service.create(dto("boolean", "yes")));

        verify(mapper, never()).insert(any(SysConfigItem.class));
    }

    @Test
    void create_withFractionalSpareRollCount_rejectsSave() {
        ConfigItemSaveDTO dto = dto("number", "1.5");
        dto.setConfigKey("process.spareRollNoCount");

        assertThrows(BusinessException.class, () -> service.create(dto));

        verify(mapper, never()).insert(any(SysConfigItem.class));
    }

    @Test
    void create_withExcessiveSpareRollCount_rejectsSave() {
        ConfigItemSaveDTO dto = dto("number", "101");
        dto.setConfigKey("process.spareRollNoCount");

        assertThrows(BusinessException.class, () -> service.create(dto));

        verify(mapper, never()).insert(any(SysConfigItem.class));
    }

    @Test
    void delete_withBuiltInItem_rejectsDeletion() {
        SysConfigItem item = new SysConfigItem();
        item.setUuid("config-1");
        item.setBuiltIn(1);
        when(mapper.selectById("config-1")).thenReturn(item);

        assertThrows(BusinessException.class, () -> service.delete("config-1"));

        verify(mapper, never()).deleteById("config-1");
    }

    @Test
    void update_withBuiltInMetadataChange_rejectsSave() {
        SysConfigItem item = builtInItem();
        when(mapper.selectById("config-1")).thenReturn(item);
        ConfigItemSaveDTO dto = dto("number", "4");
        dto.setConfigKey("process.renamed");

        assertThrows(BusinessException.class, () -> service.update("config-1", dto));

        verify(mapper, never()).updateById(any(SysConfigItem.class));
    }

    @Test
    void update_withBuiltInValueChange_preservesMetadata() {
        SysConfigItem item = builtInItem();
        when(mapper.selectById("config-1")).thenReturn(item);
        when(mapper.updateById(any(SysConfigItem.class))).thenReturn(1);
        ConfigItemSaveDTO dto = dto("number", "4");

        service.update("config-1", dto);

        verify(mapper).updateById(eq(item));
        assertEquals("test.value", item.getConfigKey());
        assertEquals("number", item.getValueType());
        assertEquals("4", item.getConfigValue());
    }

    private SysConfigItem builtInItem() {
        SysConfigItem item = new SysConfigItem();
        item.setUuid("config-1");
        item.setConfigGroup("test");
        item.setConfigKey("test.value");
        item.setConfigName("测试参数");
        item.setConfigValue("3");
        item.setValueType("number");
        item.setSortNo(null);
        item.setStatus(1);
        item.setBuiltIn(1);
        return item;
    }

    private ConfigItemSaveDTO dto(String valueType, String configValue) {
        ConfigItemSaveDTO dto = new ConfigItemSaveDTO();
        dto.setConfigGroup("test");
        dto.setConfigKey("test.value");
        dto.setConfigName("测试参数");
        dto.setConfigValue(configValue);
        dto.setValueType(valueType);
        dto.setStatus(1);
        return dto;
    }
}
