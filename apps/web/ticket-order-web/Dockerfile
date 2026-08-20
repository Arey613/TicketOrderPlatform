FROM node:26-alpine AS build

WORKDIR /workspace

COPY apps/web/ticket-order-web/package*.json apps/web/ticket-order-web/
RUN npm --prefix apps/web/ticket-order-web ci

COPY contracts/openapi/ticket-order-api contracts/openapi/ticket-order-api
COPY apps/web/ticket-order-web apps/web/ticket-order-web
RUN npm --prefix apps/web/ticket-order-web run generate:api
RUN npm --prefix apps/web/ticket-order-web run build

FROM nginx:1.29-alpine

COPY apps/web/ticket-order-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/apps/web/ticket-order-web/dist /usr/share/nginx/html

EXPOSE 80
