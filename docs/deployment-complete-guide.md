# Guide Complet de Déploiement - Virtual Companion Application

## Table des Matières

1. [Vue d'ensemble de l'Architecture](#vue-densemble)
2. [Prérequis Système](#prerequis)
3. [Installation de l'Infrastructure](#infrastructure)
4. [Déploiement des Microservices](#microservices)
5. [Configuration de Production](#production)
6. [Monitoring et Maintenance](#monitoring)
7. [Troubleshooting](#troubleshooting)

## Vue d'ensemble de l'Architecture {#vue-densemble}

### Architecture Microservices

```
                          ┌─────────────────┐
                          │   Client Web    │
                          │  (React/Next)   │
                          └────────┬────────┘
                                   │
                          ┌────────▼────────┐
                          │   API Gateway   │
                          │   (Port 8080)   │
                          └────────┬────────┘
                                   │
       ┌───────────────────────────┼───────────────────────────┐
       │                           │                           │
┌──────▼──────┐ ┌─────────▼─────────┐ ┌──────────▼──────────┐
│ User Service│ │ Character Service │ │Conversation Service │
│ (Port 8081) │ │   (Port 8082)     │ │   (Port 8083)      │
└─────────────┘ └───────────────────┘ └─────────────────────┘
       │                           │                           │
┌──────▼──────┐ ┌─────────▼─────────┐ ┌──────────▼──────────┐
│Media Service│ │ Billing Service   │ │Moderation Service   │
│ (Port 8084) │ │   (Port 8085)     │ │   (Port 8086)      │
└─────────────┘ └───────────────────┘ └─────────────────────┘
```

### Technologies Utilisées

- **Backend**: Java 21, Spring Boot 3.2, Spring Cloud Gateway
- **Bases de données**: PostgreSQL, MongoDB, Redis
- **Messaging**: Apache Kafka
- **Recherche**: Elasticsearch
- **Stockage**: MinIO (S3-compatible)
- **Streaming**: Kurento Media Server
- **Monitoring**: Prometheus + Grafana
- **Container**: Docker + Kubernetes

## Prérequis Système {#prerequis}

### Configuration Minimale (Développement)
- **CPU**: 4 cores
- **RAM**: 16 GB
- **Stockage**: 50 GB SSD
- **OS**: Ubuntu 20.04+ / macOS 12+ / Windows 11 avec WSL2

### Configuration Recommandée (Production)
- **CPU**: 16 cores
- **RAM**: 64 GB
- **Stockage**: 500 GB SSD NVMe
- **GPU**: NVIDIA RTX 3090 (pour l'IA)
- **OS**: Ubuntu 22.04 LTS

### Logiciels Requis

```bash
# Vérifier les versions
java -version          # OpenJDK 21
mvn -version          # Maven 3.9+
docker --version      # Docker 24+
docker-compose --version  # Docker Compose 2.20+
node --version        # Node.js 18+
kubectl version       # Kubernetes 1.28+ (production)
```

## Installation de l'Infrastructure {#infrastructure}

### 1. Cloner le Projet

```bash
git clone https://github.com/votre-repo/virtual-companion-app.git
cd virtual-companion-app
```

### 2. Configuration Initiale

```bash
# Copier et configurer les variables d'environnement
cp .env.example .env

# IMPORTANT: Éditer .env et modifier TOUS les mots de passe
nano .env
```

Variables critiques à modifier :
```env
# Bases de données
DB_PASSWORD=nouveau_mot_de_passe_securise
REDIS_PASSWORD=redis_password_securise

# Sécurité
JWT_SECRET=cle_jwt_tres_longue_minimum_512_bits
ENCRYPTION_KEY=cle_encryption_32_caracteres_ici

# Services externes
STRIPE_API_KEY=sk_live_votre_cle_stripe
OPENAI_API_KEY=sk-votre_cle_openai

# Monitoring
GRAFANA_PASSWORD=grafana_admin_password
```

### 3. Démarrer l'Infrastructure Docker

```bash
# Démarrer tous les services d'infrastructure
docker-compose up -d

# Vérifier le statut
docker-compose ps

# Voir les logs
docker-compose logs -f
```

### 4. Initialiser les Bases de Données

```bash
# Créer les schémas
docker exec -it vc-postgres psql -U vc_admin -d virtual_companion_db << EOF
CREATE SCHEMA IF NOT EXISTS user_service;
CREATE SCHEMA IF NOT EXISTS character_service;
CREATE SCHEMA IF NOT EXISTS conversation_service;
CREATE SCHEMA IF NOT EXISTS media_service;
CREATE SCHEMA IF NOT EXISTS billing_service;
CREATE SCHEMA IF NOT EXISTS moderation_service;
EOF

# Créer la base MongoDB
docker exec -it vc-mongodb mongosh << EOF
use virtual_companion_messages
db.createCollection("messages")
db.createCollection("conversation_contexts")
db.messages.createIndex({ "conversationId": 1, "timestamp": -1 })
EOF
```

## Déploiement des Microservices {#microservices}

### Option 1: Déploiement Manuel (Développement)

#### Compiler tous les services
```bash
# Script de compilation global
#!/bin/bash
SERVICES="user-service character-service conversation-service media-service billing-service moderation-service gateway"

for service in $SERVICES; do
    echo "Building $service..."
    cd backend/$service
    mvn clean package -DskipTests
    cd ../..
done
```

#### Démarrer les services
```bash
# Ordre de démarrage important!
# 1. User Service (authentification)
cd backend/user-service
java -jar target/user-service-1.0.0.jar &

# 2. Character Service
cd ../character-service
java -jar target/character-service-1.0.0.jar &

# 3. Conversation Service
cd ../conversation-service
java -jar target/conversation-service-1.0.0.jar &

# 4. Media Service
cd ../media-service
java -jar target/media-service-1.0.0.jar &

# 5. Billing Service
cd ../billing-service
java -jar target/billing-service-1.0.0.jar &

# 6. API Gateway (en dernier)
cd ../gateway
java -jar target/gateway-1.0.0.jar &
```

### Option 2: Docker Compose Complet

```yaml
# docker-compose.full.yml
version: '3.9'

services:
  # Infrastructure (déjà dans docker-compose.yml)
  
  # Microservices
  user-service:
    build: ./backend/user-service
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      - postgres
      - redis
      - kafka
    networks:
      - vc-network

  character-service:
    build: ./backend/character-service
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      - postgres
      - elasticsearch
      - minio
    networks:
      - vc-network

  # ... autres services

  gateway:
    build: ./backend/gateway
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      - user-service
      - character-service
      - conversation-service
    networks:
      - vc-network
```

Démarrer avec :
```bash
docker-compose -f docker-compose.yml -f docker-compose.full.yml up --build
```

### Option 3: Kubernetes (Production)

```bash
# Installer les charts Helm
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# PostgreSQL
helm install postgresql bitnami/postgresql \
  --set auth.postgresPassword=$DB_PASSWORD \
  --set auth.database=virtual_companion_db

# Redis
helm install redis bitnami/redis \
  --set auth.password=$REDIS_PASSWORD

# Kafka
helm install kafka bitnami/kafka \
  --set auth.enabled=false

# Déployer les microservices
kubectl apply -f k8s/
```

## Configuration de Production {#production}

### 1. Configuration HTTPS/TLS

```nginx
# nginx.conf
server {
    listen 443 ssl http2;
    server_name api.virtualcompanion.app;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://gateway:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /ws/ {
        proxy_pass http://conversation-service:8083;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 2. Optimisation des Performances

#### JVM Tuning
```bash
# Variables d'environnement pour chaque service
export JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -Djava.security.egd=file:/dev/./urandom"
```

#### PostgreSQL Tuning
```sql
-- postgresql.conf
shared_buffers = 4GB
effective_cache_size = 12GB
work_mem = 128MB
maintenance_work_mem = 1GB
max_connections = 200
checkpoint_completion_target = 0.9
wal_buffers = 16MB
random_page_cost = 1.1
```

#### Redis Configuration
```conf
# redis.conf
maxmemory 4gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
```

### 3. Sécurité Production

#### Firewall Rules
```bash
# UFW Configuration
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw allow 8080/tcp  # API Gateway (internal only)
ufw enable
```

#### Secrets Management
```bash
# Utiliser HashiCorp Vault ou AWS Secrets Manager
vault kv put secret/virtual-companion \
  db_password="$DB_PASSWORD" \
  jwt_secret="$JWT_SECRET" \
  stripe_api_key="$STRIPE_API_KEY"
```

## Monitoring et Maintenance {#monitoring}

### 1. Dashboards Grafana

Importer les dashboards :
```bash
# Copier les dashboards
cp monitoring/dashboards/*.json /var/lib/grafana/dashboards/

# Redémarrer Grafana
docker-compose restart grafana
```

Dashboards disponibles :
- **System Overview** : Métriques système globales
- **API Gateway** : Latence, throughput, erreurs
- **Microservices Health** : État de chaque service
- **Database Performance** : PostgreSQL, Redis, MongoDB
- **Business Metrics** : Utilisateurs, conversations, revenus

### 2. Alertes Prometheus

```yaml
# prometheus-alerts.yml
groups:
  - name: virtual-companion
    rules:
      - alert: ServiceDown
        expr: up{job=~".*-service"} == 0
        for: 5m
        annotations:
          summary: "Service {{ $labels.job }} is down"
          
      - alert: HighErrorRate
        expr: rate(http_server_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate on {{ $labels.service }}"
          
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        annotations:
          summary: "Database connection pool almost exhausted"
```

### 3. Backups Automatiques

```bash
#!/bin/bash
# backup.sh - À exécuter via cron

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/$DATE"

# PostgreSQL
pg_dump -U vc_admin virtual_companion_db | gzip > $BACKUP_DIR/postgres.sql.gz

# MongoDB
mongodump --uri="mongodb://localhost:27017/virtual_companion_messages" \
  --archive=$BACKUP_DIR/mongodb.archive --gzip

# Redis
redis-cli --rdb $BACKUP_DIR/redis.rdb

# MinIO/S3
mc mirror minio/media $BACKUP_DIR/media/

# Upload to S3
aws s3 sync $BACKUP_DIR s3://backup-bucket/virtual-companion/$DATE/
```

### 4. Maintenance Planifiée

#### Scripts de Maintenance
```bash
# Nettoyage des logs
find /var/log/virtual-companion -name "*.log" -mtime +30 -delete

# Vacuum PostgreSQL
psql -U vc_admin -d virtual_companion_db -c "VACUUM ANALYZE;"

# Nettoyage des sessions expirées
psql -U vc_admin -d virtual_companion_db -c "SELECT cleanup_expired_sessions();"

# Optimisation des index Elasticsearch
curl -X POST "localhost:9200/_forcemerge?max_num_segments=1"
```

## Troubleshooting {#troubleshooting}

### Problèmes Courants et Solutions

#### 1. Service ne démarre pas
```bash
# Vérifier les logs
docker logs <service-name>
journalctl -u <service-name> -f

# Causes communes :
# - Port déjà utilisé
# - Base de données non accessible
# - Configuration manquante
```

#### 2. Erreurs de connexion entre services
```bash
# Tester la connectivité
docker exec <service> ping <other-service>
docker exec <service> curl http://<other-service>:port/actuator/health

# Vérifier le réseau Docker
docker network inspect vc-network
```

#### 3. Performance dégradée
```bash
# Analyser les métriques
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/http.server.requests

# Thread dump
curl http://localhost:8080/actuator/threaddump > threaddump.json

# Heap dump (attention : gros fichier)
curl http://localhost:8080/actuator/heapdump > heapdump.hprof
```

#### 4. Problèmes de base de données
```sql
-- Connexions actives
SELECT pid, usename, application_name, client_addr, state 
FROM pg_stat_activity;

-- Requêtes lentes
SELECT query, mean_exec_time, calls 
FROM pg_stat_statements 
ORDER BY mean_exec_time DESC 
LIMIT 10;

-- Taille des tables
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) 
FROM pg_tables 
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Commandes Utiles

```bash
# État global du système
./scripts/health-check.sh

# Redémarrer un service spécifique
docker-compose restart <service-name>

# Mise à jour d'un service sans downtime
docker-compose up -d --no-deps --build <service-name>

# Rollback en cas de problème
docker-compose down
git checkout <previous-version>
docker-compose up -d
```

## Scripts d'Administration

### health-check-all.sh
```bash
#!/bin/bash
echo "=== Vérification complète du système ==="

# Services
SERVICES="user-service character-service conversation-service media-service billing-service gateway"
for service in $SERVICES; do
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:808X/actuator/health)
    if [ "$response" = "200" ]; then
        echo "✅ $service : OK"
    else
        echo "❌ $service : KO (HTTP $response)"
    fi
done

# Infrastructure
echo -e "\n=== Infrastructure ==="
docker-compose ps
```

### deploy-update.sh
```bash
#!/bin/bash
# Script de déploiement avec rollback automatique

SERVICE=$1
OLD_VERSION=$(docker ps --format "table {{.Image}}" | grep $SERVICE | awk -F: '{print $2}')

echo "Déploiement de $SERVICE..."
docker-compose up -d --no-deps --build $SERVICE

sleep 30

# Vérifier la santé
if curl -f http://localhost:808X/actuator/health; then
    echo "✅ Déploiement réussi"
else
    echo "❌ Échec, rollback en cours..."
    docker-compose stop $SERVICE
    docker run -d --name $SERVICE $SERVICE:$OLD_VERSION
fi
```

## Conclusion

Cette documentation couvre l'ensemble du processus de déploiement de l'application Virtual Companion. Pour des environnements de production à grande échelle, considérez :

1. **CDN** : CloudFlare ou AWS CloudFront pour les assets statiques
2. **Load Balancing** : HAProxy ou AWS ALB pour la répartition de charge
3. **Auto-scaling** : Kubernetes HPA ou AWS Auto Scaling
4. **Monitoring avancé** : DataDog, New Relic ou AWS CloudWatch
5. **CI/CD** : GitLab CI, GitHub Actions ou Jenkins

Pour toute question ou problème, consultez :
- Documentation technique détaillée dans `/docs`
- Logs des services dans `/logs`
- Métriques sur http://localhost:3001 (Grafana)