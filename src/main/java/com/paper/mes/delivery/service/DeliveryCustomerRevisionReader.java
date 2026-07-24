package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryCustomerRevisionSummaryVO;
import com.paper.mes.delivery.dto.DeliveryCustomerRevisionDetailVO;
import com.paper.mes.delivery.dto.DeliveryCustomerRevisionItemVO;
import com.paper.mes.delivery.entity.DeliveryCustomerRevision;
import com.paper.mes.delivery.entity.DeliveryCustomerRevisionItem;
import com.paper.mes.delivery.mapper.DeliveryCustomerRevisionItemMapper;
import com.paper.mes.delivery.mapper.DeliveryCustomerRevisionMapper;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryCustomerRevisionReader {

    public static final String SYSTEM_REQUEST_PREFIX = "SYSTEM_CONFIRM:";

    private final DeliveryCustomerRevisionMapper revisionMapper;
    private final DeliveryCustomerRevisionItemMapper itemMapper;
    private final FinishRollMapper finishMapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<DeliveryCustomerRevisionSummaryVO> history(String deliveryUuid) {
        return revisions(deliveryUuid).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, DeliveryCustomerRevisionItem> latestItems(
            String deliveryUuid, Collection<String> detailUuids) {
        if (detailUuids.isEmpty()) return Map.of();
        List<DeliveryCustomerRevision> revisions = revisions(deliveryUuid);
        if (revisions.isEmpty()) return Map.of();
        List<String> revisionUuids = revisions.stream().map(DeliveryCustomerRevision::getUuid).toList();
        Map<String, List<DeliveryCustomerRevisionItem>> byRevision = itemMapper.selectList(
                        new LambdaQueryWrapper<DeliveryCustomerRevisionItem>()
                                .in(DeliveryCustomerRevisionItem::getRevisionUuid, revisionUuids)
                                .in(DeliveryCustomerRevisionItem::getDeliveryDetailUuid, detailUuids))
                .stream().collect(Collectors.groupingBy(DeliveryCustomerRevisionItem::getRevisionUuid));
        Map<String, DeliveryCustomerRevisionItem> latest = new LinkedHashMap<>();
        for (DeliveryCustomerRevision revision : revisions) {
            for (DeliveryCustomerRevisionItem item : byRevision.getOrDefault(revision.getUuid(), List.of())) {
                latest.putIfAbsent(item.getDeliveryDetailUuid(), item);
            }
        }
        return latest;
    }

    public DeliveryCustomerRevision findByRequest(String deliveryUuid, String requestId) {
        return revisionMapper.selectOne(new LambdaQueryWrapper<DeliveryCustomerRevision>()
                .eq(DeliveryCustomerRevision::getDeliveryUuid, deliveryUuid)
                .eq(DeliveryCustomerRevision::getRequestId, requestId.trim())
                .last("LIMIT 1"));
    }

    public DeliveryCustomerRevisionSummaryVO summary(DeliveryCustomerRevision revision) {
        DeliveryCustomerRevisionSummaryVO result = new DeliveryCustomerRevisionSummaryVO();
        result.setUuid(revision.getUuid());
        result.setDeliveryUuid(revision.getDeliveryUuid());
        result.setRevisionNo(revision.getRevisionNo());
        result.setReason(revision.getReason());
        result.setItemCount(revision.getItemCount());
        result.setCustomerTotalWeight(revision.getCustomerTotalWeight());
        result.setOperator(revision.getCreateBy());
        result.setCreatedAt(revision.getCreateTime());
        return result;
    }

    public DeliveryCustomerRevisionDetailVO detail(String deliveryUuid, String revisionUuid) {
        DeliveryCustomerRevision revision = revisionMapper.selectOne(new LambdaQueryWrapper<DeliveryCustomerRevision>()
                .eq(DeliveryCustomerRevision::getUuid, revisionUuid)
                .eq(DeliveryCustomerRevision::getDeliveryUuid, deliveryUuid));
        if (revision == null) throw new BusinessException("客户更正版不存在");
        DeliveryCustomerRevisionDetailVO result = new DeliveryCustomerRevisionDetailVO();
        result.setUuid(revision.getUuid());
        result.setDeliveryUuid(revision.getDeliveryUuid());
        result.setRevisionNo(revision.getRevisionNo());
        result.setReason(revision.getReason());
        result.setItemCount(revision.getItemCount());
        result.setCustomerTotalWeight(revision.getCustomerTotalWeight());
        result.setOperator(revision.getCreateBy());
        result.setCreatedAt(revision.getCreateTime());
        List<DeliveryCustomerRevisionItem> items = itemMapper.selectList(new LambdaQueryWrapper<DeliveryCustomerRevisionItem>()
                .eq(DeliveryCustomerRevisionItem::getRevisionUuid, revisionUuid)
                .orderByAsc(DeliveryCustomerRevisionItem::getCreateTime));
        Map<String, String> rollNos = loadRollNos(items);
        result.setItems(items.stream().map(source -> item(source, rollNos.get(source.getFinishUuid()))).toList());
        return result;
    }

    private Map<String, String> loadRollNos(List<DeliveryCustomerRevisionItem> items) {
        List<String> finishUuids = items.stream().map(DeliveryCustomerRevisionItem::getFinishUuid).distinct().toList();
        if (finishUuids.isEmpty()) return Map.of();
        return finishMapper.selectBatchIds(finishUuids).stream().collect(Collectors.toMap(
                FinishRoll::getUuid, FinishRoll::getFinishRollNo, (left, right) -> left));
    }

    private DeliveryCustomerRevisionItemVO item(DeliveryCustomerRevisionItem source, String finishRollNo) {
        DeliveryCustomerRevisionItemVO result = new DeliveryCustomerRevisionItemVO();
        result.setDeliveryDetailUuid(source.getDeliveryDetailUuid());
        result.setFinishUuid(source.getFinishUuid());
        result.setFinishRollNo(finishRollNo);
        result.setPhysicalPaperName(source.getPhysicalPaperName());
        result.setPhysicalGramWeight(source.getPhysicalGramWeight());
        result.setPhysicalFinishWidth(source.getPhysicalFinishWidth());
        result.setPhysicalDeliveryWeight(source.getPhysicalDeliveryWeight());
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
        result.setCustomerRemark(source.getCustomerRemark());
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

    public int nextRevisionNo(String deliveryUuid) {
        List<DeliveryCustomerRevision> revisions = revisions(deliveryUuid);
        return revisions.isEmpty() ? 1 : revisions.getFirst().getRevisionNo() + 1;
    }

    public DeliveryCustomerRevision latestRevision(String deliveryUuid) {
        return revisionMapper.selectOne(new LambdaQueryWrapper<DeliveryCustomerRevision>()
                .eq(DeliveryCustomerRevision::getDeliveryUuid, deliveryUuid)
                .orderByDesc(DeliveryCustomerRevision::getRevisionNo)
                .last("LIMIT 1"));
    }

    private List<DeliveryCustomerRevision> revisions(String deliveryUuid) {
        return revisionMapper.selectList(new LambdaQueryWrapper<DeliveryCustomerRevision>()
                .eq(DeliveryCustomerRevision::getDeliveryUuid, deliveryUuid)
                .orderByDesc(DeliveryCustomerRevision::getRevisionNo));
    }
}
