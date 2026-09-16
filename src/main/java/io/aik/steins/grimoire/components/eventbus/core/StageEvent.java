package io.aik.steins.grimoire.components.eventbus.core;

import lombok.Getter;

/**
 * -anchor 阶段事件基类
 *
 * <p>所有业务事件的父类，持有阶段标识，供监听器按阶段过滤。</p>
 * <p>子类通过构造函数传入 {@link StageEnum} 声明自身所属阶段。</p>
 *
 * @param <T> 事件携带的数据类型
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote 事件类命名约定：业务动作 + Event（如 TaskCompletedEvent）
 * @since 2026/09/16
 * -
 */
@Getter
public abstract class StageEvent<T> {

    /**
     * 事件所属阶段
     */
    private final StageEnum stage;

    /**
     * 事件携带的数据
     */
    private final T payload;

    protected StageEvent(StageEnum stage, T payload) {
        this.stage = stage;
        this.payload = payload;
    }
}
