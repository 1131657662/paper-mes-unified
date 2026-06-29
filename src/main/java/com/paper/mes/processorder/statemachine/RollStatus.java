package com.paper.mes.processorder.statemachine;

import java.util.Arrays;
import java.util.Set;

/**
 * 原纸单卷状态。合法流转严格按 V4.1 §3.5。
 * 1待加工 → 2加工中 → 3完成 / 4直发 / 5报废
 */
public enum RollStatus implements StateNode<RollStatus> {

    PENDING(1, "待加工"),
    PROCESSING(2, "加工中"),
    DONE(3, "完成"),
    DIRECT(4, "直发"),
    SCRAP(5, "报废");

    private final int code;
    private final String desc;

    RollStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static RollStatus of(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("非法原纸状态码：" + code));
    }

    public Set<RollStatus> allowedTargets() {
        return switch (this) {
            case PENDING -> Set.of(PROCESSING);
            case PROCESSING -> Set.of(DONE, DIRECT, SCRAP);
            case DONE, DIRECT, SCRAP -> Set.of();
        };
    }
}
