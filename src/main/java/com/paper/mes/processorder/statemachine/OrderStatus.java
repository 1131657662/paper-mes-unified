package com.paper.mes.processorder.statemachine;

import java.util.Arrays;
import java.util.Set;

/**
 * 加工单状态。合法流转严格按 V4.1 §3.5 状态迁移矩阵。
 * 0草稿 → 1待下发 → 2加工中 → 3待回录 → 4已完成 → 5已结算
 * 含回退：待回录→待下发、已完成→待回录。
 */
public enum OrderStatus implements StateNode<OrderStatus> {

    DRAFT(0, "草稿"),
    PENDING(1, "待下发"),
    PROCESSING(2, "加工中"),
    TO_RECORD(3, "待回录"),
    FINISHED(4, "已完成"),
    SETTLED(5, "已结算");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderStatus of(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("非法加工单状态码：" + code));
    }

    /** 本状态允许流转到的目标状态集合。 */
    @Override
    public Set<OrderStatus> allowedTargets() {
        return switch (this) {
            case DRAFT -> Set.of(PENDING);
            case PENDING -> Set.of(PROCESSING);
            case PROCESSING -> Set.of(TO_RECORD);
            case TO_RECORD -> Set.of(FINISHED, PENDING);
            case FINISHED -> Set.of(SETTLED, TO_RECORD);
            case SETTLED -> Set.of();
        };
    }
}
