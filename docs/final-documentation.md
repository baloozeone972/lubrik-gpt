# Virtual Companion - Documentation Complète

## Table des Matières

1. [Vue d'ensemble](#vue-densemble)
2. [Documentation API](#documentation-api)
3. [Guide de Déploiement Production](#guide-de-déploiement-production)
4. [Sécurité et Conformité](#sécurité-et-conformité)
5. [Monitoring et Observabilité](#monitoring-et-observabilité)
6. [Plan de Scaling](#plan-de-scaling)
7. [Troubleshooting](#troubleshooting)
8. [Scripts de Migration](#scripts-de-migration)

## Vue d'ensemble

Virtual Companion est une plateforme SaaS de compagnons virtuels basée sur l'IA, construite avec une architecture microservices scalable.

### Stack Technique

- **Backend**: Java 21, Spring Boot 3.2, PostgreSQL 15, Redis 7
- **Frontend**: Next.js 14, React 18, TypeScript, Tailwind CSS
- **Mobile**: React Native, Expo
- **IA/ML**: Ollama (Llama 2), Stable Diffusion XL, Coqui TTS
- **Infrastructure**: Kubernetes, Docker, AWS
- **Monitoring**: Prometheus, Grafana, Jaeger

### Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Web Client    │     │  Mobile Client  │     │   Admin Panel   │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                         │
         └───────────────────────┴─────────────────────────┘
                                 │
                        ┌────────▼────────┐
                        │   API Gateway   │
                        │  (Spring Cloud) │
                        └────────┬────────┘
                                 │
      ┌──────────────────────────┼──────────────────────────┐
      │                          │                          │
┌─────▼─────┐  ┌─────────┐  ┌───▼─────┐  ┌──────────┐  ┌──▼──────┐
│   User    │  │Character│  │  Conv   │  │  Media   │  │ Billing │
│ Service   │  │ Service │  │Service  │  │ Service  │  │ Service │
└─────┬─────┘  └────┬────┘  └────┬────┘  └────┬─────┘  └────┬────┘
      │             │             │             │              │
      └─────────────┴─────────────┴─────────────┴──────────────┘
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
              ┌─────▼─────┐ ┌────▼────┐ ┌────▼────┐
              │PostgreSQL │ │  Redis  │ │   S3    │
              └───────────┘ └─────────┘ └─────────┘
```

## Documentation API

### Authentication

Toutes les requêtes API nécessitent un token JWT dans le header Authorization:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Base URL

- Production: `https://api.virtualcompanion.ai/v1`
- Staging: `https://api-staging.virtualcompanion.ai/v1`

### Endpoints

#### User Service

##### POST /auth/register
Créer un nouveau compte utilisateur.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "displayName": "John Doe"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "displayName": "John Doe",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

##### POST /auth/login
Authentification utilisateur.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "displayName": "John Doe"
  }
}
```

##### GET /users/profile
Récupérer le profil de l'utilisateur connecté.

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "displayName": "John Doe",
  "avatarUrl": "https://cdn.virtualcompanion.ai/avatars/user123.jpg",
  "bio": "AI enthusiast and tech lover",
  "level": 5,
  "experience": 2450,
  "subscription": {
    "tier": "premium",
    "validUntil": "2024-12-31T23:59:59Z"
  }
}
```

#### Character Service

##### GET /characters
Liste des personnages disponibles.

**Query Parameters:**
- `page` (int): Numéro de page (défaut: 0)
- `size` (int): Taille de page (défaut: 20)
- `category` (string): Filtrer par catégorie
- `search` (string): Recherche textuelle

**Response:**
```json
{
  "content": [
    {
      "id": "char-001",
      "name": "Luna",
      "description": "A wise and mystical guide",
      "avatarUrl": "https://cdn.virtualcompanion.ai/characters/luna.jpg",
      "category": "fantasy",
      "traits": ["wise", "mystical", "helpful"],
      "rating": 4.8,
      "conversationCount": 15420,
      "isPublic": true
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0
}
```

##### POST /characters
Créer un personnage personnalisé (Premium uniquement).

**Request:**
```json
{
  "name": "Custom Assistant",
  "description": "My personal AI assistant",
  "traits": ["professional", "efficient", "friendly"],
  "category": "assistant",
  "personality": {
    "greeting": "Hello! How can I assist you today?",
    "tone": "professional",
    "interests": ["productivity", "technology"]
  }
}
```

#### Conversation Service

##### POST /conversations
Démarrer une nouvelle conversation.

**Request:**
```json
{
  "characterId": "char-001",
  "initialMessage": "Hello Luna!"
}
```

**Response:**
```json
{
  "id": "conv-12345",
  "characterId": "char-001",
  "userId": "user-123",
  "title": "Chat with Luna",
  "createdAt": "2024-01-15T10:30:00Z",
  "messages": [
    {
      "id": "msg-001",
      "content": "Hello Luna!",
      "role": "user",
      "timestamp": "2024-01-15T10:30:00Z"
    },
    {
      "id": "msg-002",
      "content": "Greetings, traveler! I sense you seek wisdom today.",
      "role": "assistant",
      "timestamp": "2024-01-15T10:30:01Z"
    }
  ]
}
```

##### POST /conversations/{id}/messages
Envoyer un message dans une conversation.

**Request:**
```json
{
  "content": "Tell me about the stars",
  "type": "text"
}
```

**WebSocket Alternative:**
```javascript
ws.send(JSON.stringify({
  type: 'message',
  conversationId: 'conv-12345',
  content: 'Tell me about the stars'
}));
```

#### Media Service

##### POST /media/upload
Upload de fichiers média.

**Request:**
- Method: POST
- Content-Type: multipart/form-data
- Fields:
  - `file`: Le fichier à uploader
  - `type`: Type de média (image, audio, video)
  - `characterId` (optionnel): ID du personnage associé

**Response:**
```json
{
  "id": "media-789",
  "url": "https://cdn.virtualcompanion.ai/media/image-789.jpg",
  "thumbnailUrl": "https://cdn.virtualcompanion.ai/media/image-789-thumb.jpg",
  "type": "image",
  "size": 1048576,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### Rate Limiting

- Free tier: 100 requêtes/minute
- Premium tier: 1000 requêtes/minute
- Enterprise: Illimité

Headers de réponse:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705318800
```

### Codes d'Erreur

| Code | Description |
|------|-------------|
| 400 | Bad Request - Paramètres invalides |
| 401 | Unauthorized - Token manquant ou invalide |
| 403 | Forbidden - Accès refusé |
| 404 | Not Found - Ressource introuvable |
| 429 | Too Many Requests - Rate limit dépassé |
| 500 | Internal Server Error |

## Guide de Déploiement Production

### Prérequis

- Kubernetes 1.28+
- Helm 3.12+
- Terraform 1.5+
- AWS CLI configuré
- Docker 24+

### 1. Infrastructure Setup

```bash
# Clone du repository
git clone https://github.com/your-org/virtual-companion.git
cd virtual-companion

# Configuration Terraform
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Éditer terraform.tfvars avec vos valeurs

# Initialisation et déploiement
terraform init
terraform plan
terraform apply -auto-approve

# Récupération des outputs
terraform output -json > ../outputs.json
```

### 2. Configuration des Secrets

```bash
# Créer le namespace
kubectl create namespace virtual-companion

# Créer les secrets
kubectl create secret generic app-secrets \
  --from-literal=DATABASE_PASSWORD=$(openssl rand -base64 32) \
  --from-literal=JWT_SECRET=$(openssl rand -base64 64) \
  --from-literal=STRIPE_API_KEY=$STRIPE_API_KEY \
  --from-literal=AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
  --from-literal=AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
  -n virtual-companion
```

### 3. Déploiement des Services

```bash
# Installation avec Helm
helm install virtual-companion ./kubernetes/helm \
  --namespace virtual-companion \
  --values ./kubernetes/helm/values-production.yaml \
  --set image.tag=$GIT_SHA \
  --wait

# Vérification du déploiement
kubectl get pods -n virtual-companion
kubectl get svc -n virtual-companion
```

### 4. Configuration DNS

```bash
# Récupérer l'IP du LoadBalancer
INGRESS_IP=$(kubectl get svc api-gateway -n virtual-companion -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Configurer les enregistrements DNS
# A record: api.virtualcompanion.ai -> $INGRESS_IP
# CNAME: app.virtualcompanion.ai -> cloudfront.distribution.domain
```

### 5. SSL/TLS Setup

```bash
# Installation de cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Créer l'issuer Let's Encrypt
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@virtualcompanion.ai
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

### 6. Database Migration

```bash
# Port-forward vers PostgreSQL
kubectl port-forward -n virtual-companion svc/postgres-service 5432:5432 &

# Exécuter les migrations
cd services/user-service
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/virtualcompanion

# Répéter pour chaque service
```

### 7. Monitoring Setup

```bash
# Installation de Prometheus
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --values monitoring/prometheus-values.yaml

# Installation de Grafana
kubectl apply -f monitoring/grafana-dashboards.yaml
```

## Sécurité et Conformité

### 1. Chiffrement

#### Au repos
- Base de données: AES-256 encryption
- S3: SSE-S3 encryption
- Redis: Encryption at rest activé

#### En transit
- TLS 1.3 pour toutes les communications
- mTLS entre services internes
- VPN pour accès administratif

### 2. Authentification & Autorisation

```java
// Configuration Spring Security
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors().and()
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            .and()
            .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint())
            .and()
            .build();
    }
}
```

### 3. GDPR Compliance

#### Droits des utilisateurs
- **Accès**: GET /api/v1/users/data-export
- **Rectification**: PUT /api/v1/users/profile
- **Effacement**: DELETE /api/v1/users/account
- **Portabilité**: GET /api/v1/users/data-export?format=json

#### Data Retention Policy
```sql
-- Suppression automatique des données anciennes
CREATE OR REPLACE FUNCTION cleanup_old_data()
RETURNS void AS $$
BEGIN
    -- Suppression des messages > 2 ans
    DELETE FROM messages 
    WHERE created_at < NOW() - INTERVAL '2 years'
    AND conversation_id IN (
        SELECT id FROM conversations 
        WHERE user_id IN (
            SELECT id FROM users 
            WHERE subscription_tier = 'free'
        )
    );
    
    -- Anonymisation des comptes inactifs
    UPDATE users 
    SET email = CONCAT('deleted_', id, '@example.com'),
        display_name = 'Deleted User',
        avatar_url = NULL
    WHERE last_login < NOW() - INTERVAL '1 year'
    AND deletion_requested = true;
END;
$$ LANGUAGE plpgsql;

-- Exécution quotidienne
CREATE EXTENSION IF NOT EXISTS pg_cron;
SELECT cron.schedule('cleanup-old-data', '0 2 * * *', 'SELECT cleanup_old_data()');
```

### 4. Security Headers

```nginx
# nginx.conf
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' wss://api.virtualcompanion.ai https://api.virtualcompanion.ai;" always;
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;
```

### 5. Audit Logging

```java
@Component
@Aspect
public class AuditAspect {
    
    @Autowired
    private AuditService auditService;
    
    @Around("@annotation(Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        String user = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        String action = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        AuditLog log = AuditLog.builder()
            .userId(user)
            .action(action)
            .resource(joinPoint.getTarget().getClass().getSimpleName())
            .timestamp(Instant.now())
            .ipAddress(getClientIP())
            .build();
        
        try {
            Object result = joinPoint.proceed();
            log.setStatus("SUCCESS");
            return result;
        } catch (Exception e) {
            log.setStatus("FAILURE");
            log.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            auditService.log(log);
        }
    }
}
```

## Monitoring et Observabilité

### 1. Métriques Prometheus

```yaml
# Métriques personnalisées
- name: virtual_companion_active_users
  help: Number of active users in the last 24 hours
  type: gauge
  
- name: virtual_companion_messages_sent_total
  help: Total number of messages sent
  type: counter
  labels: [character_id, user_tier]
  
- name: virtual_companion_ai_generation_duration_seconds
  help: Time taken for AI response generation
  type: histogram
  buckets: [0.1, 0.5, 1, 2, 5, 10]
```

### 2. Alertes Critiques

```yaml
groups:
- name: critical-alerts
  rules:
  - alert: HighErrorRate
    expr: |
      sum(rate(http_requests_total{status=~"5.."}[5m])) 
      / sum(rate(http_requests_total[5m])) > 0.05
    for: 5m
    labels:
      severity: page
    annotations:
      summary: "High error rate detected"
      
  - alert: DatabaseConnectionPoolExhausted
    expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
    for: 5m
    labels:
      severity: critical
      
  - alert: AIServiceDown
    expr: up{job="ai-service"} == 0
    for: 1m
    labels:
      severity: critical
```

### 3. Dashboards Grafana

- **Service Overview**: Latence, throughput, error rate
- **Business Metrics**: Users actifs, messages/jour, revenue
- **Infrastructure**: CPU, mémoire, réseau, storage
- **AI Performance**: Temps de génération, tokens/sec

### 4. Distributed Tracing

```java
@RestController
@Slf4j
public class ConversationController {
    
    @Autowired
    private Tracer tracer;
    
    @PostMapping("/conversations/{id}/messages")
    public Mono<MessageResponse> sendMessage(
            @PathVariable String id,
            @RequestBody SendMessageRequest request) {
        
        Span span = tracer.nextSpan()
            .name("send-message")
            .tag("conversation.id", id)
            .start();
        
        return Mono.defer(() -> {
            try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
                span.tag("message.length", String.valueOf(request.getContent().length()));
                
                return conversationService.sendMessage(id, request)
                    .doOnSuccess(response -> {
                        span.tag("response.generated", "true");
                        span.end();
                    })
                    .doOnError(error -> {
                        span.error(error);
                        span.end();
                    });
            }
        });
    }
}
```

## Plan de Scaling

### 1. Scaling Horizontal

#### Auto-scaling Configuration
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: conversation-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: conversation-service
  minReplicas: 3
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Pods
    pods:
      metric:
        name: websocket_connections
      target:
        type: AverageValue
        averageValue: "100"
```

#### Database Scaling
```sql
-- Partitioning pour les tables volumineuses
CREATE TABLE messages (
    id UUID DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    content TEXT,
    created_at TIMESTAMP DEFAULT NOW()
) PARTITION BY RANGE (created_at);

-- Création automatique des partitions mensuelles
CREATE OR REPLACE FUNCTION create_monthly_partitions()
RETURNS void AS $$
DECLARE
    start_date date;
    end_date date;
    partition_name text;
BEGIN
    start_date := date_trunc('month', CURRENT_DATE);
    end_date := start_date + interval '1 month';
    partition_name := 'messages_' || to_char(start_date, 'YYYY_MM');
    
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF messages 
                    FOR VALUES FROM (%L) TO (%L)',
                    partition_name, start_date, end_date);
END;
$$ LANGUAGE plpgsql;
```

### 2. Optimisations Performance

#### Query Optimization
```java
// Utilisation de projections pour réduire la charge
@Query("""
    SELECT new com.vc.dto.ConversationSummary(
        c.id, c.title, c.lastMessageAt, 
        ch.name, ch.avatarUrl,
        COUNT(m.id)
    )
    FROM Conversation c
    JOIN c.character ch
    LEFT JOIN c.messages m
    WHERE c.userId = :userId
    GROUP BY c.id, c.title, c.lastMessageAt, ch.name, ch.avatarUrl
    ORDER BY c.lastMessageAt DESC
    """)
Page<ConversationSummary> findUserConversationSummaries(
    @Param("userId") UUID userId, 
    Pageable pageable
);
```

#### Caching Strategy
```java
@Service
public class CharacterService {
    
    @Cacheable(
        value = "characters", 
        key = "#id",
        condition = "#result != null",
        unless = "#result.isPrivate()"
    )
    public Character findById(UUID id) {
        return characterRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Character not found"));
    }
    
    @CacheEvict(value = "characters", key = "#character.id")
    public Character update(Character character) {
        return characterRepository.save(character);
    }
}
```

### 3. Cost Optimization

#### Spot Instances pour Workloads Non-Critiques
```yaml
nodeSelector:
  node.kubernetes.io/lifecycle: spot
tolerations:
- key: "spot"
  operator: "Equal"
  value: "true"
  effect: "NoSchedule"
```

#### Reserved Capacity Planning
```python
# Script d'analyse des coûts
import boto3
import pandas as pd
from datetime import datetime, timedelta

def analyze_usage_for_reservations():
    ce = boto3.client('ce')
    
    response = ce.get_cost_and_usage(
        TimePeriod={
            'Start': (datetime.now() - timedelta(days=90)).strftime('%Y-%m-%d'),
            'End': datetime.now().strftime('%Y-%m-%d')
        },
        Granularity='DAILY',
        Metrics=['UnblendedCost', 'UsageQuantity'],
        GroupBy=[
            {'Type': 'DIMENSION', 'Key': 'INSTANCE_TYPE'},
            {'Type': 'DIMENSION', 'Key': 'AVAILABILITY_ZONE'}
        ]
    )
    
    # Analyser et recommander les réservations
    df = pd.DataFrame(response['ResultsByTime'])
    recommendations = calculate_optimal_reservations(df)
    
    return recommendations
```

## Troubleshooting

### 1. Problèmes Courants

#### High Memory Usage
```bash
# Identifier les pods consommateurs
kubectl top pods -n virtual-companion --sort-by=memory

# Analyser heap dump Java
kubectl exec -it <pod-name> -n virtual-companion -- jmap -dump:format=b,file=/tmp/heap.bin 1
kubectl cp virtual-companion/<pod-name>:/tmp/heap.bin ./heap.bin

# Analyser avec Eclipse MAT ou jhat
```

#### Slow Queries
```sql
-- Identifier les requêtes lentes
SELECT 
    query,
    calls,
    mean_exec_time,
    total_exec_time
FROM pg_stat_statements
WHERE mean_exec_time > 100
ORDER BY mean_exec_time DESC
LIMIT 20;

-- Analyser le plan d'exécution
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM conversations 
WHERE user_id = '...' 
ORDER BY last_message_at DESC 
LIMIT 20;
```

#### WebSocket Connection Issues
```javascript
// Client-side debugging
const ws = new WebSocket('wss://api.virtualcompanion.ai/ws');

ws.onerror = (error) => {
    console.error('WebSocket error:', error);
    // Implement exponential backoff
    reconnectWithBackoff();
};

ws.onclose = (event) => {
    console.log(`WebSocket closed: ${event.code} - ${event.reason}`);
    if (event.code !== 1000) {
        // Abnormal closure
        handleAbnormalClosure(event.code);
    }
};
```

### 2. Scripts de Diagnostic

```bash
#!/bin/bash
# health-check.sh

echo "=== Virtual Companion Health Check ==="
echo

# Check pods status
echo "Pod Status:"
kubectl get pods -n virtual-companion -o wide

echo -e "\n=== Service Endpoints ==="
for svc in user-service character-service conversation-service; do
    echo -n "$svc: "
    kubectl exec -n virtual-companion deploy/api-gateway -- \
        curl -s -o /dev/null -w "%{http_code}" http://$svc:8080/actuator/health
    echo
done

echo -e "\n=== Database Connections ==="
kubectl exec -n virtual-companion deploy/user-service -- \
    curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq '.measurements[0].value'

echo -e "\n=== Redis Status ==="
kubectl exec -n virtual-companion deploy/redis -- redis-cli ping

echo -e "\n=== Recent Errors ==="
kubectl logs -n virtual-companion -l app=api-gateway --tail=50 | grep ERROR | tail -10
```

### 3. Rollback Procedure

```bash
#!/bin/bash
# rollback.sh

NAMESPACE="virtual-companion"
SERVICE=$1
REVISION=$2

if [ -z "$SERVICE" ] || [ -z "$REVISION" ]; then
    echo "Usage: ./rollback.sh <service> <revision>"
    exit 1
fi

echo "Rolling back $SERVICE to revision $REVISION..."

# Rollback deployment
kubectl rollout undo deployment/$SERVICE -n $NAMESPACE --to-revision=$REVISION

# Wait for rollout to complete
kubectl rollout status deployment/$SERVICE -n $NAMESPACE

# Verify health
sleep 10
kubectl exec -n $NAMESPACE deploy/api-gateway -- \
    curl -s http://$SERVICE:8080/actuator/health | jq .

echo "Rollback completed"
```

## Scripts de Migration

### 1. Migration de Données

```sql
-- migrate_to_v2.sql
BEGIN;

-- Ajouter nouvelles colonnes
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferences JSONB DEFAULT '{}';
ALTER TABLE characters ADD COLUMN IF NOT EXISTS voice_id VARCHAR(50);
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

-- Créer nouveaux index
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_preferences 
ON users USING gin(preferences);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_conversations_metadata 
ON conversations USING gin(metadata);

-- Migrer données existantes
UPDATE users 
SET preferences = jsonb_build_object(
    'language', COALESCE(language, 'en'),
    'theme', COALESCE(theme, 'light'),
    'notifications', true
)
WHERE preferences = '{}';

-- Créer nouvelles tables
CREATE TABLE IF NOT EXISTS voice_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    sample_url TEXT NOT NULL,
    model_data JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Fonction de mise à jour automatique
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_voice_profiles_updated_at
BEFORE UPDATE ON voice_profiles
FOR EACH ROW
EXECUTE FUNCTION update_updated_at();

COMMIT;
```

### 2. Script de Backup

```bash
#!/bin/bash
# backup.sh

set -e

NAMESPACE="virtual-companion"
BACKUP_DIR="/backups/$(date +%Y%m%d_%H%M%S)"
S3_BUCKET="virtual-companion-backups"

echo "Starting backup..."
mkdir -p $BACKUP_DIR

# Backup PostgreSQL
echo "Backing up PostgreSQL..."
kubectl exec -n $NAMESPACE postgres-0 -- \
    pg_dump -U postgres virtualcompanion | gzip > $BACKUP_DIR/postgres.sql.gz

# Backup Redis
echo "Backing up Redis..."
kubectl exec -n $NAMESPACE deploy/redis -- \
    redis-cli BGSAVE

sleep 5

kubectl cp $NAMESPACE/redis-0:/data/dump.rdb $BACKUP_DIR/redis.rdb

# Backup configurations
echo "Backing up configurations..."
kubectl get configmap -n $NAMESPACE -o yaml > $BACKUP_DIR/configmaps.yaml
kubectl get secret -n $NAMESPACE -o yaml > $BACKUP_DIR/secrets.yaml

# Upload to S3
echo "Uploading to S3..."
aws s3 cp $BACKUP_DIR s3://$S3_BUCKET/$(date +%Y/%m/%d)/ --recursive

# Cleanup old backups
find /backups -type d -mtime +7 -exec rm -rf {} \;

echo "Backup completed successfully"
```

### 3. Disaster Recovery

```bash
#!/bin/bash
# disaster-recovery.sh

set -e

BACKUP_DATE=$1
S3_BUCKET="virtual-companion-backups"
NAMESPACE="virtual-companion"

if [ -z "$BACKUP_DATE" ]; then
    echo "Usage: ./disaster-recovery.sh YYYYMMDD"
    exit 1
fi

echo "Starting disaster recovery from backup $BACKUP_DATE..."

# Download backup from S3
RESTORE_DIR="/tmp/restore_$BACKUP_DATE"
mkdir -p $RESTORE_DIR

aws s3 cp s3://$S3_BUCKET/$BACKUP_DATE/ $RESTORE_DIR --recursive

# Restore PostgreSQL
echo "Restoring PostgreSQL..."
kubectl exec -i -n $NAMESPACE postgres-0 -- \
    psql -U postgres -c "DROP DATABASE IF EXISTS virtualcompanion;"

kubectl exec -i -n $NAMESPACE postgres-0 -- \
    psql -U postgres -c "CREATE DATABASE virtualcompanion;"

gunzip -c $RESTORE_DIR/postgres.sql.gz | \
    kubectl exec -i -n $NAMESPACE postgres-0 -- \
    psql -U postgres virtualcompanion

# Restore Redis
echo "Restoring Redis..."
kubectl cp $RESTORE_DIR/redis.rdb $NAMESPACE/redis-0:/data/dump.rdb
kubectl exec -n $NAMESPACE deploy/redis -- redis-cli SHUTDOWN NOSAVE
kubectl rollout restart deployment/redis -n $NAMESPACE
kubectl rollout status deployment/redis -n $NAMESPACE

# Verify restoration
echo "Verifying restoration..."
./health-check.sh

echo "Disaster recovery completed"
```

## Conclusion

Cette documentation couvre tous les aspects essentiels du projet Virtual Companion :

- ✅ Architecture complète et scalable
- ✅ API REST et WebSocket documentées
- ✅ Guide de déploiement production détaillé
- ✅ Sécurité et conformité GDPR
- ✅ Monitoring et observabilité
- ✅ Plan de scaling et optimisation
- ✅ Scripts de maintenance et troubleshooting

Le projet est maintenant **prêt pour la production** avec tous les composants nécessaires pour une plateforme SaaS robuste et scalable.

### Prochaines Étapes Recommandées

1. **Phase 1** : Déployer l'infrastructure de base et les services core
2. **Phase 2** : Implémenter les tests de charge et optimiser
3. **Phase 3** : Lancer en beta fermée avec 100 utilisateurs
4. **Phase 4** : Scaling progressif et ouverture publique

Temps estimé jusqu'au lancement : **8-10 semaines** avec une équipe de 3-4 développeurs.