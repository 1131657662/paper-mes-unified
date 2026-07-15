package com.paper.mes.safety;

import com.paper.mes.customer.dto.CustomerSaveDTO;
import com.paper.mes.processorder.dto.DraftOrderBaseDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MasterDataDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void draftOrder_withInvalidBillingValues_isRejected() {
        DraftOrderBaseDTO dto = validDraft();
        dto.setPriority(4);
        dto.setSettleDay(32);
        dto.setTaxRate(new BigDecimal("101"));
        dto.setUrgentFee(new BigDecimal("-0.01"));

        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("priority", "settleDay", "taxRate", "urgentFee");
    }

    @Test
    void customer_withInvalidPricesAndEnums_isRejected() {
        CustomerSaveDTO dto = new CustomerSaveDTO();
        dto.setCustomerName("测试客户");
        dto.setSettleType(3);
        dto.setSettleDay(0);
        dto.setSawPrice(new BigDecimal("-1"));
        dto.setRewindPrice(new BigDecimal("-2"));
        dto.setDefaultInvoice(0);
        dto.setPriceIncludeTax(3);
        dto.setTaxRate(new BigDecimal("100.01"));
        dto.setCustomerLevel(4);

        assertThat(validator.validate(dto))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("settleType", "settleDay", "sawPrice", "rewindPrice",
                        "defaultInvoice", "priceIncludeTax", "taxRate", "customerLevel");
    }

    @Test
    void validDraftAndCustomer_areAccepted() {
        CustomerSaveDTO customer = new CustomerSaveDTO();
        customer.setCustomerName("测试客户");
        customer.setSettleType(2);
        customer.setSettleDay(25);
        customer.setSawPrice(BigDecimal.ZERO);
        customer.setRewindPrice(new BigDecimal("200"));
        customer.setTaxRate(new BigDecimal("13"));

        assertThat(validator.validate(validDraft())).isEmpty();
        assertThat(validator.validate(customer)).isEmpty();
    }

    private DraftOrderBaseDTO validDraft() {
        DraftOrderBaseDTO dto = new DraftOrderBaseDTO();
        dto.setCustomerUuid("customer-uuid");
        dto.setOrderDate(LocalDate.now());
        dto.setPriority(1);
        dto.setSettleType(2);
        dto.setIsInvoice(2);
        dto.setTaxRate(BigDecimal.ZERO);
        return dto;
    }
}
