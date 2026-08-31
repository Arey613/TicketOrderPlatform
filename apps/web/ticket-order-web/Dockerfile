FROM node:26-alpine AS build

WORKDIR /workspace

RUN apk add --no-cache openjdk21-jre

COPY apps/web/ticket-order-web/package*.json apps/web/ticket-order-web/
RUN npm --prefix apps/web/ticket-order-web ci

COPY contracts/openapi/ticket-order-api contracts/openapi/ticket-order-api
COPY apps/web/ticket-order-web apps/web/ticket-order-web
RUN npm --prefix apps/web/ticket-order-web run generate:api

ARG VITE_TICKET_API_BASE_URL=http://localhost:8080
ENV VITE_TICKET_API_BASE_URL=$VITE_TICKET_API_BASE_URL
RUN npm --prefix apps/web/ticket-order-web run build

FROM nginx:1.29-alpine

ARG VITE_TICKET_API_BASE_URL=http://localhost:8080
ENV TICKET_API_ORIGIN=$VITE_TICKET_API_BASE_URL

COPY apps/web/ticket-order-web/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /workspace/apps/web/ticket-order-web/dist /usr/share/nginx/html

EXPOSE 80
