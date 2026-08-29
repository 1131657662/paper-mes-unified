package com.paper.mes.integration;

import com.paper.mes.customerdisplay.formula.CustomerWeightCalculationMode;
import com.paper.mes.customerdisplay.formula.CustomerWeightZeroPolicy;
import com.paper.mes.processorder.dto.FinishCustomerRevisionPreviewVO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionRequestDTO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionSummaryVO;
import com.paper.mes.processorder.dto.FinishCustomerSpecItemDTO;
import com.paper.mes.processorder.dto.FinishCustomerSpecVO;
import com.paper.mes.processorder.dto.PrintViewVersion;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.oplog.entity.OperationLog;
import com.paper.mes.oplog.mapper.OperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.processorder.service.FinishCustomerRevisionPreviewService;
import com.paper.mes.processorder.service.FinishCustomerRevisionPublisher;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.service.ProcessOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class ProcessOrderCustomerSpecReissueBusinessFlowIT extends AuthenticatedBusinessFlowIT {

    @Autowired private RepresentativeOrderFixture fixture;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private ProcessOrderMapper orderMapper;
    @Autowired private OperationLogMapper operationLogMapper;
    @Autowired private FinishCustomerRevisionPreviewService previewService;
    @Autowired private FinishCustomerRevisionPublisher publisher;

    @Test
    void issuedOrder_whenPrintedCustomerSpecificationChanges_keepsV1AndAutomaticallyIssuesV2() {
        RepresentativeOrderFixture.Scenario scenario = fixture.createStandardSaw();
        String orderUuid = scenario.orderUuid();
        processOrderService.issue(orderUuid);
        String v1Snapshot = orderMapper.selectById(orderUuid).getSnapPrint();
        FinishCustomerRevisionPreviewVO current = previewService.current(orderUuid);
        String v1CustomerPaperName = processOrderService.getPrintView(orderUuid, PrintViewVersion.ISSUED)
                .getDetail().getFinishRolls().getFirst().getCustomerPaperName();

        FinishCustomerRevisionSummaryVO published = publisher.publish(orderUuid,
                request(current.getItems().getFirst(), current.getOrderVersion()));

        String v2CustomerPaperName = processOrderService.getPrintView(orderUuid, PrintViewVersion.ISSUED)
                .getDetail().getFinishRolls().getFirst().getCustomerPaperName();
        String historicalV1CustomerPaperName = processOrderService.getHistoricalIssuePrintView(orderUuid, 1)
                .getDetail().getFinishRolls().getFirst().getCustomerPaperName();
        var persisted = orderMapper.selectById(orderUuid);
        assertThat(published.isReissued()).isTrue();
        assertThat(published.getIssueVersion()).isEqualTo(2);
        assertThat(persisted.getOrderStatus()).isEqualTo(2);
        assertThat(persisted.getSnapPrint()).isNotEqualTo(v1Snapshot);
        assertThat(v2CustomerPaperName).isEqualTo("更新后的客户品名");
        assertThat(historicalV1CustomerPaperName).isEqualTo(v1CustomerPaperName);
        assertCompactReissueAudit(orderUuid);
    }

    private void assertCompactReissueAudit(String orderUuid) {
        List<OperationLog> logs = operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getBizUuid, orderUuid));
        assertThat(logs).anySatisfy(log -> {
            assertThat(log.getActionType()).isEqualTo(OperationLogService.ACTION_REISSUE);
            assertThat(log.getRemark()).contains("下发版本 V1 -> V2", "字节", "SHA-256=");
            assertThat(log.getOldValue()).isNull();
            assertThat(log.getNewValue()).isNull();
        });
        assertThat(logs).anySatisfy(log -> {
            assertThat(log.getFieldName()).contains("客户品名");
            assertThat(log.getOldValue()).isNotBlank();
            assertThat(log.getNewValue()).isEqualTo("更新后的客户品名");
            assertThat(log.getRemark()).contains("客户规格V1", "请求编号");
        });
    }

    private FinishCustomerRevisionRequestDTO request(FinishCustomerSpecVO row, Integer orderVersion) {
        FinishCustomerSpecItemDTO item = new FinishCustomerSpecItemDTO();
        item.setFinishUuid(row.getFinishUuid());
        item.setExpectedVersion(row.getFinishVersion());
        item.setCustomerPaperName("更新后的客户品名");
        item.setCustomerGramWeight(row.getPreviousCustomerGramWeight());
        item.setCustomerFinishWidth(row.getPreviousCustomerFinishWidth());
        item.setCalculationMode(CustomerWeightCalculationMode.KEEP);
        item.setRoundingScale(3);
        item.setRoundingMode(RoundingMode.HALF_UP);
        item.setZeroPolicy(CustomerWeightZeroPolicy.SKIP);
        FinishCustomerRevisionRequestDTO request = new FinishCustomerRevisionRequestDTO();
        request.setRequestId("customer-spec-reissue-" + System.nanoTime());
        request.setExpectedOrderVersion(orderVersion);
        request.setReason("客户确认更新生产标签");
        request.setItems(List.of(item));
        return request;
    }
}
