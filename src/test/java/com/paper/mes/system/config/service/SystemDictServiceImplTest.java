package com.paper.mes.system.config.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.config.dto.DictItemSaveDTO;
import com.paper.mes.system.config.entity.SysDictItem;
import com.paper.mes.system.config.mapper.SysDictItemMapper;
import com.paper.mes.system.config.service.impl.SystemDictServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemDictServiceImplTest {

    private SysDictItemMapper mapper;
    private SystemDictServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysDictItemMapper.class);
        service = new SystemDictServiceImpl(mock(OperationLogService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void delete_withBuiltInItem_rejectsDeletion() {
        SysDictItem item = new SysDictItem();
        item.setUuid("dict-1");
        item.setBuiltIn(1);
        when(mapper.selectById("dict-1")).thenReturn(item);

        assertThrows(BusinessException.class, () -> service.delete("dict-1"));

        verify(mapper, never()).deleteById("dict-1");
    }

    @Test
    void update_withBuiltInCodeChange_rejectsSave() {
        SysDictItem item = builtInItem();
        when(mapper.selectById("dict-1")).thenReturn(item);
        DictItemSaveDTO dto = dto(item);
        dto.setItemCode("renamed");

        assertThrows(BusinessException.class, () -> service.update("dict-1", dto));

        verify(mapper, never()).updateById(any(SysDictItem.class));
    }

    @Test
    void update_withBuiltInLabelChange_allowsSave() {
        SysDictItem item = builtInItem();
        when(mapper.selectById("dict-1")).thenReturn(item);
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.updateById(any(SysDictItem.class))).thenReturn(1);
        DictItemSaveDTO dto = dto(item);
        dto.setItemName("新标签");

        service.update("dict-1", dto);

        assertEquals("新标签", item.getItemName());
        verify(mapper).updateById(item);
    }

    private SysDictItem builtInItem() {
        SysDictItem item = new SysDictItem();
        item.setUuid("dict-1");
        item.setDictType("settle_type");
        item.setDictName("结算类型");
        item.setItemCode("cash");
        item.setItemName("现结");
        item.setItemValue(1);
        item.setSortNo(10);
        item.setStatus(1);
        item.setBuiltIn(1);
        return item;
    }

    private DictItemSaveDTO dto(SysDictItem item) {
        DictItemSaveDTO dto = new DictItemSaveDTO();
        dto.setDictType(item.getDictType());
        dto.setDictName(item.getDictName());
        dto.setItemCode(item.getItemCode());
        dto.setItemName(item.getItemName());
        dto.setItemValue(item.getItemValue());
        dto.setSortNo(item.getSortNo());
        dto.setStatus(item.getStatus());
        return dto;
    }
}
