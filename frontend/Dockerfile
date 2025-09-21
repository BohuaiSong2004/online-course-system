# One Dockerfile for every zone: docker build --build-arg APP=catalog .
ARG APP=shell

FROM node:22-alpine AS deps
WORKDIR /repo
COPY package.json package-lock.json ./
COPY packages/shared/package.json packages/shared/
COPY apps/shell/package.json apps/shell/
COPY apps/catalog/package.json apps/catalog/
COPY apps/learn/package.json apps/learn/
COPY apps/studio/package.json apps/studio/
COPY apps/admin/package.json apps/admin/
RUN npm ci --no-audit --no-fund

FROM node:22-alpine AS build
ARG APP
# The shell's rewrites (zone + API origins) are resolved at build time.
ARG BACKEND_URL=http://backend:8080
ARG CATALOG_URL=http://catalog:3001
ARG LEARN_URL=http://learn:3002
ARG STUDIO_URL=http://studio:3003
ARG ADMIN_URL=http://admin:3004
ENV BACKEND_URL=$BACKEND_URL CATALOG_URL=$CATALOG_URL LEARN_URL=$LEARN_URL STUDIO_URL=$STUDIO_URL ADMIN_URL=$ADMIN_URL \
    NEXT_TELEMETRY_DISABLED=1
WORKDIR /repo
COPY --from=deps /repo/node_modules ./node_modules
COPY . .
RUN npm run build -w @ocs/$APP

FROM node:22-alpine AS run
ARG APP
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 HOSTNAME=0.0.0.0 APP=$APP
WORKDIR /app
RUN adduser -S -u 10001 app
COPY --from=build /repo/apps/$APP/.next/standalone ./
COPY --from=build /repo/apps/$APP/.next/static ./apps/$APP/.next/static
USER app
CMD ["sh", "-c", "node apps/$APP/server.js"]
