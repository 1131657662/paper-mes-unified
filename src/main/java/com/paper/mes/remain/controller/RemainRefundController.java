package com.paper.mes.remain.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.remain.dto.RemainRefundDecisionDTO;
import com.paper.mes.remain.dto.RemainRefundVO;
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
@RequestMapping("/api/remain-refunds")
@RequiredArgsConstructor
public class RemainRefundController {

    private final RemainRefundQueryService queryService;
    private final RemainRefundService refundService;

    @GetMapping
    @RequirePermission(Permissions.REMAIN_VIEW)
    public R<List<RemainRefundVO>> list() {
        return R.success(queryService.list());
    }

    @GetMapping("/{uuid}")
    @RequirePermission(Permissions.REMAIN_VIEW)
    public R<RemainRefundVO> detail(@PathVariable String uuid) {
        return R.success(queryService.detail(uuid));
    }

    @PostMapping("/{uuid}/approve")
    @RequirePermission(Permissions.SETTLE_DISCOUNT_APPROVE)
    public R<RemainRefundVO> approve(@PathVariable String uuid,
                                     @Valid @RequestBody RemainRefundDecisionDTO request) {
        return R.success(queryService.toView(refundService.approve(uuid, request)));
    }

    @PostMapping("/{uuid}/pay")
    @RequirePermission(Permissions.SETTLE_RECEIVE)
    public R<RemainRefundVO> pay(@PathVariable String uuid,
                                 @Valid @RequestBody RemainRefundDecisionDTO request) {
        return R.success(queryService.toView(refundService.pay(uuid, request)));
    }

    @PostMapping("/{uuid}/cancel")
    @RequirePermission(Permissions.REMAIN_ROLLBACK)
    public R<RemainRefundVO> cancel(@PathVariable String uuid,
                                    @Valid @RequestBody RemainRefundDecisionDTO request) {
        return R.success(queryService.toView(refundService.cancel(uuid, request)));
    }
}
