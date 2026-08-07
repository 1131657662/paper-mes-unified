package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;

import java.util.Objects;

final class ProcessOrderAppendVersionPolicy {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING = 1;

    private ProcessOrderAppendVersionPolicy() {
    }

    static void requireAppendableStatus(Integer status) {
        if (status == null || (status != STATUS_DRAFT && status != STATUS_PENDING)) {
            throw new BusinessException(ErrorCode.E001, "仅草稿或待下发加工单可追加母卷");
        }
    }

    static void requireCurrentVersion(Integer current, Integer expected) {
        if (!Objects.equals(current, expected)) {
            throw new BusinessException(ErrorCode.E006, "加工单已被其他页面修改，请刷新后重试");
        }
    }
}
