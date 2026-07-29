package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;

import java.util.Locale;

/** 回录阶段对预生成成品的实际产出动作。 */
enum BackRecordFinishAction {
    PRODUCED,
    NOT_PRODUCED,
    ADDED;

    static BackRecordFinishAction from(String value) {
        if (value == null || value.isBlank()) return PRODUCED;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("成品产出动作无效：" + value);
        }
    }
}
