package com.paper.mes.processorder.statemachine;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;

/**
 * 通用状态机引擎：仅负责"转换合法性校验"，不含业务前置条件。
 * 业务前置（卷号已生成、三级重量校验、未结算等）由各业务接口（P1-2/3/4）在调用本引擎前后自行判定。
 */
public final class StateMachine {

    private StateMachine() {
    }

    /**
     * 校验从 from 到 to 是否为合法流转，非法抛 BusinessException。
     * from==to 视为非法（无意义的自转换）。
     */
    public static <E extends Enum<E> & StateNode<E>> void assertTransition(E from, E to) {
        if (from == to) {
            throw new BusinessException(ErrorCode.E001,
                    "当前已是【" + to.getDesc() + "】状态，无需变更");
        }
        if (!from.allowedTargets().contains(to)) {
            throw new BusinessException(ErrorCode.E001,
                    "状态不允许从【" + from.getDesc() + "】流转到【" + to.getDesc() + "】");
        }
    }

    public static <E extends Enum<E> & StateNode<E>> boolean canTransition(E from, E to) {
        return from != to && from.allowedTargets().contains(to);
    }
}
