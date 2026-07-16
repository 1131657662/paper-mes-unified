package com.paper.mes.integration;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.settle.dto.ReceiveDTO;
import com.paper.mes.settle.dto.SettleDiscountApprovalRequestDTO;
import com.paper.mes.settle.mapper.ReceiveRecordMapper;
import com.paper.mes.settle.mapper.SettleDiscountApprovalMapper;
import com.paper.mes.settle.service.SettleDiscountApprovalService;
import com.paper.mes.settle.service.SettleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class SettleDiscountApprovalBusinessFlowIT {
    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private SettleService settleService;
    @Autowired private SettleDiscountApprovalService approvalService;
    @Autowired private SettleDiscountApprovalMapper approvalMapper;
    @Autowired private ReceiveRecordMapper receiveRecordMapper;

    @AfterEach
    void clearUser() {
        AuthContextHolder.clear();
    }

    @Test
    void discountAboveThreshold_whenIndependentlyApproved_isConsumedByReceipt() {
        String settleUuid = createSettlement();
        AuthContextHolder.setCurrentUser(finance());
        String approvalUuid = approvalService.request(settleUuid, approvalRequest("5.00"));
        AuthContextHolder.setCurrentUser(admin());
        approvalService.approve(settleUuid, approvalUuid);
        AuthContextHolder.setCurrentUser(finance());

        settleService.receive(settleUuid, receiveRequest(approvalUuid));

        var approval = approvalMapper.selectById(approvalUuid);
        var receive = receiveRecordMapper.selectList(null).stream()
                .filter(item -> settleUuid.equals(item.getSettleUuid())).findFirst().orElseThrow();
        assertThat(approval.getApprovalStatus()).isEqualTo(3);
        assertThat(receive.getOperator()).isEqualTo("财务测试员");
        assertThat(receive.getDiscountApprovedBy()).isEqualTo("审批管理员");
        assertThat(receive.getDiscountReason()).isEqualTo("客户确认优惠");
    }

    @Test
    void approve_whenRequesterIsApprover_rejectsSelfApproval() {
        String settleUuid = createSettlement();
        AuthContextHolder.setCurrentUser(admin());
        String approvalUuid = approvalService.request(settleUuid, approvalRequest("5.00"));

        assertThatThrownBy(() -> approvalService.approve(settleUuid, approvalUuid))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能是同一账号");
    }

    private String createSettlement() {
        var scenario = fixtures.createCompletedOrderWithTwoFinishes();
        return settleService.createByOrder(SettlementTestRequestFactory.byOrder(
                settleService, scenario.order().getUuid()));
    }

    private SettleDiscountApprovalRequestDTO approvalRequest(String amount) {
        SettleDiscountApprovalRequestDTO request = new SettleDiscountApprovalRequestDTO();
        request.setRequestId(UUID.randomUUID().toString());
        request.setDiscountAmount(new BigDecimal(amount));
        request.setReason("客户确认优惠");
        return request;
    }

    private ReceiveDTO receiveRequest(String approvalUuid) {
        ReceiveDTO request = new ReceiveDTO();
        request.setRequestId(UUID.randomUUID().toString());
        request.setCashAmount(new BigDecimal("95.00"));
        request.setDiscountAmount(new BigDecimal("5.00"));
        request.setDiscountReason("客户确认优惠");
        request.setDiscountApprovalUuid(approvalUuid);
        request.setPayMethod(2);
        request.setPayNo("TX-APPROVED-1");
        return request;
    }

    private CurrentUser finance() {
        return user("finance-user", "finance", "财务测试员");
    }

    private CurrentUser admin() {
        return user("admin-user", "admin", "审批管理员");
    }

    private CurrentUser user(String uuid, String role, String name) {
        return CurrentUser.builder().uuid(uuid).username(uuid).realName(name).roleCode(role).build();
    }
}
