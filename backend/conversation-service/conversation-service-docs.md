# Documentation Module Conversation Service

## Vue d'ensemble

Le Conversation Service est le cœur de l'application Virtual Companion. Il gère toutes les interactions en temps réel entre les utilisateurs et les personnages virtuels, incluant le traitement du langage naturel, la gestion de la mémoire conversationnelle, et le streaming des réponses.

## Architecture du Module

### Technologies Utilisées
- **Spring WebFlux** pour la programmation réactive
- **PostgreSQL** avec R2DBC pour les métadonnées
- **MongoDB** pour le stockage des messages
- **Redis** pour le cache et les sessions temps réel
- **WebSocket/SSE** pour le streaming
- **Kafka** pour l'event streaming
- **LangChain4j** pour l'orchestration LLM
- **Milvus** pour la base de données vectorielle

### Architecture Réactive
```
Client (WebSocket/REST)
    ↓
API Gateway
    ↓
Conversation Service (WebFlux)
    ├── Message Handler (Reactive Streams)
    ├── AI Processor (Async)
    ├── Memory Manager (Vector DB)
    └── Analytics Pipeline (Kafka)
```

## Installation et Configuration

### Prérequis
1. **MongoDB** pour le stockage des messages
2. **Milvus** pour les embeddings vectoriels
3. **Modèle LLM** local (Llama 2, Mistral, etc.)
4. Services **User** et **Character** opérationnels

### Configuration Spécifique

#### 1. MongoDB
```bash
# Créer la base de données
mongosh
use virtual_companion_messages
db.createCollection("messages")
db.createCollection("conversation_contexts")

# Créer les index
db.messages.createIndex({ "conversationId": 1, "timestamp": -1 })
db.messages.createIndex({ "userId": 1 })
db.conversation_contexts.createIndex({ "conversationId": 1 }, { unique: true })
```

#### 2. Milvus (Vector Database)
```bash
# Démarrer Milvus avec Docker
docker-compose -f milvus-docker-compose.yml up -d

# Créer la collection via script
python scripts/init-milvus.py
```

#### 3. Modèle IA Local
```bash
# Télécharger le modèle
./scripts/download-model.sh llama2-7b-chat

# Ou utiliser un modèle personnalisé
cp /path/to/your/model /models/character-ai/
```

## API Endpoints

### WebSocket Endpoint

#### Connexion WebSocket
```javascript
const ws = new WebSocket('ws://localhost:8083/ws/chat');

ws.on('open', () => {
  ws.send(JSON.stringify({
    type: 'AUTH',
    token: 'Bearer {jwt_token}',
    conversationId: 'conversation-uuid'
  }));
});

ws.on('message', (data) => {
  const message = JSON.parse(data);
  // Types: MESSAGE, TYPING, ERROR, STATUS
});
```

### REST API

#### Créer une Conversation
```http
POST /api/v1/conversations
Authorization: Bearer {token}
Content-Type: application/json

{
  "characterId": "character-uuid",
  "title": "Nouvelle conversation avec Sophia",
  "languageCode": "fr"
}
```

#### Envoyer un Message
```http
POST /api/v1/conversations/{conversationId}/messages
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "Bonjour, comment vas-tu aujourd'hui ?",
  "attachments": []
}
```

#### Obtenir l'Historique
```http
GET /api/v1/conversations/{conversationId}/messages
Authorization: Bearer {token}
Query Parameters:
  - page: number
  - size: number
  - before: timestamp
```

#### Streaming SSE
```http
GET /api/v1/conversations/{conversationId}/stream
Authorization: Bearer {token}
Accept: text/event-stream
```

### Gestion des Conversations

#### Lister mes Conversations
```http
GET /api/v1/conversations
Authorization: Bearer {token}
Query Parameters:
  - characterId: uuid (optionnel)
  - status: ACTIVE|ARCHIVED
  - sortBy: lastMessage|created
  - page: number
  - size: number
```

#### Mettre à Jour une Conversation
```http
PATCH /api/v1/conversations/{conversationId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Nouveau titre",
  "isFavorite": true,
  "tags": ["important", "philosophie"]
}
```

#### Archiver/Supprimer
```http
PUT /api/v1/conversations/{conversationId}/archive
DELETE /api/v1/conversations/{conversationId}
Authorization: Bearer {token}
```

### Fonctionnalités Avancées

#### Recherche dans les Conversations
```http
POST /api/v1/conversations/search
Authorization: Bearer {token}
Content-Type: application/json

{
  "query": "discussion sur les voyages",
  "conversationIds": ["uuid1", "uuid2"],
  "dateRange": {
    "from": "2024-01-01",
    "to": "2024-12-31"
  }
}
```

#### Obtenir le Contexte
```http
GET /api/v1/conversations/{conversationId}/context
Authorization: Bearer {token}
```

#### Régénérer une Réponse
```http
POST /api/v1/conversations/{conversationId}/messages/{messageId}/regenerate
Authorization: Bearer {token}
```

#### Feedback sur un Message
```http
POST /api/v1/conversations/{conversationId}/messages/{messageId}/feedback
Authorization: Bearer {token}
Content-Type: application/json

{
  "rating": "positive|negative",
  "reason": "inappropriate|inaccurate|other",
  "comment": "Détails optionnels"
}
```

