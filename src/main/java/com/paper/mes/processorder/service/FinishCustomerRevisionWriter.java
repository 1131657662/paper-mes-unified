package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.processorder.dto.FinishCustomerRevisionPreviewVO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionRequestDTO;
import com.paper.mes.processorder.dto.FinishCustomerSpecItemDTO;
import com.paper.mes.processorder.dto.FinishCustomerSpecVO;
import com.paper.mes.processorder.entity.FinishCustomerRevision;
import com.paper.mes.processorder.entity.FinishCustomerRevisionItem;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishCustomerRevisionItemMapper;
import com.paper.mes.processorder.mapper.FinishCustomerRevisionMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FinishCustomerRevisionWriter {

    private final FinishCustomerRevisionMapper revisionMapper;
    private final FinishCustomerRevisionItemMapper itemMapper;
    private final FinishRollMapper finishMapper;
    private final ProcessOrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    public FinishCustomerRevision write(FinishCustomerRevisionWriteCommand command) {
        ProcessOrder order = orderMapper.selectById(command.orderUuid());
        FinishCustomerRevisionPreviewVO preview = command.preview();
        FinishCustomerRevisionRequestDTO request = command.request();
        FinishCustomerRevision revision = revision(order, command.requestHash(), preview, request);
        revisionMapper.insert(revision);
        Map<String, FinishCustomerSpecItemDTO> inputs = request.getItems().stream()
                .collect(Collectors.toMap(FinishCustomerSpecItemDTO::getFinishUuid, item -> item));
        for (FinishCustomerSpecVO row : preview.getItems()) {
            FinishCustomerSpecItemDTO input = inputs.get(row.getFinishUuid());
            itemMapper.insert(revisionItem(revision.getUuid(), row, input));
            updateFinishCache(row, input, request.getReason());
        }
        return revision;
    }

    private FinishCustomerRevision revision(ProcessOrder order,
                                              String requestHash,
                                              FinishCustomerRevisionPreviewVO preview,
                                              FinishCustomerRevisionRequestDTO request) {
        FinishCustomerRevision revision = new FinishCustomerRevision();
        revision.setOrderUuid(order.getUuid());
        revision.setRevisionNo(preview.getNextRevisionNo());
        revision.setRequestId(request.getRequestId().trim());
        revision.setRequestHash(requestHash);
        revision.setSourceStage(sourceStage(order.getOrderStatus()));
        revision.setReason(request.getReason().trim());
        revision.setItemCount(preview.getItemCount());
        revision.setCustomerTotalWeight(positiveOrNull(preview.getCustomerTotalWeight()));
        return revision;
    }

    private FinishCustomerRevisionItem revisionItem(String revisionUuid,
                                                      FinishCustomerSpecVO row,
                                                      FinishCustomerSpecItemDTO input) {
        FinishCustomerRevisionItem item = new FinishCustomerRevisionItem();
        item.setRevisionUuid(revisionUuid);
        item.setFinishUuid(row.getFinishUuid());
        item.setPhysicalPaperName(row.getPhysicalPaperName());
        item.setPhysicalGramWeight(row.getPhysicalGramWeight());
        item.setPhysicalFinishWidth(row.getPhysicalFinishWidth());
        item.setPhysicalWeightSnapshot(row.getPhysicalWeight());
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
        item.setRemark(trimToNull(input.getRemark()));
        return item;
    }

    private void updateFinishCache(FinishCustomerSpecVO row,
                                   FinishCustomerSpecItemDTO input,
                                   String reason) {
        String operator = AuthContextHolder.currentDisplayName();
        LocalDateTime now = LocalDateTime.now();
        ConcurrencyGuard.requireRowUpdated(finishMapper.update(null, new LambdaUpdateWrapper<FinishRoll>()
                .eq(FinishRoll::getUuid, row.getFinishUuid())
                .eq(FinishRoll::getVersion, input.getExpectedVersion())
                .set(FinishRoll::getCustomerPaperName, row.getCustomerPaperName())
                .set(FinishRoll::getCustomerGramWeight, row.getCustomerGramWeight())
                .set(FinishRoll::getCustomerFinishWidth, row.getCustomerFinishWidth())
                .set(FinishRoll::getCustomerDisplayWeight, row.getCustomerDisplayWeight())
                .set(FinishRoll::getCustomerSpecOverrideReason, reason.trim())
                .set(FinishRoll::getCustomerSpecOverrideBy, operator)
                .set(FinishRoll::getCustomerSpecOverrideAt, now)
                .set(FinishRoll::getUpdateBy, operator)
                .set(FinishRoll::getUpdateTime, now)
                .setSql("version = version + 1")));
    }

    private String toJson(Map<String, BigDecimal> variables) {
        if (variables == null || variables.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("客户重量公式参数无法保存");
        }
    }

    private String sourceStage(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "DRAFT";
            case 1 -> "PENDING";
            case 2 -> "PROCESSING";
            case 3 -> "BACK_RECORD";
            case 4 -> "COMPLETED";
            case 5 -> "SETTLED";
            default -> "UNKNOWN";
        };
    }

    private BigDecimal positiveOrNull(BigDecimal value) {
        return value != null && value.signum() > 0 ? value : null;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
