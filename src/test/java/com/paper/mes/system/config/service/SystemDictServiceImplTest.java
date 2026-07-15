package com.paper.mes.system.config.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.config.entity.SysDictItem;
import com.paper.mes.system.config.mapper.SysDictItemMapper;
import com.paper.mes.system.config.service.impl.SystemDictServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
