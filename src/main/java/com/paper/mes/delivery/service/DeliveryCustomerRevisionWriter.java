package com.paper.mes.delivery.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.*;
import com.paper.mes.delivery.entity.*;
import com.paper.mes.delivery.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DeliveryCustomerRevisionWriter {

    private final DeliveryCustomerRevisionMapper revisionMapper;
    private final DeliveryCustomerRevisionItemMapper itemMapper;
    private final DeliveryOrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    public DeliveryCustomerRevision write(DeliveryCustomerRevisionWriteCommand command) {
        DeliveryOrder order = orderMapper.selectById(command.deliveryUuid());
        DeliveryCustomerRevision revision = revision(order, command);
        revisionMapper.insert(revision);
        Map<String, DeliveryCustomerSpecItemDTO> inputs = command.request().getItems().stream()
                .collect(Collectors.toMap(DeliveryCustomerSpecItemDTO::getDeliveryDetailUuid, item -> item));
        for (DeliveryCustomerSpecVO row : command.preview().getItems()) {
            itemMapper.insert(revisionItem(revision.getUuid(), row, inputs.get(row.getDeliveryDetailUuid())));
        }
        return revision;
    }

    private DeliveryCustomerRevision revision(
            DeliveryOrder order, DeliveryCustomerRevisionWriteCommand command) {
        DeliveryCustomerRevision revision = new DeliveryCustomerRevision();
        revision.setDeliveryUuid(order.getUuid());
        revision.setRevisionNo(command.preview().getNextRevisionNo());
        revision.setRequestId(command.request().getRequestId().trim());
        revision.setRequestHash(command.requestHash());
        revision.setReason(command.request().getReason().trim());
        revision.setItemCount(command.preview().getItemCount());
        revision.setCustomerTotalWeight(command.preview().getCustomerTotalWeight());
        return revision;
    }

    private DeliveryCustomerRevisionItem revisionItem(
            String revisionUuid, DeliveryCustomerSpecVO row, DeliveryCustomerSpecItemDTO input) {
        DeliveryCustomerRevisionItem item = new DeliveryCustomerRevisionItem();
        item.setRevisionUuid(revisionUuid);
        item.setDeliveryDetailUuid(row.getDeliveryDetailUuid());
        item.setFinishUuid(row.getFinishUuid());
        item.setPhysicalPaperName(row.getPhysicalPaperName());
        item.setPhysicalGramWeight(row.getPhysicalGramWeight());
        item.setPhysicalFinishWidth(row.getPhysicalFinishWidth());
        item.setPhysicalDeliveryWeight(row.getPhysicalDeliveryWeight());
        item.setCustomerPaperName(row.getCustomerPaperName());
        item.setCustomerGramWeight(row.getCustomerGramWeight());
        item.setCustomerFinishWidth(row.getCustomerFinishWidth());
        item.setCustomerDisplayWeight(row.getCustomerDisplayWeight());
        item.setCalculationMode(input.getCalculationMode().name());
        item.setWeightOperand(input.getWeightOperand());
        item.setFormulaExpression(trimToNull(input.getFormulaExpression()));
        item.setFormulaInputs(toJson(input.getFormulaVariables()));
        item.setRoundingScale(input.getRoundingScale());
        item.setRoundingMode(input.getRoundingMode().name());
        item.setZeroPolicy(input.getZeroPolicy().name());
        item.setCustomerRemark(trimToNull(input.getCustomerRemark()));
        return item;
    }

    private String toJson(Map<String, BigDecimal> variables) {
        if (variables == null || variables.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("客户重量公式参数无法保存");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
