# Web App Navigation

Fast local guide for web-only work. Read this module's `AGENTS.md` first.

## Start Here

| Need             | Path             |
| ---------------- | ---------------- |
| React entrypoint | `src/main.jsx`   |
| Main app         | `src/App.jsx`    |
| Styles           | `src/App.css`    |
| npm scripts      | `package.json`   |
| Vite config      | `vite.config.js` |
| Static server    | `nginx.conf`     |

## Layout

| Concern              | Path                |
| -------------------- | ------------------- |
| Source               | `src`               |
| HTML shell           | `index.html`        |
| Generated API client | `src/generated/api` |
| Build output         | `dist`              |

`src/generated/api` and `dist` are generated output.

## Search

| Goal           | Command           |
| -------------- | ----------------- |
| Files          | `rg --files`      |
| Components     | `rg -n "function  | const .* = \\( | export default" src` |
| Hooks          | `rg -n "use[A-Z]  | useState       | useEffect" src`      |
| Styles         | `rg -n "^\\.      | @media         | :root" src/*.css`    |
| API/generation | `rg -n "generated | api            | fetch                | openapi" src package.json openapitools.json` |

## Commands

Run from the repository root.

| Need                | Command                       |
| ------------------- | ----------------------------- |
| Install             | `make install-web`            |
| Validate/build      | `make test-web`               |
| Package             | `make package-web`            |
| Run dev server      | `make run-web`                |
| Generate API client | `make generate-web-contracts` |

Docker requires task-specific permission.

## Rules Of Thumb

- Keep UI source under `src`.
- Keep `src/main.jsx` as the entrypoint.
- Keep `src/App.jsx` as top-level composition until real routing/features appear.
- Do not edit generated OpenAPI client files by hand.
