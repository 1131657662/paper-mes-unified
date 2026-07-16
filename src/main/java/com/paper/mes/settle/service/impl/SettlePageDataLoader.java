package com.paper.mes.settle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.ReceiveRecordMapper;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class SettlePageDataLoader {

    private static final int RECEIVE_STATUS_ACTIVE = 1;

    private final SettleDetailMapper settleDetailMapper;
    private final ReceiveRecordMapper receiveRecordMapper;
    private final ProcessOrderService processOrderService;
    private final CustomerService customerService;

    PageData load(List<SettleOrder> settlements) {
        if (settlements.isEmpty()) {
            return PageData.empty();
        }
        List<String> settleUuids = settlements.stream().map(SettleOrder::getUuid).toList();
        List<SettleDetail> details = loadDetails(settleUuids);
        return new PageData(groupDetails(details), loadOrders(details),
                loadCustomers(settlements), loadReceiveTotals(settleUuids));
    }

    private List<SettleDetail> loadDetails(List<String> settleUuids) {
        return settleDetailMapper.selectList(new LambdaQueryWrapper<SettleDetail>()
                .eq(SettleDetail::getIsDeleted, 0)
                .in(SettleDetail::getSettleUuid, settleUuids));
    }

    private Map<String, List<SettleDetail>> groupDetails(List<SettleDetail> details) {
        Map<String, List<SettleDetail>> grouped = new LinkedHashMap<>();
        for (SettleDetail detail : details) {
            grouped.computeIfAbsent(detail.getSettleUuid(), ignored -> new ArrayList<>()).add(detail);
        }
        return grouped;
    }

    private Map<String, ProcessOrder> loadOrders(List<SettleDetail> details) {
        List<String> orderUuids = details.stream().map(SettleDetail::getOrderUuid)
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (orderUuids.isEmpty()) {
            return Map.of();
        }
        Map<String, ProcessOrder> result = new LinkedHashMap<>();
        processOrderService.listByIds(orderUuids).forEach(order -> result.put(order.getUuid(), order));
        return result;
    }

    private Map<String, Customer> loadCustomers(List<SettleOrder> settlements) {
        List<String> customerUuids = settlements.stream().map(SettleOrder::getCustomerUuid)
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (customerUuids.isEmpty()) {
            return Map.of();
        }
        Map<String, Customer> result = new LinkedHashMap<>();
        customerService.listByIds(customerUuids).forEach(customer -> result.put(customer.getUuid(), customer));
        return result;
    }

    private Map<String, SettleReceiveTotals> loadReceiveTotals(List<String> settleUuids) {
        List<ReceiveRecord> records = receiveRecordMapper.selectList(new LambdaQueryWrapper<ReceiveRecord>()
                .eq(ReceiveRecord::getIsDeleted, 0)
                .in(ReceiveRecord::getSettleUuid, settleUuids));
        Map<String, SettleReceiveTotals> result = new LinkedHashMap<>();
        for (ReceiveRecord record : records) {
            if (record.getRecordStatus() == null || record.getRecordStatus() == RECEIVE_STATUS_ACTIVE) {
                result.merge(record.getSettleUuid(), SettleReceiveTotals.zero().add(record),
                        SettlePageDataLoader::sumTotals);
            }
        }
        return result;
    }

    private static SettleReceiveTotals sumTotals(SettleReceiveTotals left, SettleReceiveTotals right) {
        return new SettleReceiveTotals(left.receiveAmount().add(right.receiveAmount()),
                left.cashAmount().add(right.cashAmount()),
                left.scrapOffsetAmount().add(right.scrapOffsetAmount()),
                left.discountAmount().add(right.discountAmount()));
    }

    record PageData(Map<String, List<SettleDetail>> detailsBySettle,
                    Map<String, ProcessOrder> orderByUuid,
                    Map<String, Customer> customerByUuid,
                    Map<String, SettleReceiveTotals> receiveTotalsBySettle) {

        static PageData empty() {
            return new PageData(Map.of(), Map.of(), Map.of(), Map.of());
        }
    }
}
