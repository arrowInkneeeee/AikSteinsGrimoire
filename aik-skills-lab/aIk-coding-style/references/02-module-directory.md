# 模块目录结构规范

> 来源：aIk-coding-style 规范 · 标准包结构与目录说明

### 标准包结构
```
{module}/
├── api/                          # API接口文档(.md)
│   └── {module}-api.md
├── common/                       # 公共包
│   ├── constant/                 # 常量定义
│   ├── dto/                      # 数据传输对象
│   │   ├── {Entity}Dto.java
│   │   └── {Entity}QueryDto.java
│   ├── enums/                    # 枚举类
│   │   └── {Entity}StatusEnum.java
│   ├── po/                       # 持久化对象(实体类)
│   │   └── {Entity}Po.java
│   ├── vo/                       # 视图对象
│   │   └── {Entity}Vo.java
│   ├── config/                   # 配置类
│   ├── exception/                # 自定义异常
│   └── utils/                    # 工具类
├── controller/                   # 控制器层
│   └── {Entity}Controller.java
├── dao/                          # 数据访问层
│   ├── {Entity}Mapper.java       # Mapper接口
│   └── mapping/                  # XML映射文件(可选)
│       └── {Entity}Mapping.xml   # 复杂SQL才需要
├── service/                      # 服务层
│   ├── {Entity}Service.java      # 服务接口
│   └── impl/                     # 服务实现
│       └── {Entity}ServiceImpl.java
├── sql/                          # 数据库脚本
│   └── {table_name}.sql
└── README.md                     # 模块说明文档
```

### 目录说明

| 目录 | 说明 |
|------|------|
| `api/` | API接口文档，Markdown格式 |
| `common/` | 公共包，与controller同级 |
| `common/constant/` | 常量定义，简单枚举值 |
| `common/dto/` | DTO数据传输对象 |
| `common/enums/` | 枚举类定义 |
| `common/po/` | PO持久化对象（原model） |
| `common/vo/` | VO视图对象 |
| `common/config/` | 模块配置类 |
| `common/exception/` | 自定义异常 |
| `common/utils/` | 工具类 |
| `controller/` | 控制器层 |
| `dao/` | Mapper接口 |
| `dao/mapping/` | XML映射文件，仅复杂SQL需要 |
| `service/` | 服务接口 |
| `service/impl/` | 服务实现类 |
| `sql/` | 数据库脚本 |

### 包命名规范
- 模块包名：`{basePackage}.{module}`
- Service Bean名称：
  - 无子模块：`@Service("{module}.{ServiceName}")`
  - 有子模块：`@Service("{module}.{subModule}.{ServiceName}")`
