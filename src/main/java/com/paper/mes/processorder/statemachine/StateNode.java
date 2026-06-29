package com.paper.mes.processorder.statemachine;

import java.util.Set;

/**
 * 状态机节点统一抽象，供 StateMachine 通用校验。
 *
 * @param <E> 具体状态枚举自身类型
 */
public interface StateNode<E extends Enum<E> & StateNode<E>> {

    int getCode();

    String getDesc();

    Set<E> allowedTargets();
}
