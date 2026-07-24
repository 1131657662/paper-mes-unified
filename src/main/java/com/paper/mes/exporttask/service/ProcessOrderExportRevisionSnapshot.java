package com.paper.mes.exporttask.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.exporttask.dto.ProcessOrderExportTaskPayload;
import com.paper.mes.processorder.entity.FinishCustomerRevision;
import com.paper.mes.processorder.service.FinishCustomerRevisionReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessOrderExportRevisionSnapshot {
    private final FinishCustomerRevisionReader revisionReader;
    private final ObjectMapper objectMapper;

    public String capture(String orderUuid, Integer expectedRevisionNo) {
        int currentRevisionNo = currentRevisionNo(orderUuid);
        if (expectedRevisionNo != null && expectedRevisionNo != currentRevisionNo) {
            throw new BusinessException("加工单客户口径版本已变化，请刷新后重新导出");
        }
        return serialize(new ProcessOrderExportTaskPayload(
                ProcessOrderExportTaskPayload.CURRENT_SCHEMA_VERSION, currentRevisionNo));
    }

    public void verifyCurrent(String orderUuid, String value) {
        if (value == null || value.isBlank()) return;
        ProcessOrderExportTaskPayload payload = parse(value);
        if (currentRevisionNo(orderUuid) != payload.customerRevisionNo()) {
            throw new BusinessException("加工单客户口径版本已变化，请重新创建导出任务");
        }
    }

    private int currentRevisionNo(String orderUuid) {
        FinishCustomerRevision revision = revisionReader.latestRevision(orderUuid);
        return revision == null || revision.getRevisionNo() == null ? 0 : revision.getRevisionNo();
    }

    private String serialize(ProcessOrderExportTaskPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("加工单客户口径版本无法保存");
        }
    }

    private ProcessOrderExportTaskPayload parse(String value) {
        try {
            ProcessOrderExportTaskPayload payload = objectMapper.readValue(
                    value, ProcessOrderExportTaskPayload.class);
            if (payload.schemaVersion() != ProcessOrderExportTaskPayload.CURRENT_SCHEMA_VERSION) {
                throw new BusinessException("加工单导出任务的客户口径版本格式不受支持");
            }
            return payload;
        } catch (JsonProcessingException exception) {
            throw new BusinessException("加工单导出任务的客户口径版本快照损坏");
        }
    }
}
