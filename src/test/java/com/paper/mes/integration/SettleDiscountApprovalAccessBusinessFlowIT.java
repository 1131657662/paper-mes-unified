package com.paper.mes.integration;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.settle.dto.SettleDiscountApprovalQuery;
import com.paper.mes.settle.dto.SettleDiscountApprovalRequestDTO;
import com.paper.mes.settle.mapper.SettleDiscountApprovalMapper;
import com.paper.mes.settle.service.SettleDiscountApprovalQueryService;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class SettleDiscountApprovalAccessBusinessFlowIT {
    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private SettleService settleService;
    @Autowired private SettleDiscountApprovalService approvalService;
    @Autowired private SettleDiscountApprovalQueryService approvalQueryService;
    @Autowired private SettleDiscountApprovalMapper approvalMapper;

    @AfterEach
    void clearUser() {
        AuthContextHolder.clear();
    }

    @Test
    void adminLevelApproval_canBeApprovedByAnIndependentAdmin() {
        String settleUuid = createSettlement();
        AuthContextHolder.setCurrentUser(finance());
        String approvalUuid = approvalService.request(settleUuid, approvalRequest("80.00", "20.00"));
        AuthContextHolder.setCurrentUser(admin());

        approvalService.approve(settleUuid, approvalUuid, "业务负责人已确认");

        assertThat(approvalMapper.selectById(approvalUuid).getApprovalStatus()).isEqualTo(2);
    }

    @Test
    void pendingInbox_excludesTheRequesterButIncludesAnIndependentApprover() {
        String settleUuid = createSettlement();
        AuthContextHolder.setCurrentUser(finance());
        String approvalUuid = approvalService.request(settleUuid, approvalRequest("95.00", "5.00"));

        assertThat(approvalQueryService.page(pendingQuery()).getRecords())
                .noneMatch(item -> approvalUuid.equals(item.getUuid()));
        AuthContextHolder.setCurrentUser(user("finance-approver", "finance", "财务复核员"));
        assertThat(approvalQueryService.page(pendingQuery()).getRecords())
                .anyMatch(item -> approvalUuid.equals(item.getUuid()));
    }

    private String createSettlement() {
        var scenario = fixtures.createCompletedOrderWithTwoFinishes();
        return settleService.createByOrder(SettlementTestRequestFactory.byOrder(
                settleService, scenario.order().getUuid()));
    }

    private SettleDiscountApprovalRequestDTO approvalRequest(String cash, String discount) {
        SettleDiscountApprovalRequestDTO request = new SettleDiscountApprovalRequestDTO();
        request.setRequestId(UUID.randomUUID().toString());
        request.setCashAmount(new BigDecimal(cash));
        request.setScrapOffsetAmount(BigDecimal.ZERO);
        request.setDiscountAmount(new BigDecimal(discount));
        request.setUnreceivedSnapshot(new BigDecimal("100.00"));
        request.setReason("客户确认优惠");
        return request;
    }

    private SettleDiscountApprovalQuery pendingQuery() {
        SettleDiscountApprovalQuery query = new SettleDiscountApprovalQuery();
        query.setScope("pending");
        return query;
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
