# ticket-order-web

React and Vite web module for TicketOrderPlatform.

## Stack

- React 19
- Vite 8
- npm

## Structure

```text
src/App.jsx      Main application component
src/main.jsx     React entrypoint
src/App.css      Module styles
vite.config.js   Vite configuration
```

## Commands

From the repository root:

```sh
make install-web
make test-web
make package-web
make run-web
```

From this module directory:

```sh
npm install
npm run test
npm run build
npm run dev
```

## Docker

Build from the repository root:

```sh
docker build -f apps/web/ticket-order-web/Dockerfile -t ticket-order-web .
```

Run:

```sh
docker run --rm -p 5173:80 ticket-order-web
```

## Local Development

The Vite development server runs on:

```text
http://localhost:5173
```

The Java API CORS configuration allows this origin by default.
