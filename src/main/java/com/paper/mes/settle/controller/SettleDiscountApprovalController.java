package com.paper.mes.settle.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.settle.dto.SettleActionReasonDTO;
import com.paper.mes.settle.dto.SettleDiscountApprovalQuery;
import com.paper.mes.settle.dto.SettleDiscountApprovalVO;
import com.paper.mes.settle.dto.SettleDiscountDecisionDTO;
import com.paper.mes.settle.service.SettleDiscountApprovalQueryService;
import com.paper.mes.settle.service.SettleDiscountApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settle-discount-approvals")
@RequiredArgsConstructor
@RequirePermission({Permissions.SETTLE_DISCOUNT, Permissions.SETTLE_DISCOUNT_APPROVE,
        Permissions.SETTLE_DISCOUNT_ADMIN_APPROVE})
public class SettleDiscountApprovalController {
    private final SettleDiscountApprovalService commandService;
    private final SettleDiscountApprovalQueryService queryService;

    @GetMapping
    public R<PageResult<SettleDiscountApprovalVO>> page(@Valid SettleDiscountApprovalQuery query) {
        return R.success(queryService.page(query));
    }

    @GetMapping("/{uuid}")
    public R<SettleDiscountApprovalVO> detail(@PathVariable String uuid) {
        SettleDiscountApprovalVO detail = queryService.detail(uuid);
        if (detail == null) throw new BusinessException(ErrorCode.E002, "优惠审批记录不存在");
        return R.success(detail);
    }

    @PostMapping("/{uuid}/approve")
    @RequirePermission({Permissions.SETTLE_DISCOUNT_APPROVE, Permissions.SETTLE_DISCOUNT_ADMIN_APPROVE})
    public R<Void> approve(@PathVariable String uuid,
                           @Valid @RequestBody(required = false) SettleDiscountDecisionDTO dto) {
        SettleDiscountApprovalVO detail = requireDetail(uuid);
        commandService.approve(detail.getSettleUuid(), uuid, dto == null ? null : dto.getReason());
        return R.success();
    }

    @PostMapping("/{uuid}/reject")
    @RequirePermission({Permissions.SETTLE_DISCOUNT_APPROVE, Permissions.SETTLE_DISCOUNT_ADMIN_APPROVE})
    public R<Void> reject(@PathVariable String uuid, @Valid @RequestBody SettleActionReasonDTO dto) {
        commandService.reject(uuid, dto.getReason());
        return R.success();
    }

    @PostMapping("/{uuid}/cancel")
    @RequirePermission(Permissions.SETTLE_DISCOUNT)
    public R<Void> cancel(@PathVariable String uuid, @Valid @RequestBody SettleActionReasonDTO dto) {
        commandService.cancel(uuid, dto.getReason());
        return R.success();
    }

    private SettleDiscountApprovalVO requireDetail(String uuid) {
        SettleDiscountApprovalVO detail = queryService.detail(uuid);
        if (detail == null) throw new BusinessException(ErrorCode.E002, "优惠审批记录不存在");
        return detail;
    }
}
