package com.paper.mes.exporttask.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryCustomerRevisionPreviewVO;
import com.paper.mes.delivery.dto.DeliveryCustomerSpecVO;
import com.paper.mes.delivery.entity.DeliveryCustomerRevision;
import com.paper.mes.delivery.service.DeliveryCustomerRevisionPreviewService;
import com.paper.mes.delivery.service.DeliveryCustomerRevisionReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeliveryOrderExportRevisionSnapshotTest {
    private DeliveryCustomerRevisionReader revisionReader;
    private DeliveryCustomerRevisionPreviewService previewService;
    private ObjectMapper objectMapper;
    private DeliveryOrderExportRevisionSnapshot snapshot;

    @BeforeEach
    void setUp() {
        revisionReader = mock(DeliveryCustomerRevisionReader.class);
        previewService = mock(DeliveryCustomerRevisionPreviewService.class);
        objectMapper = new ObjectMapper();
        snapshot = new DeliveryOrderExportRevisionSnapshot(revisionReader, previewService, objectMapper);
    }

    @Test
    void capture_whenExpectedRevisionMatches_persistsCurrentRevision() throws Exception {
        when(previewService.current("delivery-1")).thenReturn(preview("paper-a"));

        String payload = snapshot.capture("delivery-1", 2, 1);

        assertThat(objectMapper.readTree(payload).get("customerRevisionNo").asInt()).isEqualTo(2);
        assertThat(objectMapper.readTree(payload).get("documentFingerprint").asText()).hasSize(64);
    }

    @Test
    void capture_whenExpectedRevisionIsStale_rejectsTaskCreation() {
        when(previewService.current("delivery-1")).thenReturn(preview("paper-a", 3));

        assertThatThrownBy(() -> snapshot.capture("delivery-1", 2, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("版本已变化");
    }

    @Test
    void verifyCurrent_whenCustomerContentChanges_rejectsStalePayload() {
        when(previewService.current("delivery-1"))
                .thenReturn(preview("paper-a"), preview("paper-b"));
        String payload = snapshot.capture("delivery-1", 2, 1);

        assertThatThrownBy(() -> snapshot.verifyCurrent("delivery-1", payload))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内容已变化");
    }

    @Test
    void verifyCurrent_whenSchemaIsUnknown_rejectsPayload() {
        when(previewService.current("delivery-1")).thenReturn(preview("paper-a"));

        assertThatThrownBy(() -> snapshot.verifyCurrent("delivery-1",
                "{\"schemaVersion\":99,\"customerRevisionNo\":2,\"documentFingerprint\":\"x\"}"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("格式不受支持");
    }

    @Test
    void verifyVoided_whenCurrentPayloadHasNonVoidFingerprint_rejectsPayload() {
        assertThatThrownBy(() -> snapshot.verifyVoided(
                "{\"schemaVersion\":2,\"customerRevisionNo\":0,\"documentFingerprint\":\"x\"}"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态已变化");
    }

    private DeliveryCustomerRevisionPreviewVO preview(String paperName) {
        return preview(paperName, 2);
    }

    private DeliveryCustomerRevisionPreviewVO preview(String paperName, int revisionNo) {
        DeliveryCustomerRevisionPreviewVO preview = new DeliveryCustomerRevisionPreviewVO();
        preview.setDeliveryUuid("delivery-1");
        preview.setDeliveryVersion(1);
        preview.setDeliveryStatus(1);
        preview.setCurrentRevisionNo(revisionNo);
        DeliveryCustomerSpecVO item = new DeliveryCustomerSpecVO();
        item.setDeliveryDetailUuid("detail-1");
        item.setDetailVersion(1);
        item.setCustomerPaperName(paperName);
        item.setValid(true);
        preview.setItems(java.util.List.of(item));
        return preview;
    }

    private DeliveryCustomerRevision revision(int revisionNo) {
        DeliveryCustomerRevision revision = new DeliveryCustomerRevision();
        revision.setRevisionNo(revisionNo);
        return revision;
    }
}
