FROM node:26-alpine AS build

WORKDIR /app

COPY apps/web/ticket-order-web/package*.json ./
RUN npm ci

COPY apps/web/ticket-order-web/ ./
RUN npm run build

FROM nginx:1.29-alpine

COPY apps/web/ticket-order-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
