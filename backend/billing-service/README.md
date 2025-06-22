# Documentation Module Billing Service

## Vue d'ensemble

Le Billing Service gère l'ensemble du système de facturation et d'abonnements de l'application Virtual Companion. Il
s'intègre avec Stripe et PayPal pour les paiements, gère les différents niveaux d'abonnement, génère les factures et
suit l'utilisation des ressources.

## Architecture du Module

### Technologies Utilisées

- **Spring Boot** pour le framework
- **Stripe Java SDK** pour les paiements
- **PayPal SDK** pour les paiements alternatifs
- **Quartz Scheduler** pour les tâches planifiées
- **iText** pour la génération de PDF
- **PostgreSQL** pour la persistance

### Flux de Paiement

```
Utilisateur → Choix Plan → Paiement (Stripe/PayPal) → Activation → Facture
                              ↓ Webhook
                         Mise à jour Status
```

## Installation et Configuration

### Prérequis

1. Compte **Stripe** avec clés API
2. Compte **PayPal** Business (optionnel)
3. Service **User** opérationnel

### Configuration Stripe

#### 1. Créer les Produits dans Stripe

```bash
# Via Stripe CLI
stripe products create \
  --name="Virtual Companion Standard" \
  --description="Abonnement Standard"

stripe prices create \
  --product=prod_xxx \
  --unit-amount=999 \
  --currency=eur \
  --recurring[interval]=month
```

#### 2. Configurer les Webhooks

```bash
# Endpoint local pour développement
stripe listen --forward-to localhost:8085/api/v1/webhooks/stripe

# Production
# Ajouter https://api.virtualcompanion.app/api/v1/webhooks/stripe
# Events à écouter :
# - payment_intent.succeeded
# - payment_intent.failed
# - customer.subscription.created
# - customer.subscription.updated
# - customer.subscription.deleted
# - invoice.payment_succeeded
# - invoice.payment_failed
```

### Configuration PayPal

```bash
# Variables d'environnement
PAYPAL_CLIENT_ID=your_client_id
PAYPAL_CLIENT_SECRET=your_secret
PAYPAL_MODE=sandbox # ou live

# Webhook URL
# https://api.virtualcompanion.app/api/v1/webhooks/paypal
```

## API Endpoints

### Plans et Tarifs

#### Obtenir les Plans Disponibles

```http
GET /api/v1/billing/plans
```

Réponse :

```json
{
  "plans": [
    {
      "id": "free",
      "name": "Gratuit",
      "price": 0,
      "currency": "EUR",
      "features": [
        "1 personnage",
        "50 messages/jour",
        "Chat texte uniquement"
      ],
      "limits": {
        "maxCharacters": 1,
        "messagesPerDay": 50,
        "tokensPerMonth": 50000
      }
    },
    {
      "id": "standard",
      "name": "Standard",
      "price": 9.99,
      "currency": "EUR",
      "billingCycles": [
        "MONTHLY",
        "YEARLY"
      ],
      "yearlyDiscount": 0.20,
      "features": [
        "3 personnages",
        "500 messages/jour",
        "Chat vocal inclus"
      ]
    }
  ]
}
```

### Gestion des Abonnements

#### Souscrire à un Plan

```http
POST /api/v1/subscriptions/subscribe
Authorization: Bearer {token}
Content-Type: application/json

{
  "planType": "STANDARD",
  "billingCycle": "MONTHLY",
  "paymentMethodId": "pm_xxxx", // Stripe payment method
  "promoCode": "WELCOME20"
}
```

#### Obtenir mon Abonnement

```http
GET /api/v1/subscriptions/current
Authorization: Bearer {token}
```

Réponse :

```json
{
  "id": "sub-uuid",
  "planType": "STANDARD",
  "status": "ACTIVE",
  "currentPeriodStart": "2024-01-15T00:00:00Z",
  "currentPeriodEnd": "2024-02-15T00:00:00Z",
  "cancelAtPeriodEnd": false,
  "limits": {
    "maxCharacters": 3,
    "messagesPerDay": 500,
    "videoChatEnabled": false,
    "voiceChatEnabled": true
  },
  "nextInvoice": {
    "amount": 9.99,
    "dueDate": "2024-02-15T00:00:00Z"
  }
}
```

#### Changer de Plan

```http
PUT /api/v1/subscriptions/change-plan
Authorization: Bearer {token}
Content-Type: application/json

{
  "newPlanType": "PREMIUM",
  "billingCycle": "YEARLY",
  "immediate": false  // true = prorata immédiat
}
```

#### Annuler l'Abonnement

