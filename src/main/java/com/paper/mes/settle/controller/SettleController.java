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
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.service.SettleService;
import jakarta.servlet.http.HttpServletResponse;
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

    @GetMapping
    @RequirePermission(Permissions.SETTLE_VIEW)
    public R<PageResult<SettleOrder>> page(SettleQuery query) {
        return R.success(settleService.page(query));
    }

    @GetMapping("/candidates")
    @RequirePermission(Permissions.SETTLE_MANAGE)
    public R<List<SettleCandidateVO>> candidates(SettleCandidateQuery query) {
        return R.success(settleService.listCandidates(query));
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

    @GetMapping("/{uuid}/export")
    @RequirePermission(Permissions.SETTLE_VIEW)
    public void export(@PathVariable String uuid, HttpServletResponse response) {
        settleService.exportDetail(uuid, response);
    }

    @PostMapping("/{uuid}/receive")
    @RequirePermission(Permissions.SETTLE_RECEIVE)
    public R<Void> receive(@PathVariable String uuid, @Valid @RequestBody ReceiveDTO dto) {
        settleService.receive(uuid, dto);
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
