# 系统设计文档 (SDD) — system 模块

> 生成时间: 2026-05-15
> 版本: v1.0
> 基于 PRD: v1.0

---

## 1. 架构设计

### 1.1 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| Web | Spring Boot | 2.7.18 | Web MVC |
| ORM | MyBatis-Plus | 3.5.5 | CRUD + 分页 |
| DB | MySQL | 8.0 | 关系型数据库 |
| Pool | Druid | 1.2.20 | 连接池 |
| Tools | Hutool | 5.8.25 | 工具库 |
| API Doc | SpringDoc | 1.7.0 | OpenAPI |
| JSON | Fastjson | 2.0.43 | 序列化 |
| Validation | JSR-303 | - | 参数校验 |

### 1.2 包结构

```
io.aik.steins.grimoire.system/
├── controller/                    # REST 接口层
│   ├── DictTypeController.java
│   ├── DictItemController.java
│   ├── SystemParamController.java
│   └── FileController.java
├── service/                       # 业务接口层
│   ├── DictTypeService.java
│   ├── DictItemService.java
│   ├── SystemParamService.java
│   └── FileService.java
│   └── impl/                      # 业务实现层
│       ├── DictTypeServiceImpl.java
│       ├── DictItemServiceImpl.java
│       ├── SystemParamServiceImpl.java
│       └── FileServiceImpl.java
├── dao/                           # 数据访问层
│   ├── DictTypeMapper.java
│   ├── DictItemMapper.java
│   ├── SystemParamMapper.java
│   └── FileMapper.java
│   └── mapping/                   # XML 映射文件
│       ├── DictTypeMapper.xml
│       ├── DictItemMapper.xml
│       ├── SystemParamMapper.xml
│       └── FileMapper.xml
└── common/                        # 模块内公共组件
    ├── po/                        # 实体类（继承 BaseEntity）
    │   ├── DictTypePo.java
    │   ├── DictItemPo.java
    │   ├── SystemParamPo.java
    │   └── FileRecordPo.java
    ├── dto/                       # 数据传输对象
    │   ├── DictTypeQuery.java
    │   ├── DictItemQuery.java
    │   ├── SystemParamQuery.java
    │   ├── FileQuery.java
    │   ├── DictTypeDto.java
    │   ├── DictItemDto.java
    │   ├── SystemParamDto.java
    │   └── DictItemListDto.java
    ├── vo/                        # 视图对象
    │   ├── DictTypeVo.java
    │   ├── DictItemVo.java
    │   ├── SystemParamVo.java
    │   ├── FileVo.java
    │   └── DictTypeItemsVo.java
    ├── constant/                  # 模块常量
    │   └── SystemConstant.java
    └── enums/                     # 模块枚举
        └── SystemParamGroupEnum.java
```

### 1.3 分层职责

| 层级 | 职责 | 约束 |
|------|------|------|
| Controller | 接收请求、参数校验、调用 Service、返回响应 | 仅做参数校验和结果封装，不含业务逻辑 |
| Service | 业务逻辑编排、事务控制、缓存管理 | 不允许直接操作 Mapper，通过接口调用 |
| Mapper | 数据访问、SQL 执行 | 复杂查询写 XML，简单查询用注解 |
| PO | 数据库实体映射 | 仅用于数据持久化，禁止跨层传递 |
| DTO | 接口入参封装 | 含校验注解，Service 层入参 |
| VO | 接口出参封装 | 仅用于响应，可含嵌套结构 |

### 1.4 通用组件复用清单

| 组件 | 来源 | 说明 |
|------|------|------|
| BaseEntity | core/po | id, createTime, modifyTime |
| BaseMetaObjectHandler | core/config | 自动填充时间字段 |
| ApiResponse | core/dto | 统一返回封装 |
| PageQuery | core/dto | 分页查询参数 |
| ResultCode | core/enums | 响应码枚举 |
| BusinessException | core/exception | 业务异常 |
| AssertUtils | core/utils | 业务断言 |
| FileStorageConfig | core/config | 文件存储配置项 |

---

## 2. 数据库设计

### 2.1 ER 关系

