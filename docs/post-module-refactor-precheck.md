# 帖子模块渐进式重构前审查报告

> 范围：仅做现状扫描、问题核对、改造规划；不进行大规模重写。

## 1. 核查结果

### 1.1 核心类清单核查
- **与仓库一致**：以下类均存在。  
  - Controller：`PostController`、`PostThumbController`、`PostFavourController`、`PostCommentController`  
  - Service：`PostService` / `PostServiceImpl`、`PostThumbService` / `PostThumbServiceImpl`、`PostFavourService` / `PostFavourServiceImpl`、`PostCommentService` / `PostCommentServiceImpl`

### 1.2 能力与接口核查
- **帖子详情接口 `GET /post/get/vo`：与仓库一致（已存在）**。
- **评论分页接口 `POST /post_comment/list/page/vo`：与仓库一致（已存在）**。
- **ES 全量 / 增量同步任务：部分一致**。
  - 类存在：`FullSyncPostToEs`、`IncSyncPostToEs`
  - 但两个任务类默认未启用（`@Component` 被注释），因此运行态通常不会自动执行。
- **tags/images 统一编解码组件：不一致（当前未统一）**。
  - 现状为 Controller、VO、ES DTO 各自做 JSON 转换，尚无帖子领域统一 codec/assembler。

### 1.3 问题判断核查
- **PostController 中重复处理 tags/images JSON 序列化：与仓库一致（存在重复）**。
- **点赞 / 收藏逻辑高度相似但没有公共抽象：与仓库一致**。
- **并发控制可能依赖 `synchronized(userId.intern())`：与仓库一致**。
- **`post_thumb` / `post_favour` 缺少 `(postId, userId)` 唯一约束：与仓库一致**。
- **`post.thumbNum` / `favourNum` 缺少系统化对账修复：与仓库一致**（未看到对账任务/脚本/管理接口）。
- **PostVO 同时保留 `tags` 和 `tagList` 历史兼容字段：与仓库一致**。
- **ES 查询字段与 mapping 可能不一致：与仓库一致（存在明确风险）**。
  - 查询里使用了 `description` 字段，但 mapping 未定义该字段。
  - ES DTO 有 `images` 字段，但 mapping 未定义 `images`。
- **前端混用 generated SDK 与 axios：与仓库一致**。

## 2. 受影响文件清单

### 2.1 Controller
- `oj-backend/src/main/java/com/sjj/oj_backend/controller/PostController.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/controller/PostThumbController.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/controller/PostFavourController.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/controller/PostCommentController.java`

### 2.2 Service
- `oj-backend/src/main/java/com/sjj/oj_backend/service/PostService.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/service/PostThumbService.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/service/PostFavourService.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/service/PostCommentService.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/service/impl/PostServiceImpl.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/service/impl/PostThumbServiceImpl.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/service/impl/PostFavourServiceImpl.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/service/impl/PostCommentServiceImpl.java`

### 2.3 DTO / VO / Entity
- `oj-backend/src/main/java/com/sjj/oj_backend/model/entity/Post.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/entity/PostThumb.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/entity/PostFavour.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/entity/PostComment.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/dto/post/PostAddRequest.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/dto/post/PostEditRequest.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/dto/post/PostUpdateRequest.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/dto/post/PostQueryRequest.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/dto/post/PostEsDTO.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/dto/postthumb/PostThumbAddRequest.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/dto/postfavour/PostFavourAddRequest.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/dto/postfavour/PostFavourQueryRequest.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/dto/postcomment/PostCommentAddRequest.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/dto/postcomment/PostCommentQueryRequest.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/vo/PostVO.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/model/vo/PostCommentVO.java`

### 2.4 Mapper / XML
- `oj-backend/src/main/java/com/sjj/oj_backend/mapper/PostMapper.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/mapper/PostThumbMapper.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/mapper/PostFavourMapper.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/mapper/PostCommentMapper.java`
- `oj-backend/src/main/resources/mapper/PostMapper.xml`
- `oj-backend/src/main/resources/mapper/PostThumbMapper.xml`
- `oj-backend/src/main/resources/mapper/PostFavourMapper.xml`

### 2.5 SQL
- `oj-backend/sql/create_table.sql`

### 2.6 ES 相关
- `oj-backend/src/main/java/com/sjj/oj_backend/model/dto/post/PostEsDTO.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/service/impl/PostServiceImpl.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/esdao/PostEsDao.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/job/once/FullSyncPostToEs.java`
- `oj-backend/src/main/java/com/sjj/oj_backend/job/cycle/IncSyncPostToEs.java`
- `oj-backend/sql/post_es_mapping.json`

### 2.7 前端页面和 API
- 页面：
  - `oj-frontend/src/views/post/DiscussionView.vue`
  - `oj-frontend/src/views/post/PostDetailView.vue`
