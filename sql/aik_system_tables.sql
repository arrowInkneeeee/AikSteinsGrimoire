-- system 模块建表脚本
-- 数据库: aik_grimoire
-- 字符集: utf8mb4

-- 字典类型表
CREATE TABLE IF NOT EXISTS aik_dict_type (
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
CREATE TABLE IF NOT EXISTS aik_dict_item (
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
CREATE TABLE IF NOT EXISTS aik_system_param (
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

-- 文件记录表
CREATE TABLE IF NOT EXISTS aik_file_record (
    id BIGINT NOT NULL COMMENT '主键',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_name VARCHAR(255) NOT NULL COMMENT '存储文件名（UUID）',
    file_path VARCHAR(512) NOT NULL COMMENT '相对存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(128) COMMENT 'MIME类型',
    download_count INT NOT NULL DEFAULT 0 COMMENT '下载次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_original_name (original_name),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';