```
aik_dict_type (1) --------< (N) aik_dict_item
     | dict_code                 | dict_code (逻辑外键)
     |                           |
     | id PK                     | id PK
     | dict_code UK              | dict_code + item_code UK
     | dict_name                 | item_code
     | description               | item_name
     | status                    | sort_order
     | create_time               | status
     | modify_time               | remark
                               | create_time
                               | modify_time

aik_system_param              aik_file_record
     | id PK                       | id PK
     | param_key UK                | original_name
     | param_value                 | stored_name
     | description                 | file_path
     | param_group                 | file_size
     | editable                    | file_type
     | create_time                 | download_count
     | modify_time                 | create_time
                                   | modify_time
```

**关系说明**:
- `aik_dict_type` 与 `aik_dict_item` 为 **一对多** 关系，通过 `dict_code` 逻辑关联（无外键约束，应用层校验）
- `aik_system_param` 和 `aik_file_record` 为独立表，无关联关系

### 2.2 表结构设计

#### aik_dict_type（字典类型表）

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|---------|--------|------|
| id | BIGINT | 20 | 否 | - | 主键，Snowflake ID |
| dict_code | VARCHAR | 64 | 否 | - | 字典编码，UK |
| dict_name | VARCHAR | 128 | 否 | - | 字典名称 |
| description | VARCHAR | 512 | 是 | - | 描述 |
| status | TINYINT | 1 | 否 | 1 | 状态：1-启用，0-禁用 |
| create_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| modify_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 修改时间 |

**索引**:
```sql
PRIMARY KEY (id),
UNIQUE KEY uk_dict_code (dict_code),
KEY idx_status (status)
```

#### aik_dict_item（字典项表）

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|---------|--------|------|
| id | BIGINT | 20 | 否 | - | 主键，Snowflake ID |
| dict_code | VARCHAR | 64 | 否 | - | 字典类型编码 |
| item_code | VARCHAR | 64 | 否 | - | 字典项编码 |
| item_name | VARCHAR | 128 | 否 | - | 字典项名称 |
| sort_order | INT | 11 | 否 | 0 | 排序号 |
| status | TINYINT | 1 | 否 | 1 | 状态：1-启用，0-禁用 |
| remark | VARCHAR | 512 | 是 | - | 备注 |
| create_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| modify_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 修改时间 |

**索引**:
```sql
PRIMARY KEY (id),
UNIQUE KEY uk_dict_item (dict_code, item_code),
KEY idx_dict_code (dict_code),
KEY idx_status (status),
KEY idx_sort_order (sort_order)
```

#### aik_system_param（系统参数表）

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|---------|--------|------|
| id | BIGINT | 20 | 否 | - | 主键，Snowflake ID |
| param_key | VARCHAR | 128 | 否 | - | 参数键，UK |
| param_value | VARCHAR | 2048 | 否 | - | 参数值 |
| description | VARCHAR | 512 | 是 | - | 描述 |
| param_group | VARCHAR | 64 | 是 | - | 分组 |
| editable | TINYINT | 1 | 否 | 1 | 是否可编辑：1-是，0-否 |
| create_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| modify_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 修改时间 |

**索引**:
```sql
PRIMARY KEY (id),
UNIQUE KEY uk_param_key (param_key),
KEY idx_param_group (param_group)
```

#### aik_file_record（文件记录表）

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|---------|--------|------|
| id | BIGINT | 20 | 否 | - | 主键，Snowflake ID |
| original_name | VARCHAR | 255 | 否 | - | 原始文件名 |
| stored_name | VARCHAR | 255 | 否 | - | 存储文件名（UUID） |
| file_path | VARCHAR | 512 | 否 | - | 相对存储路径 |
| file_size | BIGINT | 20 | 否 | - | 文件大小（字节） |
| file_type | VARCHAR | 128 | 是 | - | MIME 类型 |
| download_count | INT | 11 | 否 | 0 | 下载次数 |
| create_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| modify_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 修改时间 |

**索引**:
```sql
PRIMARY KEY (id),
KEY idx_original_name (original_name),
KEY idx_create_time (create_time)
```

### 2.3 建表 SQL

