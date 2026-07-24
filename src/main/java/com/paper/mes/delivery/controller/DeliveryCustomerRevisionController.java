package com.paper.mes.delivery.controller;

import com.paper.mes.auth.permission.*;
import com.paper.mes.common.R;
import com.paper.mes.delivery.dto.*;
import com.paper.mes.delivery.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-orders/{deliveryUuid}/customer-specs")
@RequiredArgsConstructor
public class DeliveryCustomerRevisionController {

    private final DeliveryCustomerRevisionPreviewService previewService;
    private final DeliveryCustomerRevisionReader reader;
    private final DeliveryCustomerRevisionPublisher publisher;

    @GetMapping
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<DeliveryCustomerRevisionPreviewVO> current(@PathVariable String deliveryUuid) {
        return R.success(previewService.current(deliveryUuid));
    }

    @GetMapping("/revisions")
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<List<DeliveryCustomerRevisionSummaryVO>> history(@PathVariable String deliveryUuid) {
        return R.success(reader.history(deliveryUuid));
    }

    @GetMapping("/revisions/{revisionUuid}")
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<DeliveryCustomerRevisionDetailVO> detail(@PathVariable String deliveryUuid,
                                                      @PathVariable String revisionUuid) {
        return R.success(reader.detail(deliveryUuid, revisionUuid));
    }

    @PostMapping("/preview")
    @RequirePermission(Permissions.DELIVERY_VIEW)
    public R<DeliveryCustomerRevisionPreviewVO> preview(
            @PathVariable String deliveryUuid,
            @Valid @RequestBody DeliveryCustomerRevisionRequestDTO request) {
        return R.success(previewService.preview(deliveryUuid, request));
    }

    @PostMapping("/revisions")
    @RequirePermission(Permissions.DELIVERY_MANAGE)
    public R<DeliveryCustomerRevisionSummaryVO> publish(
            @PathVariable String deliveryUuid,
            @Valid @RequestBody DeliveryCustomerRevisionRequestDTO request) {
        return R.success(publisher.publish(deliveryUuid, request));
    }
}
