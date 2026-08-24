package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.mapper.RemainRegistrationLineMapper;
import com.paper.mes.remain.mapper.RemainRegistrationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class RemainRegistrationTotalsService {
    private final RemainRegistrationMapper registrationMapper;
    private final RemainRegistrationLineMapper lineMapper;
    private final RemainLockService lockService;

    public void refresh(Collection<String> registrationUuids) {
        registrationUuids.stream().filter(this::present).distinct().sorted().forEach(this::refreshOne);
    }

    private void refreshOne(String registrationUuid) {
        lockService.lockRegistration(registrationUuid);
        RemainRegistration registration = registrationMapper.selectById(registrationUuid);
        if (registration == null) {
            return;
        }
        var lines = lineMapper.selectList(new LambdaQueryWrapper<RemainRegistrationLine>()
                .eq(RemainRegistrationLine::getRegistrationUuid, registrationUuid));
        registration.setTotalRolledBackWeight(sum(lines, true));
        registration.setTotalProcessedWeight(sum(lines, false));
        boolean fullyRolledBack = lines.stream().allMatch(line ->
                value(line.getRolledBackSystemWeight()).compareTo(value(line.getTransferredSystemWeight())) == 0);
        boolean partlyRolledBack = lines.stream().anyMatch(line ->
                value(line.getRolledBackSystemWeight()).signum() > 0);
        registration.setStatus(fullyRolledBack ? "FULL_ROLLED_BACK"
                : partlyRolledBack ? "PARTIAL_ROLLED_BACK" : "ACTIVE");
        ConcurrencyGuard.requireRowUpdated(registrationMapper.updateById(registration));
    }

    private BigDecimal sum(java.util.List<RemainRegistrationLine> lines, boolean rollback) {
        return lines.stream().map(line -> value(rollback
                        ? line.getRolledBackSystemWeight() : line.getProcessedSystemWeight()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
