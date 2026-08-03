package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;

public final class ProcessStepEditStatusPolicy {

    private static final int DRAFT = 0;
    private static final int PENDING = 1;

    private ProcessStepEditStatusPolicy() {
    }

    public static void requireAddAllowed(Integer status, boolean extraStep) {
        int current = status == null ? DRAFT : status;
        if (current == PENDING) return;
        if (current == DRAFT && extraStep) return;
        if (current == DRAFT) {
            throw new BusinessException(ErrorCode.E001, "草稿阶段只能配置附加工艺，主工艺请在加工方案中选择");
        }
        throw new BusinessException(ErrorCode.E001,
                "已下发计划不能通过普通入口直接新增工序，请使用当前状态对应的受控变更命令");
    }

    public static void requireChangeAllowed(Integer status) {
        int current = status == null ? DRAFT : status;
        if (current == DRAFT || current == PENDING) return;
        throw new BusinessException(ErrorCode.E001, "只能在草稿或待下发状态修改工序");
    }
}
