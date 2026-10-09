# 单元测试类模板

```java
package com.{company}.{project}.{module}.service;

import com.{company}.{project}.{module}.entity.po.{Entity}Po;
import com.{company}.{project}.{module}.mapper.{Entity}Mapper;
import com.{company}.{project}.{module}.service.{Entity}Service;
import com.{company}.{project}.{module}.service.impl.{Entity}ServiceImpl;
import com.{company}.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * -anchor {被测类}单元测试
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote 使用 JUnit 5 + Mockito + AssertJ，Mock 所有外部依赖
 * @since {yyyy/MM/dd}
 * -
 **/
@ExtendWith(MockitoExtension.class)
@DisplayName("{被测类}测试")
class {ClassName}Test {

    // ==================== Mock 依赖 ====================

    @Mock
    private {Entity}Mapper {entity}Mapper;

    @Mock
    private {Other}Service {other}Service;

    @InjectMocks
    private {Entity}ServiceImpl {entity}Service;

    // ==================== 测试常量 ====================

    private static final Long TEST_ID = 1L;
    private static final Long TEST_USER_ID = 100L;
    private static final String TEST_NAME = "test-value";
    private static final BigDecimal TEST_AMOUNT = new BigDecimal("100.00");

    // ==================== 辅助方法 ====================

    /**
     * 构建测试用的实体对象
     */
    private {Entity}Po buildTestPo() {
        {Entity}Po po = new {Entity}Po();
        po.setId(TEST_ID);
        po.set{Field}(TEST_NAME);
        return po;
    }

    // ==================== 正常场景测试 ====================

    @Nested
    @DisplayName("创建{实体}")
    class Create{Entity}Test {

        @Test
        @DisplayName("正常创建成功，返回ID")
        void create_success() {
            // -anchor given
            {Entity}Po po = buildTestPo();
            when({entity}Mapper.insert(any({Entity}Po.class)))
                    .thenAnswer(invocation -> {
                        {Entity}Po arg = invocation.getArgument(0);
                        arg.setId(TEST_ID);
                        return 1;
                    });

            // -anchor when
            Long result = {entity}Service.create(po);

            // -anchor then
            assertThat(result).isEqualTo(TEST_ID);
            verify({entity}Mapper).insert(any({Entity}Po.class));
        }

        @Test
        @DisplayName("参数为null时抛出BusinessException")
        void create_nullParam_throwsException() {
            // -anchor given
            // -anchor when & then
            assertThatThrownBy(() -> {entity}Service.create(null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("参数不能为空");

            verify({entity}Mapper, never()).insert(any());
        }

        @Test
        @DisplayName("必填字段为空时抛出BusinessException")
        void create_missingRequiredField_throwsException() {
            // -anchor given
            {Entity}Po po = new {Entity}Po();
            // 故意不设必填字段

            // -anchor when & then
            assertThatThrownBy(() -> {entity}Service.create(po))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能为空");

            verify({entity}Mapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("查询{实体}")
    class Query{Entity}Test {

        @Test
        @DisplayName("根据ID查询成功")
        void getById_success() {
            // -anchor given
            {Entity}Po po = buildTestPo();
            when({entity}Mapper.selectById(TEST_ID)).thenReturn(po);

            // -anchor when
            {Entity}Po result = {entity}Service.getById(TEST_ID);

            // -anchor then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_ID);
            assertThat(result.get{Field}()).isEqualTo(TEST_NAME);
            verify({entity}Mapper).selectById(TEST_ID);
        }

        @Test
        @DisplayName("ID不存在时抛出BusinessException")
        void getById_notFound_throwsException() {
            // -anchor given
            when({entity}Mapper.selectById(TEST_ID)).thenReturn(null);

            // -anchor when & then
            assertThatThrownBy(() -> {entity}Service.getById(TEST_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不存在");
        }
    }

    @Nested
    @DisplayName("更新{实体}")
    class Update{Entity}Test {

        @Test
        @DisplayName("正常更新成功")
        void update_success() {
            // -anchor given
            {Entity}Po po = buildTestPo();
            when({entity}Mapper.selectById(TEST_ID)).thenReturn(po);
            when({entity}Mapper.updateById(any({Entity}Po.class))).thenReturn(1);

            // -anchor when
            boolean result = {entity}Service.update(po);

            // -anchor then
            assertThat(result).isTrue();
            verify({entity}Mapper).selectById(TEST_ID);
            verify({entity}Mapper).updateById(any({Entity}Po.class));
        }

        @Test
        @DisplayName("更新的数据不存在时抛出BusinessException")
        void update_notFound_throwsException() {
            // -anchor given
            {Entity}Po po = buildTestPo();
            when({entity}Mapper.selectById(TEST_ID)).thenReturn(null);

            // -anchor when & then
            assertThatThrownBy(() -> {entity}Service.update(po))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不存在");

            verify({entity}Mapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("删除{实体}")
    class Delete{Entity}Test {

        @Test
        @DisplayName("正常逻辑删除成功")
        void delete_success() {
            // -anchor given
            {Entity}Po po = buildTestPo();
            when({entity}Mapper.selectById(TEST_ID)).thenReturn(po);
            when({entity}Mapper.deleteById(TEST_ID)).thenReturn(1);

            // -anchor when
            boolean result = {entity}Service.deleteById(TEST_ID);

            // -anchor then
            assertThat(result).isTrue();
            verify({entity}Mapper).selectById(TEST_ID);
            verify({entity}Mapper).deleteById(TEST_ID);
        }
    }

    @Nested
    @DisplayName("列表查询{实体}")
    class List{Entity}Test {

        @Test
        @DisplayName("正常分页查询返回数据")
        void list_success() {
            // -anchor given
            List<{Entity}Po> poList = Arrays.asList(buildTestPo(), buildTestPo());
            when({entity}Mapper.selectList(any()))
                    .thenReturn(poList);

            // -anchor when
            List<{Entity}Po> result = {entity}Service.list();

            // -anchor then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            verify({entity}Mapper).selectList(any());
        }

        @Test
        @DisplayName("无数据时返回空列表")
        void list_empty() {
            // -anchor given
            when({entity}Mapper.selectList(any()))
                    .thenReturn(Collections.emptyList());

            // -anchor when
            List<{Entity}Po> result = {entity}Service.list();

            // -anchor then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }
}
```

