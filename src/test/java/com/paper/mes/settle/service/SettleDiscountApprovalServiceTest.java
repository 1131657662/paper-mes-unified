package com.paper.mes.settle.service;

import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.settle.dto.SettleDiscountApprovalRequestDTO;
import com.paper.mes.settle.entity.SettleDiscountApproval;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SettleDiscountApprovalServiceTest {
    private SettleOrderMapper settleOrderMapper;
    private SettleDiscountApprovalRequestFactory requestFactory;
    private SettleDiscountApprovalStore approvalStore;
    private SettleDiscountApprovalService service;

    @BeforeEach
    void setUp() {
        settleOrderMapper = mock(SettleOrderMapper.class);
        requestFactory = mock(SettleDiscountApprovalRequestFactory.class);
        approvalStore = mock(SettleDiscountApprovalStore.class);
        service = new SettleDiscountApprovalService(
                settleOrderMapper,
                mock(PermissionChecker.class),
                mock(BusinessLockService.class),
                mock(SettleDiscountApprovalNotificationService.class),
                requestFactory,
                approvalStore,
                mock(OperationLogService.class));
    }

    @Test
    void replaysTheOriginalRequestBeforeValidatingCurrentSettlementState() {
        SettleDiscountApprovalRequestDTO request = request("request-1", "500.00");
        SettleDiscountApproval existing = existingApproval(request);
        when(approvalStore.findByRequestId("settle-1", "request-1")).thenReturn(existing);

        String result = service.request("settle-1", request);

        assertThat(result).isEqualTo("approval-1");
        verifyNoInteractions(settleOrderMapper, requestFactory);
    }

    @Test
    void rejectsARequestIdReusedForDifferentReceiptPlan() {
        SettleDiscountApprovalRequestDTO original = request("request-1", "500.00");
        SettleDiscountApprovalRequestDTO changed = request("request-1", "400.00");
        when(approvalStore.findByRequestId("settle-1", "request-1"))
                .thenReturn(existingApproval(original));

        assertThatThrownBy(() -> service.request("settle-1", changed))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请求号已用于其他优惠审批申请");
        verifyNoInteractions(settleOrderMapper, requestFactory);
    }

    private SettleDiscountApproval existingApproval(SettleDiscountApprovalRequestDTO request) {
        SettleDiscountApproval approval = new SettleDiscountApproval();
        approval.setUuid("approval-1");
        approval.setRequestHash(SettleDiscountApprovalFingerprint.of("settle-1", request));
        return approval;
    }

    private SettleDiscountApprovalRequestDTO request(String requestId, String cashAmount) {
        SettleDiscountApprovalRequestDTO request = new SettleDiscountApprovalRequestDTO();
        request.setRequestId(requestId);
        request.setCashAmount(new BigDecimal(cashAmount));
        request.setScrapOffsetAmount(BigDecimal.ZERO);
        request.setDiscountAmount(new BigDecimal("1000.00"));
        request.setUnreceivedSnapshot(new BigDecimal("1500.00"));
        request.setReason("业务减免");
        return request;
    }
}
