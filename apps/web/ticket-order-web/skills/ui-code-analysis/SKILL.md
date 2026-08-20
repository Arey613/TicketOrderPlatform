---
name: ui-code-analysis
description: Use this project-local skill when analyzing the TicketOrderPlatform UI module for code quality, maintainability, accessibility, responsive behavior, generated API usage, frontend tests, and practical improvement opportunities.
---

# UI Code Analysis

Use this skill only for the TicketOrderPlatform UI module at `apps/web/ticket-order-web`.
Do not use it for Java API, database, infrastructure, or repository-wide analysis unless the issue directly affects the UI module.

## Review Scope

Check UI code against the local app guidance in `../AGENTS.md` before giving findings.
Analyze the current implementation, then suggest improvements only when they are supported by concrete code evidence.

## Analysis Workflow

1. Inspect `apps/web/ticket-order-web/AGENTS.md`, `package.json`, `vite.config.ts`, and the relevant files under `src` and `tests`.
2. If the request is about a diff or PR, inspect changed files first, then follow references into related production or test code.
3. Check whether generated OpenAPI client files are involved. Do not review generated files directly except to confirm type availability or generation assumptions.
4. Identify correctness, maintainability, accessibility, responsive layout, API/state, and test coverage issues.
5. Separate must-fix findings from optional improvements.
6. Recommend validation commands that match the touched surface.

## Output Format

Use code-review style output:

- Start with **Findings**.
- Order findings by severity.
- Include concrete file and line references.
- If no issues are found, say that clearly.
- Add **Suggested Improvements** only for non-blocking ideas that would improve the UI module.
- Add **Validation** with the commands that should be run or were run.
- Keep summaries short and secondary to findings.

Do not create code changes unless the user explicitly asks to implement the suggested improvements.

## Structure

- Production UI code must stay under `src`.
- Unit and component tests must stay under `tests/unit`.
- Playwright tests must stay under `tests/e2e`.
- Shared test setup, data, and helpers must stay under `tests/support`.
- Keep components split by functional area, for example `features/auth`, `features/events`, and `features/orders`.
- Avoid large single-file UI implementations.
- Add shared UI components only after real reuse appears.
- Keep project-local skills under `skills`.
- Avoid mixing analysis-only guidance with production UI source.

## TypeScript

- New UI source must use TypeScript.
- Prefer generated OpenAPI types at the HTTP boundary.
- Avoid `any`; use small local UI types when generated models are too broad.
- Keep generated API models out of component internals when a simple view model is clearer.

## Styling

- Use Tailwind utilities for normal component styling.
- Keep global CSS limited to Tailwind imports, base defaults, CSS variables, and app-wide resets.
- Add feature CSS only when Tailwind cannot express the behavior clearly.
- Watch for text overflow, layout shifts, inaccessible focus states, and mobile breakage.
- Avoid decorative-heavy pages, nested cards, and one-color themes.
- Check desktop and mobile implications when layout classes, fixed sizes, sticky elements, dialogs, or responsive grids change.

## Accessibility

- Forms need visible or programmatically associated labels.
- Dialogs and panels need clear accessible names.
- Auth dialogs must support keyboard close and visible focus.
- Buttons need clear accessible names, especially icon-only buttons.
- Prefer semantic HTML before custom roles.

## API And State

- Use the generated OpenAPI TypeScript client for backend calls.
- Do not hardcode backend URLs outside API client configuration.
- Use `VITE_TICKET_API_BASE_URL` with `http://localhost:8080` as the local fallback.
- Send credentials for session-based calls and handle CSRF for mutating auth calls.
- Local UI state is preferred while the app is small.
- Use localStorage only for non-sensitive UI restore data; do not store passwords, tokens, or password hashes.
- Use TanStack Query once real backend workflows need loading, retry, cache, or mutation state.
- Flag likely production runtime gaps, such as missing generated client setup, CORS credentials mismatch, or Docker build paths that cannot access contracts.

## Testing

- Vitest specs should live under `tests/unit`.
- Playwright specs should live under `tests/e2e`.
- Shared test-only code should live under `tests/support`.
- Prefer behavior-focused tests over snapshot-heavy tests.
- Cover auth panel behavior, API wrapper behavior, localStorage restore/clear behavior, and browser-level login/logout flow.
- Keep generated reports and build output out of git.

## Validation

For UI-only changes, prefer:

```sh
make test-web
```

When Playwright behavior or layout is touched, also run from `apps/web/ticket-order-web`:

```sh
npm run test:e2e
```

For generated API client or OpenAPI changes, run:

```sh
make validate-contracts
make generate
make test
```
