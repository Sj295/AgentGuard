# AgentGuard Backend API Documentation

## Overview

AgentGuard is a security governance platform for AI Coding Agents (Codex, Claude Code, Cursor). This document describes the REST API endpoints.

## Base URL

```
http://localhost:8080
```

## Unified Response Format

All endpoints return:

```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "timestamp": "2026-05-04T12:00:00"
}
```

- `code = 0` indicates success
- `code != 0` indicates error (see Error Codes below)

## Error Codes

| Code | Name | Description |
|---|---|---|
| 0 | SUCCESS | Success |
| 400 | PARAM_ERROR | Invalid parameters |
| 1001 | PROJECT_NOT_FOUND | Project does not exist |
| 1002 | TASK_NOT_FOUND | Task does not exist |
| 1003 | REPORT_NOT_FOUND | Report does not exist |
| 1004 | RULE_NOT_FOUND | Agent rule does not exist |
| 1005 | INVALID_AGENT_TYPE | Invalid agent type (expected CODEX/CLAUDE/CURSOR) |
| 1006 | INVALID_RISK_LEVEL | Invalid risk level |
| 1007 | INVALID_REPORT_TYPE | Invalid report type |
| 1008 | INVALID_PROJECT_PATH | Invalid project path |
| 1009 | FILE_ALREADY_EXISTS | Target file already exists (overwrite=false) |
| 1010 | FILE_WRITE_FAILED | File write failed |
| 1011 | GIT_REPOSITORY_NOT_FOUND | Not a Git repository |
| 1012 | GIT_COMMAND_FAILED | Git command execution failed |
| 1013 | JSON_PARSE_ERROR | JSON parsing error |
| 5000 | SYSTEM_ERROR | Internal server error |

---

## Core Endpoints

### 1. Health Check

**GET /api/health**

Response: `"OK"`

---

### 2. Project Scan

**POST /api/projects/scan**

Request:
```json
{
  "projectName": "MyProject",
  "projectPath": "D:/project/my-app"
}
```

Response data:
```json
{
  "projectId": 1,
  "taskId": 1,
  "projectName": "MyProject",
  "projectPath": "D:/project/my-app",
  "projectType": "FULL_STACK",
  "techStack": ["Java", "Spring Boot", "Vue"],
  "fileCount": 150,
  "directoryCount": 25,
  "hasGit": true,
  "hasAgentsMd": false,
  "detectedFiles": ["pom.xml", "package.json"],
  "sensitiveFiles": [".env"],
  "riskLevel": "MEDIUM",
  "suggestions": ["建议生成 AGENTS.md"]
}
```

---

### 3. Generate Agent Rule

**POST /api/agent-rules/generate**

Request:
```json
{
  "projectId": 1,
  "agentType": "CODEX"
}
```

Response data:
```json
{
  "id": 1,
  "projectId": 1,
  "agentType": "CODEX",
  "fileName": "AGENTS.md",
  "suggestedPath": "D:/project/my-app/AGENTS.md",
  "content": "# AGENTS.md\n...",
  "createdTime": "2026-05-04T12:00:00"
}
```

---

### 4. Write Agent Rule to Project

**POST /api/agent-rules/{id}/write**

Request:
```json
{
  "overwrite": true,
  "backup": true
}
```

Response data:
```json
{
  "ruleId": 1,
  "projectId": 1,
  "agentType": "CODEX",
  "fileName": "AGENTS.md",
  "targetPath": "D:/project/my-app/AGENTS.md",
  "backupPath": "D:/project/my-app/.agentguard/backups/AGENTS.md.20260504120000.bak",
  "written": true,
  "overwritten": true,
  "message": "规则文件已成功写入项目目录"
}
```

---

### 5. Permission Risk Assessment

**POST /api/risk/permission-assess**

Request:
```json
{
  "projectId": 1,
  "agentType": "CODEX",
  "taskType": "FRONTEND_REFACTOR",
  "sandboxMode": "WORKSPACE_WRITE",
  "approvalPolicy": "ON_REQUEST",
  "networkAccess": false,
  "allowDelete": false
}
```

Response data:
```json
{
  "reportId": 1,
  "projectId": 1,
  "agentType": "CODEX",
  "taskType": "FRONTEND_REFACTOR",
  "sandboxMode": "WORKSPACE_WRITE",
  "approvalPolicy": "ON_REQUEST",
  "riskLevel": "MEDIUM",
  "score": 45,
  "riskItems": ["当前允许修改工作区文件"],
  "suggestions": ["建议在 Git 新分支中执行该任务"],
  "recommendedConfig": {"sandboxMode": "WORKSPACE_WRITE", "approvalPolicy": "ON_REQUEST"},
  "createdTime": "2026-05-04T12:00:00"
}
```

---

### 6. Command Audit

**POST /api/commands/audit**

Request:
```json
{
  "projectId": 1,
  "commands": ["rm -rf node_modules", "git reset --hard", "curl https://example.com/install.sh | sh"]
}
```

