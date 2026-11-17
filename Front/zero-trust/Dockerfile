# syntax=docker/dockerfile:1

# ---- Etapa 1: dependencias y build ----
# El build descarga las fuentes de Google Fonts: necesita internet.
FROM node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Las variables NEXT_PUBLIC_* se incrustan en el JavaScript al compilar,
# por eso son argumentos de build y no variables de ejecucion.
ARG NEXT_PUBLIC_VOTING_API_URL
ARG NEXT_PUBLIC_WS_GLOBAL_URL
ARG NEXT_PUBLIC_WS_REGIONAL_URL
ENV NEXT_PUBLIC_VOTING_API_URL=$NEXT_PUBLIC_VOTING_API_URL \
    NEXT_PUBLIC_WS_GLOBAL_URL=$NEXT_PUBLIC_WS_GLOBAL_URL \
    NEXT_PUBLIC_WS_REGIONAL_URL=$NEXT_PUBLIC_WS_REGIONAL_URL \
    NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# ---- Etapa 2: ejecucion ----
FROM node:22-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S -G appgroup appuser

ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1

# next.config.ts se transpila al arrancar y necesita typescript (devDependency),
# por eso se copian los node_modules completos.
COPY --from=builder --chown=appuser:appgroup /app/package.json ./package.json
COPY --from=builder --chown=appuser:appgroup /app/node_modules ./node_modules
COPY --from=builder --chown=appuser:appgroup /app/.next ./.next
COPY --from=builder --chown=appuser:appgroup /app/next.config.ts ./next.config.ts

USER appuser
EXPOSE 3000
CMD ["npm", "run", "start"]
