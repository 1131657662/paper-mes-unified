package com.paper.mes.delivery.controller;

import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.delivery.dto.AvailableFinishVO;
import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public R<PageResult<DeliveryOrder>> page(DeliveryQuery query) {
        return R.success(deliveryService.page(query));
    }

    @GetMapping("/available")
    public R<List<AvailableFinishVO>> available(@RequestParam String customerUuid) {
        return R.success(deliveryService.listAvailable(customerUuid));
    }

    @PostMapping
    public R<String> create(@Valid @RequestBody DeliveryCreateDTO dto) {
        return R.success(deliveryService.create(dto));
    }

    @GetMapping("/{uuid}")
    public R<DeliveryDetailVO> detail(@PathVariable String uuid) {
        return R.success(deliveryService.getDetail(uuid));
    }

    @PostMapping("/{uuid}/confirm")
    public R<Void> confirm(@PathVariable String uuid,
                           @RequestBody(required = false) DeliveryConfirmDTO dto) {
        deliveryService.confirm(uuid, dto == null ? new DeliveryConfirmDTO() : dto);
        return R.success();
    }
}
