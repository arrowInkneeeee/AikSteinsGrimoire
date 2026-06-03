# Mapper规范

> 来源：aIk-coding-style 规范

## 13.1 SQL 编写原则

**优先使用 MyBatis-Plus API，复杂 SQL 使用 XML。**

| 方式 | 使用场景 | 说明 |
|------|---------|------|
| **MyBatis-Plus API** | 首选 | 简单 CRUD、条件查询、单表操作 |
| **XML** | 备选 | 复杂 SQL、多表关联、动态 SQL |

**不生成空 XML 文件：**
- 如果项目中没有复杂 SQL，**不生成 XML 文件**
- 保留 `dao/mapping/` 目录，但可以为空
- 需要时再添加 XML 文件

## 13.2 Mapper 接口规范

```java
package {package}.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * -anchor {Mapper描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
@Mapper
public interface {Entity}Mapper extends BaseMapper<{Entity}Po> {
    // 优先使用 MyBatis-Plus API 和注解方式
    // 极复杂 SQL 才使用 XML
}
```

## 13.3 MyBatis-Plus API 方式（推荐）

```java
// ServiceImpl 中使用 LambdaQueryWrapper
LambdaQueryWrapper<EntityPo> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(EntityPo::getStatus, 1)
       .like(StringUtils.isNotBlank(keyword), EntityPo::getName, keyword)
       .orderByDesc(EntityPo::getCreateTime);
List<EntityPo> list = this.list(wrapper);
```

## 13.4 XML 方式（复杂 SQL）

**仅在以下情况使用 XML：**
- 动态 SQL 过于复杂（如多表关联、嵌套查询）
- 需要使用 MyBatis 高级特性（如 `<resultMap>`、`<collection>`）
- 注解方式无法实现的 SQL 逻辑

**XML 文件位置：**
```
dao/
└── mapping/
    └── {Entity}Mapping.xml  # 仅在需要时创建
```

**示例：**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="{package}.dao.{Entity}Mapper">

    <!-- 仅在注解无法实现时使用 -->
    <select id="selectComplexData" resultType="{package}.common.vo.{Entity}Vo">
        SELECT
            t1.*,
            t2.name as relatedName
        FROM {table_name} t1
        LEFT JOIN other_table t2 ON t1.other_id = t2.id
        WHERE t1.status = #{status}
        <if test="keyword != null and keyword != ''">
            AND t1.name LIKE CONCAT('%', #{keyword}, '%')
        </if>
    </select>

</mapper>
```

## 13.5 Mapper规范总结

| 检查项 | 规范 |
|--------|------|
| **SQL 方式** | 优先 MyBatis-Plus API，复杂 SQL 用 XML |
| **空 XML** | 不生成空 XML 文件 |
| **复杂 SQL** | API 无法实现时（多表关联、动态 SQL）再用 XML |
| **目录保留** | 保留 `dao/mapping/` 目录，但可为空 |
