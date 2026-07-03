package com.paper.mes.system.config.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.entity.SysNoRule;

import java.util.List;

final class FinishRollNoFormatter {

    private static final String LETTERS = "ABCDEFGHJKMNPQRSTUVWXY";
    private static final String REGEXP = "^[ABCDEFGHJKMNPQRSTUVWXY][0-9]{6}$";
    private static final int SERIAL_LENGTH = 6;
    private static final long MAX_PER_LETTER = 999_999L;

    private FinishRollNoFormatter() {
    }

    static String regexp() {
        return REGEXP;
    }

    static String format(SysNoRule rule, long sequence) {
        if (sequence <= 0) {
            throw new BusinessException("成品卷号流水必须大于0");
        }
        int startIndex = startIndex(rule);
        long offset = sequence - 1L;
        int letterIndex = startIndex + (int) (offset / MAX_PER_LETTER);
        if (letterIndex >= LETTERS.length()) {
            throw new BusinessException("成品卷号流水已超出可用字母范围，请联系管理员扩容规则");
        }
        long serial = offset % MAX_PER_LETTER + 1L;
        return LETTERS.charAt(letterIndex) + String.format("%0" + SERIAL_LENGTH + "d", serial);
    }

    static long maxPersistedValue(SysNoRule rule, List<String> persistedNos) {
        int startIndex = startIndex(rule);
        long max = 0L;
        for (String value : persistedNos) {
            max = Math.max(max, sequenceValue(value, startIndex));
        }
        return max;
    }

    private static int startIndex(SysNoRule rule) {
        String prefix = rule.getPrefix() == null ? "" : rule.getPrefix().trim().toUpperCase();
        if (prefix.length() != 1 || LETTERS.indexOf(prefix.charAt(0)) < 0) {
            throw new BusinessException("成品卷号前缀必须是1位大写字母，且不能使用 I/O/L/Z");
        }
        return LETTERS.indexOf(prefix.charAt(0));
    }

    private static long sequenceValue(String finishRollNo, int startIndex) {
        if (finishRollNo == null || finishRollNo.length() != 7) {
            return 0L;
        }
        int letterIndex = LETTERS.indexOf(finishRollNo.charAt(0));
        if (letterIndex < startIndex) {
            return 0L;
        }
        long serial = Long.parseLong(finishRollNo.substring(1));
        return (letterIndex - startIndex) * MAX_PER_LETTER + serial;
    }
}
