package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.remain.dto.RemainAdjustmentLineVO;
import com.paper.mes.remain.dto.RemainAdjustmentVO;
import com.paper.mes.remain.entity.RemainAdjustment;
import com.paper.mes.remain.entity.RemainAdjustmentLine;
import com.paper.mes.remain.mapper.RemainAdjustmentLineMapper;
import com.paper.mes.remain.mapper.RemainAdjustmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RemainAdjustmentQueryService {

    private final RemainAdjustmentMapper adjustmentMapper;
    private final RemainAdjustmentLineMapper lineMapper;

    public List<RemainAdjustmentVO> list() {
        return adjustmentMapper.selectList(new LambdaQueryWrapper<RemainAdjustment>()
                        .orderByDesc(RemainAdjustment::getCreateTime)
                        .orderByDesc(RemainAdjustment::getUuid))
                .stream().map(this::toView).toList();
    }

    public RemainAdjustmentVO detail(String adjustmentUuid) {
        RemainAdjustment adjustment = adjustmentMapper.selectById(adjustmentUuid);
        if (adjustment == null) {
            throw new BusinessException("余料结算调整不存在");
        }
        RemainAdjustmentVO result = toView(adjustment);
        result.setLines(lineMapper.selectList(new LambdaQueryWrapper<RemainAdjustmentLine>()
                        .eq(RemainAdjustmentLine::getAdjustmentUuid, adjustmentUuid))
                .stream().map(this::toLineView).toList());
        return result;
    }

    private RemainAdjustmentVO toView(RemainAdjustment source) {
        RemainAdjustmentVO result = new RemainAdjustmentVO();
        result.setUuid(source.getUuid());
        result.setAdjustmentNo(source.getAdjustmentNo());
        result.setRegistrationUuid(source.getRegistrationUuid());
        result.setSourceSettleUuid(source.getSourceSettleUuid());
        result.setTargetSettleUuid(source.getTargetSettleUuid());
        result.setCustomerUuid(source.getCustomerUuid());
        result.setTargetType(source.getTargetType());
        result.setStatus(source.getStatus());
        result.setAmount(source.getAmount());
        result.setWeight(source.getWeight());
        result.setReason(source.getReason());
        return result;
    }

    private RemainAdjustmentLineVO toLineView(RemainAdjustmentLine source) {
        RemainAdjustmentLineVO result = new RemainAdjustmentLineVO();
        result.setRegistrationLineUuid(source.getRegistrationLineUuid());
        result.setAmount(source.getAmount());
        result.setWeight(source.getWeight());
        return result;
    }
}
