package com.paper.mes.integration;

import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.mapper.CustomerMapper;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class BusinessFlowFixtureFactory {

    private final CustomerMapper customerMapper;
    private final ProcessOrderMapper processOrderMapper;
    private final FinishRollMapper finishRollMapper;
    private final ProcessStepMapper processStepMapper;

    Scenario createCompletedOrderWithTwoFinishes() {
        String token = id();
        Customer customer = customer(token);
        customerMapper.insert(customer);
        return createCompletedOrderForCustomer(customer);
    }

    Scenario createCompletedOrderForCustomer(Customer customer) {
        String token = id();
        ProcessOrder order = completedOrder(token, customer);
        FinishRoll first = finish(order, 1, token.substring(0, 6));
        FinishRoll second = finish(order, 2, token.substring(6, 12));

        processOrderMapper.insert(order);
        processStepMapper.insert(sawStep(order));
        finishRollMapper.insert(first);
        finishRollMapper.insert(second);
        return new Scenario(customer, order, first, second);
    }

    private Customer customer(String token) {
        Customer customer = new Customer();
        customer.setUuid(id());
        customer.setCustomerCode("ITC" + token.substring(0, 12));
        customer.setCustomerName("business-flow-" + token.substring(0, 8));
        customer.setSettleType(2);
        customer.setDefaultInvoice(2);
        customer.setPriceIncludeTax(2);
        customer.setTaxRate(BigDecimal.ZERO);
        return customer;
    }

    private ProcessOrder completedOrder(String token, Customer customer) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid(id());
        order.setOrderNo("IT" + token.substring(0, 18));
        order.setCustomerUuid(customer.getUuid());
        order.setCustomerName(customer.getCustomerName());
        order.setOrderDate(LocalDate.now());
        order.setPriority(1);
        order.setIsInvoice(2);
        order.setSettleType(2);
        order.setTaxRate(BigDecimal.ZERO);
        order.setTotalProcessAmount(money("80"));
        order.setTotalExtraAmount(money("20"));
        order.setProcessAmountNoTax(money("80"));
        order.setExtraAmountNoTax(money("20"));
        order.setTotalAmountNoTax(money("100"));
        order.setTotalAmountTax(BigDecimal.ZERO);
        order.setTotalAmount(money("100"));
        order.setTotalStepCount(1);
        order.setHasExtraStep(0);
        order.setOrderStatus(4);
        order.setPrintStatus(1);
        order.setPrintCount(1);
        order.setIsMixProcess(0);
        return order;
    }

    private ProcessStep sawStep(ProcessOrder order) {
        ProcessStep step = new ProcessStep();
        step.setUuid(id());
        step.setOrderUuid(order.getUuid());
        step.setOriginalUuid(id());
        step.setInputType(1);
        step.setStageLevel(1);
        step.setStepSort(1);
        step.setStepType(1);
        step.setStepName("saw");
        step.setIsMain(1);
        step.setKnifeCount(10);
        step.setUnitPrice(money("8"));
        step.setStepAmount(money("80"));
        step.setLossWeight(BigDecimal.ZERO);
        return step;
    }

    private FinishRoll finish(ProcessOrder order, int rowSort, String suffix) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(id());
        finish.setOrderUuid(order.getUuid());
        finish.setRowSort(rowSort);
        finish.setFinishRollNo("Z" + suffix.toUpperCase());
        finish.setRollNoStatus(2);
        finish.setIsSpare(0);
        finish.setPaperName("integration-paper");
        finish.setGramWeight(80);
        finish.setFinishWidth(1000);
        finish.setSourceType(1);
        finish.setActualWeight(weight("100"));
        finish.setRemainingWeight(weight("100"));
        finish.setIsWeightAdjust(0);
        finish.setIsManualEdit(0);
        finish.setIsRemain(0);
        finish.setIsAbnormal(0);
        finish.setScrapWeight(BigDecimal.ZERO);
        finish.setQualityStatus(2);
        finish.setFinishStatus(2);
        return finish;
    }

    private static String id() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }

    private static BigDecimal weight(String value) {
        return new BigDecimal(value).setScale(3);
    }

    record Scenario(Customer customer, ProcessOrder order, FinishRoll first, FinishRoll second) {
    }
}
