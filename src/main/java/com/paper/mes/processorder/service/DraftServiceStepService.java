package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.ProcessStepBatchDTO;
import com.paper.mes.processorder.dto.ProcessStepBatchResultVO;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DraftServiceStepService {

    private static final int DRAFT_STATUS = 0;

    private final BusinessLockService businessLockService;
    private final ProcessOrderMapper orderMapper;
    private final ProcessStepMapper stepMapper;
    private final ProcessOrderService processOrderService;
    private final DraftOrderVersionGuard versionGuard;

    @Transactional(rollbackFor = Exception.class)
    public void add(String orderUuid, ProcessStepDTO dto) {
        requireDraftOrder(orderUuid, dto.getExpectedVersion());
        requireServiceRequest(dto);
        processOrderService.addProcessStep(orderUuid, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessStepBatchResultVO addBatch(String orderUuid, ProcessStepBatchDTO dto) {
        requireDraftOrder(orderUuid, dto.getExpectedVersion());
        dto.getSteps().forEach(this::requireServiceRequest);
        return processOrderService.addProcessSteps(orderUuid, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String stepUuid, ProcessStepDTO dto) {
        ProcessStep initial = requireStep(stepUuid, ErrorCode.E002);
        requireDraftOrder(initial.getOrderUuid(), dto.getExpectedVersion());
        ProcessStep current = requireStep(stepUuid, ErrorCode.E006);
        requireServiceTarget(current);
        requireServiceRequest(dto);
        processOrderService.updateProcessStep(stepUuid, dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String stepUuid, Integer expectedVersion) {
        ProcessStep initial = requireStep(stepUuid, ErrorCode.E002);
        requireDraftOrder(initial.getOrderUuid(), expectedVersion);
        requireServiceTarget(requireStep(stepUuid, ErrorCode.E006));
        processOrderService.deleteProcessStep(stepUuid);
    }

    private ProcessOrder requireDraftOrder(String orderUuid, Integer expectedVersion) {
        requireExpectedVersion(expectedVersion);
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = orderMapper.selectById(orderUuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (!Integer.valueOf(DRAFT_STATUS).equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.E001, "新建工作台只能维护草稿加工单的附加工艺");
        }
        versionGuard.assertExpected(order, expectedVersion);
        versionGuard.assertLockedExpected(orderUuid, expectedVersion);
        return order;
    }

    private void requireExpectedVersion(Integer expectedVersion) {
        if (expectedVersion == null || expectedVersion < 0) {
            throw new BusinessException(ErrorCode.E006, "加工单版本缺失，请刷新后重试");
        }
    }

    private ProcessStep requireStep(String stepUuid, ErrorCode errorCode) {
        ProcessStep step = stepMapper.selectById(stepUuid);
        if (step == null) {
            throw new BusinessException(errorCode, "附加工艺不存在或已被删除");
        }
        return step;
    }

    private void requireServiceRequest(ProcessStepDTO dto) {
        if (!isServiceStep(dto.getStepType()) || Integer.valueOf(1).equals(dto.getIsMain())) {
            throw new BusinessException(ErrorCode.E003, "新建工作台只能维护剥损整理或重新包装");
        }
    }

    private void requireServiceTarget(ProcessStep step) {
        if (Integer.valueOf(1).equals(step.getIsMain()) || !isServiceStep(step.getStepType())) {
            throw new BusinessException(ErrorCode.E003, "目标工序不是可维护的附加工艺");
        }
    }

    private boolean isServiceStep(Integer stepType) {
        return Integer.valueOf(3).equals(stepType) || Integer.valueOf(4).equals(stepType);
    }
}