```http
POST /api/v1/subscriptions/cancel
Authorization: Bearer {token}
Content-Type: application/json

{
  "reason": "Trop cher",
  "feedback": "J'aimerais un plan intermédiaire",
  "cancelImmediately": false
}
```

#### Réactiver l'Abonnement

```http
POST /api/v1/subscriptions/reactivate
Authorization: Bearer {token}
```

### Méthodes de Paiement

#### Ajouter une Carte

```http
POST /api/v1/payment-methods/card
Authorization: Bearer {token}
Content-Type: application/json

{
  "stripePaymentMethodId": "pm_xxxx",
  "setAsDefault": true
}
```

#### Lister mes Méthodes de Paiement

```http
GET /api/v1/payment-methods
Authorization: Bearer {token}
```

#### Supprimer une Méthode

```http
DELETE /api/v1/payment-methods/{methodId}
Authorization: Bearer {token}
```

#### Définir comme Défaut

```http
PUT /api/v1/payment-methods/{methodId}/set-default
Authorization: Bearer {token}
```

### Historique et Factures

#### Historique des Paiements

```http
GET /api/v1/payments/history
Authorization: Bearer {token}
Query Parameters:
  - page: 0
  - size: 20
  - status: COMPLETED|FAILED|REFUNDED
```

#### Obtenir mes Factures

```http
GET /api/v1/invoices
Authorization: Bearer {token}
Query Parameters:
  - year: 2024
  - status: PAID|OPEN
```

#### Télécharger une Facture

```http
GET /api/v1/invoices/{invoiceId}/download
Authorization: Bearer {token}
```

Retourne un PDF de la facture.

#### Demander un Remboursement

```http
POST /api/v1/payments/{paymentId}/refund
Authorization: Bearer {token}
Content-Type: application/json

{
  "reason": "Service non satisfaisant",
  "amount": 9.99,  // optionnel, remboursement partiel
  "details": "Description détaillée"
}
```

### Utilisation et Quotas

#### Obtenir mon Utilisation

```http
GET /api/v1/usage/current
Authorization: Bearer {token}
```

Réponse :

```json
{
  "period": "2024-01",
  "usage": {
    "messagesUsed": 1250,
    "messagesLimit": 15000,
    "tokensUsed": 125000,
    "tokensLimit": 500000,
    "storageUsedGB": 2.5,
    "storageLimitGB": 10,
    "charactersCreated": 2,
    "charactersLimit": 3
  },
  "percentages": {
    "messages": 8.33,
    "tokens": 25.0,
    "storage": 25.0,
    "characters": 66.67
  },
  "resetDate": "2024-02-01T00:00:00Z"
}
```

#### Historique d'Utilisation

```http
GET /api/v1/usage/history
Authorization: Bearer {token}
Query Parameters:
  - months: 6
  - type: MESSAGE_SENT|TOKEN_USED|VIDEO_MINUTE
```

### Webhooks

#### Stripe Webhook

```http
POST /api/v1/webhooks/stripe
Stripe-Signature: {signature}
Content-Type: application/json

{
  "type": "payment_intent.succeeded",
  "data": {
    "object": {
      // Stripe event data
    }
  }
}
```

#### PayPal Webhook

```http
POST /api/v1/webhooks/paypal
PayPal-Transmission-Sig: {signature}
Content-Type: application/json

{
  "event_type": "PAYMENT.CAPTURE.COMPLETED",
  "resource": {
    // PayPal event data
  }
}
```

## Gestion des Abonnements

### États des Abonnements

```
TRIALING → ACTIVE → PAST_DUE → CANCELED
                 ↓
            INCOMPLETE → INCOMPLETE_EXPIRED
```

### Cycle de Vie

1. **Trial** : 7 jours gratuits (nouveaux utilisateurs)
2. **Active** : Paiement réussi, accès complet
3. **Past Due** : Échec de paiement, 3 tentatives sur 7 jours
4. **Canceled** : Annulé par l'utilisateur ou échec définitif
5. **Paused** : Suspension temporaire (sur demande)

### Gestion des Échecs de Paiement

```yaml
retry-schedule:
  attempt-1: immediate
  attempt-2: 3 days
  attempt-3: 5 days
  attempt-4: 7 days
  final: cancel subscription

notifications:
  - payment-failed-email
  - payment-retry-email
  - subscription-canceled-email
```

## Facturation et Conformité

### Format des Factures

Les factures sont générées au format PDF avec :

- Numéro unique (VC-2024-000123)
- Informations légales complètes
- TVA applicable selon le pays
- Détail des services
- QR Code pour paiement (si impayé)

### Conformité Fiscale

