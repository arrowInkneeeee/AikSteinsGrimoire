-- =============================================================================
-- 附件两层模型迁移脚本（实测现状 → 目标结构）
-- 设计依据: grimoire-files/design-docs/system-module/SDD.md §2.3.0 / §2.3.1 / §2.5
-- 数据库:   aik_steins_grimoire (MySQL 8.0)
--
-- 变更内容:
--   A. aik_sys_file        DROP url、DROP del_flag（本表改为「物理删除」，移除 @TableLogic）
--                          保留非唯一索引 KEY idx_md5（秒传查重依赖，不新增任何唯一约束）
--   B. aik_sys_attachment  重写为通用挂载表:
--                          DROP attach_url、knowledge_id 改名 biz_id、
--                          ADD file_id / biz_type / del_flag、
--                          重建索引 uk_biz_file / idx_biz / idx_file_id、删 idx_knowledge_id
--
-- 幂等性:
--   MySQL 8 不支持 DROP COLUMN IF EXISTS，故所有变更均由 information_schema 预检包裹。
--   本脚本【可重复执行】：第二次执行时全部条件分支判定为「已完成」，不报错、不改动结构。
--
-- 前置条件（脚本内含硬断言，违反即中止）:
--   ① aik_sys_file / aik_sys_attachment 必须均为 0 行（SDD §2.3.3 硬断言 ①）。
--     原因：新增的 file_id / biz_type 是 NOT NULL 且无默认值，非空表不可执行。
--   ② aik_sys_file.md5 上不得存在唯一约束（md5 去重是应用层约定，不是 DB 约束，SDD §2.5.4）。
--   ③ 目标库已由客户端选中（脚本不写 USE，避免锁死库名）。
--
-- 执行方式（WSL 内 127.0.0.1:3306 不通，必须用 172.29.48.1）:
--   mysql -h172.29.48.1 -uroot -p aik_steins_grimoire < sql/attachment_two_layer_migration.sql
--
-- 回滚凭据（结构备份，两表 0 行无需备份数据）:
--   mysqldump -h172.29.48.1 -uroot -p --no-data aik_steins_grimoire aik_sys_file aik_sys_attachment \
--     > attachment_pre_migration.sql
--
-- 兜底路径：两表 0 行时，DROP TABLE + 重建（sql/aik_system_tables.sql 的 CREATE TABLE）等价。
--   本脚本存在的意义是「万一有数据」时也有一条可复核的路径（见 SDD §2.3.1 决策记录）。
-- =============================================================================

DROP PROCEDURE IF EXISTS aik_migrate_attachment_two_layer;

DELIMITER $$

