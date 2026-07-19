package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.common.PageRequestBounds;
import com.paper.mes.common.PageResult;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.dto.DeliveryInventoryOrderGroupVO;
import com.paper.mes.delivery.mapper.DeliveryInventoryOrderGroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryInventoryOrderGroupPageService {

    private final DeliveryInventoryOrderGroupMapper mapper;

    public PageResult<DeliveryInventoryOrderGroupVO> page(DeliveryInventoryFinishQuery query) {
        Page<DeliveryInventoryOrderGroupVO> page = PageRequestBounds.of(query.getCurrent(), query.getSize());
        page.setTotal(mapper.count(query));
        List<DeliveryInventoryOrderGroupVO> groups = mapper.rows(query, page.offset(), page.getSize());
        attachFinishes(query, groups);
        page.setRecords(groups);
        return PageResult.of(page);
    }

    private void attachFinishes(DeliveryInventoryFinishQuery query,
                                List<DeliveryInventoryOrderGroupVO> groups) {
        if (groups.isEmpty()) {
            return;
        }
        List<String> orderUuids = groups.stream()
                .map(DeliveryInventoryOrderGroupVO::getOrderUuid)
                .toList();
        Map<String, List<DeliveryInventoryFinishVO>> byOrder = mapper.finishRows(query, orderUuids)
                .stream()
                .collect(Collectors.groupingBy(DeliveryInventoryFinishVO::getOrderUuid));
        groups.forEach(group -> group.setFinishes(
                byOrder.getOrDefault(group.getOrderUuid(), List.of())));
    }
}
