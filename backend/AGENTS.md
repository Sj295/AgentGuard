# AGENTS.md - AgentGuard Generated Rules

## Codex Operating Focus

- Respect sandbox boundaries and explicit approval requirements.
- Escalate for confirmation before any destructive command.
- Keep each change auditable and tightly scoped.

## Project Overview

- Project Name: TestProject
- Project Path: D:/project/copilot/AgentGuard/backend
- Project Type: BACKEND
- Tech Stack: Java, Maven, Spring Boot, MySQL
- Git Repository: No
- Current Risk Level: LOW

## Build and Run Commands

- `mvn test`
- `mvn package`
- `mvn spring-boot:run`

## Safe Working Rules

- Do not modify .env files.
- Do not print secrets, tokens, API keys, or private keys.
- Do not delete files without explicit confirmation.
- Do not run rm -rf, git reset --hard, git clean -fd, or docker system prune without confirmation.
- Before changing API contracts, check both frontend and backend usage.
- After code changes, summarize modified files and potential risks.
- Prefer small, incremental changes over large rewrites.

## Sensitive Files

- No sensitive file detected in latest scan.

## Project-specific Notes

- 建议生成 AGENTS.md 以提升 AI Coding Agent 的项目理解能力。
- 当前项目未检测到 Git 仓库，建议先初始化 Git 或创建备份后再让 AI Agent 修改代码。
- 检测到 Maven 项目，AI Agent 修改后建议执行 mvn test 或 mvn package。

## Recommended Workflow

- Create or switch to a Git branch before large changes.
- Run tests or build commands after modifications.
- Review git diff before committing.
- Ask for confirmation before destructive commands.
- Keep changes scoped to the requested task.
