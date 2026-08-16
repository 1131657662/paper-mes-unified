package com.paper.mes.integration;

import com.paper.mes.processorder.dto.BackRecordCompleteDTO;
import com.paper.mes.processorder.dto.BackRecordResultVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.model.ProcessRollDispositionAction;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class BackRecordCompletionBusinessFlowIT extends AuthenticatedBusinessFlowIT {

    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private ProcessOrderMapper processOrderMapper;
    @Autowired private OriginalRollMapper originalRollMapper;
    @Autowired private FinishRollMapper finishRollMapper;
    @Autowired private WarehouseMapper warehouseMapper;

    @Test
    void completeBackRecord_afterPriorBatchAndDisposition_closesWithoutRewritingInventory() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        prepareToRecordOrder(scenario);
        LocalDateTime firstStockIn = finishRollMapper.selectById(scenario.first().getUuid()).getStockInTime();
        ProcessOrder current = processOrderMapper.selectById(scenario.order().getUuid());
        BackRecordCompleteDTO request = new BackRecordCompleteDTO();
        request.setExpectedVersion(current.getVersion());

        BackRecordResultVO result = processOrderService.completeBackRecord(current.getUuid(), request);

        ProcessOrder completed = processOrderMapper.selectById(current.getUuid());
        assertThat(result.isOrderCompleted()).isTrue();
        assertThat(result.getRecordedRollCount()).isZero();
        assertThat(completed.getOrderStatus()).isEqualTo(4);
        assertThat(completed.getSnapFinish()).contains("\"name\": \"业务流集成测试仓\"");
        assertThat(finishRollMapper.selectById(scenario.first().getUuid()).getStockInTime())
                .isEqualTo(firstStockIn);
    }

    @Test
    void completeBackRecord_allRollsCancelledWithoutOutput_voidsOrder() {
        BusinessFlowFixtureFactory.Scenario owner = fixtures.createCompletedOrderWithTwoFinishes();
        ProcessOrder order = cancelledOrder(owner);
        processOrderMapper.insert(order);
        originalRollMapper.insert(cancelledRoll(order));
        BackRecordCompleteDTO request = new BackRecordCompleteDTO();
        request.setExpectedVersion(order.getVersion());

        BackRecordResultVO result = processOrderService.completeBackRecord(order.getUuid(), request);

        ProcessOrder voided = processOrderMapper.selectById(order.getUuid());
        assertThat(result.isOrderCompleted()).isTrue();
        assertThat(voided.getOrderStatus()).isEqualTo(6);
        assertThat(voided.getVoidReason()).contains("全部母卷已取消或转单");
        assertThat(voided.getSnapFinish()).contains("\"closure\"");
    }

    private void prepareToRecordOrder(BusinessFlowFixtureFactory.Scenario scenario) {
        OriginalRoll roll = scenario.original();
        roll.setIsChecked(1);
        roll.setActualGramWeight(roll.getGramWeight());
        roll.setActualWidth(roll.getOriginalWidth());
        roll.setActualWeight(new BigDecimal("200.000"));
        originalRollMapper.updateById(roll);

        ProcessOrder order = scenario.order();
        order.setOrderStatus(3);
        order.setSnapFinish(null);
        processOrderMapper.updateById(order);

        Warehouse warehouse = warehouseMapper.selectById(order.getWarehouseUuid());
        warehouse.setStatus(2);
        warehouseMapper.updateById(warehouse);
    }

    private ProcessOrder cancelledOrder(BusinessFlowFixtureFactory.Scenario owner) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid(id());
        order.setOrderNo("IT-CANCEL-" + id().substring(0, 12));
        order.setCustomerUuid(owner.customer().getUuid());
        order.setCustomerName(owner.customer().getCustomerName());
        order.setWarehouseUuid(owner.order().getWarehouseUuid());
        order.setOrderDate(LocalDate.now());
        order.setPriority(1);
        order.setIsInvoice(2);
        order.setSettleType(2);
        order.setTaxRate(BigDecimal.ZERO);
        order.setOrderStatus(3);
        order.setPrintStatus(1);
        order.setPrintCount(1);
        order.setIsMixProcess(0);
        return order;
    }

    private OriginalRoll cancelledRoll(ProcessOrder order) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(id());
        roll.setOrderUuid(order.getUuid());
        roll.setRowSort(1);
        roll.setPaperName("cancelled-paper");
        roll.setGramWeight(80);
        roll.setOriginalWidth(1000);
        roll.setRollWeight(new BigDecimal("200.000"));
        roll.setPieceNum(1);
        roll.setTotalWeight(new BigDecimal("200.000"));
        roll.setProcessMode(1);
        roll.setRollStatus(1);
        roll.setIsChecked(1);
        roll.setDispositionAction(ProcessRollDispositionAction.CANCEL);
        return roll;
    }

    private String id() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
