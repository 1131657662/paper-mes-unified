package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.remain.dto.RemainCreditReverseDTO;
import com.paper.mes.remain.entity.RemainAdjustment;
import com.paper.mes.remain.entity.RemainCustomerCreditAccount;
import com.paper.mes.remain.entity.RemainCustomerCreditLedger;
import com.paper.mes.remain.mapper.RemainAdjustmentMapper;
import com.paper.mes.remain.mapper.RemainCustomerCreditAccountMapper;
import com.paper.mes.remain.mapper.RemainCustomerCreditLedgerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemainCustomerCreditService {

    private final RemainAdjustmentMapper adjustmentMapper;
    private final RemainCustomerCreditAccountMapper accountMapper;
    private final RemainCustomerCreditLedgerMapper ledgerMapper;
    private final RemainLockService remainLockService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    public RemainAdjustment credit(String adjustmentUuid, String requestId) {
        RemainCustomerCreditLedger replay = ledgerMapper.selectOne(new LambdaQueryWrapper<RemainCustomerCreditLedger>()
                .eq(RemainCustomerCreditLedger::getRequestId, requestId));
        if (replay != null) {
            return adjustmentMapper.selectById(replay.getAdjustmentUuid());
        }
        RemainAdjustment adjustment = pending(adjustmentUuid);
        remainLockService.lockAdjustment(adjustmentUuid);
        adjustment = pending(adjustmentUuid);
        RemainCustomerCreditAccount account = account(adjustment.getCustomerUuid());
        remainLockService.lockCreditAccount(adjustment.getCustomerUuid());
        account = account(adjustment.getCustomerUuid());
        BigDecimal before = value(account.getCurrentAmount());
        BigDecimal after = before.add(adjustment.getAmount());
        RemainCustomerCreditLedger ledger = ledger(adjustment, account, "CREDIT", before, after, requestId, null);
        ledgerMapper.insert(ledger);
        account.setCurrentAmount(after);
        account.setLastLedgerUuid(ledger.getUuid());
        ConcurrencyGuard.requireRowUpdated(accountMapper.updateById(account));
        adjustment.setTargetType("CUSTOMER_CREDIT");
        adjustment.setStatus("APPLIED");
        ConcurrencyGuard.requireRowUpdated(adjustmentMapper.updateById(adjustment));
        return adjustment;
    }

    @Transactional(rollbackFor = Exception.class)
    public RemainAdjustment reverse(String adjustmentUuid, RemainCreditReverseDTO request) {
        RemainCustomerCreditLedger replay = ledgerMapper.selectOne(new LambdaQueryWrapper<RemainCustomerCreditLedger>()
                .eq(RemainCustomerCreditLedger::getRequestId, request.getRequestId()));
        if (replay != null) {
            return adjustmentMapper.selectById(replay.getAdjustmentUuid());
        }
        RemainAdjustment adjustment = adjustmentMapper.selectById(adjustmentUuid);
        if (adjustment == null || !"CUSTOMER_CREDIT".equals(adjustment.getTargetType())
                || !"APPLIED".equals(adjustment.getStatus())) {
            throw new BusinessException("客户余款调整不存在或不可反向");
        }
        RemainCustomerCreditLedger original = ledgerMapper.selectOne(new LambdaQueryWrapper<RemainCustomerCreditLedger>()
                .eq(RemainCustomerCreditLedger::getAdjustmentUuid, adjustmentUuid)
                .eq(RemainCustomerCreditLedger::getEventType, "CREDIT"));
        if (original == null) {
            throw new BusinessException("客户余款入账流水不存在");
        }
        remainLockService.lockAdjustment(adjustmentUuid);
        remainLockService.lockCreditAccount(adjustment.getCustomerUuid());
        remainLockService.lockCreditLedger(original.getUuid());
        RemainCustomerCreditAccount account = account(adjustment.getCustomerUuid());
        BigDecimal before = value(account.getCurrentAmount());
        if (before.compareTo(original.getAmount()) < 0) {
            throw new BusinessException("客户余款余额不足，不能反向");
        }
        BigDecimal after = before.subtract(original.getAmount());
        RemainCustomerCreditLedger reverse = ledger(adjustment, account, "REVERSE", before, after,
                request.getRequestId(), original.getUuid());
        ledgerMapper.insert(reverse);
        account.setCurrentAmount(after);
        account.setLastLedgerUuid(reverse.getUuid());
        ConcurrencyGuard.requireRowUpdated(accountMapper.updateById(account));
        adjustment.setTargetType("PENDING");
        adjustment.setStatus("PENDING");
        ConcurrencyGuard.requireRowUpdated(adjustmentMapper.updateById(adjustment));
        return adjustment;
    }

    private RemainAdjustment pending(String adjustmentUuid) {
        RemainAdjustment adjustment = adjustmentMapper.selectById(adjustmentUuid);
        if (adjustment == null || !"PENDING".equals(adjustment.getStatus())
                || !"PENDING".equals(adjustment.getTargetType())) {
            throw new BusinessException("余料结算调整不存在或已处理");
        }
        return adjustment;
    }

    private RemainCustomerCreditAccount account(String customerUuid) {
        RemainCustomerCreditAccount account = accountMapper.selectOne(new LambdaQueryWrapper<RemainCustomerCreditAccount>()
                .eq(RemainCustomerCreditAccount::getCustomerUuid, customerUuid));
        if (account != null) {
            return account;
        }
        String uuid = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT IGNORE INTO biz_remain_customer_credit_account
                    (uuid, customer_uuid, current_amount, is_deleted, version)
                VALUES (?, ?, 0, 0, 1)
                """, uuid, customerUuid);
        RemainCustomerCreditAccount created = accountMapper.selectOne(new LambdaQueryWrapper<RemainCustomerCreditAccount>()
                .eq(RemainCustomerCreditAccount::getCustomerUuid, customerUuid));
        if (created == null) {
            throw new BusinessException("客户余款账户创建失败");
        }
        return created;
    }

    private RemainCustomerCreditLedger ledger(RemainAdjustment adjustment,
                                              RemainCustomerCreditAccount account,
                                              String eventType, BigDecimal before, BigDecimal after,
                                              String requestId, String reversalOfUuid) {
        RemainCustomerCreditLedger result = new RemainCustomerCreditLedger();
        result.setUuid(UUID.randomUUID().toString());
        result.setAccountUuid(account.getUuid());
        result.setAdjustmentUuid(adjustment.getUuid());
        result.setCustomerUuid(adjustment.getCustomerUuid());
        result.setEventType(eventType);
        result.setAmount(adjustment.getAmount());
        result.setWeight(adjustment.getWeight());
        result.setBeforeAmount(before);
        result.setAfterAmount(after);
        result.setRequestId(requestId.trim());
        result.setReversalOfUuid(reversalOfUuid);
        result.setCreateBy(AuthContextHolder.currentDisplayName());
        result.setCreateTime(LocalDateTime.now());
        return result;
    }

    private BigDecimal value(BigDecimal source) {
        return source == null ? BigDecimal.ZERO : source;
    }
}
