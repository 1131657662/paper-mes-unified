package com.paper.mes.remain.service;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.remain.entity.RemainAdjustment;
import com.paper.mes.remain.entity.RemainAdjustmentLine;
import com.paper.mes.remain.entity.RemainApplication;
import com.paper.mes.remain.entity.RemainApplicationLine;
import com.paper.mes.remain.entity.RemainRegistration;
import com.paper.mes.remain.mapper.RemainApplicationLineMapper;
import com.paper.mes.remain.mapper.RemainApplicationMapper;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.ReceiveRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class RemainAdjustmentApplicationWriter {

    private final RemainApplicationMapper applicationMapper;
    private final RemainApplicationLineMapper applicationLineMapper;
    private final ReceiveRecordMapper receiveRecordMapper;

    RemainApplication write(RemainAdjustment adjustment, RemainRegistration registration,
                            SettleOrder settle, List<RemainAdjustmentLine> lines, String requestId) {
        RemainApplication application = newApplication(adjustment, registration, settle, requestId);
        applicationMapper.insert(application);
        writeLines(application.getUuid(), lines);
        ReceiveRecord receive = newReceive(application, settle);
        receiveRecordMapper.insert(receive);
        application.setReceiveUuid(receive.getUuid());
        ConcurrencyGuard.requireRowUpdated(applicationMapper.updateById(application));
        return application;
    }

    private RemainApplication newApplication(RemainAdjustment adjustment, RemainRegistration registration,
                                              SettleOrder settle, String requestId) {
        RemainApplication result = new RemainApplication();
        result.setUuid(UUID.randomUUID().toString());
        result.setRegistrationUuid(registration.getUuid());
        result.setSettleUuid(settle.getUuid());
        result.setAdjustmentUuid(adjustment.getUuid());
        result.setCustomerUuid(registration.getCustomerUuid());
        result.setApplicationType("APPLY");
        result.setStatus("ACTIVE");
        result.setAmount(adjustment.getAmount());
        result.setWeight(adjustment.getWeight());
        result.setRequestId(requestId.trim());
        result.setRequestHash(RemainRequestFingerprint.application(registration.getUuid(), settle.getUuid(),
                adjustment.getAmount(), adjustment.getWeight()));
        result.setCreateBy(AuthContextHolder.currentDisplayName());
        result.setVersion(1);
        result.setIsDeleted(0);
        return result;
    }

    private void writeLines(String applicationUuid, List<RemainAdjustmentLine> lines) {
        for (RemainAdjustmentLine source : lines) {
            RemainApplicationLine line = new RemainApplicationLine();
            line.setUuid(UUID.randomUUID().toString());
            line.setApplicationUuid(applicationUuid);
            line.setRegistrationLineUuid(source.getRegistrationLineUuid());
            line.setAmount(source.getAmount());
            line.setWeight(source.getWeight());
            applicationLineMapper.insert(line);
        }
    }

    private ReceiveRecord newReceive(RemainApplication application, SettleOrder settle) {
        ReceiveRecord record = new ReceiveRecord();
        record.setUuid(UUID.randomUUID().toString());
        record.setSettleUuid(settle.getUuid());
        record.setRequestId(application.getRequestId());
        record.setRequestHash(application.getRequestHash());
        record.setReceiveDate(LocalDateTime.now());
        record.setReceiveAmount(application.getAmount());
        record.setCashAmount(BigDecimal.ZERO);
        record.setScrapOffsetAmount(application.getAmount());
        record.setDiscountAmount(BigDecimal.ZERO);
        record.setScrapWeight(application.getWeight());
        record.setScrapUnitPrice(application.getAmount().divide(application.getWeight(), 4, RoundingMode.HALF_UP));
        record.setReceiveType(2);
        record.setSourceType("REMAIN_OFFSET");
        record.setRemainApplicationUuid(application.getUuid());
        record.setOperator(AuthContextHolder.currentDisplayName());
        record.setRecordStatus(1);
        record.setIsDeleted(0);
        record.setVersion(1);
        return record;
    }
}
