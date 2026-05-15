package io.aik.steins.grimoire.core.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充处理器 -anchor
 *
 * @author a I k .
 */
@Slf4j
@Component
public class BaseMetaObjectHandler implements MetaObjectHandler {

    /**
     * 获取当前操作人
     *
     * <p>TODO 接入鉴权后，从 SecurityContext 或自定义用户上下文中获取当前用户ID/名称</p>
     *
     * @return 当前操作人标识
     */
    private String getCurrentUser() {
        //anchor 临时返回 system，鉴权接入后替换为实际当前用户
        return "system";
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        //anchor 插入时自动填充时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "modifyTime", LocalDateTime.class, LocalDateTime.now());

        //anchor 插入时自动填充操作人
        this.strictInsertFill(metaObject, "createBy", String.class, getCurrentUser());
        this.strictInsertFill(metaObject, "modifyBy", String.class, getCurrentUser());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        //anchor 更新时自动填充修改时间和修改人
        this.strictUpdateFill(metaObject, "modifyTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "modifyBy", String.class, getCurrentUser());
    }
}
