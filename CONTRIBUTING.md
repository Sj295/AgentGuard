# Contributing

Thanks for helping improve AgentGuard.

## Development Flow

1. Create a branch from `main`.
2. Keep backend and frontend API contracts aligned.
3. Add focused tests for backend risk logic, persistence, or frontend data mapping changes.
4. Run the relevant checks before opening a pull request.

## Checks

Backend:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
npm run build
```

## Code Style

- Prefer small, reviewable changes.
- Keep risk scoring and risk level mappings explicit.
- Avoid committing local reports, test fixture directories, build outputs, `.env` files, or credentials.
- Use environment variables for local secrets.

## Security-Sensitive Changes

For changes touching command audit, file writing, Git operations, permission scoring, report persistence, or path handling, include at least one regression test that covers the risky behavior.
