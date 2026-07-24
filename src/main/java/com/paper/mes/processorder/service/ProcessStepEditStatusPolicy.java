package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;

public final class ProcessStepEditStatusPolicy {

    private static final int DRAFT = 0;
    private static final int PENDING = 1;
    private static final int TO_RECORD = 3;

    private ProcessStepEditStatusPolicy() {
    }

    public static void requireAddAllowed(Integer status, boolean extraStep) {
        int current = status == null ? DRAFT : status;
        if (current == PENDING) return;
        if ((current == DRAFT || current == TO_RECORD) && extraStep) return;
        if (current == DRAFT) {
            throw new BusinessException(ErrorCode.E001, "草稿阶段只能配置附加工艺，主工艺请在加工方案中选择");
        }
        if (current == TO_RECORD) {
            throw new BusinessException(ErrorCode.E001, "待回录只能新增附加工艺");
        }
        throw new BusinessException(ErrorCode.E001, "只能在草稿、待下发或待回录状态新增工序");
    }

    public static void requireChangeAllowed(Integer status) {
        int current = status == null ? DRAFT : status;
        if (current == DRAFT || current == PENDING) return;
        throw new BusinessException(ErrorCode.E001, "只能在草稿或待下发状态修改工序");
    }
}
