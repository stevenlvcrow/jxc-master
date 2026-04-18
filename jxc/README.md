# jxc

基于 `Spring Boot 4.0.5 + MyBatis-Plus 3.5.16 + PostgreSQL 17 + Redis 7` 的后端工程。

## 分层结构

```text
src/main/java/com/boboboom/jxc
├── common                     # 通用响应、异常处理
├── config                     # 基础配置
└── system                     # 示例业务模块
    ├── application            # 业务编排层：命令、DTO、应用服务
    ├── domain                 # 业务抽象层：仓储接口、业务规则
    ├── infrastructure         # 基础设施层：DO、Mapper、仓储实现
    └── interfaces             # 接口层：REST API
``` 

## 运行方式

默认使用 `PostgreSQL + Redis`：

```bash
mvn spring-boot:run
```

如果本机 `8080` 端口经常被占用，可使用仓库脚本在启动前自动结束占用进程：

```bash
# Linux / macOS
bash scripts/start-backend.sh

# Windows PowerShell
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
```

也支持透传 Maven 运行参数：

```bash
bash scripts/start-backend.sh -Dspring-boot.run.arguments=--spring.profiles.active=dev
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1 -MavenArgs "-Dspring-boot.run.arguments=--spring.profiles.active=dev"
```

默认连接参数已按当前环境预置，可通过环境变量覆盖：

```bash
DB_HOST=192.168.88.168
DB_PORT=5432
DB_NAME=jxc
DB_USERNAME=lizhijie
DB_PASSWORD=123456
REDIS_HOST=192.168.88.168
REDIS_PORT=6379
REDIS_PASSWORD=123456
REDIS_DATABASE=0
```

为后续建表和缓存接入，已经补充两个基础工具：

- [PostgresDdlExecutor.java](src/main/java/com/boboboom/jxc/infrastructure/support/PostgresDdlExecutor.java)：执行单条/批量 DDL、按当前 schema 检查表是否存在、读取当前 schema
- [RedisOperator.java](src/main/java/com/boboboom/jxc/infrastructure/support/RedisOperator.java)：基础 `set/get/delete/ping` 操作封装

访问示例：

```bash
curl http://localhost:8080/api/users
curl -X POST http://localhost:8080/api/users ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"alice\",\"phone\":\"13900000000\"}"
```

测试环境与运行环境统一使用 `PostgreSQL`，执行测试前请确保数据库连接可用：

```bash
mvn test
```

## 分层约定

- `interfaces` 负责协议适配，例如 REST 入参和出参。
- `application` 负责业务流程编排、事务边界和权限判断。
- `domain` 负责仓储接口等业务抽象。
- `infrastructure` 负责 MyBatis-Plus 落库实现和数据映射，也承载 PostgreSQL、Redis 等外部资源适配。


服务端开发统一标准见 [doc/服务端开发统一标准-基于当前项目.md](doc/服务端开发统一标准.md)。

## 采购入库流程接入

- 采购入库业务编码固定为 `PURCHASE_INBOUND`
- 在“流程管理”里创建并发布对应流程后，再把 `PURCHASE_INBOUND` 这个业务编码绑定到已发布的流程模板
- 采购入库单保存时会自动发起流程实例，批量审核时会推进当前待办节点
- 如果未配置采购入库流程，系统会回退到原有的直接审核逻辑
