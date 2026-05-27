# YukiTest

JLPT（日本語能力試験）N1-N5 在线真题练习系统，支持多用户、错题本、AI 解析。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.3.5 · MyBatis-Plus · MySQL · JWT 认证 |
| 前端 | Vue 3 · Vite 6 · Vue Router · Pinia |
| AI | DeepSeek V4 SSE 流式分析 |

## 功能

- **真题练习** — 按 JLPT 等级（N1-N5）分类，支持词汇、语法、阅读、听力四大板块
- **考试计时** — 按照 JLPT 官方时长自动倒计时
- **成绩评分** — 交卷后即时出分，查看每题详情
- **错题本** — 自动收集错题，支持按等级筛选和标记已掌握
- **AI 解析** — 对单道题目调用 DeepSeek 进行流式解析（需自行配置 API Key）
- **做题进度保存** — 考试中途可自动保存草稿，下次继续

## 项目结构

```
├── src/                        # Spring Boot 后端
│   ├── main/java/com/yuki/test/
│   │   ├── controller/         # 8 个 REST 控制器
│   │   ├── service/            # 业务逻辑层
│   │   ├── mapper/             # MyBatis-Plus 数据访问
│   │   ├── entity/             # 实体类
│   │   ├── dto/                # 请求/响应 DTO
│   │   ├── interceptor/        # JWT 认证拦截器
│   │   └── config/             # 配置类
│   └── main/resources/
│       ├── application.yml     # 主配置（MySQL）
│       ├── application-dev.yml # 开发配置（H2 内存库）
│       └── db/                 # DDL 和种子数据
├── frontend/                   # Vue 3 前端
│   └── src/
│       ├── views/              # 7 个页面组件
│       ├── api/                # API 请求封装
│       ├── router/             # 路由配置
│       ├── stores/             # Pinia 状态管理
│       └── assets/             # 全局样式
└── pom.xml
```

## 快速开始

### 环境要求

- Java 17+
- MySQL 8.0+
- Node.js 18+

### 1. 创建数据库

```sql
CREATE DATABASE yuki_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 启动后端

```bash
# 设置环境变量（可选，不设置则使用默认值）
export DB_PASSWORD=你的MySQL密码

# 启动（首次启动自动建表和导入种子数据）
./mvnw spring-boot:run
```

后端运行在 `http://localhost:8080`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`，API 请求自动代理到后端。

## 环境变量

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DB_URL` | MySQL 连接地址 | `jdbc:mysql://localhost:3306/yuki_test` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | （空） |
| `SERVER_PORT` | 服务端口 | `8080` |
| `JWT_SECRET` | JWT 签名密钥 | 内置默认值 |
| `CRYPTO_KEY` | AES 加密密钥 | 内置默认值 |
| `AI_BASE_URL` | AI API 地址 | `https://api.deepseek.com/v1` |
| `AI_MODEL` | AI 模型名称 | `deepseek-chat` |

## 导入试卷

通过 `POST /api/yuki/admin/import` 接口导入，需携带 JWT Token：

```bash
curl -X POST http://localhost:8080/api/yuki/admin/import \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "paper": { "level": "N1", "paperName": "2025年7月 N1 真题", "examMinutes": 175 },
    "questionMains": [{
      "section": "vocabulary",
      "title": "言語知識（文字）",
      "questionItems": [{
        "content": "题目内容",
        "options": {"A": "选项A", "B": "选项B", "C": "选项C", "D": "选项D"},
        "correctAnswer": "A"
      }]
    }]
  }'
```

导入后的试卷默认为草稿状态，需在数据库中手动发布：

```sql
UPDATE yuki_paper SET is_published = 1 WHERE id = <paperId>;
```

## License

MIT
