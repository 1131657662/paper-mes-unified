package com.paper.mes.processorder.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.processorder.dto.FinishCustomerRevisionDetailVO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionPreviewVO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionRequestDTO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionSummaryVO;
import com.paper.mes.processorder.service.FinishCustomerRevisionPreviewService;
import com.paper.mes.processorder.service.FinishCustomerRevisionPublisher;
import com.paper.mes.processorder.service.FinishCustomerRevisionReader;
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
@RequestMapping("/api/process-orders/{orderUuid}/customer-specs")
@RequiredArgsConstructor
public class FinishCustomerRevisionController {

    private final FinishCustomerRevisionPreviewService previewService;
    private final FinishCustomerRevisionReader reader;
    private final FinishCustomerRevisionPublisher publisher;

    @GetMapping
    @RequirePermission(Permissions.ORDER_VIEW)
    public R<FinishCustomerRevisionPreviewVO> current(@PathVariable String orderUuid) {
        return R.success(previewService.current(orderUuid));
    }

    @GetMapping("/revisions")
    @RequirePermission(Permissions.ORDER_VIEW)
    public R<List<FinishCustomerRevisionSummaryVO>> history(@PathVariable String orderUuid) {
        return R.success(reader.history(orderUuid));
    }

    @GetMapping("/revisions/{revisionUuid}")
    @RequirePermission(Permissions.ORDER_VIEW)
    public R<FinishCustomerRevisionDetailVO> detail(@PathVariable String orderUuid,
                                                    @PathVariable String revisionUuid) {
        return R.success(reader.detail(orderUuid, revisionUuid));
    }

    @PostMapping("/preview")
    @RequirePermission(Permissions.ORDER_VIEW)
    public R<FinishCustomerRevisionPreviewVO> preview(
            @PathVariable String orderUuid,
            @Valid @RequestBody FinishCustomerRevisionRequestDTO request) {
        return R.success(previewService.preview(orderUuid, request));
    }

    @PostMapping("/revisions")
    @RequirePermission(Permissions.ORDER_MANAGE)
    public R<FinishCustomerRevisionSummaryVO> publish(
            @PathVariable String orderUuid,
            @Valid @RequestBody FinishCustomerRevisionRequestDTO request) {
        return R.success(publisher.publish(orderUuid, request));
    }
}
