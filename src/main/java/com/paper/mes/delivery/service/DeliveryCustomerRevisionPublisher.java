package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.dto.*;
import com.paper.mes.delivery.entity.DeliveryCustomerRevision;
import com.paper.mes.oplog.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryCustomerRevisionPublisher {

    private final BusinessLockService lockService;
    private final DeliveryCustomerRevisionPreviewService previewService;
    private final DeliveryCustomerRevisionReader reader;
    private final DeliveryCustomerRevisionWriter writer;
    private final OperationLogService operationLogService;

    @Transactional(rollbackFor = Exception.class)
    public DeliveryCustomerRevisionSummaryVO publish(
            String deliveryUuid, DeliveryCustomerRevisionRequestDTO request) {
        lockService.lockDeliveryOrder(deliveryUuid);
        String requestHash = DeliveryCustomerRevisionFingerprint.of(request);
        DeliveryCustomerRevision replay = reader.findByRequest(deliveryUuid, request.getRequestId());
        if (replay != null) return verifyReplay(replay, requestHash);
        DeliveryCustomerRevisionPreviewVO preview = previewService.preview(deliveryUuid, request);
        if (preview.isHasErrors()) throw new BusinessException("客户更正版预览存在错误，不能发布");
        DeliveryCustomerRevision revision = writer.write(
                new DeliveryCustomerRevisionWriteCommand(deliveryUuid, requestHash, preview, request));
        recordOperation(preview, revision, request);
        return reader.summary(revision);
    }

    private DeliveryCustomerRevisionSummaryVO verifyReplay(
            DeliveryCustomerRevision replay, String requestHash) {
        if (!requestHash.equals(replay.getRequestHash())) {
            throw new BusinessException("同一请求号不能用于不同的客户更正版内容");
        }
        return reader.summary(replay);
    }

    private void recordOperation(
            DeliveryCustomerRevisionPreviewVO preview,
            DeliveryCustomerRevision revision,
            DeliveryCustomerRevisionRequestDTO request) {
        operationLogService.record(OperationLogService.BIZ_TYPE_DELIVERY,
                preview.getDeliveryUuid(), preview.getDeliveryNo(),
                OperationLogService.ACTION_CUSTOMER_SPEC_REVISION, null,
                "客户更正版V" + revision.getRevisionNo() + "，影响" + revision.getItemCount()
                        + "件：" + request.getReason().trim());
    }
}
