package com.paper.mes.integration;

import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.mapper.CustomerMapper;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.dto.BackRecordStepDTO;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.dto.ProcessOrderCreateDTO;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.service.ProcessOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class RepresentativeOrderFixture {

    private final CustomerMapper customerMapper;
    private final ProcessOrderService processOrderService;

    Scenario createStandardSaw() {
        Customer customer = createCustomer();
        String orderUuid = processOrderService.create(order(customer, standardSawRoll()));
        String rollUuid = processOrderService.getDetail(orderUuid).getOriginalRolls().getFirst().getUuid();
        processOrderService.saveFinishConfig(orderUuid, rollUuid, standardSawConfig());
        return new Scenario(orderUuid, customer.getUuid());
    }

    Scenario createDirectShip() {
        Customer customer = createCustomer();
        String orderUuid = processOrderService.create(order(customer, directShipRoll()));
        return new Scenario(orderUuid, customer.getUuid());
    }

    void issueAndComplete(Scenario scenario) {
        processOrderService.print(scenario.orderUuid(), new PrintDTO());
        processOrderService.changeStatus(scenario.orderUuid(), 3, null);
        ProcessOrderDetailVO detail = processOrderService.getDetail(scenario.orderUuid());
        processOrderService.backRecord(scenario.orderUuid(), backRecord(detail));
    }

    private Customer createCustomer() {
        String token = token();
        Customer customer = new Customer();
        customer.setUuid(token());
        customer.setCustomerCode("ITR" + token.substring(0, 12));
        customer.setCustomerName("representative-" + token.substring(0, 8));
        customer.setSettleType(2);
        customer.setDefaultInvoice(2);
        customer.setPriceIncludeTax(2);
        customer.setTaxRate(BigDecimal.ZERO);
        customer.setSawPrice(new BigDecimal("12.00"));
        customer.setRewindPrice(new BigDecimal("150.00"));
        customerMapper.insert(customer);
        return customer;
    }

    private ProcessOrderCreateDTO order(Customer customer, OriginalRollDTO roll) {
        ProcessOrderCreateDTO dto = new ProcessOrderCreateDTO();
        dto.setCustomerUuid(customer.getUuid());
        dto.setOrderDate(LocalDate.now());
        dto.setPriority(1);
        dto.setIsInvoice(2);
        dto.setSettleType(2);
        dto.setOriginalRolls(List.of(roll));
        return dto;
    }

    private OriginalRollDTO standardSawRoll() {
        OriginalRollDTO dto = new OriginalRollDTO();
        dto.setRollNo("IT-SAW-" + token().substring(0, 8));
        dto.setPaperName("代表性锯纸测试纸");
        dto.setGramWeight(80);
        dto.setOriginalWidth(2000);
        dto.setRollWeight(new BigDecimal("1000.000"));
        dto.setPieceNum(1);
        dto.setProcessMode(1);
        dto.setMainStepType(1);
        return dto;
    }

    private OriginalRollDTO directShipRoll() {
        OriginalRollDTO dto = standardSawRoll();
        dto.setRollNo("IT-DIRECT-" + token().substring(0, 8));
        dto.setPaperName("代表性直发测试纸");
        dto.setOriginalWidth(1600);
        dto.setRollWeight(new BigDecimal("800.000"));
        dto.setProcessMode(3);
        dto.setMainStepType(null);
        return dto;
    }

    private FinishConfigSaveDTO standardSawConfig() {
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        dto.setProcessMode(1);
        dto.setMainStepType(1);
        dto.setSpareCount(0);
        dto.setUnitPrice(new BigDecimal("12.00"));
        dto.setFinishSpecs(List.of(finishSpec(950, 2), trimSpec(100)));
        return dto;
    }

    private FinishConfigSpecDTO finishSpec(int width, int count) {
        FinishConfigSpecDTO dto = new FinishConfigSpecDTO();
        dto.setItemType("FINISH");
        dto.setFinishWidth(width);
        dto.setCount(count);
        return dto;
    }

    private FinishConfigSpecDTO trimSpec(int width) {
        FinishConfigSpecDTO dto = finishSpec(width, 1);
        dto.setItemType("TRIM");
        return dto;
    }

    private BackRecordDTO backRecord(ProcessOrderDetailVO detail) {
        BackRecordDTO dto = new BackRecordDTO();
        dto.setRolls(detail.getOriginalRolls().stream().map(this::rollRecord).toList());
        dto.setFinishes(detail.getFinishRolls().stream()
                .filter(finish -> finish.getRollNoStatus() == null || finish.getRollNoStatus() != 3)
                .filter(finish -> finish.getSourceType() == null || finish.getSourceType() != 2)
                .map(this::finishRecord)
                .toList());
        dto.setSteps(detail.getSteps().stream().map(this::stepRecord).toList());
        return dto;
    }

    private BackRecordRollDTO rollRecord(OriginalRoll roll) {
        BackRecordRollDTO dto = new BackRecordRollDTO();
        dto.setUuid(roll.getUuid());
        dto.setActualGramWeight(roll.getGramWeight());
        dto.setActualWidth(roll.getOriginalWidth());
        dto.setActualWeight(roll.getTotalWeight());
        return dto;
    }

    private BackRecordFinishDTO finishRecord(FinishRoll finish) {
        BackRecordFinishDTO dto = new BackRecordFinishDTO();
        dto.setUuid(finish.getUuid());
        dto.setFinishWidth(finish.getFinishWidth());
        dto.setActualWeight(finish.getEstimateWeight());
        dto.setIsRemain(finish.getIsRemain());
        dto.setIsAbnormal(0);
        return dto;
    }

    private BackRecordStepDTO stepRecord(ProcessStep step) {
        BackRecordStepDTO dto = new BackRecordStepDTO();
        dto.setUuid(step.getUuid());
        dto.setKnifeCount(step.getKnifeCount());
        dto.setLossWeight(BigDecimal.ZERO);
        return dto;
    }

    private static String token() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    record Scenario(String orderUuid, String customerUuid) {
    }
}
