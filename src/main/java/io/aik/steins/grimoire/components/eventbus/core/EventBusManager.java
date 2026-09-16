package io.aik.steins.grimoire.components.eventbus.core;

import com.google.common.eventbus.EventBus;
import io.aik.steins.grimoire.components.eventbus.annotation.SafeSubscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * -anchor 事件总线管理器
 *
 * <p>封装 Guava EventBus，提供统一的注册与发布入口。</p>
 * <ul>
 *     <li>所有监听器通过 {@link #register(Object)} 注册</li>
 *     <li>所有事件通过 {@link #post(Object)} 发布</li>
 *     <li>同步分发，监听器与发布者在同一线程/事务中</li>
 * </ul>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8, Guava EventBus
 * @apiNote 同步模型：监听器异常会影响发布者事务
 * @since 2026/09/16
 * -
 */
@Slf4j
@Component
public class EventBusManager {

    private final EventBus eventBus;
    private final List<Object> registeredListeners;

    public EventBusManager() {
        this.eventBus = new EventBus("grimoire-event-bus");
        this.registeredListeners = new ArrayList<>();
        log.info("EventBusManager initialized");
    }

    /**
     * 注册监听器
     *
     * <p>监听器对象中所有标注 {@link SafeSubscribe} 的方法会被自动发现并订阅。</p>
     *
     * @param listener 监听器实例
     */
    public void register(Object listener) {
        eventBus.register(listener);
        registeredListeners.add(listener);
        log.debug("Listener registered: {}", listener.getClass().getSimpleName());
    }

    /**
     * 发布事件
     *
     * <p>同步分发给所有匹配的订阅方法，按注册顺序执行。</p>
     *
     * @param event 事件对象
     */
    public void post(Object event) {
        log.debug("Event posted: {}", event.getClass().getSimpleName());
        eventBus.post(event);
    }

    /**
     * 获取已注册监听器数量
     *
     * @return 监听器数量
     */
    public int getListenerCount() {
        return registeredListeners.size();
    }
}