```sql
-- 字典类型表
CREATE TABLE aik_dict_type (
    id BIGINT NOT NULL COMMENT '主键',
    dict_code VARCHAR(64) NOT NULL COMMENT '字典编码',
    dict_name VARCHAR(128) NOT NULL COMMENT '字典名称',
    description VARCHAR(512) COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_code (dict_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- 字典项表
CREATE TABLE aik_dict_item (
    id BIGINT NOT NULL COMMENT '主键',
    dict_code VARCHAR(64) NOT NULL COMMENT '字典类型编码',
    item_code VARCHAR(64) NOT NULL COMMENT '字典项编码',
    item_name VARCHAR(128) NOT NULL COMMENT '字典项名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_item (dict_code, item_code),
    KEY idx_dict_code (dict_code),
    KEY idx_status (status),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';

-- 系统参数表
CREATE TABLE aik_system_param (
    id BIGINT NOT NULL COMMENT '主键',
    param_key VARCHAR(128) NOT NULL COMMENT '参数键',
    param_value VARCHAR(2048) NOT NULL COMMENT '参数值',
    description VARCHAR(512) COMMENT '描述',
    param_group VARCHAR(64) COMMENT '参数分组',
    editable TINYINT NOT NULL DEFAULT 1 COMMENT '是否可编辑：1-是，0-否',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_param_key (param_key),
    KEY idx_param_group (param_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';

-- 文件记录表
CREATE TABLE aik_file_record (
    id BIGINT NOT NULL COMMENT '主键',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_name VARCHAR(255) NOT NULL COMMENT '存储文件名（UUID）',
    file_path VARCHAR(512) NOT NULL COMMENT '相对存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(128) COMMENT 'MIME类型',
    download_count INT NOT NULL DEFAULT 0 COMMENT '下载次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_original_name (original_name),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';
```

### 2.4 MyBatis-Plus 实体类

#### DictTypePo

```java
package io.aik.steins.grimoire.system.common.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 字典类型 -anchor
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("aik_dict_type")
public class DictTypePo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String dictCode;

    private String dictName;

    private String description;

    private Integer status;
}
```

#### DictItemPo

```java
package io.aik.steins.grimoire.system.common.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 字典项 -anchor
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("aik_dict_item")
public class DictItemPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String dictCode;

    private String itemCode;

    private String itemName;

    private Integer sortOrder;

    private Integer status;

    private String remark;
}
```

#### SystemParamPo

```java
package io.aik.steins.grimoire.system.common.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 系统参数 -anchor
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("aik_system_param")
public class SystemParamPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String paramKey;

    private String paramValue;

    private String description;

    private String paramGroup;

    private Integer editable;
}
```

#### FileRecordPo

```java
package io.aik.steins.grimoire.system.common.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 文件记录 -anchor
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("aik_file_record")
public class FileRecordPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String originalName;

    private String storedName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private Integer downloadCount;
}
```

---

## 3. 接口设计

### 3.1 API 列表

| 接口 | 方法 | URL | 说明 |
|------|------|-----|------|
| **字典类型** |
| 分页查询 | POST | /grimoire/dictType/findPage | 分页列表，支持模糊搜索 |
| 查询详情 | GET | /grimoire/dictType/findById | 根据 ID 查询 |
| 新增 | POST | /grimoire/dictType/add | 创建字典类型 |
| 修改 | POST | /grimoire/dictType/modify | 修改字典类型 |
| 删除 | POST | /grimoire/dictType/remove | 物理删除（需检查是否有字典项） |
| **字典项** |
| 按类型查询 | GET | /grimoire/dictItem/findListByType | 不分页，返回全部启用项 |
| 分页查询 | POST | /grimoire/dictItem/findPage | 分页列表，支持模糊搜索 |
| 新增 | POST | /grimoire/dictItem/add | 创建字典项 |
| 修改 | POST | /grimoire/dictItem/modify | 修改字典项 |
| 删除 | POST | /grimoire/dictItem/remove | 物理删除 |
| **系统参数** |
| 分页查询 | POST | /grimoire/systemParam/findPage | 分页列表，支持模糊搜索 |
| 根据键查询 | GET | /grimoire/systemParam/findByKey | 根据 param_key 查询 |
| 新增 | POST | /grimoire/systemParam/add | 创建系统参数 |
| 修改 | POST | /grimoire/systemParam/modify | 修改系统参数（热更新） |
| 删除 | POST | /grimoire/systemParam/remove | 物理删除 |
| 刷新缓存 | POST | /grimoire/systemParam/refreshCache | 手动刷新参数缓存 |
| **文件管理** |
| 上传 | POST | /grimoire/file/upload | multipart/form-data |
| 下载 | GET | /grimoire/file/download | 流式下载 |
| 分页查询 | POST | /grimoire/file/findPage | 分页列表 |
| 删除 | POST | /grimoire/file/remove | 物理删除文件+记录 |

