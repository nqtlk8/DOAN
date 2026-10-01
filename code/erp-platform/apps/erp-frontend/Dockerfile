FROM node:20-slim AS builder
WORKDIR /app

# Copy root configurations
COPY package*.json ./

# Copy source code for all workspaces
COPY apps ./apps
COPY packages ./packages

# Bỏ package-lock.json: lockfile tạo trên Windows thiếu binding Linux của Tailwind
# (@tailwindcss/oxide-linux-*), cài mới để npm chọn đúng binding cho Linux.
RUN rm -rf node_modules apps/*/node_modules packages/*/node_modules package-lock.json

# npm 10 (đi kèm node:20) crash "Cannot read properties of null (reading 'edgesOut')"
# khi resolve từ đầu bộ react 19 + vitest 4.1 → build fail, image cũ vẫn chạy.
# npm 11 resolve được (kiểm 2026-10-01 với npm 11.21.0).
RUN npm install -g npm@11.21.0

# Install dependencies (will resolve workspace links)
RUN npm install --no-audit --no-fund

# Build the specific workspace
RUN npm run build --workspace=apps/erp-frontend

FROM nginx:alpine
# Copy built assets
COPY --from=builder /app/apps/erp-frontend/dist /usr/share/nginx/html
# Copy custom nginx config
COPY apps/erp-frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
