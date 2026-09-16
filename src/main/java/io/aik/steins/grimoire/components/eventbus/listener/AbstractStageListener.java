package io.aik.steins.grimoire.components.eventbus.listener;

import io.aik.steins.grimoire.components.eventbus.core.EventBusManager;
import io.aik.steins.grimoire.components.eventbus.core.StageEnum;
import io.aik.steins.grimoire.components.eventbus.core.StageEvent;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

/**
 * -anchor 阶段监听器抽象基类
 *
 * <p>提供标准的监听器骨架，子类只需实现 {@link #handleEvent(StageEvent)} 方法。</p>
 *
 * <p><b>四道防线（必须遵守）：</b></p>
 * <ol>
 *     <li>阶段过滤 — 只在目标阶段执行</li>
 *     <li>空值防御 — 防止 payload 为 null</li>
 *     <li>类型过滤 — 确保 payload 类型正确</li>
 *     <li>业务逻辑 — 实际联动操作</li>
 * </ol>
 *
 * <p><b>子类示例：</b></p>
 * <pre>{@code
 * @Component
 * public class TaskCompletedListener extends AbstractStageListener<TaskCompletedEvent, Long> {
 *
 *     @Autowired
 *     private EventBusManager eventBusManager;
 *
 *     @PostConstruct
 *     public void reg() {
 *         registerSelf(eventBusManager);
 *     }
 *
 *     @Override
 *     protected StageEnum getTargetStage() {
 *         return StageEnum.ON;
 *     }
 *
 *     @Override
 *     protected void handleEvent(TaskCompletedEvent event) {
 *         // 四道防线后，直接写业务逻辑
 *         List<Long> taskIds = event.getPayload();
 *         // ... 联动操作
 *     }
 * }
 * }</pre>
 *
 * @param <E> 事件类型（必须继承 StageEvent）
 * @param <T> 事件携带的数据类型
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote 子类必须加 @Component 注解，并通过 @PostConstruct 调用 registerSelf()
 * @since 2026/09/16
 * -
 */
@Slf4j
public abstract class AbstractStageListener<E extends StageEvent<T>, T> {

    /**
     * 获取目标执行阶段
     *
     * <p>子类必须实现此方法，声明本监听器关注的阶段。</p>
     *
     * @return 目标阶段枚举值
     */
    protected abstract StageEnum getTargetStage();

    /**
     * 处理事件（核心业务逻辑）
     *
     * <p>此方法在通过四道防线后被调用，子类实现具体的联动逻辑。</p>
     *
     * @param event 事件对象
     */
    protected abstract void handleEvent(E event);

    /**
     * 注册自身到事件总线
     *
     * <p>子类必须在 @PostConstruct 方法中调用此方法完成注册。</p>
     *
     * @param eventBusManager 事件总线管理器
     */
    protected void registerSelf(EventBusManager eventBusManager) {
        eventBusManager.register(this);
        log.info("Listener registered: {}", this.getClass().getSimpleName());
    }

    /**
     * 事件入口（由 @SafeSubscribe 方法调用）
     *
     * <p>执行四道防线后委托给 {@link #handleEvent(StageEvent)}。</p>
     * <p>子类在 @SafeSubscribe 方法中调用此方法：</p>
     * <pre>{@code
     * @SafeSubscribe
     * public void onTaskCompleted(TaskCompletedEvent event) {
     *     processEvent(event);
     * }
     * }</pre>
     *
     * @param event 事件对象
     */
    @SuppressWarnings("unchecked")
    protected void processEvent(StageEvent<?> event) {
        // 防线 1：阶段过滤
        if (!getTargetStage().equals(event.getStage())) {
            return;
        }

        // 防线 2：空值防御
        T payload = (T) event.getPayload();
        if (payload == null) {
            log.debug("Event payload is null, skip. listener={}", this.getClass().getSimpleName());
            return;
        }

        // 防线 3：类型过滤（由子类覆盖 isPayloadTypeValid 进行扩展检查）
        if (!isPayloadTypeValid(payload)) {
            log.debug("Event payload type invalid, skip. listener={}", this.getClass().getSimpleName());
            return;
        }

        // 防线 4：执行业务逻辑
        try {
            handleEvent((E) event);
        } catch (Exception e) {
            log.error("Listener handleEvent failed, listener={}, event={}",
                    this.getClass().getSimpleName(), event.getClass().getSimpleName(), e);
        }
    }

    /**
     * 校验 payload 类型是否有效
     *
     * <p>默认返回 true，子类可覆盖以添加类型检查。</p>
     *
     * @param payload 事件数据
     * @return 是否有效
     */
    protected boolean isPayloadTypeValid(T payload) {
        return true;
    }
}
