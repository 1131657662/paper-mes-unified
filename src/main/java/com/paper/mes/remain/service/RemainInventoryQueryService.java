package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.remain.dto.RemainInventoryQuery;
import com.paper.mes.remain.dto.RemainInventoryVO;
import com.paper.mes.remain.entity.RemainInventoryLot;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.mapper.RemainInventoryLotMapper;
import com.paper.mes.remain.mapper.RemainRegistrationLineMapper;
import com.paper.mes.remain.mapper.RemainRegistrationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RemainInventoryQueryService {

    private final RemainInventoryLotMapper lotMapper;
    private final RemainRegistrationLineMapper lineMapper;
    private final RemainRegistrationMapper registrationMapper;

    public List<RemainInventoryVO> list(RemainInventoryQuery query) {
        LambdaQueryWrapper<RemainInventoryLot> wrapper = new LambdaQueryWrapper<RemainInventoryLot>()
                .orderByDesc(RemainInventoryLot::getCreateTime)
                .orderByDesc(RemainInventoryLot::getUuid);
        wrapper.eq(query.getCustomerUuid() != null && !query.getCustomerUuid().isBlank(),
                RemainInventoryLot::getCustomerUuid, query.getCustomerUuid());
        if (query.getRegistrationUuid() != null && !query.getRegistrationUuid().isBlank()) {
            List<String> lineUuids = lineUuidByRegistration(query.getRegistrationUuid());
            if (lineUuids.isEmpty()) {
                return List.of();
            }
            wrapper.in(RemainInventoryLot::getRegistrationLineUuid, lineUuids);
        }
        if (Boolean.TRUE.equals(query.getAvailableOnly())) {
            wrapper.gt(RemainInventoryLot::getCurrentWeight, 0);
            wrapper.eq(RemainInventoryLot::getStatus, "IN_OWN_STOCK");
        }
        List<RemainInventoryLot> lots = lotMapper.selectList(wrapper);
        if (lots.isEmpty()) {
            return List.of();
        }
        Map<String, RemainRegistrationLine> lines = lineMapper.selectBatchIds(lots.stream()
                        .map(RemainInventoryLot::getRegistrationLineUuid).toList()).stream()
                .collect(Collectors.toMap(RemainRegistrationLine::getUuid, Function.identity()));
        Map<String, RemainRegistration> registrations = registrationMapper.selectBatchIds(lines.values().stream()
                        .map(RemainRegistrationLine::getRegistrationUuid).distinct().toList()).stream()
                .collect(Collectors.toMap(RemainRegistration::getUuid, Function.identity()));
        return lots.stream().map(lot -> toView(lot, lines.get(lot.getRegistrationLineUuid()), registrations)).toList();
    }

    private List<String> lineUuidByRegistration(String registrationUuid) {
        return lineMapper.selectList(new LambdaQueryWrapper<RemainRegistrationLine>()
                        .eq(RemainRegistrationLine::getRegistrationUuid, registrationUuid))
                .stream().map(RemainRegistrationLine::getUuid).toList();
    }

    private RemainInventoryVO toView(RemainInventoryLot lot, RemainRegistrationLine line,
                                     Map<String, RemainRegistration> registrations) {
        RemainInventoryVO result = new RemainInventoryVO();
        result.setLotUuid(lot.getUuid());
        result.setRegistrationLineUuid(lot.getRegistrationLineUuid());
        result.setSourceFinishRollUuid(lot.getSourceFinishRollUuid());
        result.setCustomerUuid(lot.getCustomerUuid());
        result.setWarehouseUuid(lot.getWarehouseUuid());
        result.setCurrentWeight(lot.getCurrentWeight());
        result.setStatus(lot.getStatus());
        result.setPriceStatus(lot.getPriceStatus());
        if (line != null) {
            result.setRegistrationUuid(line.getRegistrationUuid());
            RemainRegistration registration = registrations.get(line.getRegistrationUuid());
            if (registration != null) {
                result.setRegistrationNo(registration.getRegistrationNo());
            }
        }
        return result;
    }
}
