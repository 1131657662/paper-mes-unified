package com.paper.mes.processorder.statemachine;

import java.util.Arrays;
import java.util.Set;

/**
 * 成品卷状态。合法流转严格按 V4.1 §3.5。
 * 1待入库 → 2已入库 → 3已出库；1待入库 → 4报废
 */
public enum FinishStatus implements StateNode<FinishStatus> {

    PENDING_IN(1, "待入库"),
    IN_STOCK(2, "已入库"),
    OUT_STOCK(3, "已出库"),
    SCRAP(4, "报废");

    private final int code;
    private final String desc;

    FinishStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static FinishStatus of(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("非法成品状态码：" + code));
    }

    public Set<FinishStatus> allowedTargets() {
        return switch (this) {
            case PENDING_IN -> Set.of(IN_STOCK, SCRAP);
            case IN_STOCK -> Set.of(OUT_STOCK);
            case OUT_STOCK, SCRAP -> Set.of();
        };
    }
}
