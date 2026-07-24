package com.paper.mes.processorder.model;

import com.paper.mes.common.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Locale;

/** Defines how unused source width is represented after processing. */
public enum WidthDifferencePolicy {
    LOSS,
    ALLOCATE,
    REMAINDER;

    public static WidthDifferencePolicy resolve(String value) {
        if (!StringUtils.hasText(value)) {
            return ALLOCATE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("门幅差额处理只能选择计损耗、分摊或留余料");
        }
    }
}
