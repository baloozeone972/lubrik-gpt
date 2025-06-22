# Documentation Module User Service

## Vue d'ensemble

Le User Service est le module central de gestion des utilisateurs et de l'authentification pour l'application Virtual
Companion. Il gère l'inscription, la connexion, la vérification d'âge, l'authentification à deux facteurs (2FA), et la
conformité légale (GDPR/CCPA).

## Architecture du Module

### Technologies Utilisées

- **Java 21** avec Spring Boot 3.2.0
- **Spring Security** pour l'authentification et l'autorisation
- **PostgreSQL** pour la persistance des données
- **Redis** pour le cache et les sessions
- **Kafka** pour les événements asynchrones
- **JWT** pour les tokens d'authentification
- **Flyway** pour les migrations de base de données

### Structure du Projet

```
user-service/
├── src/
│   ├── main/
│   │   ├── java/com/virtualcompanion/userservice/
│   │   │   ├── config/         # Configuration Spring
│   │   │   ├── controller/     # REST Controllers
│   │   │   ├── dto/           # Data Transfer Objects
│   │   │   ├── entity/        # Entités JPA
│   │   │   ├── exception/     # Exceptions personnalisées
│   │   │   ├── mapper/        # MapStruct Mappers
│   │   │   ├── repository/    # Repositories JPA
│   │   │   ├── security/      # Configuration sécurité
│   │   │   ├── service/       # Logique métier
│   │   │   ├── util/          # Utilitaires
│   │   │   └── validation/    # Validateurs personnalisés
│   │   └── resources/
│   │       ├── db/migration/  # Scripts Flyway
│   │       ├── templates/     # Templates email
│   │       └── application.yml
│   └── test/
└── pom.xml
```

## Installation et Configuration

### Prérequis

1. **Java 21** installé et configuré
2. **PostgreSQL** en cours d'exécution
3. **Redis** en cours d'exécution
4. **Kafka** (optionnel pour le développement)

### Étapes d'Installation

#### 1. Base de Données

```bash
# Créer la base de données
psql -U postgres
CREATE DATABASE virtual_companion_db;
CREATE USER vc_admin WITH PASSWORD 'votre_mot_de_passe';
GRANT ALL PRIVILEGES ON DATABASE virtual_companion_db TO vc_admin;
\q
```

#### 2. Configuration de l'Environnement

```bash
# Dans le répertoire user-service
cp .env.example .env

# Éditer .env avec vos valeurs
nano .env
```

Variables importantes à configurer :

- `DB_PASSWORD` : Mot de passe PostgreSQL
- `REDIS_PASSWORD` : Mot de passe Redis
- `JWT_SECRET` : Secret JWT (minimum 512 bits)
- `ENCRYPTION_KEY` : Clé de chiffrement (32 caractères)

#### 3. Installation des Dépendances

```bash
cd user-service
mvn clean install
```

#### 4. Exécution des Migrations

```bash
# Les migrations s'exécutent automatiquement au démarrage
# Ou manuellement :
mvn flyway:migrate
```

#### 5. Démarrage du Service

```bash
# Mode développement
mvn spring-boot:run

# Ou avec un profil spécifique
mvn spring-boot:run -Dspring.profiles.active=development
```

Le service sera accessible sur `http://localhost:8081`

## API Endpoints

### Authentification

#### Inscription

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "birthDate": "1990-01-01",
  "phoneNumber": "+33612345678",
  "acceptedTerms": true,
  "marketingConsent": false
}
```

#### Connexion

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

Réponse :

```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "username": "johndoe",
    "subscriptionLevel": "FREE",
    "emailVerified": false,
    "twoFaEnabled": false
  }
}
```

#### Rafraîchir le Token

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

#### Déconnexion

```http
POST /api/v1/auth/logout
Authorization: Bearer {accessToken}
```

### Gestion du Profil

#### Obtenir le Profil

```http
GET /api/v1/users/profile
Authorization: Bearer {accessToken}
```

#### Mettre à Jour le Profil

```http
PUT /api/v1/users/profile
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe Updated",
  "phoneNumber": "+33687654321"
}
```

#### Changer le Mot de Passe

```http
POST /api/v1/users/change-password
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "currentPassword": "OldPass123!",
  "newPassword": "NewSecurePass456!",
  "confirmPassword": "NewSecurePass456!"
}
```

### Vérification

#### Vérifier l'Email

```http
GET /api/v1/auth/verify-email?token={verificationToken}
```

#### Renvoyer l'Email de Vérification

```http
POST /api/v1/auth/resend-verification
Authorization: Bearer {accessToken}
```

#### Vérifier l'Âge

```http
POST /api/v1/users/verify-age
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data

