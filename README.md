# 闲品校园 CampusTrade

> 面向在校大学生的 C2C 二手交易平台，深度集成 AI 智能助手

## 项目简介

闲品校园 CampusTrade 是一个面向在校大学生的闲置物品交易平台，覆盖商品发布、智能搜索、在线沟通、订单交易、会员体系、管理后台等完整业务链路。平台深度集成 AI 智能助手，支持识图预填、RAG 知识库问答、联网搜索等功能，显著提升发布效率与平台治理能力。

## 技术栈

### 前端
- Vue 3 + Vite + Element Plus
- Pinia 状态管理
- ECharts 数据可视化
- Axios HTTP 客户端

### 后端
- Spring Boot 3 + Spring AI 1.1
- MyBatis + MySQL 8.0 + Redis
- JWT 双角色鉴权
- BCrypt 密码哈希

### AI 能力
- 阿里云百炼 DashScope
  - qwen-max：对话生成
  - qwen-vl-plus：图像识别
  - text-embedding-v3：向量嵌入
- RAG 检索增强生成
- Function Calling 工具调用
- SSE 流式输出

### 部署
- Nginx 反向代理
- 前后端分离部署

## 核心功能

### 用户体系
- 手机号注册 / 登录 / 找回密码
- JWT 双角色鉴权（user / admin）
- VIP 会员（8 折优惠 + 炫彩昵称）
- 个人资料管理

### 商品交易
- 商品发布（AI 识图预填 + 合规质检）
- 多维搜索（关键字 / 分类 / 价格区间）
- 商品标签体系
- 收藏 / 详情页

### 订单系统
- 下单（幂等 + 库存乐观锁）
- 支付 / 发货 / 确认收货
- 取消订单 / 订单状态机

### 即时通讯
- 买卖家私信（商品维度会话）
- 未读消息提醒

### AI 智能助手
- 右下角悬浮聊天
- SSE 流式输出
- 9 个 Function Calling 工具调用
- RAG 知识库问答（平台公告 + 规则）
- 联网搜索
- 识图预填商品信息

### 管理后台
- 用户管理 / 商品审核（违规原因标注）
- 订单管理 / 公告管理
- 平台数据可视化大屏（深色科技风）

## 技术亮点

### 1. 高并发订单安全与 Redis 缓存架构
- 库存乐观锁（CAS 原子条件更新 `WHERE stock > 0`）杜绝超卖
- 前端 requestId + 后端 Redis SETNX 实现订单幂等
- `@Transactional` 保障库存扣减与订单创建的事务原子性
- 验证码、登录限流、商品热点缓存全量 Redis 化
- Cache-Aside 模式，更新时主动失效缓存
- 封装 `RedisUtil` 实现 Redis 不可用时自动降级内存模式

### 2. RAG 检索增强与上下文工程
- Function Calling 多工具调用（9 个平台服务工具）
- SSE 流式输出
- RAG 检索增强（平台知识库向量化 + 余弦相似度检索 + 引用溯源）
- 持久会话与用户画像长期记忆
- Token 感知上下文滑动窗口压缩
- Prompt 注入检测防护

### 3. 全链路安全体系
- JWT 双角色令牌（user / admin 隔离）+ `AuthInterceptor` 统一鉴权
- BCrypt 密码哈希，支持存量明文用户平滑迁移
- 商品操作带属主条件 SQL 防止水平越权
- 无鉴权危险接口收编至管理端统一管控
- 自研图形验证码（一次性消费 + Redis TTL）
- 登录失败限流（失败 5 次锁定 15 分钟）

## 项目结构

```
idleschool/
├── 前端代码/
│   └── SchoolSellVue3/       # Vue3 前端（主维护版本）
│       ├── src/
│       │   ├── api/           # API 接口
│       │   ├── components/    # 公共组件
│       │   ├── views/         # 页面
│       │   ├── router/        # 路由
│       │   └── store/         # Pinia 状态
│       └── package.json
├── 后端代码/
│   └── SchoolSellApi/         # Spring Boot 后端
│       ├── src/main/
│       │   ├── java/com/xuyan/fm/
│       │   │   ├── controller/   # 控制器
│       │   │   ├── service/      # 服务层
│       │   │   ├── dao/          # 数据访问
│       │   │   ├── model/        # 实体模型
│       │   │   ├── config/       # 配置类
│       │   │   └── common/       # 公共工具
│       │   └── resources/
│       │       ├── mapper/        # MyBatis XML
│       │       └── application.yml
│       └── pom.xml
└── README.md
```

## 快速开始

### 环境要求
- JDK 17+
- Node.js 16+
- MySQL 8.0
- Redis 5.0+

### 后端启动
```bash
cd 后端代码/SchoolSellApi
# 修改 application.yml 中的数据库和 Redis 配置
mvn spring-boot:run
```
后端默认端口：9321

### 前端启动
```bash
cd 前端代码/SchoolSellVue3
npm install
npm run dev
```
前端默认端口：5173

### 数据库初始化
- 创建数据库 `itsource_18`
- 执行项目中的 SQL 初始化脚本

## 许可证

MIT License
