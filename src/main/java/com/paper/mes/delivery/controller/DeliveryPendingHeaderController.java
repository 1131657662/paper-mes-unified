package com.paper.mes.delivery.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.delivery.dto.DeliveryPendingUpdateDTO;
import com.paper.mes.delivery.service.DeliveryPendingHeaderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery-orders")
@RequiredArgsConstructor
public class DeliveryPendingHeaderController {

    private final DeliveryPendingHeaderService pendingHeaderService;

    @PutMapping("/{uuid}")
    @RequirePermission(Permissions.DELIVERY_MANAGE)
    public R<Void> update(@PathVariable String uuid,
                          @Valid @RequestBody DeliveryPendingUpdateDTO dto) {
        pendingHeaderService.update(uuid, dto);
        return R.success();
    }
}
