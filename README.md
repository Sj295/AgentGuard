# AgentGuard

![Java 17](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![Vue 3](https://img.shields.io/badge/Vue-3-42B883?logo=vuedotjs&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Status](https://img.shields.io/badge/status-MVP-blue)

AgentGuard 是一个面向 Codex、Claude Code、Cursor 等 AI Coding Agent 的项目上下文生成与权限安全治理平台。它帮助团队在真实项目里解决上下文缺失、权限配置不透明、危险命令不可控、敏感文件暴露、代码变更难审计等问题。

## Why AgentGuard

AI Coding Agent 越强，越需要清晰的边界。AgentGuard 把项目扫描、Agent 规则生成、权限评估、命令审计、执行前预检、Git Diff 审计和安全报告串成一条可复查的链路，让团队能在“让 Agent 做事”之前知道风险在哪里，也能在事后还原发生过什么。

## Highlights

| 能力 | 说明 |
|---|---|
| 项目扫描 | 识别技术栈、关键文件、敏感文件、Git/AGENTS 状态和基础风险 |
| Agent 规则生成 | 为 CODEX、CLAUDE、CURSOR 生成项目上下文规则文件 |
| 权限风险评估 | 评估 sandbox、approval、network、delete 等配置风险 |
| 危险命令审计 | 检测删除、强制 Git、远程脚本执行、敏感文件读取等命令 |
| Preflight Check | 在 Agent 执行前综合检查项目、规则、权限、命令和变更状态 |
| Git Diff 审计 | 对未提交变更做风险识别，并给出回滚建议 |
| 报告持久化 | 风险分数、摘要、结构化 payload 入库，支持历史查询还原 |
| 安全时间线 | 汇总扫描、规则、报告和审计事件，形成项目安全轨迹 |

## Screens

AgentGuard 前端提供项目扫描、仪表盘、权限评估、命令审计、Preflight、Git 审计、报告和时间线视图。启动前后端后访问：

```text
http://localhost:5173
```

## Architecture

```mermaid
flowchart LR
  UI["Vue 3 Frontend"] --> API["Spring Boot REST API"]
  API --> Scan["Project Scanner"]
  API --> Rules["Agent Rule Generator"]
  API --> Risk["Risk & Command Engines"]
  API --> Preflight["Preflight Checker"]
  API --> Git["Git Diff Auditor"]
  API --> Report["Markdown Report Generator"]
  API --> DB[("MySQL")]
  DB --> History["History / Timeline"]
```

## Tech Stack

| Layer | Stack |
|---|---|
| Backend | Java 17, Spring Boot 3.3, MyBatis-Plus, MySQL |
| Frontend | Vue 3, TypeScript, Vite, Element Plus, Axios |
| Test | JUnit 5, Spring Boot Test, H2 integration database |
| Tooling | Maven, npm, CI workflow template |

## Quick Start

### 1. Initialize Database

```sql
CREATE DATABASE agentguard DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
cd backend
mysql -u root -p agentguard < src/main/resources/db/init.sql
```

### 2. Configure Backend

Backend supports environment variables, so credentials do not need to be committed.

```powershell
$env:AGENTGUARD_DB_URL="jdbc:mysql://localhost:3306/agentguard?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$env:AGENTGUARD_DB_USERNAME="root"
$env:AGENTGUARD_DB_PASSWORD="your-password"
```

### 3. Start Backend

```bash
cd backend
mvn spring-boot:run
```

Health check:

```text
GET http://localhost:8080/api/health
```

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

## Validation

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run build
```

The backend test suite includes a real HTTP integration test with a temporary H2 database. It covers project scanning, report persistence, and historical query restoration.

## Documentation

- [Backend README](backend/README.md)
- [API Documentation](backend/docs/API.md)
- [Demo Guide](backend/docs/DEMO.md)
- [Risk report schema upgrade](backend/docs/SCHEMA_UPGRADE_20260504_RISK_REPORT.sql)
- [GitHub Actions CI template](docs/ci-github-actions.example.yml)
- [Contributing](CONTRIBUTING.md)
- [Security Notes](SECURITY.md)

## Repository Hygiene

The repository ignores local reports, test fixtures, `.env` files, build outputs, Maven targets, `node_modules`, and IDE metadata. Before publishing, the intended source tree was scanned for common secret patterns such as passwords, API keys, tokens, private keys, and PEM blocks.

## Roadmap

- Authentication and role-based access control
- Configurable risk rules and organization policies
- SARIF / CI security report export
- More Agent adapters and policy templates
- Project-level trend analytics

## License

MIT
