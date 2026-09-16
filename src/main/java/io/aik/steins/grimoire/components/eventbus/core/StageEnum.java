package io.aik.steins.grimoire.components.eventbus.core;

/**
 * -anchor 事件阶段枚举
 *
 * <p>定义事件处理器的执行阶段，控制监听器在业务流程中的执行时机：</p>
 * <ul>
 *     <li>BEFORE — 前置拦截，在业务操作持久化之前执行</li>
 *     <li>ON — 核心联动，在业务操作完成后立即执行</li>
 *     <li>AFTER — 后置处理，在所有 ON 阶段完成后执行</li>
 * </ul>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote 三阶段语义严格区分，不可混用
 * @since 2026/09/16
 * -
 */
public enum StageEnum {

    /**
     * 前置拦截阶段
     * <p>在业务操作持久化之前执行，典型用途：修改入参、设置初始状态字段</p>
     */
    BEFORE,

    /**
     * 核心联动阶段
     * <p>在业务操作完成后立即执行，典型用途：级联状态变更、生成关联任务</p>
     */
    ON,

    /**
     * 后置处理阶段
     * <p>在所有 ON 阶段完成后执行，典型用途：同步外部系统、记录审计日志</p>
     */
    AFTER
}
