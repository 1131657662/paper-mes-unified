package com.paper.mes.delivery.controller;

import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.delivery.dto.AvailableFinishVO;
import com.paper.mes.delivery.dto.DeliveryAppendItemsDTO;
import com.paper.mes.delivery.dto.DeliveryBatchConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCancelDTO;
import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.dto.DeliveryRollbackDTO;
import com.paper.mes.delivery.dto.DeliveryListSummaryVO;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.service.DeliveryListSummaryService;
import com.paper.mes.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-orders")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final DeliveryListSummaryService deliveryListSummaryService;

    @GetMapping
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<PageResult<DeliveryOrder>> page(@Valid DeliveryQuery query) {
        return R.success(deliveryService.page(query));
    }

    @GetMapping("/summary")
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<DeliveryListSummaryVO> summary(@Valid DeliveryQuery query) {
        return R.success(deliveryListSummaryService.summarize(query));
    }

    @GetMapping("/available")
    @RequirePermission(Permissions.DELIVERY_MANAGE)
    public R<List<AvailableFinishVO>> available(@RequestParam String customerUuid,
                                                @RequestParam(required = false) String warehouseUuid) {
        return R.success(deliveryService.listAvailable(customerUuid, warehouseUuid));
    }

    @PostMapping
    @RequirePermission(Permissions.DELIVERY_MANAGE)
    public R<String> create(@Valid @RequestBody DeliveryCreateDTO dto) {
        return R.success(deliveryService.create(dto));
    }

    @GetMapping("/{uuid}")
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<DeliveryDetailVO> detail(@PathVariable String uuid) {
        return R.success(deliveryService.getDetail(uuid));
    }

    @PostMapping("/{uuid}/confirm")
    @RequirePermission({Permissions.DELIVERY_MANAGE, Permissions.DELIVERY_RELEASE})
    public R<Void> confirm(@PathVariable String uuid,
                           @Valid @RequestBody(required = false) DeliveryConfirmDTO dto) {
        deliveryService.confirm(uuid, dto == null ? new DeliveryConfirmDTO() : dto);
        return R.success();
    }

    @PostMapping("/batch-confirm")
    @RequirePermission({Permissions.DELIVERY_MANAGE, Permissions.DELIVERY_RELEASE})
    public R<Void> confirmBatch(@Valid @RequestBody DeliveryBatchConfirmDTO dto) {
        deliveryService.confirmBatch(dto);
        return R.success();
    }

    @PostMapping("/{uuid}/details")
    @RequirePermission(Permissions.DELIVERY_MANAGE)
    public R<Void> appendDetails(@PathVariable String uuid,
                                 @Valid @RequestBody DeliveryAppendItemsDTO dto) {
        deliveryService.appendDetails(uuid, dto);
        return R.success();
    }

    @PostMapping("/{uuid}/rollback")
    @RequirePermission(Permissions.DELIVERY_MANAGE)
    public R<Void> rollback(@PathVariable String uuid,
                            @Valid @RequestBody DeliveryRollbackDTO dto) {
        deliveryService.rollback(uuid, dto);
        return R.success();
    }

    @DeleteMapping("/{uuid}/details/{detailUuid}")
    @RequirePermission(Permissions.DELIVERY_MANAGE)
    public R<Void> removeDetail(@PathVariable String uuid,
                                @PathVariable String detailUuid) {
        deliveryService.removeDetail(uuid, detailUuid);
        return R.success();
    }

    @PostMapping("/{uuid}/cancel")
    @RequirePermission(Permissions.DELIVERY_MANAGE)
    public R<Void> cancelPending(@PathVariable String uuid,
                                 @Valid @RequestBody DeliveryCancelDTO dto) {
        deliveryService.cancelPending(uuid, dto);
        return R.success();
    }
}
