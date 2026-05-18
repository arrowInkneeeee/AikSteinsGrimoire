-- knowledge 模块建表脚本
-- 数据库: aik_grimoire
-- 字符集: utf8mb4

-- 知识分类表
CREATE TABLE IF NOT EXISTS aik_knowledge_category (
    id BIGINT NOT NULL COMMENT '主键',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID，0表示根节点',
    category_name VARCHAR(128) NOT NULL COMMENT '分类名称',
    category_code VARCHAR(64) COMMENT '分类编码',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识分类表';

-- 知识标签表
CREATE TABLE IF NOT EXISTS aik_knowledge_tag (
    id BIGINT NOT NULL COMMENT '主键',
    tag_name VARCHAR(64) NOT NULL COMMENT '标签名称',
    tag_color VARCHAR(16) COMMENT '标签颜色',
    use_count INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识标签表';

-- 知识标签关联表
CREATE TABLE IF NOT EXISTS aik_knowledge_tag_relation (
    id BIGINT NOT NULL COMMENT '主键',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    knowledge_id BIGINT NOT NULL COMMENT '知识条目ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_knowledge (tag_id, knowledge_id),
    KEY idx_knowledge_id (knowledge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识标签关联表';

-- 知识条目主表
CREATE TABLE IF NOT EXISTS aik_knowledge (
    id BIGINT NOT NULL COMMENT '主键',
    title VARCHAR(256) NOT NULL COMMENT '标题（组件名/方案名/笔记标题/片段描述）',
    code VARCHAR(128) COMMENT '编码（组件编码/方案编码）',
    type TINYINT NOT NULL COMMENT '类型：1-笔记 2-组件 3-方案 4-片段',
    summary VARCHAR(512) COMMENT '摘要/用途描述',
    content TEXT COMMENT '正文（笔记内容/代码片段内容/方案描述）',
    source_project VARCHAR(256) COMMENT '来源项目',
    source_path VARCHAR(512) COMMENT '来源路径',
    resource_path VARCHAR(512) COMMENT '资源路径（指向components/或solutions/下的包路径）',
    ext_json JSON COMMENT '扩展字段JSON（不同类型特有属性）',
    category_id BIGINT COMMENT '分类ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_type (type),
    KEY idx_category_id (category_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识条目主表';

-- 知识附件表
CREATE TABLE IF NOT EXISTS aik_knowledge_attachment (
    id BIGINT NOT NULL COMMENT '主键',
    knowledge_id BIGINT NOT NULL COMMENT '知识条目ID',
    attach_name VARCHAR(256) NOT NULL COMMENT '附件名称',
    attach_url VARCHAR(512) NOT NULL COMMENT '附件URL/存储路径',
    description VARCHAR(512) COMMENT '描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_knowledge_id (knowledge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识附件表';
