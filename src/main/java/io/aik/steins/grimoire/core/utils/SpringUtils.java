package io.aik.steins.grimoire.core.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Spring 上下文工具 -anchor
 *
 * @author a I k .
 */
@Component
public class SpringUtils implements ApplicationContextAware, DisposableBean {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        SpringUtils.applicationContext = context;
    }

    @Override
    public void destroy() {
        SpringUtils.applicationContext = null;
    }

    /**
     * 获取 ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 通过 class 获取 Bean
     */
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    /**
     * 通过 name 和 class 获取 Bean
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return applicationContext.getBean(name, clazz);
    }

    /**
     * 获取配置项
     */
    public static String getProperty(String key) {
        return applicationContext.getEnvironment().getProperty(key);
    }

    /**
     * 获取 Environment
     */
    public static Environment getEnvironment() {
        return applicationContext.getEnvironment();
    }

    /**
     * 发布事件
     */
    public static void publishEvent(ApplicationEvent event) {
        applicationContext.publishEvent(event);
    }

    /**
     * 判断 Spring 上下文是否已加载
     */
    public static boolean isActive() {
        return applicationContext != null;
    }
}
