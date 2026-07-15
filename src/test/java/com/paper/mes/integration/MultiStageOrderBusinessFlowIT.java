package com.paper.mes.integration;

import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.mapper.CustomerMapper;
import com.paper.mes.processorder.dto.DraftOrderBaseDTO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.service.ProcessOrderDraftService;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.processorder.service.ProcessRouteDraftManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MultiStageOrderBusinessFlowIT {

    @Autowired private CustomerMapper customerMapper;
    @Autowired private ProcessOrderDraftService draftService;
    @Autowired private ProcessRouteDraftManager routeDraftManager;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private RepresentativeOrderFixture orderFixture;
    @Autowired private BusinessFlowOrderCleanup cleanup;

    private RepresentativeOrderFixture.Scenario scenario;

    @AfterEach
    void tearDown() {
        if (scenario != null) cleanup.delete(scenario.orderUuid());
    }

    @Test
    void sawThenRewindRoute_submitsIntermediateOutputsAndCompletesOrder() {
        Customer customer = customer();
        String orderUuid = draftService.createDraft(base(customer));
        scenario = new RepresentativeOrderFixture.Scenario(orderUuid, customer.getUuid());
        String rollUuid = draftService.replaceOriginalRolls(orderUuid, List.of(roll())).getFirst();
        routeDraftManager.save(orderUuid, rollUuid, route(rollUuid));

        var submitted = draftService.submit(orderUuid);
        orderFixture.issueAndComplete(scenario);

        var detail = processOrderService.getDetail(orderUuid);
        assertThat(submitted.getOrderStatus()).isEqualTo(1);
        assertThat(detail.getOrder().getOrderStatus()).isEqualTo(4);
        assertThat(detail.getSteps()).hasSize(2);
        assertThat(detail.getRollProductions().getFirst().getStageOutputs()).hasSize(3);
        assertThat(detail.getFinishRolls()).hasSize(2);
        assertThat(detail.getFinishRolls()).allMatch(item -> item.getActualWeight() != null);
    }

    private Customer customer() {
        String token = token();
        Customer customer = new Customer();
        customer.setUuid(token());
        customer.setCustomerCode("ITM" + token.substring(0, 12));
        customer.setCustomerName("multi-stage-" + token.substring(0, 8));
        customer.setSettleType(2);
        customer.setDefaultInvoice(2);
        customer.setPriceIncludeTax(2);
        customer.setTaxRate(BigDecimal.ZERO);
        customer.setSawPrice(new BigDecimal("8.00"));
        customer.setRewindPrice(new BigDecimal("200.00"));
        customerMapper.insert(customer);
        return customer;
    }

    private DraftOrderBaseDTO base(Customer customer) {
        DraftOrderBaseDTO dto = new DraftOrderBaseDTO();
        dto.setCustomerUuid(customer.getUuid());
        dto.setOrderDate(LocalDate.now());
        dto.setPriority(1);
        dto.setIsInvoice(2);
        dto.setSettleType(2);
        return dto;
    }

    private OriginalRollDTO roll() {
        OriginalRollDTO dto = new OriginalRollDTO();
        dto.setRollNo("IT-CHAIN-" + token().substring(0, 8));
        dto.setPaperName("链式工艺测试纸");
        dto.setGramWeight(100);
        dto.setOriginalWidth(1000);
        dto.setRollWeight(new BigDecimal("1000.000"));
        dto.setPieceNum(1);
        dto.setProcessMode(1);
        dto.setMainStepType(1);
        return dto;
    }

    private ProcessRoutePreviewDTO route(String rollUuid) {
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(rollUuid);
        dto.setStages(List.of(sawStage(), rewindStage()));
        return dto;
    }

    private ProcessRoutePreviewDTO.RouteStageDTO sawStage() {
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(1, 1, "锯纸");
        stage.setKnifeCount(1);
        stage.setUnitPrice(new BigDecimal("8.00"));
        stage.setOutputs(List.of(output("stage-a", 900, "450.000"), output("stage-b", 100, "550.000")));
        return stage;
    }

    private ProcessRoutePreviewDTO.RouteStageDTO rewindStage() {
        ProcessRoutePreviewDTO.RouteStageDTO stage = stage(2, 2, "复卷");
        stage.setInputOutputKeys(List.of("stage-a"));
        stage.setUnitPrice(new BigDecimal("200.00"));
        stage.setOutputs(List.of(output("stage-final", 900, "450.000")));
        return stage;
    }

    private ProcessRoutePreviewDTO.RouteStageDTO stage(int level, int type, String name) {
        ProcessRoutePreviewDTO.RouteStageDTO dto = new ProcessRoutePreviewDTO.RouteStageDTO();
        dto.setStageLevel(level);
        dto.setStepType(type);
        dto.setStepName(name);
        return dto;
    }

    private ProcessRoutePreviewDTO.RouteOutputDTO output(String key, int width, String weight) {
        ProcessRoutePreviewDTO.RouteOutputDTO dto = new ProcessRoutePreviewDTO.RouteOutputDTO();
        dto.setOutputKey(key);
        dto.setPaperName("链式工艺测试纸");
        dto.setGramWeight(100);
        dto.setFinishWidth(width);
        dto.setEstimateWeight(new BigDecimal(weight));
        return dto;
    }

    private static String token() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
