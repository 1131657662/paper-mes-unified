package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FinishRollSourceBinder {

    private static final int DIRECT_SHIP = 3;

    private final OriginalRollMapper originalRollMapper;
    private final FinishOriginalRelMapper relationMapper;

    public void bind(BindRequest request) {
        OriginalRoll source = resolveSource(request.orderUuid(), request.originalUuid());
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setOrderUuid(request.orderUuid());
        relation.setFinishUuid(request.finish().getUuid());
        relation.setOriginalUuid(source.getUuid());
        relation.setShareRatio(new BigDecimal("100.00"));
        relation.setRemark(request.remark());
        relationMapper.insert(relation);
    }

    private OriginalRoll resolveSource(String orderUuid, String requestedUuid) {
        List<OriginalRoll> candidates = originalRollMapper.selectList(
                new LambdaQueryWrapper<OriginalRoll>()
                        .eq(OriginalRoll::getOrderUuid, orderUuid)
                        .ne(OriginalRoll::getProcessMode, DIRECT_SHIP)
                        .orderByAsc(OriginalRoll::getRowSort));
        if (StringUtils.hasText(requestedUuid)) {
            return candidates.stream()
                    .filter(roll -> requestedUuid.equals(roll.getUuid()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("来源母卷不属于当前加工单"));
        }
        if (candidates.size() != 1) {
            throw new BusinessException("多母卷加工单生成成品卷号时必须选择来源母卷");
        }
        return candidates.get(0);
    }

    public record BindRequest(String orderUuid, FinishRoll finish,
                              String originalUuid, String remark) {
    }
}
