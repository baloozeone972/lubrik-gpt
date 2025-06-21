# Documentation Module Moderation Service

## Vue d'ensemble

Le Moderation Service est le gardien de la sécurité et de la conformité légale de l'application. Il analyse tout le contenu (texte, images, audio, vidéo) pour détecter et bloquer le contenu inapproprié, vérifie l'âge des utilisateurs, et maintient un environnement sûr pour tous.

## Architecture du Module

### Technologies Utilisées
- **TensorFlow/PyTorch** pour les modèles de modération locaux
- **OpenAI Moderation API** pour l'analyse de texte
- **Google Vision API** pour l'analyse d'images
- **AWS Rekognition** pour la détection de contenu inapproprié
- **Stanford NLP** pour l'analyse linguistique
- **PostgreSQL** pour l'historique et les règles

### Pipeline de Modération
```
Contenu → Pré-filtrage → Analyse Multi-Providers → Scoring → Décision → Action
                              ↓
                        Human Review (si nécessaire)
```

## Installation et Configuration

### Prérequis
1. **Modèles IA** téléchargés localement
2. **Clés API** pour les providers externes
3. **GPU** recommandé pour les modèles locaux

### Configuration des Modèles Locaux

```bash
# Télécharger les modèles
cd /models
wget https://models.virtualcompanion.app/content-moderation-v2.tar.gz
tar -xzf content-moderation-v2.tar.gz

# Modèles requis :
# - text-toxicity-bert
# - image-nsfw-resnet
# - age-estimation-vgg
```

### Configuration des Providers

```yaml
# OpenAI
OPENAI_API_KEY=sk-your-key
OPENAI_MODERATION_ENABLED=true

# Google Vision
GOOGLE_CREDENTIALS_PATH=/config/google-vision-key.json
GOOGLE_VISION_ENABLED=true

# AWS Rekognition
AWS_REGION=eu-west-1
AWS_ACCESS_KEY=your-key
AWS_SECRET_KEY=your-secret
AWS_REKOGNITION_ENABLED=true
```

## API Endpoints

### Modération de Contenu

#### Analyser du Texte
```http
POST /api/v1/moderation/analyze/text
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "Message à analyser",
  "context": {
    "conversationId": "uuid",
    "characterId": "uuid",
    "languageCode": "fr"
  },
  "priority": "NORMAL"
}
```

Réponse :
```json
{
  "requestId": "mod-123e4567",
  "isSafe": true,
  "confidence": 0.95,
  "categories": {
    "toxicity": 0.05,
    "severeToxicity": 0.01,
    "threat": 0.02,
    "insult": 0.03,
    "profanity": 0.02,
    "sexual": 0.01
  },
  "action": "PASS",
  "warnings": [],
  "processingTime": 150
}
```

#### Analyser une Image
```http
POST /api/v1/moderation/analyze/image
Authorization: Bearer {token}
Content-Type: multipart/form-data

image: [file]
context[type]: "avatar"
priority: "HIGH"
```

#### Analyser de l'Audio
```http
POST /api/v1/moderation/analyze/audio
Authorization: Bearer {token}
Content-Type: multipart/form-data

audio: [file]
context[type]: "voice_message"
transcribe: true
```

#### Modération Batch
```http
POST /api/v1/moderation/analyze/batch
Authorization: Bearer {token}
Content-Type: application/json

{
  "items": [
    {
      "id": "item-1",
      "type": "TEXT",
      "content": "Premier message"
    },
    {
      "id": "item-2",
      "type": "IMAGE",
      "url": "https://cdn.example.com/image.jpg"
    }
  ]
}
```

### Vérification d'Âge

#### Initier la Vérification
```http
POST /api/v1/moderation/age-verification/initiate
Authorization: Bearer {token}
Content-Type: application/json

{
  "method": "DOCUMENT_UPLOAD",
  "consentGiven": true
}
```

#### Upload du Document
```http
POST /api/v1/moderation/age-verification/document
Authorization: Bearer {token}
Content-Type: multipart/form-data

document: [file]
documentType: "PASSPORT"
verificationId: "ver-123"
```

#### Statut de Vérification
```http
GET /api/v1/moderation/age-verification/status
Authorization: Bearer {token}
```

