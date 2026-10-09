# 测试覆盖率阈值与配置

## 渐进式阈值策略

| 阶段 | 行覆盖率 | 分支覆盖率 | 说明 |
|------|----------|------------|------|
| **初期** | 60% | 50% | 避免过重负担，先建立测试习惯 |
| **中期** | 70% | 60% | 核心业务逻辑充分覆盖 |
| **长期** | 80% | 70% | 关键模块达到高覆盖 |

## JaCoCo Maven 配置

```xml
<build>
    <plugins>
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

## 排除规则

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

## 关键指标说明

| 指标 | 说明 | 重点关注 |
|------|------|----------|
| Line Coverage | 行覆盖率 | 基础指标，建议 > 60% |
| Branch Coverage | 分支覆盖率 | 更重要，反映 if/else 覆盖 |
| Method Coverage | 方法覆盖率 | 方法是否被调用 |
| Class Coverage | 类覆盖率 | 类是否被实例化 |
| Complexity | 圈复杂度 | 高复杂度难测试，需重构 |

## 报告生成命令

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

## 报告位置

```
target/
├── site/
│   └── jacoco/              # HTML可视化报告
│       ├── index.html       # 总览页面
│       ├── com.example/     # 包级报告
│       └── jacoco.csv       # CSV格式数据
└── jacoco.exec              # 原始执行数据
```

## 阈值调整策略

```
如果初次覆盖率 < 40%：
  → 设定阈值 40%/30%，逐步提高
如果初次覆盖率 40-60%：
  → 设定阈值 60%/50%
如果初次覆盖率 > 60%：
  → 设定阈值 70%/60%
```

## 最佳实践

- 设定合理的初始阈值（60%/50%），逐步提高
- 关注分支覆盖率，比行覆盖率更能反映测试质量
- 优先覆盖核心业务逻辑和复杂计算
- 将覆盖率检查集成到 CI 流程
- 不要盲目追求 100% 覆盖率
- 不要为了覆盖率而写无意义的测试
- 不要对 getter/setter/配置类要求覆盖
