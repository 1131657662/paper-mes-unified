package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.FinishCustomerRevisionSummaryVO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionDetailVO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionItemVO;
import com.paper.mes.processorder.entity.FinishCustomerRevision;
import com.paper.mes.processorder.entity.FinishCustomerRevisionItem;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishCustomerRevisionItemMapper;
import com.paper.mes.processorder.mapper.FinishCustomerRevisionMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinishCustomerRevisionReader {

    private final FinishCustomerRevisionMapper revisionMapper;
    private final FinishCustomerRevisionItemMapper itemMapper;
    private final FinishRollMapper finishMapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<FinishCustomerRevisionSummaryVO> history(String orderUuid) {
        return revisionMapper.selectList(new LambdaQueryWrapper<FinishCustomerRevision>()
                        .eq(FinishCustomerRevision::getOrderUuid, orderUuid)
                        .orderByDesc(FinishCustomerRevision::getRevisionNo))
                .stream()
                .map(this::summary)
                .toList();
    }

    public FinishCustomerRevision findByRequest(String orderUuid, String requestId) {
        return revisionMapper.selectOne(new LambdaQueryWrapper<FinishCustomerRevision>()
                .eq(FinishCustomerRevision::getOrderUuid, orderUuid)
                .eq(FinishCustomerRevision::getRequestId, requestId.trim())
                .last("LIMIT 1"));
    }

    @Transactional(readOnly = true)
    public FinishCustomerRevision latestRevision(String orderUuid) {
        return revisionMapper.selectOne(new LambdaQueryWrapper<FinishCustomerRevision>()
                .eq(FinishCustomerRevision::getOrderUuid, orderUuid)
                .orderByDesc(FinishCustomerRevision::getRevisionNo)
                .last("LIMIT 1"));
    }

    public FinishCustomerRevisionSummaryVO summary(FinishCustomerRevision revision) {
        FinishCustomerRevisionSummaryVO result = new FinishCustomerRevisionSummaryVO();
        result.setUuid(revision.getUuid());
        result.setOrderUuid(revision.getOrderUuid());
        result.setRevisionNo(revision.getRevisionNo());
        result.setSourceStage(revision.getSourceStage());
        result.setReason(revision.getReason());
        result.setItemCount(revision.getItemCount());
        result.setCustomerTotalWeight(revision.getCustomerTotalWeight());
        result.setOperator(revision.getCreateBy());
        result.setCreatedAt(revision.getCreateTime());
        return result;
    }

    public FinishCustomerRevisionDetailVO detail(String orderUuid, String revisionUuid) {
        FinishCustomerRevision revision = revisionMapper.selectOne(new LambdaQueryWrapper<FinishCustomerRevision>()
                .eq(FinishCustomerRevision::getUuid, revisionUuid)
                .eq(FinishCustomerRevision::getOrderUuid, orderUuid));
        if (revision == null) throw new BusinessException("客户口径版本不存在");
        FinishCustomerRevisionDetailVO result = new FinishCustomerRevisionDetailVO();
        result.setUuid(revision.getUuid());
        result.setOrderUuid(revision.getOrderUuid());
        result.setRevisionNo(revision.getRevisionNo());
        result.setSourceStage(revision.getSourceStage());
        result.setReason(revision.getReason());
        result.setItemCount(revision.getItemCount());
        result.setCustomerTotalWeight(revision.getCustomerTotalWeight());
        result.setOperator(revision.getCreateBy());
        result.setCreatedAt(revision.getCreateTime());
        List<FinishCustomerRevisionItem> items = itemMapper.selectList(new LambdaQueryWrapper<FinishCustomerRevisionItem>()
                .eq(FinishCustomerRevisionItem::getRevisionUuid, revisionUuid)
                .orderByAsc(FinishCustomerRevisionItem::getCreateTime));
        Map<String, String> rollNos = loadRollNos(items);
        result.setItems(items.stream().map(source -> item(source, rollNos.get(source.getFinishUuid()))).toList());
        return result;
    }

    private Map<String, String> loadRollNos(List<FinishCustomerRevisionItem> items) {
        List<String> finishUuids = items.stream().map(FinishCustomerRevisionItem::getFinishUuid).distinct().toList();
        if (finishUuids.isEmpty()) return Map.of();
        return finishMapper.selectBatchIds(finishUuids).stream().collect(Collectors.toMap(
                FinishRoll::getUuid, FinishRoll::getFinishRollNo, (left, right) -> left));
    }

    private FinishCustomerRevisionItemVO item(FinishCustomerRevisionItem source, String finishRollNo) {
        FinishCustomerRevisionItemVO result = new FinishCustomerRevisionItemVO();
        result.setFinishUuid(source.getFinishUuid());
        result.setFinishRollNo(finishRollNo);
        result.setPhysicalPaperName(source.getPhysicalPaperName());
        result.setPhysicalGramWeight(source.getPhysicalGramWeight());
        result.setPhysicalFinishWidth(source.getPhysicalFinishWidth());
        result.setPhysicalWeightSnapshot(source.getPhysicalWeightSnapshot());
        result.setCustomerPaperName(source.getCustomerPaperName());
        result.setCustomerGramWeight(source.getCustomerGramWeight());
        result.setCustomerFinishWidth(source.getCustomerFinishWidth());
        result.setCustomerDisplayWeight(source.getCustomerDisplayWeight());
        result.setCalculationMode(source.getCalculationMode());
        result.setWeightOperand(source.getWeightOperand());
        result.setFormulaExpression(source.getFormulaExpression());
        result.setFormulaVariables(parseFormulaVariables(source.getFormulaInputs()));
        result.setRoundingScale(source.getRoundingScale());
        result.setRoundingMode(source.getRoundingMode());
        result.setZeroPolicy(source.getZeroPolicy());
        result.setRemark(source.getRemark());
        return result;
    }

    private Map<String, BigDecimal> parseFormulaVariables(String value) {
        if (value == null || value.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new BusinessException("客户重量公式参数快照损坏");
        }
    }
}