- API / 请求层：
  - `oj-frontend/generated/services/PostControllerService.ts`
  - `oj-frontend/generated/services/PostThumbControllerService.ts`
  - `oj-frontend/generated/services/PostFavourControllerService.ts`
  - `oj-frontend/src/plugins/axios.ts`
  - `oj-frontend/generated/models/PostVO.ts`

## 3. 当前问题分级

### P0（必须优先修）
1. **数据库缺少唯一约束，导致并发重复点赞/收藏关系风险**。  
2. **并发控制依赖 JVM 进程内 `intern` 锁，分布式场景失效**。  
3. **计数字段缺少系统化对账修复手段（`thumbNum` / `favourNum`）**。

### P1（应尽快治理）
1. **Controller / VO / ES DTO 分散处理 tags/images，协议不统一且重复代码多**。  
2. **点赞与收藏实现高度重复，维护成本高**。  
3. **ES 查询字段与 mapping 不一致（`description`、`images`）+ 同步任务默认未启用，搜索链路可靠性不足**。

### P2（可延后优化）
1. **PostVO 历史兼容字段并存（`tags` + `tagList`）导致前后端适配复杂**。  
2. **前端帖子域混用 generated SDK 与 axios，风格不统一**。  
3. **帖子细节接口参数解析、权限判断等 Controller 模板代码可继续沉淀工具化。**

## 4. 推荐的渐进式改造顺序（贴合当前仓库）

1. **先加数据一致性护栏（数据库 + 计数可修复）**
   - 先补唯一约束与巡检 SQL，降低“继续产生脏数据”的概率。
   - 同步补一个可重复执行的计数对账脚本（先离线/手工执行）。

2. **再统一帖子 tags/images 编解码协议（后端先行，兼容输出）**
   - 新增帖子领域 codec/assembler，在 Service 层集中转换。
   - Controller 去掉 JSON 手写转换；PostVO 暂时保留兼容字段避免前端联动风险。

3. **轻量瘦身 Controller（不改接口语义）**
   - 抽取 id 解析、鉴权判断、请求转领域对象等重复逻辑。
   - 保持 URL 与响应结构不变，优先降低回归成本。

4. **随后治理点赞/收藏抽象与并发策略**
   - 先在服务层抽公共模板，统一“关系存在性判断 + 计数更新 + 返回增量”。
   - 后续再引入更稳妥的分布式并发策略（如 DB 唯一约束 + 幂等重试）。

5. **最后处理搜索链路与前端 API 统一**
   - 对齐 mapping / query 字段并明确增量同步启用策略。
   - 前端帖子域统一到 SDK（或统一 axios 封装），逐页迁移。

## 5. 第一轮实际改动建议（仅规划）

> 目标仅限：**数据一致性护栏 + tags/images 统一编解码 + Controller 轻量瘦身**。

### 5.1 数据一致性护栏
- 新增迁移 SQL：
  - `post_thumb`：`unique key uk_post_user (postId, userId)`
  - `post_favour`：`unique key uk_post_user (postId, userId)`
- 迁移前置清理 SQL（按 `(postId,userId)` 去重，保留最早或最新一条）。
- 增加“计数对账 SQL 脚本”：
  - `post.thumbNum = count(post_thumb where postId=post.id)`
  - `post.favourNum = count(post_favour where postId=post.id)`
- 输出运维 Runbook（执行时机、回滚方案、观察指标）。

### 5.2 tags/images 编解码统一
- 新增帖子领域转换组件（例如 `PostContentCodec` 或 `PostFieldCodec`）：
  - `encodeTags(List<String>) -> String`
  - `encodeImages(List<String>) -> String`
  - `decodeTags(String) -> List<String>`
  - `decodeImages(String) -> List<String>`
- 改造调用点：
  - `PostController` 中 add/update/edit 改为调用 codec。
  - `PostVO` / `PostEsDTO` 的 obj-vo / obj-dto 转换统一走 codec。
- 兼容策略：
  - 仍回传 `tagList`，但在文档中标注 deprecated，后续再移除。

### 5.3 Controller 轻量瘦身
- 在不改接口路径和出参结构前提下，先做小抽取：
  - 帖子 id 解析（字符串 -> Long）的公共方法（目前仅 `get/vo` 手工 parse）。
  - “仅作者或管理员可操作”判断提取到 Service/Helper。
  - 请求 DTO -> Entity 的转换统一放到 Service/Assembler，Controller 只做参数接收与调度。
- 保持现有行为兼容：
  - 返回码、异常类型、字段名尽量不变。
  - 先不做前端联动改造。

---

## 补充观察（本轮仅记录）
- `PostMapper.xml` 的 `BaseResultMap` / `Base_Column_List` 未包含 `images` 字段，虽不一定影响 MP 自动 CRUD，但属于隐性不一致点，建议在后续顺手对齐。
- ES 增量/全量同步类默认未启用，需在“是否上线 ES”策略里明确开关，不建议长期处于“代码存在但默认无效”的状态。
