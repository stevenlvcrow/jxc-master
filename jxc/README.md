# jxc

基于 `Spring Boot 4.0.5 + MyBatis-Plus 3.5.16 + PostgreSQL 17 + Redis 7` 的 DDD 脚手架。

## 分层结构

```text
src/main/java/com/example/jxc
├── common                     # 通用响应、异常处理
├── config                     # 基础配置
└── system                     # 示例限界上下文
    ├── application            # 应用层：命令、DTO、应用服务
    ├── domain                 # 领域层：聚合、仓储接口、领域服务
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

- [PostgresDdlExecutor.java](C:/Users/Administrator/IdeaProjects/jxc/src/main/java/com/example/jxc/infrastructure/support/PostgresDdlExecutor.java)：执行单条/批量 DDL、检查表是否存在、读取当前 schema
- [RedisOperator.java](C:/Users/Administrator/IdeaProjects/jxc/src/main/java/com/example/jxc/infrastructure/support/RedisOperator.java)：基础 `set/get/delete/ping` 操作封装

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

## DDD 约定

- `application` 负责事务编排和 DTO 转换，不承载持久化细节。
- `domain` 保持纯业务模型，仓储以接口形式暴露。
- `infrastructure` 负责 MyBatis-Plus 落库实现和数据映射。
- `infrastructure` 同时承载外部资源适配，例如 PostgreSQL、Redis。
- `interfaces` 负责协议适配，例如 REST 入参和出参。
