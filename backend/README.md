# AgentGuard Backend

AI Coding Agent Security Governance Platform for Codex, Claude Code, and Cursor.

## Tech Stack

- Java 17
- Spring Boot 3.3.5
- MyBatis-Plus 3.5.7
- MySQL 8.0
- Maven

## Core Capabilities

| Module | Description |
|---|---|
| Project Scan | Scan project directory, detect tech stack, sensitive files, and risk level |
| Agent Rule Generation | Generate context rules for CODEX, CLAUDE, CURSOR |
| Rule File Writing | Write generated rules to project directory with backup |
| Permission Assessment | Evaluate sandbox/approval/task configuration risk |
| Command Audit | Detect dangerous commands (rm -rf, git reset --hard, curl\|sh, etc.) |
| Preflight Check | Comprehensive pre-execution safety check |
| Git Diff Audit | Audit uncommitted Git changes for risk |
| Markdown Report | Generate and export security reports |
| Security Timeline | Aggregate all events into a unified timeline |

## Module Structure

```
com.agentguard
├── audit/          # Git command execution and diff auditing
├── command/        # Command risk detection engine
├── common/         # Result, PageResult, JsonUtils, ErrorCode, enums
├── config/         # CORS, MyBatis-Plus pagination
├── controller/     # REST controllers
├── detector/       # Tech stack, sensitive file, risk detectors
├── dto/            # Request DTOs
├── entity/         # MyBatis-Plus entities
├── generator/      # Agent rule generators (strategy pattern)
├── mapper/         # MyBatis-Plus mappers
├── preflight/      # Preflight check engine
├── report/         # Markdown report generator
├── risk/           # Permission risk assessor
├── scanner/        # Project directory scanner
├── service/        # Business services
└── vo/             # Response View Objects
```

## Database Setup

```sql
CREATE DATABASE agentguard DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Then run: `src/main/resources/db/init.sql`

Tables: `project_info`, `scan_task`, `scan_result`, `agent_rule`, `risk_report`

## Configuration

Configure database settings with environment variables:

```powershell
$env:AGENTGUARD_DB_URL="jdbc:mysql://localhost:3306/agentguard?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$env:AGENTGUARD_DB_USERNAME="root"
$env:AGENTGUARD_DB_PASSWORD="your-password"
```

## Start

```bash
mvn spring-boot:run
```

Health check: `GET http://localhost:8080/api/health`

## Core API Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/projects/scan` | Scan project |
| POST | `/api/agent-rules/generate` | Generate agent rule |
| POST | `/api/agent-rules/{id}/write` | Write rule to project |
| POST | `/api/risk/permission-assess` | Permission risk assessment |
| POST | `/api/commands/audit` | Command audit |
| POST | `/api/preflight/check` | Preflight check |
| POST | `/api/git-audit/diff` | Git diff audit |
| POST | `/api/reports/markdown/generate` | Generate markdown report |
| POST | `/api/reports/markdown/export` | Export report to file |
| GET | `/api/timeline/project/{id}` | Security timeline |
| GET | `/api/timeline/project/{id}/overview` | Security overview |

Full API documentation: [docs/API.md](docs/API.md)

## Demo

See [docs/DEMO.md](docs/DEMO.md) for step-by-step demo guide.

Run demo script:

```powershell
.\scripts\demo-flow.ps1
```

Run high-risk scenario:

```powershell
.\scripts\high-risk-demo.ps1
```

## Tests

```bash
mvn test
```

## Roadmap

- [ ] Sensitive file scan report generation
- [ ] AI-powered risk analysis (LLM integration)
- [ ] Spring Security / JWT authentication
- [ ] Frontend dashboard
- [ ] Configurable rule engine
- [ ] CI/CD integration
