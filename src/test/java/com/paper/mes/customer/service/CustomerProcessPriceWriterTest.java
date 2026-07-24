package com.paper.mes.customer.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.customer.dto.CustomerProcessPriceSaveDTO;
import com.paper.mes.customer.mapper.CustomerProcessPriceMapper;
import com.paper.mes.processorder.dto.ProcessCatalogUnitVO;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.service.ProcessCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerProcessPriceWriterTest {

    @Mock private CustomerProcessPriceMapper priceMapper;
    @Mock private ProcessCatalogService catalogService;
    private CustomerProcessPriceWriter writer;

    @BeforeEach
    void setUp() {
        writer = new CustomerProcessPriceWriter(priceMapper, catalogService);
        when(catalogService.listActive()).thenReturn(List.of(serviceCatalog(), productionCatalog()));
    }

    @Test
    void acceptsPieceTonAndFixedOptionsWithOneDefault() {
        List<CustomerProcessPriceSaveDTO> result = writer.normalize(List.of(
                price("service", "PIECE", 8, 0),
                price("service", "TON", 180, 1),
                price("service", "FIXED", 260, 0)));

        assertEquals(List.of(0, 1, 0), result.stream()
                .map(CustomerProcessPriceSaveDTO::getIsDefault).toList());
    }

    @Test
    void promotesFirstOptionWhenNoDefaultWasSelected() {
        List<CustomerProcessPriceSaveDTO> result = writer.normalize(List.of(
                price("service", "PIECE", 8, 0), price("service", "TON", 180, 0)));

        assertEquals(1, result.getFirst().getIsDefault());
    }

    @Test
    void rejectsProductionProcessesDuplicateBasesAndMultipleDefaults() {
        assertThrows(BusinessException.class,
                () -> writer.normalize(List.of(price("saw", "PIECE", 8, 1))));
        assertThrows(BusinessException.class, () -> writer.normalize(List.of(
                price("service", "PIECE", 8, 1), price("service", "PIECE", 9, 0))));
        assertThrows(BusinessException.class, () -> writer.normalize(List.of(
                price("service", "PIECE", 8, 1), price("service", "TON", 180, 1))));
    }

    private CustomerProcessPriceSaveDTO price(String catalogUuid, String basis, double value, int isDefault) {
        CustomerProcessPriceSaveDTO dto = new CustomerProcessPriceSaveDTO();
        dto.setCatalogUuid(catalogUuid);
        dto.setBillingBasis(basis);
        dto.setPrice(BigDecimal.valueOf(value));
        dto.setIsDefault(isDefault);
        return dto;
    }

    private ProcessCatalogVO serviceCatalog() {
        return new ProcessCatalogVO("service", 3, "STRIP_SORT", "剥损整理", "SERVICE",
                "SERVICE_QUANTITY", false, true, false,
                List.of(new ProcessCatalogUnitVO("PIECE", "件", true),
                        new ProcessCatalogUnitVO("TON", "吨", false)), List.of(1, 3, 4));
    }

    private ProcessCatalogVO productionCatalog() {
        return new ProcessCatalogVO("saw", 1, "SAW", "锯纸", "PRODUCTION",
                "SAW_KNIFE", true, true, true,
                List.of(new ProcessCatalogUnitVO("KNIFE", "刀", true)), List.of(1));
    }
}
