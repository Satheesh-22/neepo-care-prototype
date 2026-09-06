FROM node:24-alpine AS build
WORKDIR /workspace
ARG VITE_API_MODE=live
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_MODE=${VITE_API_MODE}
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY frontend/package.json frontend/package-lock.json ./frontend/
RUN cd frontend && npm ci

COPY contracts/generated/typescript ./contracts/generated/typescript
COPY frontend ./frontend
RUN cd frontend && npm run build

FROM nginx:1.29-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=5s --retries=5 CMD wget -qO- http://localhost/health || exit 1