### 3.2 DTO / VO 定义

#### DictTypeQuery（分页查询入参）

```java
@Data
@Schema(description = "字典类型查询参数")
public class DictTypeQuery extends PageQuery {
    @Schema(description = "字典编码（模糊）")
    private String dictCode;
    
    @Schema(description = "字典名称（模糊）")
    private String dictName;
    
    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;
}
```

#### DictTypeDto（新增/修改入参）

```java
@Data
@Schema(description = "字典类型")
public class DictTypeDto {
    @Schema(description = "ID，新增时为空")
    private Long id;
    
    @NotBlank(message = "字典编码不能为空")
    @Size(max = 64, message = "字典编码不能超过64字符")
    @Schema(description = "字典编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictCode;
    
    @NotBlank(message = "字典名称不能为空")
    @Size(max = 128, message = "字典名称不能超过128字符")
    @Schema(description = "字典名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictName;
    
    @Size(max = 512, message = "描述不能超过512字符")
    @Schema(description = "描述")
    private String description;
    
    @Schema(description = "状态：1-启用 0-禁用", example = "1")
    private Integer status;
}
```

#### DictTypeVo（出参）

```java
@Data
@Schema(description = "字典类型视图")
public class DictTypeVo {
    @Schema(description = "ID")
    private Long id;
    
    @Schema(description = "字典编码")
    private String dictCode;
    
    @Schema(description = "字典名称")
    private String dictName;
    
    @Schema(description = "描述")
    private String description;
    
    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;
}
```

#### DictItemQuery

```java
@Data
@Schema(description = "字典项查询参数")
public class DictItemQuery extends PageQuery {
    @Schema(description = "字典类型编码（精确）")
    private String dictCode;
    
    @Schema(description = "字典项编码（模糊）")
    private String itemCode;
    
    @Schema(description = "字典项名称（模糊）")
    private String itemName;
    
    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;
}
```

#### DictItemDto

```java
@Data
@Schema(description = "字典项")
public class DictItemDto {
    @Schema(description = "ID，新增时为空")
    private Long id;
    
    @NotBlank(message = "字典类型编码不能为空")
    @Schema(description = "字典类型编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictCode;
    
    @NotBlank(message = "字典项编码不能为空")
    @Schema(description = "字典项编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemCode;
    
    @NotBlank(message = "字典项名称不能为空")
    @Schema(description = "字典项名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemName;
    
    @Schema(description = "排序号", example = "0")
    private Integer sortOrder;
    
    @Schema(description = "状态：1-启用 0-禁用", example = "1")
    private Integer status;
    
    @Schema(description = "备注")
    private String remark;
}
```

#### DictItemVo

```java
@Data
@Schema(description = "字典项视图")
public class DictItemVo {
    @Schema(description = "ID")
    private Long id;
    
    @Schema(description = "字典类型编码")
    private String dictCode;
    
    @Schema(description = "字典项编码")
    private String itemCode;
    
    @Schema(description = "字典项名称")
    private String itemName;
    
    @Schema(description = "排序号")
    private Integer sortOrder;
    
    @Schema(description = "状态")
    private Integer status;
    
    @Schema(description = "备注")
    private String remark;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
```

#### DictTypeItemsVo（类型+项聚合）

```java
@Data
@Schema(description = "字典类型及字典项聚合")
public class DictTypeItemsVo {
    @Schema(description = "字典类型编码")
    private String dictCode;
    
    @Schema(description = "字典类型名称")
    private String dictName;
    
    @Schema(description = "字典项列表")
    private List<DictItemVo> items;
}
```

#### SystemParamQuery

```java
@Data
@Schema(description = "系统参数查询参数")
public class SystemParamQuery extends PageQuery {
    @Schema(description = "参数键（模糊）")
    private String paramKey;
    
    @Schema(description = "参数分组")
    private String paramGroup;
}
```

#### SystemParamDto

