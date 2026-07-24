package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.customerdisplay.formula.CustomerWeightCalculationMode;
import com.paper.mes.customerdisplay.formula.CustomerWeightZeroPolicy;
import com.paper.mes.processorder.dto.*;
import com.paper.mes.processorder.entity.FinishCustomerRevision;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishCustomerRevisionMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.service.FinishCustomerRevisionPreviewService;
import com.paper.mes.processorder.service.FinishCustomerRevisionPublisher;
import com.paper.mes.processorder.service.FinishCustomerRevisionReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class FinishCustomerRevisionBusinessFlowIT {

    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private FinishCustomerRevisionPreviewService previewService;
    @Autowired private FinishCustomerRevisionPublisher publisher;
    @Autowired private FinishCustomerRevisionReader reader;
    @Autowired private FinishCustomerRevisionMapper revisionMapper;
    @Autowired private FinishRollMapper finishMapper;
    @Autowired private ProcessOrderMapper orderMapper;

    @Test
    void publishCustomerRevision_keepsPhysicalInventoryAndSettlementFactsUntouched() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        FinishRoll beforeFinish = finishMapper.selectById(scenario.first().getUuid());
        ProcessOrder beforeOrder = orderMapper.selectById(scenario.order().getUuid());
        PhysicalBaseline baseline = PhysicalBaseline.from(beforeFinish, beforeOrder);
        FinishCustomerRevisionPreviewVO current = previewService.current(scenario.order().getUuid());
        FinishCustomerRevisionRequestDTO request = request(current.getItems().getFirst(), current.getOrderVersion());

        FinishCustomerRevisionSummaryVO published = publisher.publish(scenario.order().getUuid(), request);
        FinishCustomerRevisionSummaryVO replay = publisher.publish(scenario.order().getUuid(), request);

        assertPublishedOnce(scenario.order().getUuid(), published, replay);
        assertPhysicalFacts(finishMapper.selectById(scenario.first().getUuid()), baseline);
        assertSettlementFacts(orderMapper.selectById(scenario.order().getUuid()), baseline);
        assertCustomerCache(finishMapper.selectById(scenario.first().getUuid()));
        assertRevisionDetail(scenario.order().getUuid(), published.getUuid(), scenario.first().getFinishRollNo());
    }

    private void assertPublishedOnce(String orderUuid, FinishCustomerRevisionSummaryVO published,
                                     FinishCustomerRevisionSummaryVO replay) {
        assertThat(published.getRevisionNo()).isEqualTo(1);
        assertThat(replay.getUuid()).isEqualTo(published.getUuid());
        assertThat(revisionMapper.selectCount(new LambdaQueryWrapper<FinishCustomerRevision>()
                .eq(FinishCustomerRevision::getOrderUuid, orderUuid))).isEqualTo(1);
    }

    private void assertPhysicalFacts(FinishRoll finish, PhysicalBaseline baseline) {
        assertThat(finish.getPaperName()).isEqualTo(baseline.paperName());
        assertThat(finish.getGramWeight()).isEqualTo(baseline.gramWeight());
        assertThat(finish.getFinishWidth()).isEqualTo(baseline.finishWidth());
        assertThat(finish.getActualWeight()).isEqualByComparingTo(baseline.actualWeight());
    }

    private void assertSettlementFacts(ProcessOrder order, PhysicalBaseline baseline) {
        assertThat(order.getOrderStatus()).isEqualTo(baseline.orderStatus());
        assertThat(order.getTotalProcessAmount()).isEqualByComparingTo(baseline.processAmount());
        assertThat(order.getTotalAmount()).isEqualByComparingTo(baseline.totalAmount());
    }

    private void assertCustomerCache(FinishRoll finish) {
        assertThat(finish.getCustomerPaperName()).isEqualTo("食品卡");
        assertThat(finish.getCustomerGramWeight()).isEqualTo(100);
        assertThat(finish.getCustomerFinishWidth()).isEqualTo(500);
        assertThat(finish.getCustomerDisplayWeight()).isEqualByComparingTo("125.000");
    }

    private void assertRevisionDetail(String orderUuid, String revisionUuid, String finishRollNo) {
        FinishCustomerRevisionDetailVO detail = reader.detail(orderUuid, revisionUuid);
        assertThat(detail.getItems()).hasSize(1);
        assertThat(detail.getItems().getFirst().getFinishRollNo()).isEqualTo(finishRollNo);
        assertThat(detail.getItems().getFirst().getCustomerPaperName()).isEqualTo("食品卡");
        assertThat(detail.getItems().getFirst().getCustomerDisplayWeight()).isEqualByComparingTo("125.000");
        assertThat(detail.getItems().getFirst().getFormulaVariables())
                .containsEntry("adjustment", new BigDecimal("25"));
    }

    private FinishCustomerRevisionRequestDTO request(FinishCustomerSpecVO row, Integer orderVersion) {
        FinishCustomerSpecItemDTO item = new FinishCustomerSpecItemDTO();
        item.setFinishUuid(row.getFinishUuid());
        item.setExpectedVersion(row.getFinishVersion());
        item.setCustomerPaperName("食品卡");
        item.setCustomerGramWeight(100);
        item.setCustomerFinishWidth(500);
        item.setCalculationMode(CustomerWeightCalculationMode.FORMULA);
        item.setFormulaExpression("physicalWeight + adjustment");
        item.setFormulaVariables(Map.of("adjustment", new BigDecimal("25")));
        item.setRoundingScale(3);
        item.setRoundingMode(RoundingMode.HALF_UP);
        item.setZeroPolicy(CustomerWeightZeroPolicy.SKIP);
        FinishCustomerRevisionRequestDTO request = new FinishCustomerRevisionRequestDTO();
        request.setRequestId("finish-customer-revision-it-1");
        request.setExpectedOrderVersion(orderVersion);
        request.setReason("客户要求修改成品标签");
        request.setItems(List.of(item));
        return request;
    }

    private record PhysicalBaseline(String paperName, Integer gramWeight, Integer finishWidth,
                                    BigDecimal actualWeight, Integer orderStatus,
                                    BigDecimal processAmount, BigDecimal totalAmount) {
        private static PhysicalBaseline from(FinishRoll finish, ProcessOrder order) {
            return new PhysicalBaseline(finish.getPaperName(), finish.getGramWeight(), finish.getFinishWidth(),
                    finish.getActualWeight(), order.getOrderStatus(), order.getTotalProcessAmount(),
                    order.getTotalAmount());
        }
    }
}
