-- Phase 3 入库：EventBus 事件驱动监听器架构方案元数据
-- 执行前提：aik_steins_grimoire 数据库已初始化 aik_knowledge 表
-- 执行方式：在 Grimoire 项目启动后调用 /grimoire/knowledge/add 接口，或直接执行本 SQL

-- ============================================================
-- 方式一：直接 SQL 插入（推荐，无需启动应用）
-- ============================================================

INSERT INTO aik_knowledge (
    id,
    title,
    code,
    type,
    summary,
    content,
    source_project,
    source_path,
    resource_path,
    ext_json,
    category_id,
    status,
    create_time,
    modify_time,
    create_by,
    modify_by
) VALUES (
    -- id: 使用雪花算法或手动指定，此处用时间戳占位
    UNIX_TIMESTAMP() * 1000,
    'EventBus 事件驱动监听器架构',
    'eventbus-listener-architecture',
    3,
    '基于 Guava EventBus 的进程内发布-订阅模式，实现多实体级联状态变更的解耦架构方案。支持三阶段控制（BEFORE/ON/AFTER）、异常隔离、四道防线模板。',
    NULL,
    'hussar-web (LIMS)',
    'com/jxdinfo/lims/app/base/testing/listener',
    'solutions/eventbus-listener-architecture/',
    JSON_OBJECT(
        'componentManual', 'grimoire-files/component-manuals/eventbus-listener-architecture.md',
        'codePath', 'components/eventbus/',
        'techStack', JSON_ARRAY('Guava EventBus', 'Spring Boot', 'JDK 8'),
        'patterns', JSON_ARRAY('发布-订阅', '模板方法', '阶段控制'),
        'listenerCount', 21,
        'eventTypeCount', 15
    ),
    NULL,
    1,
    NOW(),
    NOW(),
    'aIk',
    'aIk'
);

-- ============================================================
-- 方式二：API 调用（应用运行后）
-- POST /grimoire/knowledge/add
-- Content-Type: application/json
-- ============================================================
-- {
--     "title": "EventBus 事件驱动监听器架构",
--     "code": "eventbus-listener-architecture",
--     "type": 3,
--     "summary": "基于 Guava EventBus 的进程内发布-订阅模式，实现多实体级联状态变更的解耦架构方案。支持三阶段控制（BEFORE/ON/AFTER）、异常隔离、四道防线模板。",
--     "sourceProject": "hussar-web (LIMS)",
--     "sourcePath": "com/jxdinfo/lims/app/base/testing/listener",
--     "resourcePath": "solutions/eventbus-listener-architecture/",
--     "extJson": "{\"componentManual\":\"grimoire-files/component-manuals/eventbus-listener-architecture.md\",\"codePath\":\"components/eventbus/\",\"techStack\":[\"Guava EventBus\",\"Spring Boot\",\"JDK 8\"],\"patterns\":[\"发布-订阅\",\"模板方法\",\"阶段控制\"],\"listenerCount\":21,\"eventTypeCount\":15}",
--     "status": 1,
--     "tagIds": []
-- }