Réponse :
```json
{
  "status": "VERIFIED",
  "isAdult": true,
  "verifiedAt": "2024-01-15T10:00:00Z",
  "expiresAt": "2025-01-15T10:00:00Z",
  "method": "DOCUMENT_UPLOAD"
}
```

### Gestion des Règles (Admin)

#### Lister les Règles
```http
GET /api/v1/moderation/rules
Authorization: Bearer {admin-token}
Query Parameters:
  - category: profanity|spam|violence
  - active: true|false
```

#### Créer une Règle
```http
POST /api/v1/moderation/rules
Authorization: Bearer {admin-token}
Content-Type: application/json

{
  "ruleName": "custom-spam-detector",
  "ruleType": "REGEX",
  "pattern": "(buy|cheap|discount).*?(now|today|limited)",
  "category": "spam",
  "severity": 5,
  "action": "BLOCK",
  "appliesToContentTypes": ["TEXT_MESSAGE", "CHARACTER_PROFILE"]
}
```

#### Mettre à Jour une Règle
```http
PUT /api/v1/moderation/rules/{ruleId}
Authorization: Bearer {admin-token}
```

### Historique et Statistiques

#### Historique Utilisateur
```http
GET /api/v1/moderation/users/{userId}/history
Authorization: Bearer {admin-token}
Query Parameters:
  - page: 0
  - size: 20
  - actionType: WARNING|CONTENT_REMOVED|SUSPENDED
```

#### Score de Risque Utilisateur
```http
GET /api/v1/moderation/users/{userId}/risk-score
Authorization: Bearer {admin-token}
```

Réponse :
```json
{
  "userId": "user-123",
  "overallRiskScore": 0.25,
  "scores": {
    "toxicity": 0.15,
    "spam": 0.30,
    "violations": 0.20
  },
  "history": {
    "violationCount": 2,
    "warningCount": 5,
    "contentBlockedCount": 3,
    "lastViolationAt": "2024-01-10T15:30:00Z"
  },
  "riskLevel": "MEDIUM"
}
```

#### Statistiques Globales
```http
GET /api/v1/moderation/statistics
Authorization: Bearer {admin-token}
Query Parameters:
  - period: 24h|7d|30d
  - groupBy: hour|day|contentType
```

### Actions de Modération

#### Avertir un Utilisateur
```http
POST /api/v1/moderation/actions/warn
Authorization: Bearer {admin-token}
Content-Type: application/json

{
  "userId": "user-123",
  "reason": "Langage inapproprié répété",
  "severity": 3,
  "message": "Merci de respecter les règles de la communauté"
}
```

#### Suspendre un Utilisateur
```http
POST /api/v1/moderation/actions/suspend
Authorization: Bearer {admin-token}
Content-Type: application/json

{
  "userId": "user-123",
  "reason": "Violations répétées des règles",
  "durationHours": 72,
  "notifyUser": true
}
```

#### Bloquer du Contenu
```http
POST /api/v1/moderation/actions/block-content
Authorization: Bearer {admin-token}
Content-Type: application/json

{
  "contentHash": "sha256:abc123...",
  "reason": "Contenu illégal",
  "severity": 10,
  "permanent": true
}
```

### File d'Attente de Révision Humaine

#### Obtenir les Items à Réviser
```http
GET /api/v1/moderation/review-queue
Authorization: Bearer {moderator-token}
Query Parameters:
  - priority: CRITICAL|HIGH|NORMAL
  - contentType: TEXT|IMAGE|VIDEO
  - limit: 10
```

#### Soumettre une Décision
```http
POST /api/v1/moderation/review-queue/{requestId}/decision
Authorization: Bearer {moderator-token}
Content-Type: application/json

{
  "decision": "APPROVE|REJECT|ESCALATE",
  "categories": ["spam", "mild_profanity"],
  "notes": "Contexte humoristique, pas d'intention malveillante",
  "actionTaken": "WARN"
}
```

## Catégories de Contenu

### Niveaux de Sévérité
1. **Critique (9-10)** : Contenu illégal, sécurité des enfants
2. **Élevé (7-8)** : Violence, contenu explicite, harcèlement
3. **Moyen (5-6)** : Langage inapproprié, spam
4. **Faible (1-4)** : Contenu borderline, avertissements

