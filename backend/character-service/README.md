# Documentation Module Character Service

## Vue d'ensemble

Le Character Service gère la création, la personnalisation et la gestion des personnages virtuels dans l'application. Il
inclut la gestion des personnalités, apparences, voix, et comportements des personnages IA.

## Architecture du Module

### Technologies Utilisées

- **Java 21** avec Spring Boot 3.2.0
- **PostgreSQL** pour la persistance
- **Elasticsearch** pour la recherche de personnages
- **Redis** pour le cache
- **MinIO** pour le stockage des médias
- **DJL (Deep Java Library)** pour l'intégration IA
- **Kafka** pour les événements asynchrones

### Structure du Projet

```
character-service/
├── src/
│   ├── main/
│   │   ├── java/com/virtualcompanion/characterservice/
│   │   │   ├── config/         # Configuration Spring et IA
│   │   │   ├── controller/     # REST Controllers
│   │   │   ├── dto/           # Data Transfer Objects
│   │   │   ├── entity/        # Entités JPA
│   │   │   ├── repository/    # Repositories JPA et Elasticsearch
│   │   │   ├── service/       # Services métier
│   │   │   ├── ai/           # Intégration modèles IA
│   │   │   ├── search/       # Service de recherche
│   │   │   └── storage/      # Service de stockage
│   │   └── resources/
│   │       ├── db/migration/  # Scripts Flyway
│   │       ├── ai/models/     # Modèles IA locaux
│   │       └── application.yml
│   └── test/
└── pom.xml
```

## Installation et Configuration

### Prérequis

1. **Service User** doit être opérationnel
2. **PostgreSQL** avec le schéma `character_service`
3. **Elasticsearch** pour la recherche
4. **MinIO** pour le stockage des médias
5. **Redis** pour le cache

### Configuration

#### 1. Variables d'Environnement

```bash
# Base de données
DB_HOST=localhost
DB_PORT=5432
DB_NAME=virtual_companion_db
DB_SCHEMA=character_service

# Elasticsearch
ELASTIC_HOST=localhost
ELASTIC_PORT=9200

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=your_access_key
MINIO_SECRET_KEY=your_secret_key

# IA/ML
AI_MODEL_PATH=/models/character-ai
OPENAI_API_KEY=your_api_key  # Pour la modération
```

#### 2. Démarrage du Service

```bash
cd character-service
mvn spring-boot:run -Dspring.profiles.active=development
```

Le service sera accessible sur `http://localhost:8082`

## API Endpoints

### Personnages Publics

#### Rechercher des Personnages

```http
GET /api/v1/characters/search
Query Parameters:
  - query: string (recherche textuelle)
  - category: string (FRIEND, ROMANTIC, etc.)
  - language: string (code langue)
  - accessLevel: string (FREE, PREMIUM, etc.)
  - minRating: number
  - sortBy: string (popularity, rating, newest)
  - page: number
  - size: number
```

Exemple de réponse :

```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Sophia",
      "description": "Une amie bienveillante et empathique",
      "category": "FRIEND",
      "accessLevel": "FREE",
      "images": {
        "profile": "https://cdn.example.com/sophia-profile.jpg",
        "thumbnail": "https://cdn.example.com/sophia-thumb.jpg"
      },
      "rating": 4.8,
      "interactionCount": 15420,
      "tags": [
        "friendly",
        "supportive",
        "empathetic"
      ],
      "languages": [
        "fr",
        "en",
        "es"
      ]
    }
  ],
  "totalElements": 125,
  "totalPages": 13,
  "pageNumber": 0,
  "pageSize": 10
}
```

#### Obtenir un Personnage

```http
GET /api/v1/characters/{characterId}
```

#### Explorer les Personnages

```http
GET /api/v1/characters/explore
Query Parameters:
  - type: string (trending, new, recommended)
```

### Gestion des Personnages (Authentifié)

#### Mes Personnages

```http
GET /api/v1/characters/my-characters
Authorization: Bearer {token}
```

#### Ajouter un Personnage

```http
POST /api/v1/characters/{characterId}/add
Authorization: Bearer {token}
```

#### Retirer un Personnage

```http
DELETE /api/v1/characters/{characterId}/remove
Authorization: Bearer {token}
```

#### Marquer comme Favori

```http
PUT /api/v1/characters/{characterId}/favorite
Authorization: Bearer {token}
```

#### Personnaliser un Personnage

```http
PUT /api/v1/characters/{characterId}/customize
Authorization: Bearer {token}
Content-Type: application/json

{
  "customName": "Mon Sophia",
  "preferredVoice": "voice-id-123",
  "personalityAdjustments": {
    "humor_level": 75,
    "formality_level": 30
  }
}
```

### Évaluation et Feedback

#### Évaluer un Personnage

```http
POST /api/v1/characters/{characterId}/rate
Authorization: Bearer {token}
Content-Type: application/json

{
  "rating": 5,
  "review": "Personnage très bien développé et attachant!"
}
```

#### Obtenir les Avis

```http
GET /api/v1/characters/{characterId}/reviews
Query Parameters:
  - page: number
  - size: number
  - sortBy: string (newest, helpful, rating)
```

### Création de Personnages (Premium)

#### Créer un Personnage Custom

```http
POST /api/v1/characters/create
Authorization: Bearer {token}
Content-Type: multipart/form-data

{
  "name": "Mon Personnage",
  "description": "Description du personnage",
  "category": "CUSTOM",
  "personality": {
    "traits": {...},
    "interests": [...]
  },
  "appearance": {...},
  "profileImage": [file],
  "isPrivate": true
}
```

