package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.remain.dto.RemainRefundCreateDTO;
import com.paper.mes.remain.dto.RemainRefundDecisionDTO;
import com.paper.mes.remain.entity.RemainAdjustment;
import com.paper.mes.remain.entity.RemainRefund;
import com.paper.mes.remain.mapper.RemainAdjustmentMapper;
import com.paper.mes.remain.mapper.RemainRefundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemainRefundService {

    private final RemainAdjustmentMapper adjustmentMapper;
    private final RemainRefundMapper refundMapper;
    private final RemainLockService remainLockService;

    @Transactional(rollbackFor = Exception.class)
    public RemainRefund create(String adjustmentUuid, RemainRefundCreateDTO request) {
        RemainRefund replay = refundMapper.selectOne(new LambdaQueryWrapper<RemainRefund>()
                .eq(RemainRefund::getRequestId, request.getRequestId()));
        if (replay != null) {
            return replay;
        }
        RemainAdjustment adjustment = pending(adjustmentUuid);
        remainLockService.lockAdjustment(adjustmentUuid);
        adjustment = pending(adjustmentUuid);
        RemainRefund refund = newRefund(adjustment, request);
        refundMapper.insert(refund);
        adjustment.setTargetType("REFUND");
        adjustment.setStatus("APPLIED");
        ConcurrencyGuard.requireRowUpdated(adjustmentMapper.updateById(adjustment));
        return refund;
    }

    @Transactional(rollbackFor = Exception.class)
    public RemainRefund approve(String refundUuid, RemainRefundDecisionDTO request) {
        RemainRefund refund = refundMapper.selectById(refundUuid);
        if (refund == null || "CANCELLED".equals(refund.getStatus()) || "PAID".equals(refund.getStatus())) {
            throw new BusinessException("退款申请不存在或已终态");
        }
        if ("APPROVED".equals(refund.getStatus())) {
            return refund;
        }
        requireStatus(refund, "REQUESTED");
        remainLockService.lockRefund(refundUuid);
        refund = refundMapper.selectById(refundUuid);
        requireStatus(refund, "REQUESTED");
        refund.setStatus("APPROVED");
        refund.setApprovedBy(AuthContextHolder.currentDisplayName());
        refund.setApprovedAt(LocalDateTime.now());
        refund.setApproveRequestId(request.getRequestId().trim());
        refund.setReason(request.getReason());
        ConcurrencyGuard.requireRowUpdated(refundMapper.updateById(refund));
        return refund;
    }

    @Transactional(rollbackFor = Exception.class)
    public RemainRefund pay(String refundUuid, RemainRefundDecisionDTO request) {
        RemainRefund refund = refundMapper.selectById(refundUuid);
        if (refund == null || "CANCELLED".equals(refund.getStatus())) {
            throw new BusinessException("退款申请不存在或已取消");
        }
        if ("PAID".equals(refund.getStatus())) {
            return refund;
        }
        requireStatus(refund, "APPROVED");
        if (request.getPaymentReference() == null || request.getPaymentReference().isBlank()) {
            throw new BusinessException("已支付退款必须填写支付凭证");
        }
        remainLockService.lockRefund(refundUuid);
        refund = refundMapper.selectById(refundUuid);
        requireStatus(refund, "APPROVED");
        refund.setStatus("PAID");
        refund.setPaymentReference(request.getPaymentReference().trim());
        refund.setPaidBy(AuthContextHolder.currentDisplayName());
        refund.setPaidAt(LocalDateTime.now());
        refund.setPayRequestId(request.getRequestId().trim());
        refund.setReason(request.getReason());
        ConcurrencyGuard.requireRowUpdated(refundMapper.updateById(refund));
        return refund;
    }

    @Transactional(rollbackFor = Exception.class)
    public RemainRefund cancel(String refundUuid, RemainRefundDecisionDTO request) {
        RemainRefund refund = refundMapper.selectById(refundUuid);
        if (refund == null || "PAID".equals(refund.getStatus())) {
            throw new BusinessException("退款不存在或已经支付，不能取消");
        }
        if ("CANCELLED".equals(refund.getStatus())) {
            return refund;
        }
        remainLockService.lockRefund(refundUuid);
        refund = refundMapper.selectById(refundUuid);
        if ("PAID".equals(refund.getStatus())) {
            throw new BusinessException("已支付退款不能取消");
        }
        RemainAdjustment adjustment = adjustmentMapper.selectById(refund.getAdjustmentUuid());
        if (adjustment == null || !"REFUND".equals(adjustment.getTargetType())) {
            throw new BusinessException("退款来源调整不存在");
        }
        refund.setStatus("CANCELLED");
        refund.setCancelRequestId(request.getRequestId().trim());
        refund.setReason(request.getReason());
        ConcurrencyGuard.requireRowUpdated(refundMapper.updateById(refund));
        adjustment.setTargetType("PENDING");
        adjustment.setStatus("PENDING");
        ConcurrencyGuard.requireRowUpdated(adjustmentMapper.updateById(adjustment));
        return refund;
    }

    private RemainAdjustment pending(String adjustmentUuid) {
        RemainAdjustment adjustment = adjustmentMapper.selectById(adjustmentUuid);
        if (adjustment == null || !"PENDING".equals(adjustment.getStatus())
                || !"PENDING".equals(adjustment.getTargetType())) {
            throw new BusinessException("余料结算调整不存在或已处理");
        }
        return adjustment;
    }

    private void requireStatus(RemainRefund refund, String status) {
        if (refund == null || !status.equals(refund.getStatus())) {
            throw new BusinessException("退款状态不允许当前操作");
        }
    }

    private RemainRefund newRefund(RemainAdjustment adjustment, RemainRefundCreateDTO request) {
        RemainRefund result = new RemainRefund();
        result.setUuid(UUID.randomUUID().toString());
        result.setRefundNo("REF-" + result.getUuid().replace("-", "").substring(0, 16));
        result.setAdjustmentUuid(adjustment.getUuid());
        result.setCustomerUuid(adjustment.getCustomerUuid());
        result.setAmount(adjustment.getAmount());
        result.setWeight(adjustment.getWeight());
        result.setStatus("REQUESTED");
        result.setRequestId(request.getRequestId().trim());
        result.setRequestHash(RemainRequestFingerprint.refund(adjustment.getUuid(), adjustment.getAmount(),
                adjustment.getWeight()));
        result.setReason(request.getReason());
        result.setCreateBy(AuthContextHolder.currentDisplayName());
        result.setVersion(1);
        result.setIsDeleted(0);
        return result;
    }
}
