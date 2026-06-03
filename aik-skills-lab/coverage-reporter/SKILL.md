---
name: coverage-reporter
description: 为Java Spring Boot项目生成测试覆盖率报告，使用JaCoCo插件统计行覆盖率和分支覆盖率。生成可视化报告并标识未覆盖代码，帮助团队了解测试覆盖情况。建议初次阈值：行覆盖60%，分支覆盖50%。
type: Skill
version: 1.0.0
---

# Coverage Reporter

## Purpose

为Java Spring Boot项目配置和生成测试覆盖率报告，使用JaCoCo工具统计行覆盖率和分支覆盖率，标识未覆盖的代码区域。

## When to Use

- 需要了解当前测试覆盖情况
- 需要配置CI/CD覆盖率检查
- 需要识别未测试的代码区域
- 需要设定和监控覆盖率阈值

## Coverage Thresholds

建议采用渐进式阈值策略：

| 阶段 | 行覆盖率 | 分支覆盖率 | 说明 |
|------|----------|------------|------|
| 初期 | 60% | 50% | 避免过重负担，先建立测试习惯 |
| 中期 | 70% | 60% | 核心业务逻辑充分覆盖 |
| 长期 | 80% | 70% | 关键模块达到高覆盖 |

## JaCoCo Configuration

### Maven 配置

```xml
<build>
    <plugins>
        <!-- JaCoCo插件 -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <id>prepare-agent</id>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>BUNDLE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.60</minimum>
                                    </limit>
                                    <limit>
                                        <counter>BRANCH</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.50</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 排除规则

```xml
<configuration>
    <excludes>
        <!-- 排除实体类 -->
        <exclude>**/entity/**</exclude>
        <exclude>**/dto/**</exclude>
        <exclude>**/vo/**</exclude>
        
        <!-- 排除配置类 -->
        <exclude>**/config/**</exclude>
        
        <!-- 排除启动类 -->
        <exclude>**/*Application.class</exclude>
        
        <!-- 排除异常类 -->
        <exclude>**/exception/**</exclude>
        
        <!-- 排除常量枚举 -->
        <exclude>**/constant/**</exclude>
        <exclude>**/enums/**</exclude>
        
        <!-- 排除MyBatis-Plus生成的Mapper -->
        <exclude>**/mapper/*Mapper.class</exclude>
    </excludes>
</configuration>
```

## Report Generation

### 生成报告命令

```bash
# 运行测试并生成覆盖率报告
mvn clean test jacoco:report

# 仅生成报告（测试已运行过）
mvn jacoco:report

# 运行测试并检查阈值
mvn clean test jacoco:check

# 生成所有报告（包括聚合报告，多模块项目）
mvn clean verify
```

### 报告位置

```
target/
├── site/
│   └── jacoco/              # HTML可视化报告
│       ├── index.html       # 总览页面
│       ├── com.example/     # 包级报告
│       └── jacoco.csv       # CSV格式数据
└── jacoco.exec              # 原始执行数据
```

## Report Interpretation

### HTML报告结构

```
index.html
├── 总体统计
│   ├── Missed Instructions (指令覆盖率)
│   ├── Missed Branches (分支覆盖率)
│   ├── Missed Cxty (圈复杂度)
│   ├── Missed Lines (行覆盖率)
│   └── Missed Methods (方法覆盖率)
│
├── 包级统计
│   └── 每个包的覆盖率汇总
│
└── 类级详情
    └── 每个类的逐行覆盖情况
        ├── 绿色：已覆盖
        ├── 红色：未覆盖
        └── 黄色：部分覆盖（分支）
```

### 关键指标说明

| 指标 | 说明 | 重点关注 |
|------|------|----------|
| Line Coverage | 行覆盖率 | 基础指标，建议>60% |
| Branch Coverage | 分支覆盖率 | 更重要，反映if/else覆盖 |
| Method Coverage | 方法覆盖率 | 方法是否被调用 |
| Class Coverage | 类覆盖率 | 类是否被实例化 |
| Complexity | 圈复杂度 | 高复杂度难测试，需重构 |

## Coverage Analysis Report

生成覆盖率分析报告，包含：

```markdown
# 测试覆盖率分析报告

## 总体情况

| 指标 | 覆盖率 | 阈值 | 状态 |
|------|--------|------|------|
| 行覆盖率 | 65% | 60% | 通过 |
| 分支覆盖率 | 52% | 50% | 通过 |
| 方法覆盖率 | 70% | - | - |

## 模块覆盖情况

| 模块 | 行覆盖 | 分支覆盖 | 未覆盖行数 | 风险等级 |
|------|--------|----------|------------|----------|
| order-service | 78% | 65% | 120 | 低 |
| user-service | 45% | 38% | 340 | 高 |
| product-service | 62% | 55% | 210 | 中 |

## 未覆盖代码Top 10

| 类名 | 方法名 | 未覆盖行 | 建议 |
|------|--------|----------|------|
| OrderServiceImpl | calculateDiscount | 15-30 | 添加边界值测试 |
| PaymentService | processRefund | 45-60 | 添加异常场景测试 |
| ... | ... | ... | ... |

## 改进建议

1. **user-service** 覆盖率偏低，建议优先补充核心业务流程测试
2. **calculateDiscount** 方法分支复杂，建议拆分或增加参数化测试
3. ...
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Test Coverage

on: [push, pull_request]

jobs:
  coverage:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 8
        uses: actions/setup-java@v3
        with:
          java-version: '8'
          distribution: 'temurin'
      
      - name: Run tests with coverage
        run: mvn clean test jacoco:report
      
      - name: Upload coverage report
        uses: actions/upload-artifact@v3
        with:
          name: coverage-report
          path: target/site/jacoco/
      
      - name: Check coverage thresholds
        run: mvn jacoco:check
```

### GitLab CI

```yaml
test:coverage:
  stage: test
  script:
    - mvn clean test jacoco:report
  coverage: '/Total.*?([0-9]{1,3})%/'  # 提取覆盖率百分比
  artifacts:
    reports:
      coverage_report:
        coverage_format: cobertura
        path: target/site/jacoco/coverage.xml
    paths:
      - target/site/jacoco/
```

## Best Practices

### DO

- 设定合理的初始阈值（60%/50%），逐步提高
- 关注分支覆盖率，比行覆盖率更能反映测试质量
- 优先覆盖核心业务逻辑和复杂计算
- 将覆盖率检查集成到CI流程
- 定期审查未覆盖代码，识别测试遗漏

### DON'T

- 不要盲目追求100%覆盖率
- 不要为了覆盖率而写无意义的测试
- 不要对getter/setter/配置类要求覆盖
- 不要忽视低覆盖率模块的技术债务

## Troubleshooting

### 覆盖率报告为空

检查：
1. `mvn clean` 后是否运行了测试
2. JaCoCo agent是否正确配置
3. 是否有排除规则过滤了所有类

### 阈值检查失败

```bash
# 临时跳过检查（不推荐长期使用）
mvn test -Djacoco.skip=true

# 或调整阈值
mvn test -Djacoco.line.minimum=0.50
```

### 多模块项目报告

```xml
<!-- 父pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>report-aggregate</id>
            <phase>verify</phase>
            <goals>
                <goal>report-aggregate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