## Système de Mémoire

### Architecture de la Mémoire
```
Mémoire Court Terme (Redis)
├── 10 derniers messages
├── État émotionnel actuel
└── Variables de session

Mémoire Long Terme (Milvus)
├── Faits importants
├── Préférences utilisateur
├── Historique émotionnel
└── Milestones relationnels
```

### Gestion des Mémoires
```http
GET /api/v1/conversations/{conversationId}/memories
POST /api/v1/conversations/{conversationId}/memories
DELETE /api/v1/conversations/{conversationId}/memories/{memoryId}
```

## Modération et Sécurité

### Pipeline de Modération
1. **Pré-filtrage** : Détection de contenu inapproprié
2. **Analyse de Toxicité** : Score de 0 à 1
3. **Vérification Contextuelle** : Cohérence avec la conversation
4. **Post-traitement** : Censure ou reformulation si nécessaire

### Configuration de Sécurité
```yaml
conversation:
  safety:
    content-filter-enabled: true
    toxicity-threshold: 0.7
    blocked-topics: 
      - violence
      - explicit_content
      - illegal_activities
```

## Performance et Optimisation

### Métriques de Performance
- **Latence P50** : < 200ms
- **Latence P95** : < 500ms
- **Latence P99** : < 1s
- **Throughput** : 1000 messages/seconde

### Optimisations Appliquées

#### 1. Cache Intelligent
```java
// Cache des réponses similaires
@Cacheable(value = "ai-responses", 
           condition = "#similarity > 0.95")
public Mono<String> getCachedResponse(String input)
```

#### 2. Batching des Embeddings
```java
// Traitement par batch de 32
embeddingService.batchEmbed(messages, 32)
```

#### 3. Streaming Réactif
```java
// Utilisation de Flux pour le streaming
Flux<MessageChunk> streamResponse(String input)
    .delayElements(Duration.ofMillis(50))
    .onBackpressureBuffer(100)
```

## Monitoring et Analytics

### Métriques Exposées
```
# Messages
conversation_messages_total
conversation_messages_per_second
conversation_message_processing_duration

# WebSocket
websocket_connections_active
websocket_messages_sent_total
websocket_errors_total

# AI Processing
ai_inference_duration_seconds
ai_tokens_processed_total
ai_cache_hit_ratio

# Memory System
memory_operations_total
memory_search_duration_seconds
vector_db_operations_total
```

### Dashboard Grafana
- **Vue Temps Réel** : Conversations actives, messages/sec
- **Performance IA** : Latence, utilisation GPU/CPU
- **Santé du Système** : Erreurs, timeouts, saturation

## Troubleshooting

### WebSocket ne se connecte pas
```bash
# Vérifier les logs
docker logs conversation-service | grep WebSocket

# Tester la connexion
wscat -c ws://localhost:8083/ws/chat \
  -H "Authorization: Bearer {token}"
```

### Messages lents
```bash
# Vérifier la charge du modèle IA
curl http://localhost:8083/actuator/metrics/ai.inference.duration

# Analyser les requêtes lentes
grep "duration > 1000ms" logs/conversation-service.log
```

### Mémoire non persistée
```bash
# Vérifier Milvus
curl http://localhost:19121/api/v1/health

# Réindexer les vecteurs
./scripts/reindex-memories.sh --conversation-id {id}
```

## Scripts Utiles

### Export de Conversation
```bash
./scripts/export-conversation.sh \
  --conversation-id {id} \
  --format markdown \
  --output conversation.md
```

### Analyse de Performance
```bash
./scripts/analyze-conversation.sh \
  --conversation-id {id} \
  --metrics latency,tokens,sentiment
```

### Migration de Données
```bash
./scripts/migrate-conversations.sh \
  --from mongodb://old-server \
  --to mongodb://new-server \
  --batch-size 1000
```

## Intégration avec Autres Services

### Événements Kafka Émis
```json
// conversation-events
{
  "eventType": "CONVERSATION_STARTED",
  "conversationId": "uuid",
  "userId": "uuid",
  "characterId": "uuid",
  "timestamp": "2024-01-15T10:30:00Z"
}

// message-events
{
  "eventType": "MESSAGE_SENT",
  "messageId": "uuid",
  "conversationId": "uuid",
  "userId": "uuid",
  "messageType": "USER_MESSAGE",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Appels Inter-Services
- **User Service** : Vérification des permissions et limites
- **Character Service** : Récupération de la personnalité
- **Media Service** : Traitement des pièces jointes
- **Billing Service** : Décompte des tokens utilisés

## Développement Local

### Mode Debug
```bash
# Activer les logs détaillés
export LOG_LEVEL=DEBUG
export AI_DEBUG_MODE=true

# Lancer avec profil dev
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Tests de Charge
```bash
# Utiliser Gatling
mvn gatling:test -Dgatling.simulationClass=ConversationLoadTest
```

### Simulateur de Conversation
```bash
# Tester sans frontend
./scripts/conversation-simulator.sh \
  --character-id {id} \
  --messages 100 \
  --concurrent-users 10
```