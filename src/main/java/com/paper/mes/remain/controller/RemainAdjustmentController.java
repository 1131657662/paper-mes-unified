package com.paper.mes.remain.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.remain.dto.RemainAdjustmentCreateDTO;
import com.paper.mes.remain.dto.RemainAdjustmentCancelDTO;
import com.paper.mes.remain.dto.RemainAdjustmentNextSettleDTO;
import com.paper.mes.remain.dto.RemainAdjustmentVO;
import com.paper.mes.remain.dto.RemainApplicationVO;
import com.paper.mes.remain.dto.RemainCreditApplyDTO;
import com.paper.mes.remain.dto.RemainCreditReverseDTO;
import com.paper.mes.remain.dto.RemainRefundCreateDTO;
import com.paper.mes.remain.dto.RemainRefundVO;
import com.paper.mes.remain.service.RemainAdjustmentNextSettlementService;
import com.paper.mes.remain.service.RemainAdjustmentCreationService;
import com.paper.mes.remain.service.RemainAdjustmentCancellationService;
import com.paper.mes.remain.service.RemainAdjustmentQueryService;
import com.paper.mes.remain.service.RemainApplicationQueryService;
import com.paper.mes.remain.service.RemainCustomerCreditService;
import com.paper.mes.remain.service.RemainRefundQueryService;
import com.paper.mes.remain.service.RemainRefundService;
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
@RequestMapping("/api/remain-adjustments")
@RequiredArgsConstructor
public class RemainAdjustmentController {

    private final RemainAdjustmentQueryService queryService;
    private final RemainAdjustmentCreationService creationService;
    private final RemainAdjustmentCancellationService cancellationService;
    private final RemainAdjustmentNextSettlementService nextSettlementService;
    private final RemainApplicationQueryService applicationQueryService;
    private final RemainCustomerCreditService customerCreditService;
    private final RemainRefundService refundService;
    private final RemainRefundQueryService refundQueryService;

    @GetMapping
    @RequirePermission(Permissions.REMAIN_VIEW)
    public R<List<RemainAdjustmentVO>> list() {
        return R.success(queryService.list());
    }

    @GetMapping("/{uuid}")
    @RequirePermission(Permissions.REMAIN_VIEW)
    public R<RemainAdjustmentVO> detail(@PathVariable String uuid) {
        return R.success(queryService.detail(uuid));
    }

    @PostMapping("/registrations/{registrationUuid}")
    @RequirePermission(Permissions.SETTLE_RECEIVE)
    public R<RemainAdjustmentVO> create(@PathVariable String registrationUuid,
                                         @Valid @RequestBody RemainAdjustmentCreateDTO request) {
        return R.success(queryService.detail(creationService.create(registrationUuid, request).getUuid()));
    }

    @PostMapping("/{uuid}/cancel")
    @RequirePermission(Permissions.REMAIN_ROLLBACK)
    public R<RemainAdjustmentVO> cancel(@PathVariable String uuid,
                                        @Valid @RequestBody RemainAdjustmentCancelDTO request) {
        return R.success(queryService.detail(cancellationService.cancel(uuid, request).getUuid()));
    }

    @PostMapping("/{uuid}/next-settlement")
    @RequirePermission(Permissions.SETTLE_RECEIVE)
    public R<RemainApplicationVO> bindNextSettlement(@PathVariable String uuid,
                                                      @Valid @RequestBody RemainAdjustmentNextSettleDTO request) {
        return R.success(applicationQueryService.toView(nextSettlementService.bind(uuid, request)));
    }

    @PostMapping("/{uuid}/customer-credit")
    @RequirePermission(Permissions.SETTLE_RECEIVE)
    public R<RemainAdjustmentVO> credit(@PathVariable String uuid,
                                        @Valid @RequestBody RemainCreditApplyDTO request) {
        return R.success(queryService.detail(customerCreditService.credit(uuid, request.getRequestId()).getUuid()));
    }

    @PostMapping("/{uuid}/customer-credit/reverse")
    @RequirePermission(Permissions.REMAIN_ROLLBACK)
    public R<RemainAdjustmentVO> reverseCredit(@PathVariable String uuid,
                                               @Valid @RequestBody RemainCreditReverseDTO request) {
        return R.success(queryService.detail(customerCreditService.reverse(uuid, request).getUuid()));
    }

    @PostMapping("/{uuid}/refund")
    @RequirePermission(Permissions.SETTLE_RECEIVE)
    public R<RemainRefundVO> createRefund(@PathVariable String uuid,
                                           @Valid @RequestBody RemainRefundCreateDTO request) {
        return R.success(refundQueryService.toView(refundService.create(uuid, request)));
    }
}
