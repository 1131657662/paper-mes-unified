package com.paper.mes.system.config.service;

import com.paper.mes.customer.dto.CustomerSaveDTO;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.mapper.CustomerMapper;
import com.paper.mes.customer.service.impl.CustomerServiceImpl;
import com.paper.mes.machine.dto.MachineSaveDTO;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.machine.service.impl.MachineServiceImpl;
import com.paper.mes.paper.dto.PaperSaveDTO;
import com.paper.mes.paper.entity.Paper;
import com.paper.mes.paper.mapper.PaperMapper;
import com.paper.mes.paper.service.impl.PaperServiceImpl;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.warehouse.dto.WarehouseSaveDTO;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.mapper.WarehouseMapper;
import com.paper.mes.warehouse.service.impl.WarehouseServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveAutoCodeServiceTest {

    @Test
    void createCustomer_whenPayloadHasManualCode_usesConfiguredNoRule() {
        DocumentNoService noService = mockNoService(NoRuleBizType.CUSTOMER, "KH000123");
        CustomerMapper mapper = mock(CustomerMapper.class);
        when(mapper.insert(any(Customer.class))).thenReturn(1);
        CustomerServiceImpl service = new CustomerServiceImpl(noService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        CustomerSaveDTO dto = new CustomerSaveDTO();
        dto.setCustomerCode("MANUAL");
        dto.setCustomerName("测试客户");

        service.create(dto);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(mapper).insert(captor.capture());
        assertEquals("KH000123", captor.getValue().getCustomerCode());
    }

    @Test
    void createPaper_whenPayloadHasManualCode_usesConfiguredNoRule() {
        DocumentNoService noService = mockNoService(NoRuleBizType.PAPER, "ZZ000123");
        PaperMapper mapper = mock(PaperMapper.class);
        when(mapper.insert(any(Paper.class))).thenReturn(1);
        PaperServiceImpl service = new PaperServiceImpl(noService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        PaperSaveDTO dto = new PaperSaveDTO();
        dto.setPaperCode("MANUAL");
        dto.setPaperName("白卡");

        service.create(dto);

        ArgumentCaptor<Paper> captor = ArgumentCaptor.forClass(Paper.class);
        verify(mapper).insert(captor.capture());
        assertEquals("ZZ000123", captor.getValue().getPaperCode());
    }

    @Test
    void createMachine_whenPayloadHasManualCode_usesConfiguredNoRule() {
        DocumentNoService noService = mockNoService(NoRuleBizType.MACHINE, "JT000123");
        MachineMapper mapper = mock(MachineMapper.class);
        when(mapper.insert(any(Machine.class))).thenReturn(1);
        MachineServiceImpl service = new MachineServiceImpl(noService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        MachineSaveDTO dto = new MachineSaveDTO();
        dto.setMachineCode("MANUAL");
        dto.setMachineName("一号机");

        service.create(dto);

        ArgumentCaptor<Machine> captor = ArgumentCaptor.forClass(Machine.class);
        verify(mapper).insert(captor.capture());
        assertEquals("JT000123", captor.getValue().getMachineCode());
    }

    @Test
    void createWarehouse_whenPayloadHasManualCode_usesConfiguredNoRule() {
        DocumentNoService noService = mockNoService(NoRuleBizType.WAREHOUSE, "CKD000123");
        WarehouseMapper mapper = mock(WarehouseMapper.class);
        when(mapper.insert(any(Warehouse.class))).thenReturn(1);
        WarehouseServiceImpl service = new WarehouseServiceImpl(noService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        WarehouseSaveDTO dto = new WarehouseSaveDTO();
        dto.setWarehouseCode("MANUAL");
        dto.setWarehouseName("成品仓");

        service.create(dto);

        ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
        verify(mapper).insert(captor.capture());
        assertEquals("CKD000123", captor.getValue().getWarehouseCode());
    }

    private DocumentNoService mockNoService(String bizType, String code) {
        DocumentNoService noService = mock(DocumentNoService.class);
        when(noService.next(eq(bizType), any(LocalDate.class))).thenReturn(code);
        return noService;
    }
}
