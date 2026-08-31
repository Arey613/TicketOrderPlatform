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
- Lint with `npm run lint` (Biome). This module uses Biome rather than ESLint/`typescript-eslint`,
  because `typescript-eslint` does not support the TypeScript version this module is pinned to.
  Biome ships its own TypeScript/JSX parser, so it is not coupled to the installed `typescript`
  version.
- `noUnusedLocals`/`noUnusedParameters` stay disabled in `tsconfig.json`: `tsc` type-checks every
  file reachable through imports, including the generated OpenAPI client under `src/generated`,
  which is not something this module can edit. Enabling those flags fails on generated boilerplate,
  not real UI code. Biome's `noUnusedVariables`/`noUnusedImports` rules cover unused-code detection
  for hand-written source instead, since Biome lints file-by-file and `src/generated` is excluded
  from its `files.includes` in `biome.json`.

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
- Status and error messages that appear after an async action (booking result, auth error, session
  expiry) must use an `aria-live="polite"` region so screen readers announce the change. Keep the
  element always rendered (`sr-only` when empty) rather than conditionally mounting/unmounting it,
  so the live region is registered before the update happens.
- Do not add `aria-live` to routine loading/empty-state copy ("Loading events...", "No orders
  yet."). Live regions are for genuine status changes; overusing them creates noise for screen
  reader users.

## API And State

- Use the generated OpenAPI TypeScript client for backend calls.
- Do not hardcode backend URLs outside API client configuration.
- Use `VITE_TICKET_API_BASE_URL` with `http://localhost:8080` as the local fallback.
- Send credentials for session-based calls and handle CSRF for mutating auth calls.
- Local UI state is preferred while the app is small.
- Use localStorage only for non-sensitive UI restore data; do not store passwords, tokens, or password hashes.
- Use TanStack Query once real backend workflows need loading, retry, cache, or mutation state.
- Flag likely production runtime gaps, such as missing generated client setup, CORS credentials mismatch, or Docker build paths that cannot access contracts.

## Security

- `nginx.conf.template` (processed at container start via the nginx image's `envsubst`-on-templates
  mechanism, not a static `nginx.conf`) must send `Content-Security-Policy`,
  `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, and `Permissions-Policy` on served
  responses. The CSP's `connect-src` references `${TICKET_API_ORIGIN}`, which must stay in sync with
  the `VITE_TICKET_API_BASE_URL` baked into the JS bundle at build time; both are set from the same
  Makefile/Dockerfile default.
- Any `401` response from an authenticated call (an `EventsApi` call, not login/register — a failed
  login is an expected credential error, not a session expiry) must clear stored auth state and
  return the UI to the logged-out state. Wire new authenticated API clients through
  `sessionAwareMiddleware` in `src/api/apiConfiguration.ts` rather than handling `401` locally.
- `VITE_TICKET_API_BASE_URL` must not silently default to `localhost` in a production build.
  `vite.config.ts` fails the `vite build` command itself when the variable is unset in build mode;
  `resolveApiBaseUrl()` in `src/api/apiConfiguration.ts` is a secondary runtime safety net. Local
  dev, `make test-web`, and Compose all get the same default value from the Makefile so this check
  does not require manual setup.

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

`make test-web` runs formatting, `npm run lint` (Biome), `tsc --noEmit`, Vitest, and `vite build` in
that order. Run `npm run lint` directly from `apps/web/ticket-order-web` for a fast standalone lint
pass; `npm run lint:fix` applies Biome's safe fixes (import ordering, etc.).

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