```java
@Data
@Schema(description = "系统参数")
public class SystemParamDto {
    @Schema(description = "ID，新增时为空")
    private Long id;
    
    @NotBlank(message = "参数键不能为空")
    @Schema(description = "参数键", requiredMode = Schema.RequiredMode.REQUIRED)
    private String paramKey;
    
    @NotBlank(message = "参数值不能为空")
    @Schema(description = "参数值", requiredMode = Schema.RequiredMode.REQUIRED)
    private String paramValue;
    
    @Schema(description = "描述")
    private String description;
    
    @Schema(description = "参数分组")
    private String paramGroup;
    
    @Schema(description = "是否可编辑：1-是 0-否", example = "1")
    private Integer editable;
}
```

#### SystemParamVo

```java
@Data
@Schema(description = "系统参数视图")
public class SystemParamVo {
    @Schema(description = "ID")
    private Long id;
    
    @Schema(description = "参数键")
    private String paramKey;
    
    @Schema(description = "参数值")
    private String paramValue;
    
    @Schema(description = "描述")
    private String description;
    
    @Schema(description = "参数分组")
    private String paramGroup;
    
    @Schema(description = "是否可编辑")
    private Integer editable;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
```

#### FileQuery

```java
@Data
@Schema(description = "文件查询参数")
public class FileQuery extends PageQuery {
    @Schema(description = "原始文件名（模糊）")
    private String originalName;
}
```

#### FileVo

```java
@Data
@Schema(description = "文件视图")
public class FileVo {
    @Schema(description = "ID")
    private Long id;
    
    @Schema(description = "原始文件名")
    private String originalName;
    
    @Schema(description = "存储文件名")
    private String storedName;
    
    @Schema(description = "文件路径")
    private String filePath;
    
    @Schema(description = "文件大小（字节）")
    private Long fileSize;
    
    @Schema(description = "MIME类型")
    private String fileType;
    
    @Schema(description = "下载次数")
    private Integer downloadCount;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
```

### 3.3 错误码定义

复用 `ResultCode` 枚举，新增业务错误码在 Service 层通过 `BusinessException` 抛出：

| 错误码 | 说明 | 使用场景 |
|--------|------|---------|
| 200 | 操作成功 | 通用成功 |
| 400 | 操作失败 / 参数错误 | 参数校验失败、业务规则违反 |
| 404 | 资源不存在 | 根据 ID 查询不到记录 |
| 500 | 服务器内部错误 | 异常未捕获 |

**业务异常场景**（msg 自定义）：
- 字典编码已存在
- 字典项编码在同一类型下已存在
- 参数键已存在
- 字典类型下存在字典项，不允许删除
- 文件不存在
- 文件存储失败

---

## 4. 核心流程设计

### 4.1 系统参数热更新流程

```
@PostConstruct
    |
    ▼
加载所有参数到 ConcurrentHashMap
    |
    ▼
┌────────────────────────────────────────┐
│         SystemParamService             │
│  ┌─────────────────────────────────┐   │
│  │  paramCache: ConcurrentHashMap  │   │
│  │  <String, String>               │   │
│  └─────────────────────────────────┘   │
└────────────────────────────────────────┘
    |
    ├── findByKey(key) ──► 直接从缓存读取 O(1)
    |
    ├── add(param) ──► 写入 DB ──► 刷新缓存
    |
    ├── modify(param) ──► 更新 DB ──► 刷新缓存
    |
    ├── remove(id) ──► 删除 DB ──► 刷新缓存
    |
    └── refreshCache() ──► 清空缓存 ──► 重新加载
```

**关键设计**：
- 使用 `ConcurrentHashMap` 保证线程安全
- `@PostConstruct` 初始化时全量加载
- 增删改操作：先写数据库，再刷新缓存（保证最终一致性）
- 无分布式锁（单实例应用，无需 Redis）

### 4.2 文件上传流程

```
Client
    |
    ▼
POST /grimoire/file/upload (MultipartFile)
    |
    ▼
FileController.upload()
    |
    ├── 校验：文件非空、大小不超过 max-size
    |
    ├── 生成：storedName = UUID + 原扩展名
    |
    ├── 计算：relativePath = yyyy/MM/dd/
    |
    ├── 存储：磁盘写入 {base-path}/{relativePath}/{storedName}
    |
    ├── 记录：FileRecordPo 入库
    |
    └── 返回：FileVo
```

