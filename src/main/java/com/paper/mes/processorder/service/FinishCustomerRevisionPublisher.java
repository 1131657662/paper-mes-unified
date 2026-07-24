package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.FinishCustomerRevisionPreviewVO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionRequestDTO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionSummaryVO;
import com.paper.mes.processorder.entity.FinishCustomerRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinishCustomerRevisionPublisher {

    private final BusinessLockService lockService;
    private final FinishCustomerRevisionPreviewService previewService;
    private final FinishCustomerRevisionReader reader;
    private final FinishCustomerRevisionWriter writer;
    private final OperationLogService operationLogService;

    @Transactional(rollbackFor = Exception.class)
    public FinishCustomerRevisionSummaryVO publish(String orderUuid,
                                                    FinishCustomerRevisionRequestDTO request) {
        lockService.lockProcessOrders(List.of(orderUuid));
        String requestHash = FinishCustomerRevisionFingerprint.of(request);
        FinishCustomerRevision replay = reader.findByRequest(orderUuid, request.getRequestId());
        if (replay != null) return verifyReplay(replay, requestHash);
        FinishCustomerRevisionPreviewVO preview = previewService.preview(orderUuid, request);
        if (preview.isHasErrors()) throw new BusinessException("客户口径预览存在错误，不能发布");
        FinishCustomerRevision revision = writer.write(
                new FinishCustomerRevisionWriteCommand(orderUuid, requestHash, preview, request));
        recordOperation(preview, revision, request);
        return reader.summary(revision);
    }

    private FinishCustomerRevisionSummaryVO verifyReplay(
            FinishCustomerRevision replay, String requestHash) {
        if (!requestHash.equals(replay.getRequestHash())) {
            throw new BusinessException("同一请求号不能用于不同的客户口径内容");
        }
        return reader.summary(replay);
    }

    private void recordOperation(FinishCustomerRevisionPreviewVO preview,
                                 FinishCustomerRevision revision,
                                 FinishCustomerRevisionRequestDTO request) {
        operationLogService.record(OperationLogService.BIZ_TYPE_ORDER,
                preview.getOrderUuid(), preview.getOrderNo(),
                OperationLogService.ACTION_CUSTOMER_SPEC_REVISION, null,
                "客户口径V" + revision.getRevisionNo() + "，影响" + revision.getItemCount()
                        + "件：" + request.getReason().trim());
    }
}