#### Mettre à Jour un Personnage

```http
PUT /api/v1/characters/{characterId}
Authorization: Bearer {token}
```

#### Gérer les Images

```http
POST /api/v1/characters/{characterId}/images
Authorization: Bearer {token}
Content-Type: multipart/form-data

{
  "image": [file],
  "imageType": "PROFILE"
}
```

### Administration (Admin Only)

#### Modérer un Personnage

```http
POST /api/v1/admin/characters/{characterId}/moderate
Authorization: Bearer {token}
Content-Type: application/json

{
  "action": "APPROVE|REJECT|SUSPEND",
  "reason": "Contenu inapproprié"
}
```

#### Statistiques Globales

```http
GET /api/v1/admin/characters/statistics
Authorization: Bearer {token}
```

## Modèles de Données

### Character DTO

```json
{
  "id": "uuid",
  "name": "string",
  "description": "string",
  "backstory": "string",
  "category": "enum",
  "accessLevel": "enum",
  "personality": {
    "openness": 0-100,
    "conscientiousness": 0-100,
    "extraversion": 0-100,
    "agreeableness": 0-100,
    "neuroticism": 0-100,
    "communicationStyle": {
      "formality": 0-100,
      "humor": 0-100,
      "empathy": 0-100,
      "assertiveness": 0-100
    },
    "interests": [
      "string"
    ],
    "traits": {
      "key": "value"
    }
  },
  "appearance": {
    "age": "number",
    "gender": "string",
    "ethnicity": "string",
    "physicalTraits": {
      ...
    }
  },
  "voices": [
    {
      "languageCode": "string",
      "voiceId": "string",
      "characteristics": {
        ...
      }
    }
  ],
  "supportedLanguages": [
    "string"
  ],
  "tags": [
    "string"
  ],
  "statistics": {
    "popularityScore": "number",
    "interactionCount": "number",
    "averageRating": "number",
    "ratingCount": "number"
  }
}
```

## Intégration IA

### Modèles Utilisés

1. **Génération de Personnalité** : Modèle local basé sur BERT fine-tuné
2. **Cohérence Comportementale** : Système de vérification par embeddings
3. **Adaptation Dynamique** : Apprentissage des préférences utilisateur

### Pipeline de Traitement

```
1. Analyse de la personnalité définie
   ↓
2. Génération des patterns de réponse
   ↓
3. Validation de cohérence
   ↓
4. Optimisation pour l'utilisateur
```

## Système de Cache

### Stratégie de Cache

- **Personnages populaires** : Cache Redis avec TTL de 1 heure
- **Recherches fréquentes** : Cache Elasticsearch
- **Images** : CDN avec cache navigateur
- **Préférences utilisateur** : Cache local avec sync

### Invalidation du Cache

```java
// Automatique lors de :
-Mise à
jour du
personnage
-

Nouvelle évaluation(si impact significatif)
-
Changement de
statut
-
Toutes les 24

h(cache global)
```

## Sécurité et Modération

### Validation des Contenus

1. **Création** : Vérification automatique via OpenAI Moderation API
2. **Images** : Analyse de contenu inapproprié
3. **Textes** : Détection de contenus sensibles
4. **Review manuel** : File d'attente de modération

### Restrictions d'Accès

- Vérification de l'âge pour certains personnages
- Géoblocage selon juridiction
- Limites par niveau d'abonnement

## Performance et Optimisation

### Métriques Clés

- Temps de chargement personnage : < 200ms
- Recherche : < 500ms
- Upload image : < 3s (compression incluse)

### Optimisations Appliquées

1. **Lazy Loading** : Chargement progressif des détails
2. **Compression Images** : WebP avec fallback JPEG
3. **Pagination** : Limite de 20 items par page
4. **Index Elasticsearch** : Recherche optimisée

## Monitoring

### Métriques Exposées (Prometheus)

```
character_search_requests_total
character_creation_total
character_rating_average
character_interaction_duration_seconds
character_cache_hit_ratio
```

### Dashboards Grafana

- Vue d'ensemble des personnages populaires
- Taux d'utilisation par catégorie
- Performance des recherches
- Erreurs et latences

## Troubleshooting

### Problème : Recherche lente

```bash
# Vérifier l'état d'Elasticsearch
curl -X GET "localhost:9200/_cluster/health?pretty"

# Réindexer si nécessaire
mvn spring-boot:run -Dspring.boot.run.arguments=--reindex
```

### Problème : Images non affichées

```bash
# Vérifier MinIO
mc admin info minio/

# Vérifier les permissions des buckets
mc policy get minio/avatars
```

### Problème : Personnalité incohérente

```bash
# Recharger le modèle IA
curl -X POST http://localhost:8082/admin/ai/reload-models \
  -H "Authorization: Bearer {admin-token}"
```

## Scripts Utiles

### Import en Masse de Personnages

```bash
./scripts/import-characters.sh characters.json
```

### Export des Statistiques

```bash
./scripts/export-character-stats.sh --format=csv --output=stats.csv
```

### Nettoyage des Personnages Inactifs

```bash
./scripts/cleanup-inactive-characters.sh --days=180 --dry-run
```

## Intégration avec Autres Services

### User Service

- Vérification des permissions utilisateur
- Récupération du niveau d'abonnement
- Validation de l'âge

### Conversation Service

- Fourniture du contexte de personnalité
- Historique des interactions
- Métriques de satisfaction

### Media Service

- Stockage et traitement des images
- Génération de voix
- Animations 3D

### Billing Service

- Vérification accès premium
- Décompte des personnages custom
- Facturation des extras