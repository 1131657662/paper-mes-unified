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
        String approvalUuid = approvalService.request(settleUuid, approvalRequest("95.00", "5.00", "100.00"));
        AuthContextHolder.setCurrentUser(admin());
        approvalService.approve(settleUuid, approvalUuid, null);
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
        String approvalUuid = approvalService.request(settleUuid, approvalRequest("95.00", "5.00", "100.00"));

        assertThatThrownBy(() -> approvalService.approve(settleUuid, approvalUuid, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能是同一账号");
    }

    @Test
    void adminLevelApproval_whenFinanceApproves_isRejected() {
        String settleUuid = createSettlement();
        AuthContextHolder.setCurrentUser(finance());
        String approvalUuid = approvalService.request(settleUuid,
                approvalRequest("80.00", "20.00", "100.00"));

        AuthContextHolder.setCurrentUser(user("finance-approver", "finance", "财务复核员"));
        assertThatThrownBy(() -> approvalService.approve(settleUuid, approvalUuid, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有权限");
    }

    @Test
    void rejectedApproval_canBeReplacedByANewReceiptPlan() {
        String settleUuid = createSettlement();
        AuthContextHolder.setCurrentUser(finance());
        String rejectedUuid = approvalService.request(settleUuid,
                approvalRequest("95.00", "5.00", "100.00"));
        AuthContextHolder.setCurrentUser(admin());
        approvalService.reject(rejectedUuid, "资料不足");
        AuthContextHolder.setCurrentUser(finance());

        String replacementUuid = approvalService.request(settleUuid,
                approvalRequest("94.00", "6.00", "100.00"));

        assertThat(replacementUuid).isNotEqualTo(rejectedUuid);
        assertThat(approvalMapper.selectById(rejectedUuid).getApprovalStatus()).isEqualTo(4);
        assertThat(approvalMapper.selectById(replacementUuid).getApprovalStatus()).isEqualTo(1);
    }

    @Test
    void outstandingChange_marksPendingApprovalStale() {
        String settleUuid = createSettlement();
        AuthContextHolder.setCurrentUser(finance());
        String approvalUuid = approvalService.request(settleUuid,
                approvalRequest("95.00", "5.00", "100.00"));
        settleService.receive(settleUuid, cashReceipt("1.00"));
        AuthContextHolder.setCurrentUser(admin());

        assertThatThrownBy(() -> approvalService.approve(settleUuid, approvalUuid, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已失效");
        assertThat(approvalMapper.selectById(approvalUuid).getApprovalStatus()).isEqualTo(6);
    }

    @Test
    void financeLevelApproval_canBeApprovedByAnotherFinanceAccount() {
        String settleUuid = createSettlement();
        AuthContextHolder.setCurrentUser(finance());
        String approvalUuid = approvalService.request(settleUuid,
                approvalRequest("95.00", "5.00", "100.00"));
        AuthContextHolder.setCurrentUser(user("finance-approver", "finance", "财务复核员"));

        approvalService.approve(settleUuid, approvalUuid, null);

        assertThat(approvalMapper.selectById(approvalUuid).getApprovalStatus()).isEqualTo(2);
    }

    @Test
    void rejectedApproval_samePlanWithNewRequestId_canBeSubmittedAgain() {
        String settleUuid = createSettlement();
        AuthContextHolder.setCurrentUser(finance());
        String rejectedUuid = approvalService.request(settleUuid,
                approvalRequest("95.00", "5.00", "100.00"));
        AuthContextHolder.setCurrentUser(admin());
        approvalService.reject(rejectedUuid, "请再次确认客户依据");
        AuthContextHolder.setCurrentUser(finance());

        String replacementUuid = approvalService.request(settleUuid,
                approvalRequest("95.00", "5.00", "100.00"));

        assertThat(replacementUuid).isNotEqualTo(rejectedUuid);
        assertThat(approvalMapper.selectById(replacementUuid).getApprovalStatus()).isEqualTo(1);
    }

    private String createSettlement() {
        var scenario = fixtures.createCompletedOrderWithTwoFinishes();
        return settleService.createByOrder(SettlementTestRequestFactory.byOrder(
                settleService, scenario.order().getUuid()));
    }

    private SettleDiscountApprovalRequestDTO approvalRequest(String cash, String discount, String snapshot) {
        SettleDiscountApprovalRequestDTO request = new SettleDiscountApprovalRequestDTO();
        request.setRequestId(UUID.randomUUID().toString());
        request.setCashAmount(new BigDecimal(cash));
        request.setScrapOffsetAmount(BigDecimal.ZERO);
        request.setDiscountAmount(new BigDecimal(discount));
        request.setUnreceivedSnapshot(new BigDecimal(snapshot));
        request.setReason("客户确认优惠");
        return request;
    }

    private ReceiveDTO cashReceipt(String amount) {
        ReceiveDTO request = new ReceiveDTO();
        request.setRequestId(UUID.randomUUID().toString());
        request.setCashAmount(new BigDecimal(amount));
        request.setPayMethod(2);
        request.setPayNo("TX-BALANCE-CHANGE");
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
