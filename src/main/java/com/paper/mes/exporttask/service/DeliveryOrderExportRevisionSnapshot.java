package com.paper.mes.exporttask.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
import com.paper.mes.delivery.dto.DeliveryCustomerSpecVO;
import com.paper.mes.delivery.entity.DeliveryCustomerRevision;
import com.paper.mes.delivery.service.DeliveryCustomerRevisionPreviewService;
import com.paper.mes.delivery.service.DeliveryCustomerRevisionReader;
import com.paper.mes.exporttask.dto.DeliveryOrderExportTaskPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryOrderExportRevisionSnapshot {
    private static final int DELIVERY_STATUS_VOID = 3;
    private static final String VOID_FINGERPRINT = "VOID";

    private final DeliveryCustomerRevisionReader revisionReader;
    private final DeliveryCustomerRevisionPreviewService previewService;
    private final ObjectMapper objectMapper;

    public String capture(String orderUuid, Integer expectedRevisionNo, Integer deliveryStatus) {
        DeliveryCustomerRevisionPreviewVO preview = Integer.valueOf(DELIVERY_STATUS_VOID).equals(deliveryStatus)
                ? null : previewService.current(orderUuid);
        int revisionNo = preview == null ? currentRevisionNo(orderUuid) : revisionNo(preview);
        if (expectedRevisionNo != null && expectedRevisionNo != revisionNo) {
            throw new BusinessException("出库客户口径版本已变化，请刷新后重新导出");
        }
        return serialize(new DeliveryOrderExportTaskPayload(
                DeliveryOrderExportTaskPayload.CURRENT_SCHEMA_VERSION, revisionNo,
                preview == null ? VOID_FINGERPRINT : fingerprint(preview)));
    }

    public DeliveryCustomerRevisionPreviewVO verifyCurrentAndRead(String orderUuid, String value) {
        DeliveryCustomerRevisionPreviewVO preview = previewService.current(orderUuid);
        if (value == null || value.isBlank()) return preview;
        DeliveryOrderExportTaskPayload payload = parse(value);
        verifyRevision(revisionNo(preview), payload.customerRevisionNo());
        if (payload.schemaVersion() == DeliveryOrderExportTaskPayload.CURRENT_SCHEMA_VERSION
                && !fingerprint(preview).equals(payload.documentFingerprint())) {
            throw new BusinessException("出库客户单据内容已变化，请重新创建导出任务");
        }
        return preview;
    }

    public void verifyCurrent(String orderUuid, String value) {
        verifyCurrentAndRead(orderUuid, value);
    }

    public void verifyVoided(String value) {
        if (value == null || value.isBlank()) return;
        DeliveryOrderExportTaskPayload payload = parse(value);
        if (payload.schemaVersion() == DeliveryOrderExportTaskPayload.CURRENT_SCHEMA_VERSION
                && !VOID_FINGERPRINT.equals(payload.documentFingerprint())) {
            throw new BusinessException("出库单状态已变化，请重新创建导出任务");
        }
    }

    private DeliveryOrderExportTaskPayload parse(String value) {
        try {
            DeliveryOrderExportTaskPayload payload = objectMapper.readValue(
                    value, DeliveryOrderExportTaskPayload.class);
            if (payload.schemaVersion() != DeliveryOrderExportTaskPayload.CURRENT_SCHEMA_VERSION
                    && payload.schemaVersion() != DeliveryOrderExportTaskPayload.LEGACY_SCHEMA_VERSION) {
                throw new BusinessException("出库导出任务的客户口径版本格式不受支持");
            }
            return payload;
        } catch (JsonProcessingException exception) {
            throw new BusinessException("出库导出任务的客户口径版本快照损坏");
        }
    }

    private int currentRevisionNo(String orderUuid) {
        DeliveryCustomerRevision revision = revisionReader.latestRevision(orderUuid);
        return revision == null || revision.getRevisionNo() == null ? 0 : revision.getRevisionNo();
    }

    private int revisionNo(DeliveryCustomerRevisionPreviewVO preview) {
        return preview.getCurrentRevisionNo() == null ? 0 : preview.getCurrentRevisionNo();
    }

    private void verifyRevision(int currentRevisionNo, int expectedRevisionNo) {
        if (currentRevisionNo != expectedRevisionNo) {
            throw new BusinessException("出库客户口径版本已变化，请重新创建导出任务");
        }
    }

    private String serialize(DeliveryOrderExportTaskPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("出库客户口径版本无法保存");
        }
    }

    private String fingerprint(DeliveryCustomerRevisionPreviewVO preview) {
        try {
            FingerprintDocument document = new FingerprintDocument(
                    preview.getDeliveryUuid(), preview.getDeliveryVersion(), preview.getDeliveryStatus(),
                    revisionNo(preview), fingerprintItems(preview.getItems()));
            return sha256(objectMapper.writeValueAsString(document));
        } catch (JsonProcessingException exception) {
            throw new BusinessException("出库客户单据指纹无法生成");
        }
    }

    private List<FingerprintItem> fingerprintItems(List<DeliveryCustomerSpecVO> items) {
        return (items == null ? List.<DeliveryCustomerSpecVO>of() : items).stream()
                .sorted(Comparator.comparing(DeliveryCustomerSpecVO::getDeliveryDetailUuid,
                        Comparator.nullsLast(String::compareTo)))
                .map(this::fingerprintItem)
                .toList();
    }

    private FingerprintItem fingerprintItem(DeliveryCustomerSpecVO item) {
        return new FingerprintItem(item.getDeliveryDetailUuid(), item.getDetailVersion(), item.getFinishUuid(),
                item.getFinishRollNo(), item.getOrderUuid(), item.getOrderNo(), item.getPhysicalPaperName(),
                item.getPhysicalGramWeight(), item.getPhysicalFinishWidth(), text(item.getPhysicalDeliveryWeight()),
                item.getCustomerPaperName(), item.getCustomerGramWeight(), item.getCustomerFinishWidth(),
                text(item.getCustomerDisplayWeight()), item.getCustomerRemark(), item.getCalculationMode(),
                item.getValueSource(), item.isSpecificationChanged(), item.isWeightChanged(), item.isValid(),
                item.getError());
    }

    private String text(java.math.BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private record FingerprintDocument(String deliveryUuid, Integer deliveryVersion,
                                       Integer deliveryStatus, int currentRevisionNo,
                                       List<FingerprintItem> items) { }

    private record FingerprintItem(String deliveryDetailUuid, Integer detailVersion,
                                   String finishUuid, String finishRollNo, String orderUuid, String orderNo,
                                   String physicalPaperName, Integer physicalGramWeight,
                                   Integer physicalFinishWidth, String physicalDeliveryWeight,
                                   String customerPaperName, Integer customerGramWeight,
                                   Integer customerFinishWidth, String customerDisplayWeight,
                                   String customerRemark, String calculationMode, String valueSource,
                                   boolean specificationChanged, boolean weightChanged,
                                   boolean valid, String error) { }
}