### 4.3 文件删除流程

```
Client
    |
    ▼
POST /grimoire/file/remove?id={id}
    |
    ▼
FileController.remove()
    |
    ▼
FileService.remove()
    |
    ├── 查询 FileRecordPo
    |
    ├── 校验：记录存在
    |
    ├── 删除磁盘文件（如果存在）
    |
    └── 删除数据库记录
```

### 4.4 事务设计

| 场景 | 事务边界 | 说明 |
|------|---------|------|
| 字典类型删除 | `@Transactional` | 检查字典项数量 -> 删除类型，单操作 |
| 文件删除 | 无事务 | 先删磁盘再删记录，磁盘操作无法回滚 |
| 系统参数修改 | `@Transactional` | 更新 DB + 刷新缓存 |

---

## 5. 技术方案选型

### 5.1 参数缓存

| 方案 | 选择 | 理由 |
|------|------|------|
| ConcurrentHashMap | 采用 | 单实例应用，无需分布式缓存；读写 O(1) |
| @Cacheable (Caffeine) | 不采用 | 引入额外依赖，本项目轻量为主 |
| Redis | 不采用 | 个人项目，无需外部依赖 |

### 5.2 文件存储

| 方案 | 选择 | 理由 |
|------|------|------|
| 本地磁盘 | 采用 | 项目已配置，无外部依赖，可直接浏览 |
| 对象存储(OSS) | 不采用 | 个人项目，无需云服务依赖 |
| FastDFS/MinIO | 不采用 | 增加部署复杂度 |

### 5.3 异步处理

| 方案 | 选择 | 理由 |
|------|------|------|
| Spring @Async | 不采用 | 当前无异步场景 |
| CompletableFuture | 预留 | 未来扩展使用 |

---

## 6. 设计评审

### 6.1 评审结论

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 表结构合理性 | 通过 | 4 张表，字段精简，索引覆盖查询场景 |
| 关联关系正确性 | 通过 | 字典类型-字典项一对多逻辑关联 |
| 索引设计 | 通过 | 主键、唯一键、查询索引齐全 |
| API 设计规范性 | 通过 | 符合 aIk-coding-style 动词式规范 |
| DTO/VO 分离 | 通过 | 入参用 DTO，出参用 VO |
| 复用组件检查 | 通过 | 复用 BaseEntity、ApiResponse、PageQuery 等 |
| 事务边界 | 通过 | 关键操作有事务标注 |

### 6.2 Warnings

| # | 级别 | 问题 | 说明 |
|---|------|------|------|
| W01 | warning | 字典类型删除未做级联 | 仅检查是否有字典项，不级联删除字典项（符合物理删除策略） |
| W02 | warning | 文件上传无 MIME 白名单 | 个人项目不做限制，部署时由 Nginx 或防火墙限制 |

### 6.3 Failures

无。

### 6.4 风险点

| 风险 | 缓解措施 |
|------|---------|
| 系统参数缓存与数据库不一致 | 所有写操作后刷新缓存；提供手动刷新接口 |
| 文件删除时磁盘文件已不存在 | 删除前判断文件是否存在，不存在只删记录 |
| 大文件上传内存溢出 | 配置文件大小限制 10MB；Spring multipart 配置 |

---

## 7. 附录

### 7.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 表名 | aik_ 前缀，snake_case | aik_dict_type |
| 字段 | snake_case | dict_code, create_time |
| Java 类 | 驼峰 | DictTypePo, DictTypeVo |
| Mapper | XxxMapper | DictTypeMapper |
| Service | XxxService / XxxServiceImpl | DictTypeService |
| Controller | XxxController | DictTypeController |

### 7.2 项目复用清单

| 组件 | 来源 | 说明 |
|------|------|------|
| BaseEntity | core/po | id, createTime, modifyTime |
| ApiResponse | core/dto | 统一返回封装 |
| PageQuery | core/dto | 分页查询参数 |
| ResultCode | core/enums | 响应码枚举 |
| BusinessException | core/exception | 业务异常 |
| AssertUtils | core/utils | 业务断言 |
| FileStorageConfig | core/config | 文件存储配置 |
