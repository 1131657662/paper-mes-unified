package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServiceOnlyProcessPolicy {

    private final ProcessStepMapper stepMapper;

    public boolean hasConfiguredStep(String originalUuid) {
        return stepMapper.selectCount(new LambdaQueryWrapper<ProcessStep>()
                .eq(ProcessStep::getOriginalUuid, originalUuid)
                .eq(ProcessStep::getIsMain, 0)
                .in(ProcessStep::getStepType, List.of(
                        FeeCalculator.STEP_TYPE_STRIP_SORT,
                FeeCalculator.STEP_TYPE_REPACKAGE))) > 0;
    }

    public Set<String> configuredRollUuids(List<String> originalUuids) {
        if (originalUuids == null || originalUuids.isEmpty()) {
            return Set.of();
        }
        return stepMapper.selectList(new LambdaQueryWrapper<ProcessStep>()
                        .in(ProcessStep::getOriginalUuid, originalUuids)
                        .eq(ProcessStep::getIsMain, 0)
                        .in(ProcessStep::getStepType, List.of(
                                FeeCalculator.STEP_TYPE_STRIP_SORT,
                                FeeCalculator.STEP_TYPE_REPACKAGE)))
                .stream()
                .map(ProcessStep::getOriginalUuid)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void requireConfiguredStep(String originalUuid) {
        if (!hasConfiguredStep(originalUuid)) {
            throw new BusinessException("仅附加工艺至少需要添加剥损整理或重新包装");
        }
    }
}
