# 🚀 Guide de Déploiement Production - Virtual Companion

## 📋 Prérequis Infrastructure

### 1. **Serveurs Recommandés**

#### Option A : Architecture Monolithique (Démarrage)

```yaml
# 1 serveur principal (32GB RAM, 8 vCPU)
- Applications: Tous les microservices
- Base de données: PostgreSQL
- Cache: Redis
- Stockage: MinIO

# 1 serveur GPU (24GB VRAM)
- Ollama + Llama 2
- Stable Diffusion
- Coqui TTS
```

#### Option B : Architecture Distribuée (Scale)

```yaml
# Cluster Kubernetes (3 nodes minimum)
Node 1: Control Plane + Apps
Node 2: Worker + Databases
Node 3: GPU + AI Services

  # Load Balancer
  - Nginx ou Traefik
  - SSL Termination
  - Rate Limiting
```

### 2. **Services Cloud Recommandés**

- **Hébergement** : DigitalOcean, Hetzner, OVH (bon rapport qualité/prix)
- **GPU Cloud** : Runpod.io, Vast.ai (économique)
- **CDN** : Cloudflare (gratuit pour démarrer)
- **Monitoring** : Grafana Cloud (free tier)
- **Emails** : SendGrid (free tier)

## 🔧 Configuration Production

### 1. **Variables d'Environnement**

```bash
# .env.production
# Database
DATABASE_URL=postgresql://user:password@localhost:5432/virtualcompanion
REDIS_URL=redis://:password@localhost:6379

# Security
JWT_SECRET=your-super-secret-jwt-key-min-32-chars
ENCRYPTION_KEY=your-encryption-key-32-chars
CORS_ORIGINS=https://yourdomain.com

# AI Services
OLLAMA_URL=http://gpu-server:11434
STABLE_DIFFUSION_URL=http://gpu-server:7860
COQUI_TTS_URL=http://gpu-server:5002

# Storage
MINIO_ENDPOINT=https://s3.yourdomain.com
MINIO_ACCESS_KEY=your-access-key
MINIO_SECRET_KEY=your-secret-key

# Stripe
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Monitoring
SENTRY_DSN=https://...@sentry.io/...
```

### 2. **Dockerfiles Production**

```dockerfile
# backend/user-service/Dockerfile
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache dumb-init

WORKDIR /app

COPY target/user-service.jar app.jar

EXPOSE 8081

ENTRYPOINT ["dumb-init", "java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

### 3. **Docker Compose Production**

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  # Reverse Proxy
  traefik:
    image: traefik:v3.0
    command:
      - "--providers.docker=true"
      - "--entrypoints.web.address=:80"
      - "--entrypoints.websecure.address=:443"
      - "--certificatesresolvers.letsencrypt.acme.tlschallenge=true"
      - "--certificatesresolvers.letsencrypt.acme.email=admin@yourdomain.com"
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./letsencrypt:/letsencrypt

  # Microservices
  user-service:
    build: ./backend/user-service
    environment:
      - SPRING_PROFILES_ACTIVE=production
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.user.rule=Host(`api.yourdomain.com`) && PathPrefix(`/api/v1/users`)"
      - "traefik.http.routers.user.tls=true"
      - "traefik.http.routers.user.tls.certresolver=letsencrypt"
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 1G
          cpus: '0.5'

  # Database avec backup
  postgres:
    image: postgres:15-alpine
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backups:/backups
    environment:
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    deploy:
      resources:
        limits:
          memory: 2G

  # Redis Cluster
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD} --maxmemory 1gb --maxmemory-policy allkeys-lru
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

## 📦 Déploiement Kubernetes

### 1. **Manifests K8s**

```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: virtual-companion

---
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: virtual-companion
data:
  SPRING_PROFILES_ACTIVE: "production"
  OLLAMA_URL: "http://ollama-service:11434"

---
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: virtual-companion
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
        - name: user-service
          image: registry.yourdomain.com/user-service:latest
          ports:
            - containerPort: 8081
          envFrom:
            - configMapRef:
                name: app-config
            - secretRef:
                name: app-secrets
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 10

---
# k8s/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: virtual-companion
spec:
  selector:
    app: user-service
  ports:
    - port: 8081
      targetPort: 8081
  type: ClusterIP

---
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-ingress
  namespace: virtual-companion
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  tls:
    - hosts:
        - api.yourdomain.com
      secretName: api-tls
  rules:
    - host: api.yourdomain.com
      http:
        paths:
          - path: /api/v1/users
            pathType: Prefix
            backend:
              service:
                name: user-service
                port:
                  number: 8081
```

### 2. **Helm Chart**

```yaml
# helm/values.yaml
global:
  domain: yourdomain.com
  imageRegistry: registry.yourdomain.com

userService:
  replicaCount: 3
  image:
    repository: user-service
    tag: latest
  resources:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1Gi"
      cpu: "500m"

postgresql:
  enabled: true
  auth:
    postgresPassword: changeme
  primary:
    persistence:
      size: 20Gi

redis:
  enabled: true
  auth:
    password: changeme
  master:
    persistence:
      size: 5Gi

ollama:
  enabled: true
  model: "llama2:13b"
  gpu:
    enabled: true
    type: "nvidia.com/gpu"
    count: 1
