package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.FinishCustomerRevisionRequestDTO;
import com.paper.mes.processorder.dto.PrintResultVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Creates Vn+1 atomically when an issued customer specification changes production instructions. */
@Service
@RequiredArgsConstructor
public class ProcessOrderCustomerSpecReissueService {

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_PROCESSING = 2;
    private static final int FINISH_STATUS_OUT = 3;

    private final ProcessOrderMapper orderMapper;
    private final FinishRollMapper finishMapper;
    private final SettleDetailMapper settleDetailMapper;
    private final ProcessOrderDeliveryImpactCounter deliveryImpactCounter;
    private final ProcessOrderIssueVersionService issueVersionService;
    private final ProcessOrderService processOrderService;
    private final OperationLogService operationLogService;

    public boolean prepareIfRequired(String orderUuid, FinishCustomerRevisionRequestDTO request,
                                     boolean required) {
        if (!required) return false;
        ProcessOrder order = requireProcessingOrder(orderUuid, request.getExpectedOrderVersion());
        requireNoDownstreamReference(orderUuid);
        String operator = AuthContextHolder.currentDisplayName();
        String reason = "客户规格变更：" + request.getReason().trim();
        issueVersionService.prepare(orderUuid, order.getSnapPrint(), reason, operator, LocalDateTime.now(),
                request.getRequestId(), ProcessOrderReissueFingerprint.of(
                        orderUuid, request.getExpectedOrderVersion(), reason));
        ConcurrencyGuard.requireRowUpdated(orderMapper.update(null, new LambdaUpdateWrapper<ProcessOrder>()
                .eq(ProcessOrder::getUuid, orderUuid)
                .eq(ProcessOrder::getVersion, request.getExpectedOrderVersion())
                .set(ProcessOrder::getOrderStatus, STATUS_PENDING)
                .set(ProcessOrder::getSnapPrint, null)
                .set(ProcessOrder::getSnapFinish, null)
                .set(ProcessOrder::getPrintStatus, 0)
                .set(ProcessOrder::getPrintCount, 0)
                .set(ProcessOrder::getLastPrintTime, null)
                .set(ProcessOrder::getLastPrintUser, null)
                .set(ProcessOrder::getBackRecordTime, null)
                .set(ProcessOrder::getBackRecordUser, null)
                .set(ProcessOrder::getUpdateBy, operator)
                .set(ProcessOrder::getUpdateTime, LocalDateTime.now())
                .setSql("version = version + 1")));
        operationLogService.record(OperationLogService.BIZ_TYPE_ORDER, orderUuid, order.getOrderNo(),
                OperationLogService.ACTION_REISSUE, operator,
                "客户规格变更将自动生成新的下发版本，原因：" + request.getReason().trim());
        return true;
    }

    public PrintResultVO issuePreparedVersion(String orderUuid, boolean prepared) {
        return prepared ? processOrderService.issue(orderUuid) : null;
    }

    private ProcessOrder requireProcessingOrder(String orderUuid, Integer expectedVersion) {
        ProcessOrder order = orderMapper.selectById(orderUuid);
        if (order == null) throw new BusinessException(ErrorCode.E002, "加工单不存在");
        if (!Objects.equals(order.getVersion(), expectedVersion)) {
            throw new BusinessException(ErrorCode.E006, "加工单已被其他人修改，请刷新后重试");
        }
        if (!Integer.valueOf(STATUS_PROCESSING).equals(order.getOrderStatus())
                || !StringUtils.hasText(order.getSnapPrint())) {
            throw new BusinessException(ErrorCode.E003, "当前加工单不能生成新的下发版本");
        }
        return order;
    }

    private void requireNoDownstreamReference(String orderUuid) {
        if (settleDetailMapper.selectCount(new LambdaQueryWrapper<SettleDetail>()
                .eq(SettleDetail::getOrderUuid, orderUuid).eq(SettleDetail::getIsDeleted, 0)) > 0
                || deliveryImpactCounter.hasConfirmedDelivery(orderUuid)
                || finishMapper.selectCount(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid).eq(FinishRoll::getFinishStatus, FINISH_STATUS_OUT)) > 0) {
            throw new BusinessException(ErrorCode.E003,
                    "A settled or confirmed delivery document prevents reissuing this production order");
        }
    }
}
