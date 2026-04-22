# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package.json e package-lock.json
COPY package*.json ./

# Instalar dependências
RUN npm ci --legacy-peer-deps

# Copy código source
COPY . .

# Build da aplicação
RUN npm run build

# Stage 2: Runtime (serve com nginx)
FROM nginx:alpine

# Copy nginx config
COPY --from=builder /app/build /usr/share/nginx/html

# Copy custom nginx config para SPA routing
RUN cat > /etc/nginx/conf.d/default.conf <<EOF
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html index.htm;

    # Cache busting para arquivos build
    location ~* \\.(?:js|css|map)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Static assets
    location ~* \\.(jpg|jpeg|png|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Proxy para o backend — API calls
    location /categorization/ {
        proxy_pass http://backend:8080/categorization/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }

    # Fallback para SPA
    location / {
        try_files \$uri \$uri/ /index.html;
        expires -1;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    # Health check
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
EOF

EXPOSE 80

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD wget -q -O- http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
