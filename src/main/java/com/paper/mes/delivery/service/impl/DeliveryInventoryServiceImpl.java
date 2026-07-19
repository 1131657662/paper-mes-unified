package com.paper.mes.delivery.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.common.PageRequestBounds;
import com.paper.mes.common.PageResult;
import com.paper.mes.delivery.dto.DeliveryInventoryCustomerQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryCustomerVO;
import com.paper.mes.delivery.dto.DeliveryInventoryAvailabilityRequest;
import com.paper.mes.delivery.dto.DeliveryInventoryAvailabilityVO;
import com.paper.mes.delivery.dto.DeliveryInventoryFilter;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.dto.DeliveryInventorySummaryVO;
import com.paper.mes.delivery.dto.DeliveryInventoryUnavailableVO;
import com.paper.mes.delivery.mapper.DeliveryInventoryMapper;
import com.paper.mes.delivery.service.DeliveryInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryInventoryServiceImpl implements DeliveryInventoryService {

    private final DeliveryInventoryMapper inventoryMapper;

    @Override
    public DeliveryInventorySummaryVO summary(DeliveryInventoryFilter filter) {
        DeliveryInventorySummaryVO summary = inventoryMapper.summary(filter);
        summary.setAsOf(LocalDateTime.now());
        return summary;
    }

    @Override
    public PageResult<DeliveryInventoryCustomerVO> pageCustomers(DeliveryInventoryCustomerQuery query) {
        Page<DeliveryInventoryCustomerVO> page = PageRequestBounds.of(query.getCurrent(), query.getSize());
        page.setTotal(inventoryMapper.customerCount(query));
        page.setRecords(inventoryMapper.customerRows(query, page.offset(), page.getSize()));
        return PageResult.of(page);
    }

    @Override
    public PageResult<DeliveryInventoryFinishVO> pageFinishes(DeliveryInventoryFinishQuery query) {
        Page<DeliveryInventoryFinishVO> page = PageRequestBounds.of(query.getCurrent(), query.getSize());
        page.setTotal(inventoryMapper.finishCount(query));
        page.setRecords(inventoryMapper.finishRows(query, page.offset(), page.getSize()));
        return PageResult.of(page);
    }

    @Override
    public DeliveryInventoryAvailabilityVO validateAvailability(DeliveryInventoryAvailabilityRequest request) {
        List<String> requested = request.getFinishUuids().stream().distinct().toList();
        List<DeliveryInventoryFinishVO> rows = inventoryMapper.availabilityRows(
                request.getCustomerUuid(), request.getWarehouseUuid(), requested);
        Map<String, DeliveryInventoryFinishVO> rowByUuid = rows.stream().collect(
                Collectors.toMap(DeliveryInventoryFinishVO::getFinishUuid, Function.identity()));
        DeliveryInventoryAvailabilityVO result = new DeliveryInventoryAvailabilityVO();
        for (String finishUuid : requested) classifyAvailability(finishUuid, rowByUuid.get(finishUuid), result);
        return result;
    }

    private void classifyAvailability(String uuid, DeliveryInventoryFinishVO row,
                                      DeliveryInventoryAvailabilityVO result) {
        if (row == null) {
            result.getUnavailable().add(new DeliveryInventoryUnavailableVO(uuid, "库存卷不存在或已出库"));
            return;
        }
        if (!Integer.valueOf(1).equals(row.getStockState())) {
            result.getUnavailable().add(new DeliveryInventoryUnavailableVO(uuid, "已被待出库单占用"));
            return;
        }
        result.getAvailableFinishUuids().add(uuid);
    }
}