{
  "method": "DOCUMENT",
  "documentType": "PASSPORT",
  "documentImage": [file]
}
```

### Authentification à Deux Facteurs (2FA)

#### Activer 2FA

```http
POST /api/v1/users/2fa/enable
Authorization: Bearer {accessToken}
```

Réponse :

```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qrCodeUrl": "data:image/png;base64,iVBORw0KGgo...",
  "backupCodes": [
    "A1B2C3D4",
    "E5F6G7H8",
    "I9J0K1L2",
    "M3N4O5P6",
    "Q7R8S9T0"
  ]
}
```

#### Confirmer l'Activation 2FA

```http
POST /api/v1/users/2fa/confirm
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "code": "123456"
}
```

#### Se Connecter avec 2FA

```http
POST /api/v1/auth/login/2fa
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "twoFactorCode": "123456"
}
```

### Sessions

#### Lister les Sessions Actives

```http
GET /api/v1/users/sessions
Authorization: Bearer {accessToken}
```

#### Révoquer une Session

```http
DELETE /api/v1/users/sessions/{sessionId}
Authorization: Bearer {accessToken}
```

#### Révoquer Toutes les Sessions

```http
POST /api/v1/users/sessions/revoke-all
Authorization: Bearer {accessToken}
```

### Conformité GDPR

#### Télécharger ses Données

```http
POST /api/v1/users/gdpr/export
Authorization: Bearer {accessToken}
```

#### Supprimer son Compte

```http
DELETE /api/v1/users/gdpr/delete
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "password": "CurrentPassword123!",
  "reason": "No longer using the service"
}
```

## Sécurité

### Validation des Mots de Passe

Les mots de passe doivent respecter les critères suivants :

- Minimum 8 caractères
- Au moins 1 majuscule
- Au moins 1 minuscule
- Au moins 1 chiffre
- Au moins 1 caractère spécial (!@#$%^&*(),.?":{}|<>)

### Protection contre le Brute Force

- Maximum 5 tentatives de connexion par compte
- Verrouillage du compte pendant 30 minutes après 5 échecs
- Limitation du taux de requêtes par IP

### Chiffrement

- Mots de passe : BCrypt avec 12 rounds
- Données sensibles : AES-256
- Communications : HTTPS obligatoire en production

## Monitoring et Logs

### Endpoints de Santé

```http
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
GET /actuator/prometheus
```

### Logs Structurés

Les logs sont au format JSON et incluent :

- Timestamp
- Niveau (INFO, WARN, ERROR)
- Service name
- Trace ID (pour le suivi distribué)
- User ID (si authentifié)
- Action effectuée

### Métriques Prometheus

Métriques exposées :

- `user_registration_total` : Nombre total d'inscriptions
- `user_login_attempts` : Tentatives de connexion
- `user_login_success` : Connexions réussies
- `user_active_sessions` : Sessions actives
- `jwt_tokens_issued` : Tokens JWT émis
- `email_verifications` : Vérifications d'email

## Tests

### Tests Unitaires

```bash
mvn test
```

### Tests d'Intégration

```bash
mvn verify
```

### Tests de Performance

```bash
# Utiliser JMeter avec le plan de test fourni
jmeter -n -t tests/performance/user-service-load-test.jmx
```

### Coverage

```bash
mvn jacoco:report
# Rapport disponible dans target/site/jacoco/index.html
```

## Dépannage

### Problème : "Connection refused" PostgreSQL

```bash
# Vérifier que PostgreSQL est actif
sudo systemctl status postgresql

# Vérifier la configuration
psql -U vc_admin -d virtual_companion_db -h localhost
```

### Problème : "Invalid JWT token"

- Vérifier que JWT_SECRET est identique dans tous les environnements
- Vérifier l'expiration du token
- S'assurer que l'horloge système est synchronisée

### Problème : "Email not sending"

- En développement, vérifier Mailhog sur http://localhost:8025
- En production, vérifier les credentials SMTP

### Logs Détaillés

Pour activer les logs détaillés :

```yaml
logging:
  level:
    com.virtualcompanion: DEBUG
    org.springframework.security: DEBUG
```

## Maintenance

### Backup Base de Données

```bash
# Backup manuel
pg_dump -U vc_admin virtual_companion_db > backup_$(date +%Y%m%d_%H%M%S).sql

# Restauration
psql -U vc_admin virtual_companion_db < backup_20240115_120000.sql
```

### Nettoyage des Sessions Expirées

Un job planifié nettoie automatiquement les sessions expirées toutes les heures.
Pour un nettoyage manuel :

```sql
SELECT cleanup_expired_sessions();
```

### Mise à Jour des Dépendances

```bash
# Vérifier les mises à jour disponibles
mvn versions:display-dependency-updates

# Mettre à jour les versions
mvn versions:use-latest-releases
```

## Support et Contact

Pour toute question ou problème :

- Documentation API Swagger : http://localhost:8081/swagger-ui.html
- Logs : `logs/user-service.log`
- Monitoring : http://localhost:3001 (Grafana)