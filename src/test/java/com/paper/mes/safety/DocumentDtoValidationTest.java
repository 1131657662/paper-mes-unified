package com.paper.mes.safety;

import com.paper.mes.delivery.dto.DeliveryAppendItemsDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.dto.BackRecordStepDTO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.ProcessOrderVoidDTO;
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

    @Test
    void backRecordStep_whenLossWeightNegative_reportsValidationError() {
        BackRecordDTO dto = new BackRecordDTO();
        BackRecordRollDTO roll = new BackRecordRollDTO();
        BackRecordStepDTO step = new BackRecordStepDTO();
        roll.setUuid("roll-1");
        step.setUuid("step-1");
        step.setLossWeight(new BigDecimal("-0.001"));
        dto.setRolls(List.of(roll));
        dto.setSteps(List.of(step));

        assertTrue(validateMessages(dto).contains("工序损耗不能为负数"));
    }

    @Test
    void backRecord_whenWarehouseBlank_reportsValidationError() {
        BackRecordDTO dto = new BackRecordDTO();
        BackRecordRollDTO roll = new BackRecordRollDTO();
        roll.setUuid("roll-1");
        dto.setRolls(List.of(roll));

        assertTrue(validateMessages(dto).contains("入库仓库不能为空"));
    }

    @Test
    void processOrderVoid_whenReasonBlank_reportsValidationError() {
        ProcessOrderVoidDTO dto = new ProcessOrderVoidDTO();
        dto.setReason(" ");

        assertTrue(validateMessages(dto).contains("作废原因不能为空"));
    }

    @Test
    void originalRoll_whenRequiredNumbersAreNotPositive_reportsValidationErrors() {
        OriginalRollDTO dto = new OriginalRollDTO();
        dto.setPaperName("纸");
        dto.setGramWeight(0);
        dto.setOriginalWidth(0);
        dto.setRollWeight(BigDecimal.ZERO);
        dto.setPieceNum(0);

        Set<String> messages = validateMessages(dto);

        assertTrue(messages.contains("克重必须大于0"));
        assertTrue(messages.contains("门幅必须大于0"));
        assertTrue(messages.contains("单件重量必须大于0"));
        assertTrue(messages.contains("件数至少为1"));
    }

    private Set<String> validateMessages(Object dto) {
        return validator.validate(dto).stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.toSet());
    }
}
