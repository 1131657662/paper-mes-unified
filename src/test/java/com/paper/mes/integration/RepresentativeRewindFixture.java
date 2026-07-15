package com.paper.mes.integration;

import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.mapper.CustomerMapper;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.ProcessOrderCreateDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.service.ProcessOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class RepresentativeRewindFixture {

    private final CustomerMapper customerMapper;
    private final ProcessOrderService processOrderService;

    RepresentativeOrderFixture.Scenario create(int rewindMode) {
        Customer customer = createCustomer();
        String orderUuid = processOrderService.create(order(customer));
        String rollUuid = processOrderService.getDetail(orderUuid).getOriginalRolls().getFirst().getUuid();
        processOrderService.saveFinishConfig(orderUuid, rollUuid, config(rewindMode));
        return new RepresentativeOrderFixture.Scenario(orderUuid, customer.getUuid());
    }

    RepresentativeOrderFixture.Scenario createMerge() {
        Customer customer = createCustomer();
        ProcessOrderCreateDTO order = order(customer);
        order.setOriginalRolls(List.of(rewindRoll(), rewindRoll()));
        String orderUuid = processOrderService.create(order);
        List<OriginalRoll> rolls = processOrderService.getDetail(orderUuid).getOriginalRolls();
        processOrderService.saveFinishConfig(orderUuid, rolls.getFirst().getUuid(), mergeConfig(rolls));
        return new RepresentativeOrderFixture.Scenario(orderUuid, customer.getUuid());
    }

    RepresentativeOrderFixture.Scenario createOnSite() {
        Customer customer = createCustomer();
        ProcessOrderCreateDTO order = order(customer);
        OriginalRollDTO roll = rewindRoll();
        roll.setProcessMode(2);
        order.setOriginalRolls(List.of(roll));
        String orderUuid = processOrderService.create(order);
        String rollUuid = processOrderService.getDetail(orderUuid).getOriginalRolls().getFirst().getUuid();
        FinishConfigSaveDTO config = config(2);
        config.setProcessMode(2);
        config.setRewindSegments(List.of());
        processOrderService.saveFinishConfig(orderUuid, rollUuid, config);
        return new RepresentativeOrderFixture.Scenario(orderUuid, customer.getUuid());
    }

    private Customer createCustomer() {
        String token = token();
        Customer customer = new Customer();
        customer.setUuid(token());
        customer.setCustomerCode("ITW" + token.substring(0, 12));
        customer.setCustomerName("rewind-" + token.substring(0, 8));
        customer.setSettleType(2);
        customer.setDefaultInvoice(2);
        customer.setPriceIncludeTax(2);
        customer.setTaxRate(BigDecimal.ZERO);
        customer.setRewindPrice(new BigDecimal("150.00"));
        customerMapper.insert(customer);
        return customer;
    }

    private ProcessOrderCreateDTO order(Customer customer) {
        ProcessOrderCreateDTO dto = new ProcessOrderCreateDTO();
        dto.setCustomerUuid(customer.getUuid());
        dto.setOrderDate(LocalDate.now());
        dto.setPriority(1);
        dto.setIsInvoice(2);
        dto.setSettleType(2);
        dto.setOriginalRolls(List.of(rewindRoll()));
        return dto;
    }

    private OriginalRollDTO rewindRoll() {
        OriginalRollDTO dto = new OriginalRollDTO();
        dto.setRollNo("IT-RW-" + token().substring(0, 8));
        dto.setPaperName("代表性复卷测试纸");
        dto.setGramWeight(100);
        dto.setOriginalWidth(1500);
        dto.setOriginalDiameter(40);
        dto.setCoreDiameter(3);
        dto.setRollWeight(new BigDecimal("800.000"));
        dto.setPieceNum(1);
        dto.setProcessMode(1);
        dto.setMainStepType(2);
        return dto;
    }

    private FinishConfigSaveDTO config(int rewindMode) {
        FinishConfigSaveDTO dto = new FinishConfigSaveDTO();
        dto.setProcessMode(1);
        dto.setMainStepType(2);
        dto.setSpareCount(0);
        dto.setRewindMode(rewindMode);
        dto.setUnitPrice(new BigDecimal("150.00"));
        dto.setRewindSegments(List.of(segment(rewindMode)));
        return dto;
    }

    private FinishConfigSaveDTO mergeConfig(List<OriginalRoll> rolls) {
        FinishConfigSaveDTO dto = config(5);
        RewindPlanPreviewDTO.RewindSegmentDTO segment = segment(5);
        segment.setSources(List.of(source(rolls.get(0).getUuid()), source(rolls.get(1).getUuid())));
        dto.setRewindSegments(List.of(segment));
        return dto;
    }

    private RewindPlanPreviewDTO.RewindSegmentDTO segment(int rewindMode) {
        RewindPlanPreviewDTO.RewindSegmentDTO dto = new RewindPlanPreviewDTO.RewindSegmentDTO();
        dto.setSegmentSort(1);
        dto.setSegmentRatio(BigDecimal.ONE);
        dto.setRepeatCount(1);
        dto.setFinishCoreDiameter(3);
        if (rewindMode == 2 || rewindMode == 3) dto.setTargetDiameter(30);
        dto.setLayoutItems(layout(rewindMode));
        return dto;
    }

    private List<RewindPlanPreviewDTO.RewindLayoutItemDTO> layout(int rewindMode) {
        return switch (rewindMode) {
            case 1 -> List.of(item(500, 2), item(480, 1));
            case 2 -> List.of(item(1500, 1));
            case 3 -> List.of(item(750, 2));
            case 4 -> List.of(layeredItem());
            case 5 -> List.of(item(1500, 1));
            default -> throw new IllegalArgumentException("不支持的复卷模式：" + rewindMode);
        };
    }

    private FinishConfigSpecDTO.FinishSourceDTO source(String originalUuid) {
        FinishConfigSpecDTO.FinishSourceDTO dto = new FinishConfigSpecDTO.FinishSourceDTO();
        dto.setOriginalUuid(originalUuid);
        dto.setShareRatio(new BigDecimal("50.00"));
        return dto;
    }

    private RewindPlanPreviewDTO.RewindLayoutItemDTO item(int width, int quantity) {
        RewindPlanPreviewDTO.RewindLayoutItemDTO dto = new RewindPlanPreviewDTO.RewindLayoutItemDTO();
        dto.setItemType("FINISH");
        dto.setWidth(width);
        dto.setQuantity(quantity);
        return dto;
    }

    private RewindPlanPreviewDTO.RewindLayoutItemDTO layeredItem() {
        RewindPlanPreviewDTO.RewindLayoutItemDTO dto = item(1500, 1);
        dto.setLayers(List.of(layer(30), layer(24)));
        return dto;
    }

    private FinishConfigSpecDTO.FinishLayerDTO layer(int diameter) {
        FinishConfigSpecDTO.FinishLayerDTO dto = new FinishConfigSpecDTO.FinishLayerDTO();
        dto.setOutDiameter(diameter);
        dto.setCoreDiameter(3);
        return dto;
    }

    private static String token() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