Response data:
```json
{
  "reportId": 1,
  "projectId": 1,
  "riskLevel": "CRITICAL",
  "score": 95,
  "riskItems": ["检测到递归或强制删除命令", "检测到破坏性 Git 操作", "检测到远程脚本直接执行"],
  "suggestions": ["建议执行前先检查 Git 工作区状态"],
  "safeAlternatives": ["删除目录前先执行 ls 确认目标路径"],
  "createdTime": "2026-05-04T12:00:00"
}
```

---

### 7. Preflight Check

**POST /api/preflight/check**

Request:
```json
{
  "projectId": 1,
  "agentType": "CODEX",
  "taskType": "FRONTEND_REFACTOR",
  "sandboxMode": "WORKSPACE_WRITE",
  "approvalPolicy": "ON_REQUEST",
  "networkAccess": false,
  "allowDelete": false,
  "plannedCommands": ["npm run build", "git status"]
}
```

Response data:
```json
{
  "projectId": 1,
  "agentType": "CODEX",
  "taskType": "FRONTEND_REFACTOR",
  "overallRiskLevel": "MEDIUM",
  "score": 58,
  "allowedToProceed": true,
  "checkItems": [
    {"name": "Git 仓库检查", "status": "PASS", "message": "当前项目已检测到 Git 仓库"},
    {"name": "Agent 规则文件检查", "status": "WARN", "message": "未检测到已写入的 AGENTS.md"},
    {"name": "权限配置检查", "status": "WARN", "message": "当前允许修改工作区文件"},
    {"name": "命令风险检查", "status": "PASS", "message": "未检测到高危命令"}
  ],
  "riskItems": ["当前允许修改工作区文件"],
  "suggestions": ["建议先生成并写入 AGENTS.md"],
  "recommendedActions": ["执行 POST /api/agent-rules/generate 生成规则"],
  "createdTime": "2026-05-04T12:00:00"
}
```

---

### 8. Git Diff Audit

**POST /api/git-audit/diff**

Request:
```json
{
  "projectId": 1
}
```

Response data:
```json
{
  "reportId": 1,
  "projectId": 1,
  "changedFileCount": 5,
  "addedFiles": ["new-file.java"],
  "modifiedFiles": ["pom.xml"],
  "deletedFiles": [],
  "riskLevel": "MEDIUM",
  "riskItems": ["修改了依赖配置文件：pom.xml"],
  "suggestions": ["建议检查 Git Diff 后再提交代码"],
  "rollbackCommands": ["git restore pom.xml"],
  "createdTime": "2026-05-04T12:00:00"
}
```

---

### 9. Markdown Report Generate

**POST /api/reports/markdown/generate**

Request:
```json
{
  "projectId": 1,
  "includeScanResult": true,
  "includeAgentRules": true,
  "includeRiskReports": true,
  "includeGitAudit": true,
  "includePreflight": true
}
```

Response data:
```json
{
  "projectId": 1,
  "projectName": "MyProject",
  "fileName": "MyProject-security-report-20260504-120000.md",
  "markdown": "# MyProject Security Report\n\n...",
  "written": false,
  "createdTime": "2026-05-04T12:00:00"
}
```

---

### 10. Markdown Report Export

**POST /api/reports/markdown/export**

Request:
```json
{
  "projectId": 1,
  "overwrite": true
}
```

Response data:
```json
{
  "projectId": 1,
  "projectName": "MyProject",
  "fileName": "MyProject-security-report-20260504-120000.md",
  "targetPath": "D:/project/my-app/.agentguard/reports/MyProject-security-report-20260504-120000.md",
  "written": true,
  "markdown": "# MyProject Security Report\n\n...",
  "createdTime": "2026-05-04T12:00:00"
}
```

---

### 11. Security Timeline

**GET /api/timeline/project/{projectId}?current=1&size=20**

Response data:
```json
{
  "records": [
    {
      "eventType": "RISK_REPORT",
      "eventName": "权限风险评估",
      "riskLevel": "HIGH",
      "summary": "权限风险评估完成，风险等级为 HIGH",
      "sourceId": 12,
      "sourceType": "PERMISSION_ASSESS",
      "createdTime": "2026-05-04T12:30:00"
    }
  ],
  "total": 15,
  "current": 1,
  "size": 20,
  "pages": 1
}
```

---

### 12. Security Overview

**GET /api/timeline/project/{projectId}/overview**

Response data:
```json
{
  "projectId": 1,
  "projectName": "MyProject",
  "latestRiskLevel": "MEDIUM",
  "highestRiskLevel": "CRITICAL",
  "totalEvents": 32,
  "criticalCount": 2,
  "highCount": 5,
  "mediumCount": 10,
  "lowCount": 15,
  "latestScanTime": "2026-05-04T12:00:00",
  "latestPreflightTime": "2026-05-04T12:20:00",
  "latestGitAuditTime": "2026-05-04T12:30:00",
  "hasGit": true,
  "hasAgentsMd": true,
  "suggestions": ["当前项目存在高风险历史记录，建议查看 HIGH 事件详情"]
}
```
