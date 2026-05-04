# Security Notes

AgentGuard is designed to reason about AI Coding Agent risk, so repository hygiene matters.

## Do Not Commit

- `.env` files
- API keys, tokens, private keys, certificates, keystores
- Local `.agentguard/` reports or backups
- Database dumps containing real project data
- Build outputs such as `target/`, `dist/`, and `node_modules/`

## Configuration

The backend reads database settings from environment variables:

| Variable | Purpose |
|---|---|
| `AGENTGUARD_DB_URL` | JDBC URL |
| `AGENTGUARD_DB_USERNAME` | Database username |
| `AGENTGUARD_DB_PASSWORD` | Database password |

## Reporting Security Issues

Please open a private advisory or contact the repository owner before disclosing vulnerabilities publicly.

## Current Scope

This project currently focuses on local development and security-governance workflows. Production deployments should add authentication, authorization, TLS termination, audit log retention policy, and explicit project path allowlists.
