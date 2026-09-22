-- system 模块建表脚本
-- 数据库: aik_steins_grimoire
-- 字符集: utf8mb4

-- 字典类型表
CREATE TABLE IF NOT EXISTS aik_sys_dict_type (
    id BIGINT NOT NULL COMMENT '主键',
    dict_code VARCHAR(64) NOT NULL COMMENT '字典编码',
    dict_name VARCHAR(128) NOT NULL COMMENT '字典名称',
    description VARCHAR(512) COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_code (dict_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- 字典项表
CREATE TABLE IF NOT EXISTS aik_sys_dict_item (
    id BIGINT NOT NULL COMMENT '主键',
    dict_code VARCHAR(64) NOT NULL COMMENT '字典类型编码',
    item_code VARCHAR(64) NOT NULL COMMENT '字典项编码',
    item_name VARCHAR(128) NOT NULL COMMENT '字典项名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_item (dict_code, item_code),
    KEY idx_dict_code (dict_code),
    KEY idx_status (status),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';

-- 系统参数表
CREATE TABLE IF NOT EXISTS aik_sys_param (
    id BIGINT NOT NULL COMMENT '主键',
    param_key VARCHAR(128) NOT NULL COMMENT '参数键',
    param_value VARCHAR(2048) NOT NULL COMMENT '参数值',
    description VARCHAR(512) COMMENT '描述',
    param_group VARCHAR(64) COMMENT '参数分组',
    editable TINYINT NOT NULL DEFAULT 1 COMMENT '是否可编辑：1-是，0-否',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_param_key (param_key),
    KEY idx_param_group (param_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';

-- 文件记录表（第 1 层：文件对象。物理删除，无 del_flag；md5 内容寻址）
CREATE TABLE IF NOT EXISTS aik_sys_file (
    id BIGINT NOT NULL COMMENT '主键',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名（首次上传者）',
    stored_name VARCHAR(255) NOT NULL COMMENT '存储文件名（Snowflake ID）',
    file_path VARCHAR(512) NOT NULL COMMENT '相对存储路径（存储层内部标识，不对外暴露）',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(128) COMMENT 'MIME类型',
    storage_type VARCHAR(16) NOT NULL DEFAULT 'local' COMMENT '存储类型：local/oss/sftp',
    md5 VARCHAR(32) COMMENT '文件MD5哈希（内容寻址锚点）',
    download_count INT NOT NULL DEFAULT 0 COMMENT '下载次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_original_name (original_name),
    KEY idx_create_time (create_time),
    KEY idx_storage_type (storage_type),
    KEY idx_md5 (md5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';

-- 通用附件挂载表（第 2 层：挂载点。卸载保行以保留元数据；复活契约见 SDD §2.5.7）
-- 本表原位于 sql/aik_knowledge_tables.sql，因归属 system 模块已迁至本文件（SDD §2.5.8）
CREATE TABLE IF NOT EXISTS aik_sys_attachment (
    id BIGINT NOT NULL COMMENT '主键',
    file_id BIGINT NOT NULL COMMENT '被挂载的文件对象ID，逻辑关联 aik_sys_file.id',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：knowledge（取值登记见 SDD §2.5.5）',
    biz_id BIGINT NOT NULL COMMENT '业务主键：biz_type=knowledge 时为 aik_knowledge.id',
    attach_name VARCHAR(256) NOT NULL COMMENT '用户可见文件名（权威）',
    description VARCHAR(512) COMMENT '描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    del_flag TINYINT NOT NULL DEFAULT 0 COMMENT '卸载标记：0-有效挂载，1-已卸载',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_file (biz_type, biz_id, file_id),
    KEY idx_biz (biz_type, biz_id, del_flag, sort_order),
    KEY idx_file_id (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用附件挂载表';