CREATE PROCEDURE aik_migrate_attachment_two_layer()
BEGIN
    DECLARE v_schema    VARCHAR(64) DEFAULT DATABASE();
    DECLARE v_file_rows BIGINT DEFAULT 0;
    DECLARE v_att_rows  BIGINT DEFAULT 0;
    DECLARE v_bad_idx   INT    DEFAULT 0;

    -- =========================================================================
    -- ⓪ 前置硬断言（违反即中止，不产生任何结构变更）
    -- =========================================================================

    -- ⓪-1 两表必须为空
    --      断言失败时 mysql 客户端会立即中止执行，故本存储过程会残留在库中，需手工清理：
    --      DROP PROCEDURE IF EXISTS aik_migrate_attachment_two_layer;
    SELECT COUNT(*) INTO v_file_rows FROM aik_sys_file;
    SELECT COUNT(*) INTO v_att_rows  FROM aik_sys_attachment;

    IF v_file_rows > 0 OR v_att_rows > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ABORT: aik_sys_file / aik_sys_attachment 非空，本脚本仅适用于 0 行表（SDD §2.3.3 硬断言①）';
    END IF;

    -- ⓪-2 md5 上不得有唯一约束（甲方案：md5 去重是应用层约定，非 DB 约束）
    --      此处只做断言，不做删除：出现唯一约束说明基线漂移，需要人工裁决，脚本不得静默改写设计。
    SELECT COUNT(DISTINCT s.INDEX_NAME) INTO v_bad_idx
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = v_schema
      AND s.TABLE_NAME = 'aik_sys_file'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
      AND s.COLUMN_NAME = 'md5'
      AND (SELECT COUNT(*) FROM information_schema.STATISTICS t
            WHERE t.TABLE_SCHEMA = s.TABLE_SCHEMA
              AND t.TABLE_NAME = s.TABLE_NAME
              AND t.INDEX_NAME = s.INDEX_NAME) = 1;

    IF v_bad_idx > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ABORT: aik_sys_file.md5 上存在唯一索引，违反 SDD §2.5.4 甲方案（md5 去重为应用层约定），请先人工裁决';
    END IF;

    -- =========================================================================
    -- A. aik_sys_file：删 url、删 del_flag（改物理删除）、对齐 COMMENT
    -- =========================================================================

    -- A1. DROP COLUMN url（该列无单列索引，无需处理索引）
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_file'
                  AND COLUMN_NAME = 'url') THEN
        ALTER TABLE aik_sys_file DROP COLUMN url;
    END IF;

    -- A2. DROP COLUMN del_flag
    --      注意：MySQL 删除列时会【自动连带删除只依赖该列的索引】，因此【不要】写
    --      DROP INDEX idx_del_flag —— 显式写会在重跑时直接报
    --      ERROR 1091 (Can't DROP 'idx_del_flag'; check that column/key exists)。
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_file'
                  AND COLUMN_NAME = 'del_flag') THEN
        ALTER TABLE aik_sys_file DROP COLUMN del_flag;
    END IF;

    -- A3. 幂等兜底：仅当 A2 执行到一半中断、留下孤立 idx_del_flag 时清理。
    --      正常路径下 A2 已连带删除，此分支不命中。
    IF EXISTS (SELECT 1 FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_file'
                  AND INDEX_NAME = 'idx_del_flag') THEN
        ALTER TABLE aik_sys_file DROP INDEX idx_del_flag;
    END IF;

    -- A4. 列定义/注释对齐：使迁移结果与全新安装的 CREATE TABLE 逐字段 COMMENT 等价
    --      （SDD §2.3.1「COMMENT 等价性」）。幂等：反复 MODIFY 结果相同。
    ALTER TABLE aik_sys_file
        MODIFY COLUMN original_name VARCHAR(255) NOT NULL COMMENT '原始文件名（首次上传者）',
        MODIFY COLUMN stored_name   VARCHAR(255) NOT NULL COMMENT '存储文件名（Snowflake ID）',
        MODIFY COLUMN file_path     VARCHAR(512) NOT NULL COMMENT '相对存储路径（存储层内部标识，不对外暴露）',
        MODIFY COLUMN md5           VARCHAR(32)          COMMENT '文件MD5哈希（内容寻址锚点）';

    -- A5. idx_md5 必须存在（非唯一）：秒传查重依赖它，删了会退化为全表扫描
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_file'
                      AND INDEX_NAME = 'idx_md5') THEN
        ALTER TABLE aik_sys_file ADD KEY idx_md5 (md5);
    END IF;

    -- =========================================================================
    -- B. aik_sys_attachment：重写为通用挂载表
    -- =========================================================================

    -- B1. DROP COLUMN attach_url（真冗余：字节定位由 file_id → aik_sys_file.file_path 承担）
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_attachment'
                  AND COLUMN_NAME = 'attach_url') THEN
        ALTER TABLE aik_sys_attachment DROP COLUMN attach_url;
    END IF;

    -- B2. knowledge_id 改名 biz_id
    --      【必须在 B3 之前执行】：索引 idx_knowledge_id 建在 knowledge_id 上，
    --      CHANGE COLUMN 会保留该索引（索引名不变，指向改名后的列），索引不会随列改名自动消失。
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_attachment'
                  AND COLUMN_NAME = 'knowledge_id') THEN
        ALTER TABLE aik_sys_attachment
            CHANGE COLUMN knowledge_id biz_id BIGINT NOT NULL
                COMMENT '业务主键：biz_type=knowledge 时为 aik_knowledge.id';
    END IF;

    -- B3. DROP INDEX idx_knowledge_id（列已改名，索引残留；列已不存在故必须显式删除）
    IF EXISTS (SELECT 1 FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_attachment'
                  AND INDEX_NAME = 'idx_knowledge_id') THEN
        ALTER TABLE aik_sys_attachment DROP INDEX idx_knowledge_id;
    END IF;

    -- B4. ADD file_id / biz_type（列序：id, file_id, biz_type, biz_id, ...）
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_attachment'
                      AND COLUMN_NAME = 'file_id') THEN
        ALTER TABLE aik_sys_attachment
            ADD COLUMN file_id BIGINT NOT NULL
                COMMENT '被挂载的文件对象ID，逻辑关联 aik_sys_file.id' AFTER id;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_attachment'
                      AND COLUMN_NAME = 'biz_type') THEN
        ALTER TABLE aik_sys_attachment
            ADD COLUMN biz_type VARCHAR(32) NOT NULL
                COMMENT '业务类型：knowledge（取值登记见 SDD §2.5.5）' AFTER file_id;
    END IF;

    -- B5. ADD del_flag（卸载标记，AFTER sort_order）
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_attachment'
                      AND COLUMN_NAME = 'del_flag') THEN
        ALTER TABLE aik_sys_attachment
            ADD COLUMN del_flag TINYINT NOT NULL DEFAULT 0
                COMMENT '卸载标记：0-有效挂载，1-已卸载' AFTER sort_order;
    END IF;

    -- B6. attach_name 注释对齐 + 表 COMMENT 由 '知识附件表'/'系统附件表' 改为通用语义
    ALTER TABLE aik_sys_attachment
        MODIFY COLUMN attach_name VARCHAR(256) NOT NULL COMMENT '用户可见文件名（权威）',
        COMMENT = '通用附件挂载表';

    -- B7. 索引重建（顺序：uk_biz_file → idx_biz → idx_file_id）
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_attachment'
                      AND INDEX_NAME = 'uk_biz_file') THEN
        ALTER TABLE aik_sys_attachment ADD UNIQUE KEY uk_biz_file (biz_type, biz_id, file_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_attachment'
                      AND INDEX_NAME = 'idx_biz') THEN
        ALTER TABLE aik_sys_attachment ADD KEY idx_biz (biz_type, biz_id, del_flag, sort_order);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = v_schema AND TABLE_NAME = 'aik_sys_attachment'
                      AND INDEX_NAME = 'idx_file_id') THEN
        ALTER TABLE aik_sys_attachment ADD KEY idx_file_id (file_id);
    END IF;
END$$

DELIMITER ;

CALL aik_migrate_attachment_two_layer();
DROP PROCEDURE IF EXISTS aik_migrate_attachment_two_layer;

-- =============================================================================
-- 迁移后自检（只读，SELECT 输出供人工/CI 比对；不修改任何状态）
-- 验收标准：列集合/列序与索引集合须与 sql/aik_system_tables.sql 的 CREATE TABLE 完全一致
-- =============================================================================

-- ① 两表列现状（对照 SDD §2.3.3 查询②）
SELECT TABLE_NAME, ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE,
       IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('aik_sys_file', 'aik_sys_attachment')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- ② 两表索引现状（对照 SDD §2.3.3 查询③；idx_md5 必须存在且 NON_UNIQUE=1）
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('aik_sys_file', 'aik_sys_attachment')
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- ③ 表 COMMENT
SELECT TABLE_NAME, TABLE_COMMENT, TABLE_ROWS
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('aik_sys_file', 'aik_sys_attachment');
