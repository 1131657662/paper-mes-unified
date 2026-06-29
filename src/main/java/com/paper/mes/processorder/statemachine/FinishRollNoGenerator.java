package com.paper.mes.processorder.statemachine;

import com.paper.mes.common.BusinessException;

/**
 * 成品全局唯一卷号编解码（V4.1 §5.3）。
 *
 * 编码格式：1 位大写字母（排除易手写混淆的 I/O/L/Z，剩 22 个）+ 6 位数字。
 * 每个字母块编号区间为 000001~999999（000000 不使用，保证显示数字恒 ≥1，与文档示例 A000001 起头一致）。
 * 满额进位：A999999 → B000001 → … → 末字母 999999 为全局上限。
 *
 * 纯算法、无副作用，便于单测；不感知数据库与并发。
 */
public final class FinishRollNoGenerator {

    /** 可用字母表，排除 I/O/L/Z，共 22 个。顺序即字典序，保证字符串排序与序号单调一致。 */
    private static final char[] LETTERS = "ABCDEFGHJKMNPQRSTUVWXY".toCharArray();
    private static final int PER_LETTER = 999_999;
    private static final long CAPACITY = (long) LETTERS.length * PER_LETTER;

    private FinishRollNoGenerator() {
    }

    /**
     * 序号 → 卷号。序号从 1 开始：1 → A000001，999999 → A999999，1000000 → B000001。
     */
    public static String encode(long seq) {
        if (seq < 1 || seq > CAPACITY) {
            throw new BusinessException("成品卷号序号越界：" + seq + "（容量 1~" + CAPACITY + "）");
        }
        long zeroBased = seq - 1;
        int letterIdx = (int) (zeroBased / PER_LETTER);
        int number = (int) (zeroBased % PER_LETTER) + 1;
        return LETTERS[letterIdx] + String.format("%06d", number);
    }

    /**
     * 卷号 → 序号。格式非法（长度、字母不在表内、数字越界）抛 BusinessException。
     */
    public static long decode(String rollNo) {
        if (rollNo == null || rollNo.length() != 7) {
            throw new BusinessException("成品卷号格式非法：" + rollNo);
        }
        int letterIdx = letterIndex(rollNo.charAt(0));
        if (letterIdx < 0) {
            throw new BusinessException("成品卷号字母非法：" + rollNo);
        }
        int number;
        try {
            number = Integer.parseInt(rollNo.substring(1));
        } catch (NumberFormatException e) {
            throw new BusinessException("成品卷号数字部分非法：" + rollNo);
        }
        if (number < 1 || number > PER_LETTER) {
            throw new BusinessException("成品卷号数字越界：" + rollNo);
        }
        return (long) letterIdx * PER_LETTER + number;
    }

    /** 给定当前卷号，返回下一个卷号。 */
    public static String next(String rollNo) {
        return encode(decode(rollNo) + 1);
    }

    /** 是否为合法的字母流水卷号（用于查重/校验，不抛异常）。 */
    public static boolean isValid(String rollNo) {
        if (rollNo == null || rollNo.length() != 7 || letterIndex(rollNo.charAt(0)) < 0) {
            return false;
        }
        for (int i = 1; i < 7; i++) {
            if (!Character.isDigit(rollNo.charAt(i))) {
                return false;
            }
        }
        int number = Integer.parseInt(rollNo.substring(1));
        return number >= 1 && number <= PER_LETTER;
    }

    private static int letterIndex(char c) {
        for (int i = 0; i < LETTERS.length; i++) {
            if (LETTERS[i] == c) {
                return i;
            }
        }
        return -1;
    }
}
