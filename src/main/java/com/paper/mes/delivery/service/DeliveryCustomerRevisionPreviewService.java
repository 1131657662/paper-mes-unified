package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.*;
import com.paper.mes.delivery.dto.*;
import com.paper.mes.delivery.entity.*;
import com.paper.mes.delivery.mapper.*;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DeliveryCustomerRevisionPreviewService {

    private static final int DELIVERY_STATUS_OUT = 2;
    public static final String REVISION_KIND_LIVE = "LIVE_FINISH";
    public static final String REVISION_KIND_SYSTEM = "SYSTEM_BASELINE";
    public static final String REVISION_KIND_USER = "USER_REVISION";
    public static final String REVISION_KIND_HISTORICAL = "HISTORICAL_BASELINE";

    private final DeliveryOrderMapper orderMapper;
    private final DeliveryDetailMapper detailMapper;
    private final FinishRollMapper finishMapper;
    private final DeliveryService deliveryService;
    private final DeliveryCustomerRevisionReader reader;
    private final DeliveryCustomerSpecPlanner planner;

    @Transactional(readOnly = true)
    public DeliveryCustomerRevisionPreviewVO current(String deliveryUuid) {
        DeliveryOrder order = requireOrder(deliveryUuid);
        DeliveryCustomerRevision latest = reader.latestRevision(deliveryUuid);
        List<DeliveryCustomerSpecContext> contexts = contexts(deliveryUuid, usePhysicalBaseline(order, latest));
        List<DeliveryCustomerSpecVO> items = contexts.stream().map(planner::current).toList();
        return summary(order, reader.nextRevisionNo(deliveryUuid), revisionKind(order, latest), items);
    }

    @Transactional(readOnly = true)
    public DeliveryCustomerRevisionPreviewVO preview(
            String deliveryUuid, DeliveryCustomerRevisionRequestDTO request) {
        DeliveryOrder order = requireOrder(deliveryUuid);
        validateOrder(order, request.getExpectedDeliveryVersion());
        DeliveryCustomerRevision latest = reader.latestRevision(deliveryUuid);
        Map<String, DeliveryCustomerSpecItemDTO> requested = requestedItems(request);
        Map<String, DeliveryCustomerSpecContext> contexts = contexts(
                deliveryUuid, usePhysicalBaseline(order, latest)).stream()
                .collect(java.util.stream.Collectors.toMap(
                        context -> context.detail().getUuid(), context -> context));
        if (!contexts.keySet().containsAll(requested.keySet())) {
            throw new BusinessException(ErrorCode.E002, "部分出库明细不存在或不属于当前出库单");
        }
        List<DeliveryCustomerSpecVO> items = requested.values().stream()
                .map(item -> planRow(contexts.get(item.getDeliveryDetailUuid()), item))
                .toList();
        return summary(order, reader.nextRevisionNo(deliveryUuid), revisionKind(order, latest), items);
    }

    private List<DeliveryCustomerSpecContext> contexts(String deliveryUuid, boolean usePhysicalBaseline) {
        List<DeliveryDetailItemVO> physicalItems = deliveryService.getDetail(deliveryUuid).getDetails();
        if (physicalItems.isEmpty()) return List.of();
        List<String> detailUuids = physicalItems.stream().map(DeliveryDetailItemVO::getUuid).toList();
        Map<String, DeliveryDetail> details = detailMapper.selectBatchIds(detailUuids).stream()
                .collect(java.util.stream.Collectors.toMap(DeliveryDetail::getUuid, detail -> detail));
        List<String> finishUuids = physicalItems.stream().map(DeliveryDetailItemVO::getFinishUuid).distinct().toList();
        Map<String, FinishRoll> finishes = finishMapper.selectBatchIds(finishUuids).stream()
                .collect(java.util.stream.Collectors.toMap(FinishRoll::getUuid, finish -> finish));
        if (details.size() != detailUuids.size() || finishes.size() != finishUuids.size()) {
            throw new BusinessException(ErrorCode.E002, "出库单关联的成品或明细不存在");
        }
        Map<String, DeliveryCustomerRevisionItem> previous = reader.latestItems(deliveryUuid, detailUuids);
        return physicalItems.stream().map(item -> new DeliveryCustomerSpecContext(
                item, details.get(item.getUuid()), finishes.get(item.getFinishUuid()),
                previous.get(item.getUuid()), usePhysicalBaseline))
                .toList();
    }

    private DeliveryCustomerSpecVO planRow(
            DeliveryCustomerSpecContext context, DeliveryCustomerSpecItemDTO item) {
        try {
            return planner.plan(context, item);
        } catch (BusinessException exception) {
            DeliveryCustomerSpecVO row = planner.current(context);
            row.setValid(false);
            row.setError(exception.getMessage());
            return row;
        }
    }

    private Map<String, DeliveryCustomerSpecItemDTO> requestedItems(
            DeliveryCustomerRevisionRequestDTO request) {
        Map<String, DeliveryCustomerSpecItemDTO> result = new LinkedHashMap<>();
        for (DeliveryCustomerSpecItemDTO item : request.getItems()) {
            if (result.putIfAbsent(item.getDeliveryDetailUuid(), item) != null) {
                throw new BusinessException("同一出库明细不能重复选择");
            }
        }
        return result;
    }

    private DeliveryOrder requireOrder(String deliveryUuid) {
        DeliveryOrder order = orderMapper.selectById(deliveryUuid);
        if (order == null) throw new BusinessException(ErrorCode.E002, "出库单不存在");
        if (order.getDeliveryStatus() != null && order.getDeliveryStatus() == 3) {
            throw new BusinessException(ErrorCode.E001, "已作废出库单不能创建客户更正版");
        }
        return order;
    }

    private void validateOrder(DeliveryOrder order, Integer expectedVersion) {
        if (!Objects.equals(order.getVersion(), expectedVersion)) {
            throw new BusinessException(ErrorCode.E006, "出库单已被其他人修改，请刷新后重试");
        }
    }

    private DeliveryCustomerRevisionPreviewVO summary(
            DeliveryOrder order, int nextRevisionNo, String revisionKind,
            List<DeliveryCustomerSpecVO> items) {
        BigDecimal physical = sum(items, true);
        BigDecimal customer = sum(items, false);
        DeliveryCustomerRevisionPreviewVO result = new DeliveryCustomerRevisionPreviewVO();
        result.setDeliveryUuid(order.getUuid());
        result.setDeliveryNo(order.getDeliveryNo());
        result.setDeliveryVersion(order.getVersion());
        result.setDeliveryStatus(order.getDeliveryStatus());
        result.setCurrentRevisionNo(nextRevisionNo - 1);
        result.setCurrentRevisionKind(revisionKind);
        result.setNextRevisionNo(nextRevisionNo);
        result.setItemCount(items.size());
        result.setValidItemCount((int) items.stream().filter(DeliveryCustomerSpecVO::isValid).count());
        result.setPhysicalTotalWeight(physical);
        result.setCustomerTotalWeight(customer);
        result.setDifferenceWeight(customer.subtract(physical).setScale(3));
        result.setHasErrors(result.getValidItemCount() != result.getItemCount());
        result.setItems(items);
        return result;
    }

    private boolean usePhysicalBaseline(DeliveryOrder order, DeliveryCustomerRevision latest) {
        return Integer.valueOf(DELIVERY_STATUS_OUT).equals(order.getDeliveryStatus()) && latest == null;
    }

    private String revisionKind(DeliveryOrder order, DeliveryCustomerRevision latest) {
        if (latest == null) {
            return usePhysicalBaseline(order, null) ? REVISION_KIND_HISTORICAL : REVISION_KIND_LIVE;
        }
        String requestId = latest.getRequestId();
        return requestId != null && requestId.startsWith(DeliveryCustomerRevisionReader.SYSTEM_REQUEST_PREFIX)
                ? REVISION_KIND_SYSTEM : REVISION_KIND_USER;
    }

    private BigDecimal sum(List<DeliveryCustomerSpecVO> items, boolean physical) {
        return items.stream().filter(row -> physical || row.isValid())
                .map(row -> physical ? row.getPhysicalDeliveryWeight() : row.getCustomerDisplayWeight())
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(3);
    }
}
