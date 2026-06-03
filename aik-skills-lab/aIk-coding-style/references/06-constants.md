# 常量规范

> 来源：aIk-coding-style 规范 · 魔法值处理与常量存放位置

### 魔法值处理
- **禁止直接使用魔法值**（硬编码的数字、字符串）
- 根据常量作用域选择存放位置

### 常量存放位置

| 类型 | 存放位置 | 说明 |
|------|---------|------|
| 业务级常量 | `common/constant/` 包 | 多个类共享的常量 |
| 类内部常量 | 类中定义 `private static final` | 仅当前类使用 |
| 枚举值（仅值定义） | `common/constant/` 包 | 简单的枚举值常量 |
| 枚举类（完整枚举） | `common/enums/` 包 | 需要定义枚举类的场景 |

### 类内部常量示例
```java
@Slf4j
@Service("{module}.{Entity}Service")
public class {Entity}ServiceImpl extends ServiceImpl<{Entity}Mapper, {Entity}Po>
        implements {Entity}Service {

    //note 分页大小限制（仅本类使用）
    private static final int MAX_PAGE_SIZE = 100;
    
    //note 默认页码（仅本类使用）
    private static final int DEFAULT_PAGE = 1;

    @Override
    public Page<{Entity}Po> findPage({Entity}QueryDto queryDto) {
        long currentPage = Math.max(DEFAULT_PAGE, queryDto.getCurrent());
        long pageSize = Math.max(DEFAULT_PAGE, Math.min(queryDto.getSize(), MAX_PAGE_SIZE));
        // ...
    }
}
```

### 业务级常量示例
```java
package {package}.common.constant;

/**
 * -anchor {模块}业务常量
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
public final class {Entity}Constants {

    //note 状态：启用
    public static final int STATUS_ENABLE = 1;
    
    //note 状态：禁用
    public static final int STATUS_DISABLE = 0;
    
    //note 默认排序字段
    public static final String DEFAULT_SORT_FIELD = "create_time";

    private {Entity}Constants() {
        //note 私有构造，禁止实例化
    }
}
```

### 枚举类示例
```java
package {package}.common.enums;

import lombok.Getter;

/**
 * -anchor {模块}状态枚举
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
@Getter
public enum {Entity}StatusEnum {

    //note 启用状态
    ENABLE(1, "启用"),
    
    //note 禁用状态
    DISABLE(0, "禁用");

    private final int code;
    private final String desc;

    {Entity}StatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```
