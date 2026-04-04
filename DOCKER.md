# TickeTUB - Docker Setup Guide

## Descrição

Sistema fullstack de gestão de bilhética com:
- **Backend**: Java 21 + Spring Boot 4.0.3
- **Frontend**: React 19
- **Database**: MySQL 8
- **Infraestrutura**: Docker Compose

## Requisitos

- Docker Desktop instalado e a correr
- Docker Compose 3.8+
- WSL 2 (se em Windows)

## Quick Start

### 1. Clonar ou preparar o projeto

```bash
cd Teste-Deploy
```

### 2. Copiar arquivo de ambiente

```bash
cp .env.example .env
```

Editar `.env` se necessário (Auth0, portas, etc.)

### 3. Construir e correr com Docker

```bash
# Build das imagens (primeira vez)
docker-compose build

# Iniciar os serviços
docker-compose up -d

# Verificar status
docker-compose ps

# Ver logs
docker-compose logs -f

# Parar os serviços
docker-compose down
```

## Portas mapeadas

| Serviço | Host | Container | URL |
|---------|------|-----------|-----|
| Frontend | 3000 | 80 | http://localhost:3000 |
| Backend API | 8080 | 8080 | http://localhost:8080 |
| MySQL | 3307 | 3306 | localhost:3307 |

## Variáveis de ambiente

Configuráveis no ficheiro `.env`:

```env
# Auth0 (opcional)
REACT_APP_AUTH0_DOMAIN=
REACT_APP_AUTH0_CLIENT_ID=
REACT_APP_AUTH0_AUDIENCE=

# API URL (dentro do Docker)
REACT_APP_API_URL=http://backend:8080

# Database
MYSQL_PASSWORD=password
MYSQL_USER=admin
```

## Estrutura Docker

### Backend (demo/Dockerfile)
- **Build**: Multi-stage com Maven + Java 21
- **Runtime**: Eclipse Temurin JRE 21
- **Healthcheck**: Via `/actuator/health`
- **Porta**: 8080

### Frontend (frontend/Dockerfile)
- **Build**: Node.js 20 Alpine com React build
- **Runtime**: Nginx Alpine para servir SPA
- **Healthcheck**: Via `/health`
- **Porta**: 3000

### Database (MySQL)
- **Imagem**: MySQL 8
- **Healthcheck**: ping ao MySQL
- **Volume**: `mysql_data` (persistência)

## Comandos úteis

```bash
# Logs do backend
docker-compose logs backend

# Logs do frontend
docker-compose logs frontend

# Entrar no container do backend
docker exec -it tub-backend /bin/bash

# Entrar no container do frontend
docker exec -it tub-frontend /bin/sh

# Rebuild após alterações de código
docker-compose build --no-cache backend frontend
docker-compose up -d

# Limpar volumes (CUIDADO: apaga dados)
docker-compose down -v

# Ver network
docker network ls
```

## Troubleshooting

### Frontend não carrega a API

Verificar se o backend está saudável:
```bash
docker-compose logs backend
curl http://localhost:8080/actuator/health
```

### MySQL não conecta

Verificar logs:
```bash
docker-compose logs db
```

Confirmar que a porta 3307 não está em uso:
```bash
# Windows/PowerShell
netstat -ano | findstr :3307

# Linux/Mac
lsof -i :3307
```

### Containers reiniciam infinitamente

Ver logs:
```bash
docker-compose logs --tail=50 [service-name]
```

### Limpar tudo e recomeçar

```bash
docker-compose down -v
docker system prune -a
docker-compose up -d --build
```

## Desenvolvimento

### Frontend (desenvolvimento local)

```bash
cd frontend
npm install
npm start
```

Acesso em http://localhost:3000 (React dev server com hot reload)

### Backend (desenvolvimento local)

```bash
cd demo
./mvnw spring-boot:run
```

Acesso em http://localhost:8080 (dependências do .env ou application.properties)

## Produção

Para deployment em produção:

1. Gerar builds otimizados:
   ```bash
   docker-compose build --no-cache
   ```

2. Usar variáveis de ambiente seguras (secrets do Docker/Kubernetes)

3. Configurar CORS no backend para origem correta

4. Usar reverse proxy (nginx) em frente aos containers

5. Implementar network policies e firewall

## Nota sobre Auth0

Auth0 é opcional para desenvolvimento. Se quiser desativar:
- Editar `frontend/src/hooks/useAuthFlow.js` e mudar `AUTH_ENABLED = false`
- Editar `frontend/src/index.js` e mudar `AUTH_ENABLED = false`

Então reconstruir:
```bash
docker-compose build --no-cache frontend
docker-compose up -d frontend
```
