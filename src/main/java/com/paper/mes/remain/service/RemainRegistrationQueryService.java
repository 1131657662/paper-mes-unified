package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.remain.dto.RemainRegistrationLineVO;
import com.paper.mes.remain.dto.RemainRegistrationQuery;
import com.paper.mes.remain.dto.RemainRegistrationVO;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.mapper.RemainRegistrationLineMapper;
import com.paper.mes.remain.mapper.RemainRegistrationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RemainRegistrationQueryService {

    private final RemainRegistrationMapper registrationMapper;
    private final RemainRegistrationLineMapper lineMapper;

    public RemainRegistrationVO detail(String uuid) {
        RemainRegistration registration = registrationMapper.selectById(uuid);
        if (registration == null) {
            throw new BusinessException("登记单不存在");
        }
        RemainRegistrationVO result = toView(registration);
        result.setLines(lineMapper.selectList(new LambdaQueryWrapper<RemainRegistrationLine>()
                        .eq(RemainRegistrationLine::getRegistrationUuid, uuid)
                        .orderByAsc(RemainRegistrationLine::getCreateTime))
                .stream().map(this::toLineView).toList());
        return result;
    }

    public List<RemainRegistrationVO> list(RemainRegistrationQuery query) {
        LambdaQueryWrapper<RemainRegistration> wrapper = new LambdaQueryWrapper<RemainRegistration>()
                .orderByDesc(RemainRegistration::getRegistrationDate)
                .orderByDesc(RemainRegistration::getUuid);
        wrapper.eq(query.getOrderUuid() != null && !query.getOrderUuid().isBlank(),
                RemainRegistration::getOrderUuid, query.getOrderUuid());
        wrapper.eq(query.getCustomerUuid() != null && !query.getCustomerUuid().isBlank(),
                RemainRegistration::getCustomerUuid, query.getCustomerUuid());
        return registrationMapper.selectList(wrapper).stream().map(this::toView).toList();
    }

    private RemainRegistrationVO toView(RemainRegistration source) {
        RemainRegistrationVO result = new RemainRegistrationVO();
        result.setUuid(source.getUuid());
        result.setRegistrationNo(source.getRegistrationNo());
        result.setRequestId(source.getRequestId());
        result.setOrderUuid(source.getOrderUuid());
        result.setCustomerUuid(source.getCustomerUuid());
        result.setRegistrationDate(source.getRegistrationDate());
        result.setConfirmationName(source.getConfirmationName());
        result.setConfirmationChannel(source.getConfirmationChannel());
        result.setConfirmationAt(source.getConfirmationAt());
        result.setConfirmationEvidence(source.getConfirmationEvidence());
        result.setStatus(source.getStatus());
        result.setPriceStatus(source.getPriceStatus());
        result.setPriceVersion(source.getPriceVersion());
        result.setPricingBasis(source.getPricingBasis());
        result.setPriceConfirmedAt(source.getPriceConfirmedAt());
        result.setPriceConfirmedBy(source.getPriceConfirmedBy());
        result.setTotalTransferredWeight(source.getTotalTransferredWeight());
        result.setTotalRolledBackWeight(source.getTotalRolledBackWeight());
        result.setTotalProcessedWeight(source.getTotalProcessedWeight());
        result.setTotalAmount(source.getTotalAmount());
        return result;
    }

    private RemainRegistrationLineVO toLineView(RemainRegistrationLine source) {
        RemainRegistrationLineVO result = new RemainRegistrationLineVO();
        result.setUuid(source.getUuid());
        result.setSourceFinishRollUuid(source.getSourceFinishRollUuid());
        result.setSourceSystemWeight(source.getSourceSystemWeight());
        result.setTransferredSystemWeight(source.getTransferredSystemWeight());
        result.setRolledBackSystemWeight(source.getRolledBackSystemWeight());
        result.setProcessedSystemWeight(source.getProcessedSystemWeight());
        result.setCurrentOwnWeight(source.getCurrentOwnWeight());
        result.setAmount(source.getAmount());
        result.setStatus(source.getStatus());
        return result;
    }
}