### Actions Automatiques
```yaml
child_safety:
  action: IMMEDIATE_BLOCK_AND_REPORT
  notify: [user, admin, authorities]
  
self_harm:
  action: BLOCK_AND_SUPPORT
  message: "Ressources d'aide disponibles..."
  
spam:
  action: SHADOW_BAN
  duration: 24h
  
profanity:
  action: WARN
  max_warnings: 3
```

## Vérification d'Âge

### Méthodes Disponibles
1. **Upload de Document**
   - Passeport, Permis, Carte d'identité
   - OCR + Vérification authenticité
   - Stockage : Hash uniquement

2. **Estimation Faciale**
   - Analyse par IA
   - Confidence minimale : 85%
   - Pas de stockage d'image

3. **Vérification Bancaire**
   - Via Stripe/PayPal
   - Confirmation carte crédit
   - Aucun prélèvement

### Workflow de Vérification
```
1. Consentement utilisateur
2. Choix de la méthode
3. Upload/Capture
4. Analyse automatique
5. Vérification manuelle si nécessaire
6. Résultat et certificat
```

## Conformité Légale

### RGPD/CCPA
- Pas de stockage d'images de documents
- Hash uniquement pour éviter les doublons
- Droit à l'effacement respecté
- Logs anonymisés après 90 jours

### Juridictions Spéciales
```yaml
EU:
  age_minimum: 16
  consent_parental: required_under_16
  
US:
  age_minimum: 13
  coppa_compliance: true
  state_specific_rules: true
  
UK:
  age_minimum: 13
  age_appropriate_design: true
```

## Performance et Optimisation

### Cache Strategy
```yaml
user_risk_scores:
  ttl: 1h
  invalidate_on: [new_violation, appeal_success]
  
moderation_results:
  ttl: 24h
  key: content_hash + provider
  
blocked_content:
  ttl: 7d
  storage: redis_bloom_filter
```

### Traitement Asynchrone
- Queue prioritaire pour contenu critique
- Batch processing pour efficacité
- Timeout : 5s par item
- Fallback sur modèle local si API down

## Monitoring et Métriques

### KPIs Clés
```
# Efficacité
- Temps moyen de traitement : < 200ms (texte), < 2s (image)
- Taux de faux positifs : < 5%
- Taux de faux négatifs : < 1%

# Volume
- Requêtes/seconde
- Items en file d'attente
- Taux de révision humaine

# Sécurité
- Tentatives de contournement
- Comptes suspendus/jour
- Contenu bloqué/heure
```

### Dashboards
1. **Overview** : Santé globale du système
2. **User Safety** : Incidents et actions
3. **Content Stats** : Types et catégories
4. **Performance** : Latence et throughput

## Troubleshooting

### Faux Positifs Élevés
```bash
# Analyser les patterns
SELECT pattern, COUNT(*) as false_positives
FROM moderation_requests
WHERE reviewed_by IS NOT NULL
AND final_decision = 'SAFE'
GROUP BY pattern
ORDER BY false_positives DESC;

# Ajuster les seuils
UPDATE moderation_rules
SET severity = severity - 1
WHERE id = 'rule-id';
```

### Queue de Révision Saturée
```bash
# Prioriser le contenu critique
UPDATE moderation_requests
SET priority = 'HIGH'
WHERE content_type IN ('IMAGE', 'VIDEO')
AND created_at < NOW() - INTERVAL '1 hour';

# Augmenter les reviewers
kubectl scale deployment moderation-reviewers --replicas=5
```

### Modèle Local Lent
```bash
# Vérifier l'utilisation GPU
nvidia-smi

# Optimiser le batch size
export MODEL_BATCH_SIZE=32

# Utiliser le modèle quantifié
ln -sf /models/text-toxicity-bert-int8 /models/text-toxicity-bert
```

## Scripts d'Administration

### Rapport de Modération
```bash
./scripts/moderation-report.sh \
  --period 7d \
  --format pdf \
  --include-stats \
  --output weekly-report.pdf
```

### Export pour Audit
```bash
./scripts/export-moderation-logs.sh \
  --user-id {userId} \
  --date-range 2024-01-01:2024-01-31 \
  --include-decisions \
  --format csv
```

### Mise à Jour des Règles
```bash
./scripts/update-rules.sh \
  --file new-rules.yaml \
  --test-mode \
  --rollback-on-error
```