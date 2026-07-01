package com.paper.mes.safety;

import com.paper.mes.delivery.dto.DeliveryAppendItemsDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.settle.dto.SettleByOrdersDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void deliveryCreate_whenCustomerBlankAndOutWeightNotPositive_reportsValidationErrors() {
        DeliveryCreateDTO dto = new DeliveryCreateDTO();
        DeliveryCreateDTO.Item item = new DeliveryCreateDTO.Item();
        item.setFinishUuid("finish-1");
        item.setOutWeight(BigDecimal.ZERO);
        dto.setCustomerUuid(" ");
        dto.setDeliveryDate(LocalDate.of(2026, 7, 1));
        dto.setItems(List.of(item));

        Set<String> messages = validateMessages(dto);

        assertTrue(messages.contains("客户不能为空"));
        assertTrue(messages.contains("出库重量必须大于0"));
    }

    @Test
    void deliveryAppend_whenOutWeightNegative_reportsValidationError() {
        DeliveryAppendItemsDTO dto = new DeliveryAppendItemsDTO();
        DeliveryAppendItemsDTO.Item item = new DeliveryAppendItemsDTO.Item();
        item.setFinishUuid("finish-1");
        item.setOutWeight(new BigDecimal("-1"));
        dto.setItems(List.of(item));

        assertTrue(validateMessages(dto).contains("出库重量必须大于0"));
    }

    @Test
    void settleByOrders_whenOrderUuidBlank_reportsValidationError() {
        SettleByOrdersDTO dto = new SettleByOrdersDTO();
        dto.setOrderUuids(List.of(" "));

        assertTrue(validateMessages(dto).contains("加工单uuid不能为空"));
    }

    private Set<String> validateMessages(Object dto) {
        return validator.validate(dto).stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.toSet());
    }
}
