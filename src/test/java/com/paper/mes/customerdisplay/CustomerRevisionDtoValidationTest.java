package com.paper.mes.customerdisplay;

import com.paper.mes.delivery.dto.DeliveryCustomerSpecItemDTO;
import com.paper.mes.processorder.dto.FinishCustomerSpecItemDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerRevisionDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void finishItemWithNullRoundingScaleViolatesRequestContract() {
        FinishCustomerSpecItemDTO item = new FinishCustomerSpecItemDTO();
        item.setRoundingScale(null);

        assertThat(validator.validateProperty(item, "roundingScale")).isNotEmpty();
    }

    @Test
    void deliveryItemWithNullRoundingScaleViolatesRequestContract() {
        DeliveryCustomerSpecItemDTO item = new DeliveryCustomerSpecItemDTO();
        item.setRoundingScale(null);

        assertThat(validator.validateProperty(item, "roundingScale")).isNotEmpty();
    }
}