```

## 🔐 Sécurité Production

### 1. **Checklist Sécurité**

- [ ] HTTPS partout (Let's Encrypt)
- [ ] Secrets dans Kubernetes Secrets ou Vault
- [ ] Rate limiting sur toutes les APIs
- [ ] WAF (Cloudflare ou ModSecurity)
- [ ] Backup automatique des données
- [ ] Monitoring des tentatives d'intrusion
- [ ] Audit logs activés
- [ ] CORS configuré strictement
- [ ] Headers de sécurité (HSTS, CSP, etc.)

### 2. **Script de Backup**

```bash
#!/bin/bash
# backup.sh

# Variables
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
S3_BUCKET="s3://your-backup-bucket"

# PostgreSQL Backup
docker exec postgres pg_dumpall -U postgres | gzip > $BACKUP_DIR/postgres_$DATE.sql.gz

# Redis Backup
docker exec redis redis-cli BGSAVE
docker cp redis:/data/dump.rdb $BACKUP_DIR/redis_$DATE.rdb

# MinIO Backup
mc mirror minio/virtual-companion $BACKUP_DIR/minio_$DATE/

# Upload to S3
aws s3 sync $BACKUP_DIR s3://your-backup-bucket/$(date +%Y/%m/%d)/

# Clean old backups (keep 30 days)
find $BACKUP_DIR -type f -mtime +30 -delete
```

## 🚀 Script de Déploiement

```bash
#!/bin/bash
# deploy.sh

set -e

echo "🚀 Déploiement Virtual Companion Production"

# 1. Build des images
echo "📦 Building Docker images..."
docker-compose -f docker-compose.prod.yml build

# 2. Push vers registry
echo "📤 Pushing to registry..."
docker-compose -f docker-compose.prod.yml push

# 3. Déployer sur K8s
echo "☸️ Deploying to Kubernetes..."
kubectl apply -f k8s/

# 4. Attendre que les pods soient ready
echo "⏳ Waiting for pods..."
kubectl wait --for=condition=ready pod -l app=user-service -n virtual-companion --timeout=300s

# 5. Run migrations
echo "🗄️ Running database migrations..."
kubectl exec -it deployment/user-service -n virtual-companion -- java -jar app.jar db migrate

# 6. Vérifier la santé
echo "❤️ Health check..."
curl -f https://api.yourdomain.com/actuator/health || exit 1

echo "✅ Deployment successful!"
```

## 📊 Monitoring Production

### 1. **Stack de Monitoring**

```yaml
# monitoring/prometheus.yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'user-service:8081'
          - 'character-service:8082'
        # etc...

  - job_name: 'node-exporter'
    static_configs:
      - targets: [ 'node-exporter:9100' ]
```

### 2. **Alertes Critiques**

```yaml
# alerts.yaml
groups:
  - name: critical
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 5m
        annotations:
          summary: "Service {{ $labels.job }} is down"

      - alert: HighMemoryUsage
        expr: process_resident_memory_bytes / 1e9 > 0.9
        for: 10m
        annotations:
          summary: "High memory usage on {{ $labels.instance }}"

      - alert: DatabaseConnectionsHigh
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 5m
        annotations:
          summary: "Database connection pool almost exhausted"
```

## 🔄 CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Build Docker images
        run: |
          docker build -t ${{ secrets.REGISTRY }}/user-service:${{ github.sha }} ./backend/user-service

      - name: Push to Registry
        run: |
          echo ${{ secrets.REGISTRY_PASSWORD }} | docker login -u ${{ secrets.REGISTRY_USERNAME }} --password-stdin
          docker push ${{ secrets.REGISTRY }}/user-service:${{ github.sha }}

      - name: Deploy to Kubernetes
        run: |
          kubectl set image deployment/user-service user-service=${{ secrets.REGISTRY }}/user-service:${{ github.sha }} -n virtual-companion
```

## ✅ Checklist Finale

### Avant le lancement :

- [ ] Tests de charge (10k utilisateurs simulés)
- [ ] Audit de sécurité complet
- [ ] Plan de disaster recovery
- [ ] Documentation API complète
- [ ] Monitoring et alertes configurés
- [ ] Backups automatiques testés
- [ ] SSL/TLS sur tous les endpoints
- [ ] Rate limiting configuré
- [ ] GDPR compliance vérifié
- [ ] Terms of Service et Privacy Policy

### Post-lancement :

- [ ] Monitoring 24/7 des premiers jours
- [ ] Collecte des feedbacks utilisateurs
- [ ] Optimisation des performances
- [ ] Mise à jour de la documentation
- [ ] Plan de scaling si succès

## 💰 Estimation des Coûts

### Infrastructure minimale (100-1000 users)

- **Serveurs** : 150€/mois (Hetzner)
- **GPU** : 100€/mois (Vast.ai)
- **CDN** : 0€ (Cloudflare free)
- **Monitoring** : 0€ (Grafana free)
- **Total** : ~250€/mois

### Infrastructure scale (1000-10k users)

- **Kubernetes Cluster** : 500€/mois
- **GPU Dedicated** : 400€/mois
- **CDN Premium** : 50€/mois
- **Monitoring Pro** : 100€/mois
- **Total** : ~1050€/mois

Le projet est maintenant prêt pour un déploiement en production ! 🚀