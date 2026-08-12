package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.FinishCustomerRevisionPreviewVO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionRequestDTO;
import com.paper.mes.processorder.dto.FinishCustomerSpecItemDTO;
import com.paper.mes.processorder.dto.FinishCustomerSpecVO;
import com.paper.mes.processorder.entity.FinishCustomerRevision;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishCustomerRevisionMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinishCustomerRevisionPreviewService {

    private final ProcessOrderMapper orderMapper;
    private final FinishRollMapper finishMapper;
    private final FinishCustomerRevisionMapper revisionMapper;
    private final FinishCustomerSpecPlanner planner;
    private final FinishCustomerRevisionPolicy revisionPolicy;
    private final ProcessOrderDeliveryImpactCounter deliveryImpactCounter;

    @Transactional(readOnly = true)
    public FinishCustomerRevisionPreviewVO current(String orderUuid) {
        ProcessOrder order = requireOrder(orderUuid);
        List<FinishCustomerSpecVO> items = loadActiveFinishes(orderUuid).stream()
                .map(planner::current)
                .toList();
        return summary(order, nextRevisionNo(orderUuid), items);
    }

    @Transactional(readOnly = true)
    public FinishCustomerRevisionPreviewVO preview(
            String orderUuid, FinishCustomerRevisionRequestDTO request) {
        ProcessOrder order = requireOrder(orderUuid);
        validateOrder(order, request.getExpectedOrderVersion());
        Map<String, FinishCustomerSpecItemDTO> requested = requestedItems(request);
        Map<String, FinishRoll> finishes = loadRequestedFinishes(orderUuid, requested.keySet().stream().toList());
        List<FinishCustomerSpecVO> rows = requested.values().stream()
                .map(item -> planRow(finishes.get(item.getFinishUuid()), item))
                .toList();
        revisionPolicy.requirePublishAllowed(order, rows);
        return summary(order, nextRevisionNo(orderUuid), rows);
    }

    public void requirePublishAllowed(String orderUuid, FinishCustomerRevisionPreviewVO preview) {
        ProcessOrder order = requireOrder(orderUuid);
        revisionPolicy.requirePublishAllowed(order, preview.getItems());
    }

    private FinishCustomerSpecVO planRow(FinishRoll finish, FinishCustomerSpecItemDTO item) {
        try {
            return planner.plan(finish, item);
        } catch (BusinessException exception) {
            FinishCustomerSpecVO row = planner.current(finish);
            row.setValid(false);
            row.setError(exception.getMessage());
            return row;
        }
    }

    private ProcessOrder requireOrder(String orderUuid) {
        ProcessOrder order = orderMapper.selectById(orderUuid);
        if (order == null) throw new BusinessException(ErrorCode.E002, "加工单不存在");
        revisionPolicy.requireReadable(order);
        return order;
    }

    private void validateOrder(ProcessOrder order, Integer expectedVersion) {
        if (!java.util.Objects.equals(order.getVersion(), expectedVersion)) {
            throw new BusinessException(ErrorCode.E006, "加工单已被其他人修改，请刷新后重试");
        }
    }

    private Map<String, FinishCustomerSpecItemDTO> requestedItems(
            FinishCustomerRevisionRequestDTO request) {
        Map<String, FinishCustomerSpecItemDTO> result = new LinkedHashMap<>();
        for (FinishCustomerSpecItemDTO item : request.getItems()) {
            if (result.putIfAbsent(item.getFinishUuid(), item) != null) {
                throw new BusinessException("同一成品不能重复选择");
            }
        }
        return result;
    }

    private Map<String, FinishRoll> loadRequestedFinishes(String orderUuid, List<String> finishUuids) {
        List<FinishRoll> finishes = finishMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid)
                .in(FinishRoll::getUuid, finishUuids));
        if (finishes.size() != finishUuids.size()) {
            throw new BusinessException(ErrorCode.E002, "部分成品不存在或不属于当前加工单");
        }
        Map<String, FinishRoll> result = new LinkedHashMap<>();
        finishes.forEach(finish -> result.put(finish.getUuid(), finish));
        return result;
    }

    private List<FinishRoll> loadActiveFinishes(String orderUuid) {
        return finishMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid)
                .ne(FinishRoll::getRollNoStatus, 3)
                .eq(FinishRoll::getIsSpare, 0)
                .eq(FinishRoll::getIsRemain, 0)
                .orderByAsc(FinishRoll::getRowSort));
    }

    private int nextRevisionNo(String orderUuid) {
        FinishCustomerRevision latest = revisionMapper.selectOne(
                new LambdaQueryWrapper<FinishCustomerRevision>()
                        .eq(FinishCustomerRevision::getOrderUuid, orderUuid)
                        .orderByDesc(FinishCustomerRevision::getRevisionNo)
                        .last("LIMIT 1"));
        return latest == null ? 1 : latest.getRevisionNo() + 1;
    }

    private FinishCustomerRevisionPreviewVO summary(
            ProcessOrder order, int nextRevisionNo, List<FinishCustomerSpecVO> items) {
        BigDecimal physicalTotal = sum(items, true);
        BigDecimal customerTotal = sum(items, false);
        FinishCustomerRevisionPreviewVO preview = new FinishCustomerRevisionPreviewVO();
        preview.setOrderUuid(order.getUuid());
        preview.setOrderNo(order.getOrderNo());
        preview.setOrderVersion(order.getVersion());
        preview.setSourceStage(sourceStage(order.getOrderStatus()));
        preview.setNextRevisionNo(nextRevisionNo);
        int itemCount = items.size();
        int validItemCount = (int) items.stream().filter(FinishCustomerSpecVO::isValid).count();
        preview.setItemCount(itemCount);
        preview.setValidItemCount(validItemCount);
        preview.setPhysicalTotalWeight(physicalTotal);
        preview.setCustomerTotalWeight(customerTotal);
        preview.setDifferenceWeight(customerTotal.subtract(physicalTotal).setScale(3));
        preview.setHasErrors(validItemCount != itemCount);
        preview.setReissueRequired(revisionPolicy.requiresReissue(order, items));
        preview.setPendingDeliveryCount(deliveryImpactCounter.pendingDeliveryCount(order.getUuid()));
        preview.setItems(items);
        return preview;
    }

    private BigDecimal sum(List<FinishCustomerSpecVO> items, boolean physical) {
        return items.stream()
                .filter(row -> physical || row.isValid())
                .map(row -> physical ? row.getPhysicalWeight() : row.getCustomerDisplayWeight())
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(3);
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
}
