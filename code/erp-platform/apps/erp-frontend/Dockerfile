FROM node:20-slim AS builder
WORKDIR /app

# Copy root configurations
COPY package*.json ./

# Copy source code for all workspaces
COPY apps ./apps
COPY packages ./packages

# Fix native bindings issue by ensuring fresh install
RUN rm -rf node_modules apps/*/node_modules packages/*/node_modules package-lock.json

# Install dependencies (will resolve workspace links)
RUN npm install

# Build the specific workspace
RUN npm run build --workspace=apps/erp-frontend

FROM nginx:alpine
# Copy built assets
COPY --from=builder /app/apps/erp-frontend/dist /usr/share/nginx/html
# Copy custom nginx config
COPY apps/erp-frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
