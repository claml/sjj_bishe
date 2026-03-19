# SJJ OJ 在线判题系统

## 1. 项目简介

本项目是一个前后端分离的在线判题系统（Online Judge），包含题库、提交判题、竞赛、帖子讨论、首页活动等功能。  
系统由 3 个核心服务组成：

- `oj-frontend`：用户前端（Vue 3）
- `oj-backend`：业务后端（Spring Boot）
- `sjjoj_code_sandbox`：代码运行沙箱服务（独立部署）

## 2. 核心功能

- 用户注册、登录、个人信息管理
- 题目管理、提交记录、判题结果
- 帖子与评论、点赞收藏
- 竞赛创建、报名、排行榜
- 首页活动管理
- 支持 `java / python / c / cpp` 判题

## 3. 技术栈

- 前端：Vue 3 + TypeScript + Vuex + Vue Router + Axios + Arco Design
- 后端：Spring Boot 2.7 + MyBatis-Plus + MySQL + Knife4j
- 判题沙箱：Spring Boot + 多语言本地执行（Java/Python/C/C++）
- 可选组件：Redis、Elasticsearch、腾讯云 COS、微信开放平台

## 4. 目录结构

```text
.
├─ oj-frontend            # 前端项目
├─ oj-backend             # 后端项目
│  ├─ sql
│  │  ├─ create_table.sql
│  │  └─ migration/*.sql
├─ sjjoj_code_sandbox     # 代码沙箱服务（推荐使用）
└─ sjjbox                 # 早期沙箱目录（与上面功能重叠）
```

## 5. 环境要求

- JDK 8
- Maven 3.8+
- Node.js 16+（建议 18 LTS）
- MySQL 8.x（至少 5.7）
- 可选：Redis 6+、Elasticsearch 7.x

默认端口：

- 前端开发服务：`8080`
- 后端（dev）：`8121`
- 后端（prod/test）：`8101`
- 代码沙箱：`8090`

## 6. 本地开发部署

### 6.1 初始化数据库

1. 创建数据库并导入主表：

```sql
source oj-backend/sql/create_table.sql
```

2. 按需执行增量脚本（建议全部执行一遍）：

- `oj-backend/sql/migration/contest_business.sql`
- `oj-backend/sql/migration/contest_invite_access.sql`
- `oj-backend/sql/migration/home_event_feature.sql`
- `oj-backend/sql/migration/post_interaction_deduplicate.sql`

### 6.2 启动代码沙箱（推荐先启动）

```bash
cd sjjoj_code_sandbox
mvn clean package -DskipTests
java -jar target/oj-code-sandbox-0.0.1-SNAPSHOT.jar
```

健康检查：

```bash
curl http://127.0.0.1:8090/codesandbox/healthCheck
```

后端默认会调用：

- `codesandbox.remote.url=http://127.0.0.1:8090/codesandbox/run`（代码中默认值）

如果你使用 `oj-backend/src/main/resources/application.yml` 中的远程地址，请改成你的实际沙箱地址。

### 6.3 启动后端

```bash
cd oj-backend
mvnw.cmd spring-boot:run
```

或打包运行：

```bash
cd oj-backend
mvnw.cmd clean package -DskipTests
java -jar target/oj-backend-0.0.1-SNAPSHOT.jar
```

接口文档：

- [http://localhost:8121/api/doc.html](http://localhost:8121/api/doc.html)

### 6.4 启动前端

```bash
cd oj-frontend
npm install
npm run serve
```

访问：

- [http://localhost:8080](http://localhost:8080)

## 7. 生产部署方式

### 7.1 后端部署

```bash
cd oj-backend
mvnw.cmd clean package -DskipTests
java -jar target/oj-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

配置文件：

- 公共配置：`oj-backend/src/main/resources/application.yml`
- 生产配置：`oj-backend/src/main/resources/application-prod.yml`

### 7.2 沙箱部署

建议独立进程或独立容器部署，保证与业务后端解耦。  
部署后确认后端配置项 `codesandbox.remote.url` 指向可访问的沙箱地址。

### 7.3 前端部署

```bash
cd oj-frontend
npm install
npm run build
```

将 `oj-frontend/dist` 发布到 Nginx/静态服务器。

## 8. 关键配置说明

### 8.1 必改项

- `oj-backend/src/main/resources/application.yml`
  - `spring.datasource.*`：MySQL 连接
  - `codesandbox.remote.url`：判题沙箱地址
  - `codesandbox.remote.auth-header` / `codesandbox.remote.auth-secret`：与沙箱保持一致

### 8.2 可选功能项

- Redis（分布式 Session）：
  1. 打开 `application.yml` 中 `spring.redis` 配置
  2. 打开 `spring.session.store-type: redis`
  3. 移除 `MainApplication` 中 `exclude = {RedisAutoConfiguration.class}`

- Elasticsearch（帖子搜索）：
  1. 配置 `spring.elasticsearch.*`
  2. 设置 `spring.data.elasticsearch.repositories.enabled=true`
  3. 按 `oj-backend/sql/post_es_mapping.json` 创建索引并执行同步任务

## 9. 部署注意事项

1. 前端 `oj-frontend/generated/core/OpenAPI.ts` 默认 `BASE` 是 `http://localhost:8121`，生产环境需要改成你的后端公网地址或可访问域名。
2. `oj-backend/Dockerfile` 中 jar 文件名写的是 `oj_backend-0.0.1-SNAPSHOT.jar`，若直接使用当前 Dockerfile，请先改为实际产物名 `oj-backend-0.0.1-SNAPSHOT.jar`。
3. 若只想先跑通最小可用版本：`MySQL + 后端 + 前端` 即可；判题功能再接入沙箱。
