package com.paper.mes.settle.controller;

import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.settle.dto.ReceiveDTO;
import com.paper.mes.settle.dto.SettleByMonthDTO;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.dto.SettleByOrdersDTO;
import com.paper.mes.settle.dto.SettleActionReasonDTO;
import com.paper.mes.settle.dto.SettleCandidateQuery;
import com.paper.mes.settle.dto.SettleCandidateVO;
import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.dto.SettleQuery;
import com.paper.mes.settle.dto.SettleQuoteVO;
import com.paper.mes.settle.dto.SettleQuoteByOrderDTO;
import com.paper.mes.settle.dto.SettleQuoteByOrdersDTO;
import com.paper.mes.settle.dto.SettleQuoteByMonthDTO;
import com.paper.mes.settle.dto.SettleDiscountApprovalRequestDTO;
import com.paper.mes.settle.dto.SettleDiscountApprovalVO;
import com.paper.mes.settle.dto.SettleListSummaryVO;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.oplog.entity.OperationLog;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import com.paper.mes.settle.service.SettleListSummaryService;
import com.paper.mes.settle.service.SettleDiscountApprovalService;
import com.paper.mes.settle.service.SettleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/settle-orders")
@RequiredArgsConstructor
public class SettleController {

    private final SettleService settleService;
    private final SettleListSummaryService settleListSummaryService;
    private final SettleDiscountApprovalService discountApprovalService;

    @GetMapping
    @RequirePermission(Permissions.SETTLE_VIEW)
    public R<PageResult<SettleOrder>> page(@Valid SettleQuery query) {
        return R.success(settleService.page(query));
    }

    @GetMapping("/summary")
    @RequirePermission(Permissions.SETTLE_VIEW)
    public R<SettleListSummaryVO> summary(@Valid SettleQuery query) {
        return R.success(settleListSummaryService.summarize(query));
    }

    @GetMapping("/candidates")
    @RequirePermission(Permissions.SETTLE_MANAGE)
    public R<PageResult<SettleCandidateVO>> candidates(@Valid SettleCandidateQuery query) {
        return R.success(settleService.listCandidates(query));
    }

    @PostMapping("/quote/by-orders")
    @RequirePermission(Permissions.SETTLE_MANAGE)
    public R<SettleQuoteVO> quoteByOrders(@Valid @RequestBody SettleQuoteByOrdersDTO dto) {
        return R.success(settleService.quoteByOrders(dto));
    }

    @PostMapping("/quote/by-order")
    @RequirePermission(Permissions.SETTLE_MANAGE)
    public R<SettleQuoteVO> quoteByOrder(@Valid @RequestBody SettleQuoteByOrderDTO dto) {
        return R.success(settleService.quoteByOrder(dto));
    }

    @PostMapping("/quote/by-month")
    @RequirePermission(Permissions.SETTLE_MANAGE)
    public R<SettleQuoteVO> quoteByMonth(@Valid @RequestBody SettleQuoteByMonthDTO dto) {
        return R.success(settleService.quoteByMonth(dto));
    }

    @PostMapping("/by-order")
    @RequirePermission(Permissions.SETTLE_MANAGE)
    public R<String> createByOrder(@Valid @RequestBody SettleByOrderDTO dto) {
        return R.success(settleService.createByOrder(dto));
    }

    @PostMapping("/by-orders")
    @RequirePermission(Permissions.SETTLE_MANAGE)
    public R<String> createByOrders(@Valid @RequestBody SettleByOrdersDTO dto) {
        return R.success(settleService.createByOrders(dto));
    }

    @PostMapping("/by-month")
    @RequirePermission(Permissions.SETTLE_MANAGE)
    public R<String> createByMonth(@Valid @RequestBody SettleByMonthDTO dto) {
        return R.success(settleService.createByMonth(dto));
    }

    @GetMapping("/{uuid}")
    @RequirePermission(Permissions.SETTLE_VIEW)
    public R<SettleDetailVO> detail(@PathVariable String uuid) {
        return R.success(settleService.getDetail(uuid));
    }

    @GetMapping("/{uuid}/header")
    @RequirePermission(Permissions.SETTLE_VIEW)
    public R<SettleOrder> detailHeader(@PathVariable String uuid) {
        return R.success(settleService.getDetailOrder(uuid));
    }

    @GetMapping("/{uuid}/details")
    @RequirePermission(Permissions.SETTLE_VIEW)
    public R<List<SettleDetail>> details(@PathVariable String uuid) {
        return R.success(settleService.getDetails(uuid));
    }

    @GetMapping("/{uuid}/receives")
    @RequirePermission(Permissions.SETTLE_VIEW)
    public R<List<ReceiveRecord>> receives(@PathVariable String uuid) {
        return R.success(settleService.getReceives(uuid));
    }

    @GetMapping("/{uuid}/print-lines")
    @RequirePermission(Permissions.SETTLE_VIEW)
    public R<List<SettlePrintLineVO>> printLines(@PathVariable String uuid) {
        return R.success(settleService.getPrintLines(uuid));
    }

    @GetMapping("/{uuid}/operation-logs")
    @RequirePermission(Permissions.SETTLE_VIEW)
    public R<List<OperationLog>> operationLogs(@PathVariable String uuid) {
        return R.success(settleService.getOperationLogs(uuid));
    }

    @PostMapping("/{uuid}/receive")
    @RequirePermission(Permissions.SETTLE_RECEIVE)
    public R<Void> receive(@PathVariable String uuid, @Valid @RequestBody ReceiveDTO dto) {
        settleService.receive(uuid, dto);
        return R.success();
    }

    @GetMapping("/{uuid}/discount-approvals")
    @RequirePermission(Permissions.SETTLE_RECEIVE)
    public R<List<SettleDiscountApprovalVO>> discountApprovals(@PathVariable String uuid) {
        return R.success(discountApprovalService.list(uuid));
    }

    @PostMapping("/{uuid}/discount-approvals")
    @RequirePermission(Permissions.SETTLE_DISCOUNT)
    public R<String> requestDiscountApproval(@PathVariable String uuid,
            @Valid @RequestBody SettleDiscountApprovalRequestDTO dto) {
        return R.success(discountApprovalService.request(uuid, dto));
    }

    @PostMapping("/{uuid}/discount-approvals/{approvalUuid}/approve")
    @RequirePermission(Permissions.SETTLE_DISCOUNT_APPROVE)
    public R<Void> approveDiscount(@PathVariable String uuid, @PathVariable String approvalUuid) {
        discountApprovalService.approve(uuid, approvalUuid);
        return R.success();
    }

    @PostMapping("/{uuid}/receives/{receiveUuid}/cancel")
    @RequirePermission(Permissions.SETTLE_RECEIVE)
    public R<Void> cancelReceive(@PathVariable String uuid,
                                 @PathVariable String receiveUuid,
                                 @Valid @RequestBody SettleActionReasonDTO dto) {
        settleService.cancelReceive(uuid, receiveUuid, dto);
        return R.success();
    }

    @PostMapping("/{uuid}/void")
    @RequirePermission(Permissions.SETTLE_MANAGE)
    public R<Void> voidSettle(@PathVariable String uuid, @Valid @RequestBody SettleActionReasonDTO dto) {
        settleService.voidSettle(uuid, dto);
        return R.success();
    }
}
