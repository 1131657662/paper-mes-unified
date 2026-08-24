package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.remain.dto.RemainRefundVO;
import com.paper.mes.remain.entity.RemainRefund;
import com.paper.mes.remain.mapper.RemainRefundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RemainRefundQueryService {

    private final RemainRefundMapper refundMapper;

    public List<RemainRefundVO> list() {
        return refundMapper.selectList(new LambdaQueryWrapper<RemainRefund>()
                        .orderByDesc(RemainRefund::getCreateTime)
                        .orderByDesc(RemainRefund::getUuid))
                .stream().map(this::toView).toList();
    }

    public RemainRefundVO detail(String uuid) {
        RemainRefund refund = refundMapper.selectById(uuid);
        if (refund == null) {
            throw new BusinessException("退款申请不存在");
        }
        return toView(refund);
    }

    public RemainRefundVO toView(RemainRefund source) {
        RemainRefundVO result = new RemainRefundVO();
        result.setUuid(source.getUuid());
        result.setRefundNo(source.getRefundNo());
        result.setAdjustmentUuid(source.getAdjustmentUuid());
        result.setCustomerUuid(source.getCustomerUuid());
        result.setAmount(source.getAmount());
        result.setWeight(source.getWeight());
        result.setStatus(source.getStatus());
        result.setPaymentReference(source.getPaymentReference());
        result.setReason(source.getReason());
        result.setApprovedAt(source.getApprovedAt());
        result.setPaidAt(source.getPaidAt());
        return result;
    }
}
