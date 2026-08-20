# AGENTS.md

Agent guidance for the `ticket-order-web` app.

## Scope

This file applies to `apps/web/ticket-order-web`.

## Development

- Commit messages for this module must follow `TICKET_PORTAL#123 - short description`.
- Update this module's `CHANGELOG.md` before every commit that changes this module.
- Also update the root `CHANGELOG.md` under the `ticket-order-web` module section.
- Use React 19, Vite 8, and npm.
- Use Node.js `v26.5.0` for local web development and validation.
- Keep UI source under `src`.
- Prefer small components with clear ownership.
- Separate UI code by functional area instead of building large single-file components.
- Use feature folders such as `src/features/auth`, `src/features/events`, and `src/features/orders` once those areas exist.
- Keep shared UI primitives in `src/components` only when at least two features need them.
- Keep app entrypoint logic in `src/main.tsx` after the TypeScript conversion.
- Keep top-level app composition in `src/App.tsx` until the app grows enough to justify routing or feature folders.
- Keep styling readable and local to the module. Avoid committing generated `dist` output.
- Generate API clients from `contracts/openapi/ticket-order-api/openapi.yml`.
- Do not edit generated OpenAPI client files by hand.
- Before adding Tailwind, TypeScript, icon, form, query, accessibility, or routing packages, ask for explicit dependency installation approval.

## UI Direction

- Build the actual usable product UI, not a marketing-only landing page.
- The initial public screen should introduce the platform and show event discovery content.
- Keep login and registration as separate interactions from the initial page, preferably a modal or side panel until routing is needed.
- Keep event list and booking UI aligned with the backend event model: published events, row/place selection, order creation, and owned orders.
- Prefer practical, clean ticketing-product layouts with dense but readable content.
- Make mobile and desktop layouts first-class.
- Avoid decorative-heavy pages, oversized hero-only screens, nested cards, and one-color themes.
- Ensure text does not overflow buttons, cards, forms, event rows, or navigation items.
- Static event content is placeholder UI data only; do not treat it as a frontend contract.
- Use React state for local UI state first. Use TanStack Query for server state when real API workflows begin. Avoid global stores until shared state makes them necessary.
- Use React lazy loading for route-level or heavy feature-level UI once the app has multiple real views or expensive panels. Do not lazy-load tiny components just to add indirection.

## TypeScript

- Use TypeScript for all new UI source files.
- Prefer `.tsx` for React components and `.ts` for non-React helpers.
- Convert existing JavaScript UI files to TypeScript before starting real UI implementation.
- Keep `src/main.tsx` as the React entrypoint once converted.
- Keep top-level app composition in `src/App.tsx` while the app is small.
- Type component props explicitly when a component receives props.
- Avoid `any`; use generated OpenAPI types or small local types.

## Styling

- Use Tailwind CSS for new UI styling.
- Prefer readable utility classes directly in components while the UI is small.
- Extract repeated class groups into small components only when duplication becomes meaningful.
- Keep styling close to the feature or component it belongs to.
- Avoid growing one global stylesheet with unrelated page, feature, and component rules.
- Use a small global stylesheet only for Tailwind imports, base element defaults, CSS variables, and app-wide resets.
- Put feature-specific CSS in the matching feature folder only when Tailwind utilities are not enough.
- Name feature CSS files after the owning feature or component, for example `EventList.css` or `auth-panel.css`.
- Do not duplicate the same custom CSS pattern across features; promote it to a shared component or shared style only after real reuse appears.
- Do not introduce a large custom design system early.
- Keep colors, spacing, and typography consistent through Tailwind theme values when customization is needed.
- Avoid one-off CSS files unless Tailwind cannot express the behavior clearly.
- Keep responsive behavior explicit with Tailwind breakpoints.
- Ensure all interactive states are styled: hover, focus, disabled, loading, and error.

## Accessibility

- Give dialogs, panels, forms, and navigation clear accessible names.
- Login and registration dialogs must support visible focus states, keyboard close behavior, and predictable focus movement.
- Use semantic HTML before adding custom roles.
- Keep form labels visible or programmatically associated with fields.
- Add an accessibility library such as Radix UI only when custom dialogs, dropdowns, or tabs become non-trivial to implement correctly.

## HTTP/API Calls

- Use the generated OpenAPI TypeScript client for backend calls.
- Do not use Axios unless there is a concrete need the generated client cannot cover.
- Keep generated API models at the HTTP boundary; map to small UI view models when useful.
- Put handwritten API wrappers in `src/api` once calls become more than trivial.
- Configure the API base URL through Vite environment variables, with `http://localhost:8080` as the local default.
- Do not hardcode backend URLs outside of the API client configuration.
- Send credentials for session-based calls.
- Fetch and use CSRF tokens before mutating auth requests when required by the backend.
- Use TanStack Query for loading, error, retry, cache, and mutation state once the UI has real backend workflows.
- Contract changes must start in `contracts/openapi/ticket-order-api/openapi.yml`, then regenerate the web client with `make generate-web-contracts`.

## Library Choices

- Prefer lightweight libraries that solve concrete product needs.
- Use `lucide-react` for icons when icons are needed.
- Use `clsx` for conditional class names.
- Add `tailwind-merge` only when reusable Tailwind class composition becomes repetitive.
- Use `react-hook-form` for non-trivial forms, but defer it until login, registration, or booking forms are wired.
- Use `zod` when frontend validation schemas are needed, but defer it until form validation is meaningful.
- Use `@tanstack/react-query` once the UI has real backend workflows.
- Use `react-router` only when the product has multiple real views.
- Avoid Redux, full UI kits, heavy animation libraries, and framework migrations unless requirements justify them.

## Test

- Install dependencies from the repository root when needed:

```sh
make install-web
```

- Run the web validation target from the repository root:

```sh
make test-web
```

- For API client generation changes, run from the repository root:

```sh
make generate-web-contracts
```

- `npm run test` currently runs a production Vite build. Add a dedicated test runner only when there is real component or behavior coverage to write.
- Unit/component tests run with Vitest and React Testing Library.
- Browser-level tests run with Playwright through `npm run test:e2e` from this module directory.
- Keep Playwright reports and test results out of git.

## Deploy

- Build the web image from the repository root:

```sh
make docker-build-web
```

- Run with Compose from the repository root:

```sh
make compose-up
```

- The container serves static files with Nginx on port `80`.
- Compose publishes the web app on `localhost:5173`.
- Keep Nginx fallback behavior when client-side routing is introduced.

## Next Iterations

- Add routing only when the product has multiple real views.
- Add API client code in a dedicated module or feature folder once the web app calls the Java API.
- Keep API URLs configurable for local, Compose, and deployed environments.
- Update this file whenever new frontend conventions, tools, or test targets are introduced.
