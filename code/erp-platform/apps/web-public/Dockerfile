FROM node:20-slim AS builder
WORKDIR /app

COPY package*.json ./
COPY packages ./packages
COPY apps ./apps

RUN rm -rf node_modules apps/*/node_modules packages/*/node_modules package-lock.json
RUN npm install
RUN npm run build --workspace=apps/web-public

FROM node:20-slim AS runner
WORKDIR /app
COPY --from=builder /app /app
WORKDIR /app/apps/web-public
EXPOSE 3000
CMD ["npm", "start"]