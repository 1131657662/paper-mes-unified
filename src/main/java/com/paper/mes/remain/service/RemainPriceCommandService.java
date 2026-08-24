package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.remain.dto.RemainPriceConfirmDTO;
import com.paper.mes.remain.entity.RemainPriceVersion;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.entity.RemainRegistrationLine;
import com.paper.mes.remain.mapper.RemainPriceVersionMapper;
import com.paper.mes.remain.mapper.RemainRegistrationLineMapper;
import com.paper.mes.remain.mapper.RemainRegistrationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemainPriceCommandService {

    private final RemainRegistrationMapper registrationMapper;
    private final RemainRegistrationLineMapper lineMapper;
    private final RemainPriceVersionMapper priceMapper;
    private final RemainLockService lockService;

    @Transactional(rollbackFor = Exception.class)
    public RemainRegistration confirm(String registrationUuid, RemainPriceConfirmDTO request) {
        BigDecimal amount = integerAmount(request.getTotalAmount());
        String hash = request.getPricingBasis() + "|" + amount.toPlainString();
        RemainPriceVersion replay = priceMapper.selectOne(new LambdaQueryWrapper<RemainPriceVersion>()
                .eq(RemainPriceVersion::getRequestId, request.getRequestId()));
        if (replay != null) {
            if (!hash.equals(replay.getRequestHash())) {
                throw new BusinessException("相同请求号的价格载荷不一致");
            }
            return registrationMapper.selectById(registrationUuid);
        }
        lockService.lockRegistration(registrationUuid);
        RemainRegistration registration = registrationMapper.selectById(registrationUuid);
        if (registration == null || !registrationUuid.equals(registration.getUuid())) {
            throw new BusinessException("登记单不存在");
        }
        List<RemainRegistrationLine> lines = lineMapper.selectList(new LambdaQueryWrapper<RemainRegistrationLine>()
                .eq(RemainRegistrationLine::getRegistrationUuid, registrationUuid)
                .orderByAsc(RemainRegistrationLine::getUuid));
        lockService.lockLines(lines.stream().map(RemainRegistrationLine::getUuid).toList());
        lines = lineMapper.selectList(new LambdaQueryWrapper<RemainRegistrationLine>()
                .eq(RemainRegistrationLine::getRegistrationUuid, registrationUuid)
                .orderByAsc(RemainRegistrationLine::getUuid));
        if (lines.isEmpty() || lines.stream().anyMatch(line -> nz(line.getAppliedAmount()).signum() > 0)) {
            throw new BusinessException("已进入财务分配的登记单不能直接改价");
        }
        distributeAmount(lines, amount, registration.getTotalTransferredWeight());
        for (RemainRegistrationLine line : lines) {
            ConcurrencyGuard.requireRowUpdated(lineMapper.updateById(line));
        }
        int nextVersion = nz(registration.getPriceVersion()) + 1;
        RemainPriceVersion version = newVersion(registrationUuid, nextVersion, request, amount, hash);
        priceMapper.insert(version);
        registration.setPriceVersion(nextVersion);
        registration.setPricingBasis(request.getPricingBasis().trim());
        registration.setPriceConfirmedAt(version.getConfirmedAt());
        registration.setPriceConfirmedBy(version.getConfirmedBy());
        registration.setPriceStatus("CONFIRMED");
        registration.setTotalAmount(amount);
        ConcurrencyGuard.requireRowUpdated(registrationMapper.updateById(registration));
        return registration;
    }

    private void distributeAmount(List<RemainRegistrationLine> lines, BigDecimal amount,
                                  BigDecimal totalWeight) {
        if (totalWeight == null || totalWeight.signum() <= 0) {
            throw new BusinessException("登记单没有有效转入重量");
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < lines.size(); i++) {
            RemainRegistrationLine line = lines.get(i);
            BigDecimal lineAmount = i == lines.size() - 1
                    ? amount.subtract(allocated)
                    : amount.multiply(line.getTransferredSystemWeight())
                    .divide(totalWeight, 0, RoundingMode.DOWN);
            line.setAmount(lineAmount);
            line.setAppliedAmount(nz(line.getAppliedAmount()));
            line.setAppliedWeight(nz(line.getAppliedWeight()));
            allocated = allocated.add(lineAmount);
        }
    }

    private RemainPriceVersion newVersion(String registrationUuid, int versionNo,
                                          RemainPriceConfirmDTO request, BigDecimal amount, String hash) {
        RemainPriceVersion result = new RemainPriceVersion();
        result.setUuid(UUID.randomUUID().toString());
        result.setRegistrationUuid(registrationUuid);
        result.setVersionNo(versionNo);
        result.setPricingBasis(request.getPricingBasis().trim());
        result.setTotalAmount(amount);
        result.setRequestId(request.getRequestId().trim());
        result.setRequestHash(hash);
        result.setStatus("CONFIRMED");
        result.setConfirmedAt(LocalDateTime.now());
        result.setConfirmedBy(AuthContextHolder.currentDisplayName());
        return result;
    }

    private BigDecimal integerAmount(BigDecimal value) {
        if (value.stripTrailingZeros().scale() > 0) {
            throw new BusinessException("余料金额必须为整数元");
        }
        return value.setScale(0, RoundingMode.UNNECESSARY);
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
