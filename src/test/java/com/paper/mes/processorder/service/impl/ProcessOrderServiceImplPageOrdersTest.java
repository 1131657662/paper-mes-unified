package com.paper.mes.processorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.ProcessOrderQuery;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessParamMapper;
import com.paper.mes.processorder.mapper.ProcessStageInputRelMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.processorder.service.FileStorageService;
import com.paper.mes.processorder.service.ProcessOrderExportService;
import com.paper.mes.processorder.service.RollNoSequenceService;
import com.paper.mes.processorder.service.SawPlanPreviewer;
import com.paper.mes.processorder.service.WeightCheckThresholdService;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.system.config.service.DocumentNoService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ProcessOrderServiceImplPageOrdersTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProcessOrder.class);
    }

    @Test
    void pageOrders_withoutStatus_excludesVoidedOrders() {
        CapturingProcessOrderService service = service();
        ProcessOrderQuery query = new ProcessOrderQuery();

        service.pageOrders(query);

        String sql = service.lastSqlSegment();
        assertTrue(sql.contains("order_status <>"));
        assertTrue(sql.contains("ORDER BY create_time DESC"));
    }

    @Test
    void pageOrders_withVoidedStatus_includesOnlyVoidedOrders() {
        CapturingProcessOrderService service = service();
        ProcessOrderQuery query = new ProcessOrderQuery();
        query.setOrderStatus(6);

        service.pageOrders(query);

        String sql = service.lastSqlSegment();
        assertTrue(sql.contains("order_status ="));
        assertFalse(sql.contains("order_status <>"));
    }

    private CapturingProcessOrderService service() {
        return new CapturingProcessOrderService();
    }

    private static final class CapturingProcessOrderService extends ProcessOrderServiceImpl {
        private Wrapper<ProcessOrder> lastWrapper;

        private CapturingProcessOrderService() {
            super(
                    mock(OriginalRollMapper.class),
                    mock(FinishRollMapper.class),
                    mock(ProcessStepMapper.class),
                    mock(ProcessParamMapper.class),
                    mock(ProcessStageInputRelMapper.class),
                    mock(ProcessStageOutputMapper.class),
                    mock(FinishOriginalRelMapper.class),
                    mock(DeliveryDetailMapper.class),
                    mock(SettleDetailMapper.class),
                    mock(CustomerService.class),
                    mock(OperationLogService.class),
                    new ObjectMapper(),
                    mock(FileStorageService.class),
                    mock(RollNoSequenceService.class),
                    new SawPlanPreviewer(),
                    mock(DocumentNoService.class),
                    mock(ProcessOrderExportService.class),
                    mock(BusinessLockService.class),
                    mock(MachineMapper.class),
                    mock(WeightCheckThresholdService.class));
        }

        @Override
        public <E extends IPage<ProcessOrder>> E page(E page, Wrapper<ProcessOrder> queryWrapper) {
            lastWrapper = queryWrapper;
            page.setRecords(List.of());
            return page;
        }

        private String lastSqlSegment() {
            return lastWrapper.getSqlSegment();
        }
    }
}