## 使用说明

### 1. 替换占位符

| 占位符 | 替换为 |
|--------|--------|
| `{ClassName}` | 测试类名（如 `OrderServiceTest`） |
| `{Entity}` | 实体名（如 `Order`） |
| `{entity}` | 实体名小驼峰（如 `order`） |
| `{Field}` | 字段名（如 `OrderNo`） |
| `{company}` | 公司/组织名 |
| `{project}` | 项目名 |
| `{module}` | 模块名 |
| `{Other}` | 其他依赖服务名 |
| `{yyyy/MM/dd}` | 日期 |

### 2. 测试结构规范

- **类注释**: 必须使用 `-anchor` 格式，`@author a I k .`
- **@DisplayName**: 类和方法使用中文描述
- **测试步骤**: 使用 `-anchor given` / `-anchor when` / `-anchor then` 标记
- **Mock**: 所有外部依赖（Mapper、其他 Service）使用 `@Mock`
- **断言**: 使用 AssertJ 的 `assertThat()` 风格
- **验证**: 使用 Mockito 的 `verify()` 验证方法调用

### 3. 必须覆盖的场景

- [ ] 正常业务流程
- [ ] 参数校验（null、空值、边界值）
- [ ] 数据不存在
- [ ] 业务异常抛出
- [ ] 集合为空

### 4. 依赖引入

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

---

> 测试代码严格遵循 [aIk-coding-style](../../aIk-coding-style/SKILL.md) 规范。
