package com.paper.mes.delivery.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.delivery.dto.DeliveryInventoryAvailabilityRequest;
import com.paper.mes.delivery.dto.DeliveryInventoryAvailabilityVO;
import com.paper.mes.delivery.dto.DeliveryInventoryCustomerQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryCustomerVO;
import com.paper.mes.delivery.dto.DeliveryInventoryFilter;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.dto.DeliveryInventoryOrderGroupVO;
import com.paper.mes.delivery.dto.DeliveryInventorySummaryVO;
import com.paper.mes.delivery.dto.DeliveryInventoryUnassignedOrderVO;
import com.paper.mes.delivery.dto.DeliveryInventoryUnassignedQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryWarehouseRepairRequest;
import com.paper.mes.delivery.dto.DeliveryInventoryWarehouseRepairResultVO;
import com.paper.mes.delivery.dto.AvailableFinishQuery;
import com.paper.mes.delivery.dto.AvailableFinishPageVO;
import com.paper.mes.delivery.dto.AvailableFinishVO;
import com.paper.mes.delivery.service.AvailableFinishPageService;
import com.paper.mes.delivery.service.DeliveryInventoryService;
import com.paper.mes.delivery.service.DeliveryInventoryOrderGroupPageService;
import com.paper.mes.delivery.service.DeliveryInventoryWarehouseRepairService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery-orders/inventory")
@RequiredArgsConstructor
public class DeliveryInventoryController {

    private final DeliveryInventoryService inventoryService;
    private final AvailableFinishPageService availableFinishPageService;
    private final DeliveryInventoryOrderGroupPageService orderGroupPageService;
    private final DeliveryInventoryWarehouseRepairService warehouseRepairService;

    @GetMapping("/available-finishes")
    @RequirePermission(Permissions.DELIVERY_MANAGE)
    public R<AvailableFinishPageVO> availableFinishes(@Valid AvailableFinishQuery query) {
        return R.success(availableFinishPageService.page(query));
    }

    @GetMapping("/summary")
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<DeliveryInventorySummaryVO> summary(@Valid DeliveryInventoryFilter filter) {
        return R.success(inventoryService.summary(filter));
    }

    @GetMapping("/customers")
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<PageResult<DeliveryInventoryCustomerVO>> customers(
            @Valid DeliveryInventoryCustomerQuery query) {
        return R.success(inventoryService.pageCustomers(query));
    }

    @GetMapping("/finishes")
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<PageResult<DeliveryInventoryFinishVO>> finishes(
            @Valid DeliveryInventoryFinishQuery query) {
        return R.success(inventoryService.pageFinishes(query));
    }

    @GetMapping("/order-groups")
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<PageResult<DeliveryInventoryOrderGroupVO>> orderGroups(
            @Valid DeliveryInventoryFinishQuery query) {
        return R.success(orderGroupPageService.page(query));
    }

    @PostMapping("/validate-availability")
    @RequirePermission(Permissions.DELIVERY_MANAGE)
    public R<DeliveryInventoryAvailabilityVO> validateAvailability(
            @Valid @RequestBody DeliveryInventoryAvailabilityRequest request) {
        return R.success(inventoryService.validateAvailability(request));
    }

    @GetMapping("/unassigned")
    @RequirePermission(Permissions.DELIVERY_MANAGE)
    public R<PageResult<DeliveryInventoryUnassignedOrderVO>> unassigned(
            @Valid DeliveryInventoryUnassignedQuery query) {
        return R.success(warehouseRepairService.page(query));
    }

    @PostMapping("/assign-warehouse")
    @RequirePermission(Permissions.DELIVERY_MANAGE)
    public R<DeliveryInventoryWarehouseRepairResultVO> assignWarehouse(
            @Valid @RequestBody DeliveryInventoryWarehouseRepairRequest request) {
        return R.success(warehouseRepairService.assign(request));
    }
}
