FROM node:20-slim AS builder
WORKDIR /app

COPY package*.json ./
COPY packages ./packages
COPY apps ./apps

RUN rm -rf node_modules apps/*/node_modules packages/*/node_modules package-lock.json
# npm 10 (đi kèm node:20) crash "reading 'edgesOut'" khi resolve workspace từ đầu → dùng npm 11.
# Xem giải thích trong apps/erp-frontend/Dockerfile.
RUN npm install -g npm@11.21.0
RUN npm install --no-audit --no-fund
RUN npm run build --workspace=apps/web-public

FROM node:20-slim AS runner
WORKDIR /app
COPY --from=builder /app /app
WORKDIR /app/apps/web-public
EXPOSE 3000
CMD ["npm", "start"]