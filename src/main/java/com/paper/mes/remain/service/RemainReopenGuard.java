package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.remain.entity.RemainApplication;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.mapper.RemainApplicationMapper;
import com.paper.mes.remain.mapper.RemainRegistrationLineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class RemainReopenGuard {
    private final RemainRegistrationLineMapper lineMapper;
    private final RemainApplicationMapper applicationMapper;

    public void assertAllowed(Collection<FinishRoll> finishes) {
        var finishUuids = finishes.stream().map(FinishRoll::getUuid).filter(this::present).toList();
        if (finishUuids.isEmpty()) {
            return;
        }
        var lines = lineMapper.selectList(new LambdaQueryWrapper<RemainRegistrationLine>()
                .in(RemainRegistrationLine::getSourceFinishRollUuid, finishUuids));
        for (RemainRegistrationLine line : lines) {
            if (value(line.getCurrentOwnWeight()).signum() > 0
                    || value(line.getProcessedSystemWeight()).signum() > 0) {
                throw new BusinessException("来源余料仍有我方库存或出售处理历史，不能撤回回录");
            }
            long activeApplications = applicationMapper.selectCount(new LambdaQueryWrapper<RemainApplication>()
                    .eq(RemainApplication::getRegistrationUuid, line.getRegistrationUuid())
                    .eq(RemainApplication::getApplicationType, "APPLY")
                    .eq(RemainApplication::getStatus, "ACTIVE"));
            if (activeApplications > 0) {
                throw new BusinessException("来源余料仍有有效抵扣应用，不能撤回回录");
            }
        }
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
