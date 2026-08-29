package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.FinishCustomerRevisionPreviewVO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionRequestDTO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionSummaryVO;
import com.paper.mes.processorder.dto.FinishCustomerSpecVO;
import com.paper.mes.processorder.dto.PrintResultVO;
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
    private final ProcessOrderCustomerSpecReissueService reissueService;
    private final OperationLogService operationLogService;

    @Transactional(rollbackFor = Exception.class)
    public FinishCustomerRevisionSummaryVO publish(String orderUuid,
                                                    FinishCustomerRevisionRequestDTO request) {
        lockService.lockProcessOrders(List.of(orderUuid));
        String requestHash = FinishCustomerRevisionFingerprint.of(request);
        FinishCustomerRevision replay = reader.findByRequest(orderUuid, request.getRequestId());
        if (replay != null) return verifyReplay(replay, requestHash);
        FinishCustomerRevisionPreviewVO preview = previewService.preview(orderUuid, request);
        if (preview.isHasErrors()) throw new BusinessException("客户规格预览存在错误，不能发布");
        previewService.requirePublishAllowed(orderUuid, preview);
        boolean preparedReissue = reissueService.prepareIfRequired(
                orderUuid, request, preview.isReissueRequired());
        FinishCustomerRevision revision = writer.write(
                new FinishCustomerRevisionWriteCommand(orderUuid, requestHash, preview, request));
        recordOperation(preview, revision, request);
        PrintResultVO reissue = reissueService.issuePreparedVersion(orderUuid, preparedReissue);
        FinishCustomerRevisionSummaryVO summary = reader.summary(revision);
        summary.setReissued(preparedReissue);
        if (reissue != null) summary.setIssueVersion(reissue.getIssueVersion());
        return summary;
    }

    private FinishCustomerRevisionSummaryVO verifyReplay(
            FinishCustomerRevision replay, String requestHash) {
        if (!requestHash.equals(replay.getRequestHash())) {
            throw new BusinessException("同一请求号不能用于不同的客户规格内容");
        }
        return reader.summary(replay);
    }

    private void recordOperation(FinishCustomerRevisionPreviewVO preview,
                                 FinishCustomerRevision revision,
                                 FinishCustomerRevisionRequestDTO request) {
        operationLogService.record(OperationLogService.BIZ_TYPE_ORDER,
                preview.getOrderUuid(), preview.getOrderNo(),
                OperationLogService.ACTION_CUSTOMER_SPEC_REVISION, null,
                "客户规格V" + revision.getRevisionNo() + "，影响" + revision.getItemCount()
                        + "件：" + request.getReason().trim());
        String auditContext = "客户规格V" + revision.getRevisionNo()
                + "；请求编号 " + request.getRequestId().trim();
        preview.getItems().forEach(item -> recordChangedFields(preview, item, auditContext));
    }

    private void recordChangedFields(FinishCustomerRevisionPreviewVO preview,
                                     FinishCustomerSpecVO item, String auditContext) {
        recordFieldIfChanged(preview, item, "客户品名",
                item.getPreviousCustomerPaperName(), item.getCustomerPaperName(), auditContext);
        recordFieldIfChanged(preview, item, "客户克重",
                item.getPreviousCustomerGramWeight(), item.getCustomerGramWeight(), auditContext);
        recordFieldIfChanged(preview, item, "客户门幅",
                item.getPreviousCustomerFinishWidth(), item.getCustomerFinishWidth(), auditContext);
        recordFieldIfChanged(preview, item, "客户显示重量",
                item.getPreviousCustomerDisplayWeight(), item.getCustomerDisplayWeight(), auditContext);
    }

    private void recordFieldIfChanged(FinishCustomerRevisionPreviewVO preview,
                                      FinishCustomerSpecVO item, String fieldName,
                                      Object oldValue, Object newValue, String auditContext) {
        if (java.util.Objects.equals(oldValue, newValue)) return;
        operationLogService.recordField(OperationLogService.BIZ_TYPE_ORDER,
                preview.getOrderUuid(), preview.getOrderNo(),
                item.getFinishRollNo() + " " + fieldName,
                text(oldValue), text(newValue), null, auditContext);
    }

    private String text(Object value) {
        return value == null ? null : value.toString();
    }
}