```java
// Calcul TVA automatique
-France :20%
        -Allemagne :19%
        -Espagne :21%
        -
Hors UE :0%(
reverse charge)

// Validation numéro TVA
        -
Integration VIES
API
-
Exonération B2B
intra-EU
```

### RGPD et Données de Paiement

- Aucun stockage de données carte
- Tokenisation via Stripe/PayPal
- Droit à l'effacement respecté
- Export des données de facturation

## Jobs et Automatisation

### Jobs Planifiés

```yaml
subscription-renewal:
  schedule: "0 0 2 * * ?" # 2h du matin
  tasks:
    - Vérifier les renouvellements
    - Créer les factures
    - Déclencher les paiements
    - Envoyer les confirmations

usage-reset:
  schedule: "0 0 0 1 * ?" # 1er du mois
  tasks:
    - Réinitialiser les compteurs
    - Archiver l'historique
    - Générer les rapports

payment-retry:
  schedule: "0 0 */4 * * ?" # Toutes les 4h
  tasks:
    - Identifier les échecs
    - Relancer selon schedule
    - Notifier les utilisateurs

cleanup:
  schedule: "0 0 3 * * SUN" # Dimanche 3h
  tasks:
    - Nettoyer les sessions Stripe
    - Archiver anciennes factures
    - Optimiser la base
```

## Métriques et Monitoring

### KPIs Financiers

```
# Revenue Metrics
- MRR (Monthly Recurring Revenue)
- ARR (Annual Recurring Revenue)
- ARPU (Average Revenue Per User)
- Churn Rate
- LTV (Lifetime Value)

# Subscription Metrics
- Conversion Rate (Free → Paid)
- Upgrade Rate (Standard → Premium)
- Cancellation Rate
- Payment Success Rate
```

### Dashboards Grafana

```sql
-- MRR Evolution
SELECT 
  DATE_TRUNC('month', created_at) as month,
  SUM(amount) as mrr
FROM subscriptions
WHERE status = 'ACTIVE'
GROUP BY month;

-- Churn Rate
SELECT 
  DATE_TRUNC('month', canceled_at) as month,
  COUNT(*) * 100.0 / total_active as churn_rate
FROM subscriptions
WHERE status = 'CANCELED'
GROUP BY month;
```

### Alertes Critiques

- Taux d'échec paiement > 10%
- Churn rate > 5% mensuel
- Revenus journaliers < 80% moyenne
- Échec webhook Stripe/PayPal

## Troubleshooting

### Problèmes Courants

#### Webhook non reçu

```bash
# Vérifier la signature
curl -X POST http://localhost:8085/api/v1/webhooks/stripe \
  -H "Stripe-Signature: $SIGNATURE" \
  -d @webhook-payload.json

# Logs Stripe CLI
stripe logs tail
```

#### Abonnement bloqué

```sql
-- Vérifier l'état
SELECT * FROM subscriptions WHERE user_id = 'xxx';
SELECT * FROM payments WHERE user_id = 'xxx' ORDER BY created_at DESC;

-- Forcer la mise à jour
UPDATE subscriptions 
SET status = 'ACTIVE', updated_at = NOW() 
WHERE id = 'xxx';
```

#### Calcul utilisation incorrect

```bash
# Recalculer manuellement
curl -X POST http://localhost:8085/admin/usage/recalculate \
  -H "Authorization: Bearer {admin-token}" \
  -d '{"userId": "xxx", "month": "2024-01"}'
```

## Intégration avec Autres Services

### Events Kafka Émis

```json
// subscription-events
{
  "eventType": "SUBSCRIPTION_CREATED",
  "userId": "uuid",
  "planType": "STANDARD",
  "timestamp": "2024-01-15T10:00:00Z"
}

// payment-events
{
  "eventType": "PAYMENT_COMPLETED",
  "userId": "uuid",
  "amount": 9.99,
  "currency": "EUR",
  "timestamp": "2024-01-15T10:00:00Z"
}
```

### Appels vers User Service

- Mise à jour subscription_level
- Activation/désactivation features
- Notifications utilisateur

### Intégration Character Service

- Déblocage personnages premium
- Limite de création custom

### Intégration Conversation Service

- Application rate limits
- Décompte tokens utilisés

## Scripts Utiles

### Migration de Prix

```bash
./scripts/migrate-prices.sh \
  --old-price 9.99 \
  --new-price 11.99 \
  --plan STANDARD \
  --notify-users
```

### Export Comptable

```bash
./scripts/export-accounting.sh \
  --year 2024 \
  --format csv \
  --include-vat \
  --output accounting-2024.csv
```

### Simulation Abonnement

```bash
./scripts/simulate-subscription.sh \
  --user test@example.com \
  --plan PREMIUM \
  --months 12 \
  --include-usage
```