package com.paper.mes.processorder.service;

import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.entity.ProcessOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ProcessRoutePriceResolver {

    private final CustomerService customerService;

    public void applyDefaultPrices(ProcessOrder order, ProcessRoutePreviewDTO dto) {
        Customer customer = order.getCustomerUuid() == null ? null : customerService.getById(order.getCustomerUuid());
        BigDecimal sawPrice = customer == null ? null : customer.getSawPrice();
        BigDecimal rewindPrice = customer == null ? null : customer.getRewindPrice();
        if (dto.getStages() == null) {
            return;
        }
        for (ProcessRoutePreviewDTO.RouteStageDTO stage : dto.getStages()) {
            if (stage.getUnitPrice() != null) {
                continue;
            }
            stage.setUnitPrice(defaultPrice(stage.getStepType(), sawPrice, rewindPrice));
        }
    }

    private BigDecimal defaultPrice(Integer stepType, BigDecimal sawPrice, BigDecimal rewindPrice) {
        if (stepType != null && stepType == FeeCalculator.STEP_TYPE_SAW) {
            return sawPrice;
        }
        if (stepType != null && stepType == FeeCalculator.STEP_TYPE_REWIND) {
            return rewindPrice;
        }
        return null;
    }
}
