---
name: ui-code-analysis
description: Use this project-local skill when reviewing, auditing, or validating TicketOrderPlatform frontend UI code, React components, Tailwind styling, generated API usage, accessibility, responsive behavior, and frontend tests.
---

# UI Code Analysis

Use this skill for frontend review work in `apps/web/ticket-order-web`.

## Review Scope

Check the changed UI code against the local app guidance in `../AGENTS.md` before giving findings.
Prefer code-review output: findings first, ordered by severity, with concrete file and line references.

## Structure

- Production UI code must stay under `src`.
- Unit and component tests must stay under `tests/unit`.
- Playwright tests must stay under `tests/e2e`.
- Shared test setup, data, and helpers must stay under `tests/support`.
- Keep components split by functional area, for example `features/auth`, `features/events`, and `features/orders`.
- Avoid large single-file UI implementations.
- Add shared UI components only after real reuse appears.

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
