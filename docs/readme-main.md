# Application Compagnon Virtuel - Documentation Principale

## Vue d'ensemble

Application de compagnon virtuel développée en Java 21 avec Spring Boot, offrant des interactions personnalisées avec
des personnages IA. Cette documentation couvre l'installation, la configuration et le déploiement local pour les tests.

## Prérequis Système

### Logiciels Requis

- **Java 21 (OpenJDK)** - [Télécharger](https://adoptium.net/)
- **Maven 3.9+** - [Télécharger](https://maven.apache.org/download.cgi)
- **PostgreSQL 15+** - [Télécharger](https://www.postgresql.org/download/)
- **Redis 7+** - [Télécharger](https://redis.io/download/)
- **Docker & Docker Compose** - [Télécharger](https://docs.docker.com/get-docker/)
- **Node.js 18+ & npm** - [Télécharger](https://nodejs.org/)
- **Git** - [Télécharger](https://git-scm.com/)

### Optionnel (Recommandé)

- **IntelliJ IDEA** ou **Eclipse** avec support Spring Boot
- **Postman** pour tester les APIs
- **DBeaver** pour la gestion de base de données
- **Apache Kafka** (sera installé via Docker)

## Structure du Projet

```
virtual-companion-app/
├── backend/
│   ├── user-service/
│   ├── character-service/
│   ├── conversation-service/
│   ├── media-service/
│   ├── billing-service/
│   ├── moderation-service/
│   ├── common/
│   └── gateway/
├── frontend/
│   ├── web-app/
│   └── mobile-app/
├── infrastructure/
│   ├── docker/
│   ├── kubernetes/
│   └── scripts/
├── docs/
│   ├── api/
│   ├── architecture/
│   └── deployment/
└── docker-compose.yml
```

## Installation Rapide (Docker)

### 1. Cloner le Projet

```bash
git clone https://github.com/votre-repo/virtual-companion-app.git
cd virtual-companion-app
```

### 2. Configuration Environnement

```bash
# Copier les fichiers d'environnement
cp .env.example .env

# Éditer .env avec vos configurations
# IMPORTANT: Changer les mots de passe par défaut!
```

### 3. Lancer l'Infrastructure

```bash
# Démarrer tous les services
docker-compose up -d

# Vérifier que tous les services sont actifs
docker-compose ps
```

### 4. Initialiser les Bases de Données

```bash
# Exécuter les migrations
./scripts/init-databases.sh

# Charger les données de test
./scripts/load-test-data.sh
```

## Configuration des Services

### PostgreSQL

- **Host**: localhost
- **Port**: 5432
- **Database**: virtual_companion_db
- **Username**: vc_admin
- **Password**: (défini dans .env)

### Redis

- **Host**: localhost
- **Port**: 6379
- **Password**: (défini dans .env)

### Kafka

- **Bootstrap Servers**: localhost:9092
- **Zookeeper**: localhost:2181

### Elasticsearch

- **Host**: localhost
- **Port**: 9200
- **Username**: elastic
- **Password**: (défini dans .env)

## Démarrage des Microservices

### Option 1: Via Maven (Développement)

```bash
# Terminal 1 - User Service
cd backend/user-service
mvn spring-boot:run

# Terminal 2 - Character Service
cd backend/character-service
mvn spring-boot:run

# Terminal 3 - Conversation Service
cd backend/conversation-service
mvn spring-boot:run

# Terminal 4 - Media Service
cd backend/media-service
mvn spring-boot:run

# Terminal 5 - Billing Service
cd backend/billing-service
mvn spring-boot:run

# Terminal 6 - Moderation Service
cd backend/moderation-service
mvn spring-boot:run

# Terminal 7 - API Gateway
cd backend/gateway
mvn spring-boot:run
```

### Option 2: Via Docker Compose (Recommandé)

```bash
# Construire et démarrer tous les services
docker-compose -f docker-compose.full.yml up --build
```

## Démarrage du Frontend

### Application Web

```bash
cd frontend/web-app
npm install
npm run dev
# Accessible sur http://localhost:3000
```

### Application Mobile (React Native)

```bash
cd frontend/mobile-app
npm install

# iOS (macOS uniquement)
cd ios && pod install && cd ..
npm run ios

# Android
npm run android
```

## URLs des Services

### Backend Services

- **API Gateway**: http://localhost:8080
- **User Service**: http://localhost:8081
- **Character Service**: http://localhost:8082
- **Conversation Service**: http://localhost:8083
- **Media Service**: http://localhost:8084
- **Billing Service**: http://localhost:8085
- **Moderation Service**: http://localhost:8086

### Monitoring & Admin

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Elasticsearch/Kibana**: http://localhost:5601
- **Redis Commander**: http://localhost:8087
- **Kafka UI**: http://localhost:8088

## Tests de Validation

### 1. Test de Santé des Services

```bash
# Vérifier tous les endpoints de santé
./scripts/health-check.sh

# Ou manuellement
curl http://localhost:8080/actuator/health
```

### 2. Créer un Utilisateur Test

```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!",
    "birthDate": "1990-01-01",
    "acceptedTerms": true
  }'
```

### 3. Authentification

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!"
  }'
```

## Arrêt des Services

### Docker Compose

```bash
# Arrêter tous les services
docker-compose down

# Arrêter et supprimer les volumes (ATTENTION: supprime les données)
docker-compose down -v
```

### Services Individuels

```bash
# Arrêter un service spécifique
docker-compose stop user-service

# Redémarrer un service
docker-compose restart user-service
```

## Dépannage

### Problèmes Courants

#### 1. Port déjà utilisé

```bash
# Identifier le processus utilisant le port
lsof -i :8080

# Tuer le processus
kill -9 <PID>
```

#### 2. Erreur de connexion PostgreSQL

```bash
# Vérifier que PostgreSQL est actif
docker-compose ps postgres

# Voir les logs
docker-compose logs postgres

# Redémarrer PostgreSQL
docker-compose restart postgres
```

#### 3. Erreur mémoire Java

Ajouter dans les variables d'environnement:

```bash
export JAVA_OPTS="-Xms512m -Xmx2g"
```

### Logs et Debugging

#### Voir les logs d'un service

```bash
# Via Docker
docker-compose logs -f user-service

# Via Maven
# Les logs sont dans: backend/<service>/logs/
```

#### Activer le mode debug

```bash
# Ajouter dans application.yml
logging:
  level:
    root: DEBUG
    com.virtualcompanion: DEBUG
```

## Sécurité pour l'Environnement de Test

⚠️ **IMPORTANT**: Cette configuration est pour le DÉVELOPPEMENT LOCAL uniquement!

### Checklist Sécurité Minimale

- [ ] Changer tous les mots de passe par défaut dans .env
- [ ] Ne pas exposer les ports sur des interfaces publiques
- [ ] Utiliser HTTPS même en local (certificats auto-signés)
- [ ] Activer l'authentification sur tous les outils d'admin
- [ ] Ne pas commiter les fichiers .env ou secrets

## Prochaines Étapes

1. **Lire la documentation détaillée de chaque module**
    - [Module Authentification](./docs/modules/authentication.md)
    - [Module Personnages](./docs/modules/characters.md)
    - [Module Conversations](./docs/modules/conversations.md)
    - [Module Média](./docs/modules/media.md)
    - [Module Facturation](./docs/modules/billing.md)

2. **Configurer l'IDE**
    - Importer le projet comme projet Maven multi-module
    - Configurer les profils Spring (dev, test, prod)
    - Installer les plugins recommandés (Lombok, Spring Boot)

3. **Exécuter les tests**
   ```bash
   # Tests unitaires
   mvn test
   
   # Tests d'intégration
   mvn verify
   
   # Tests de performance
   ./scripts/run-performance-tests.sh
   ```

## Support

- **Documentation API**: http://localhost:8080/swagger-ui.html
- **Wiki du Projet**: [Lien vers wiki]
- **Issues**: [Lien vers issues GitHub]

## Licence

Ce projet est sous licence propriétaire. Voir [LICENSE](./LICENSE) pour plus de détails.