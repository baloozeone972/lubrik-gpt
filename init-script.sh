#!/bin/bash
# init-local-env.sh - Script d'initialisation de l'environnement local

set -e  # Arrêter en cas d'erreur

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction pour afficher les messages
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Fonction pour vérifier si une commande existe
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Bannière
echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║        Virtual Companion - Initialisation Locale          ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Vérification des prérequis
log_info "Vérification des prérequis..."

MISSING_DEPS=()

if ! command_exists java; then
    MISSING_DEPS+=("Java 21")
elif ! java -version 2>&1 | grep -q "version \"21"; then
    log_warning "Java détecté mais version 21 requise"
    MISSING_DEPS+=("Java 21")
fi

if ! command_exists mvn; then
    MISSING_DEPS+=("Maven")
fi

if ! command_exists docker; then
    MISSING_DEPS+=("Docker")
fi

if ! command_exists docker-compose; then
    MISSING_DEPS+=("Docker Compose")
fi

if ! command_exists node; then
    MISSING_DEPS+=("Node.js")
fi

if ! command_exists npm; then
    MISSING_DEPS+=("npm")
fi

if [ ${#MISSING_DEPS[@]} -ne 0 ]; then
    log_error "Dépendances manquantes :"
    for dep in "${MISSING_DEPS[@]}"; do
        echo "  - $dep"
    done
    echo ""
    echo "Veuillez installer les dépendances manquantes avant de continuer."
    exit 1
fi

log_success "Tous les prérequis sont installés !"

# Création de la structure de dossiers
log_info "Création de la structure du projet..."

mkdir -p {backend/{user-service,character-service,conversation-service,media-service,billing-service,moderation-service,common,gateway},frontend/{web-app,mobile-app},infrastructure/{docker/{postgres,nginx,prometheus,grafana},kubernetes,scripts},docs/{api,architecture,deployment},logs}

log_success "Structure créée !"

# Configuration de l'environnement
log_info "Configuration de l'environnement..."

if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        cp .env.example .env
        log_warning "Fichier .env créé à partir de .env.example"
        log_warning "IMPORTANT : Modifiez les mots de passe dans .env avant de continuer !"
        read -p "Appuyez sur Entrée après avoir modifié .env..."
    else
        log_error "Fichier .env.example introuvable !"
        exit 1
    fi
else
    log_info "Fichier .env déjà présent"
fi

# Chargement des variables d'environnement
source .env

# Démarrage de l'infrastructure Docker
log_info "Démarrage de l'infrastructure Docker..."

docker-compose down -v 2>/dev/null || true
docker-compose up -d

# Attendre que les services soient prêts
log_info "Attente du démarrage des services..."

# Fonction pour vérifier si un service est prêt
wait_for_service() {
    local service=$1
    local port=$2
    local max_attempts=30
    local attempt=0
    
    while ! nc -z localhost $port 2>/dev/null; do
        attempt=$((attempt + 1))
        if [ $attempt -eq $max_attempts ]; then
            log_error "Le service $service n'a pas démarré après $max_attempts tentatives"
            return 1
        fi
        echo -n "."
        sleep 2
    done
    echo ""
    log_success "$service est prêt !"
}

echo -n "PostgreSQL "
wait_for_service "PostgreSQL" 5432

echo -n "Redis "
wait_for_service "Redis" 6379

echo -n "Elasticsearch "
wait_for_service "Elasticsearch" 9200

echo -n "Kafka "
wait_for_service "Kafka" 9092

# Initialisation de la base de données
log_info "Initialisation de la base de données..."

# Script SQL d'initialisation
cat > infrastructure/docker/postgres/init.sql << 'EOF'
-- Création des schémas pour chaque service
CREATE SCHEMA IF NOT EXISTS user_service;
CREATE SCHEMA IF NOT EXISTS character_service;
CREATE SCHEMA IF NOT EXISTS conversation_service;
CREATE SCHEMA IF NOT EXISTS media_service;
CREATE SCHEMA IF NOT EXISTS billing_service;
CREATE SCHEMA IF NOT EXISTS moderation_service;

-- Permissions
GRANT ALL ON SCHEMA user_service TO vc_admin;
GRANT ALL ON SCHEMA character_service TO vc_admin;
GRANT ALL ON SCHEMA conversation_service TO vc_admin;
GRANT ALL ON SCHEMA media_service TO vc_admin;
GRANT ALL ON SCHEMA billing_service TO vc_admin;
GRANT ALL ON SCHEMA moderation_service TO vc_admin;
EOF

# Exécution du script d'initialisation
PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USERNAME -d $DB_NAME -f infrastructure/docker/postgres/init.sql

log_success "Base de données initialisée !"

# Configuration Elasticsearch
log_info "Configuration d'Elasticsearch..."

# Attendre qu'Elasticsearch soit complètement prêt
sleep 10

# Créer les index
curl -u elastic:$ELASTIC_PASSWORD -X PUT "localhost:9200/users" -H 'Content-Type: application/json' -d'
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  }
}' 2>/dev/null || true

curl -u elastic:$ELASTIC_PASSWORD -X PUT "localhost:9200/conversations" -H 'Content-Type: application/json' -d'
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  }
}' 2>/dev/null || true

log_success "Elasticsearch configuré !"

# Création des topics Kafka
log_info "Création des topics Kafka..."

docker exec vc-kafka kafka-topics --create --topic user-events --bootstrap-server localhost:29092 --partitions 3 --replication-factor 1 2>/dev/null || true
docker exec vc-kafka kafka-topics --create --topic user-verification --bootstrap-server localhost:29092 --partitions 3 --replication-factor 1 2>/dev/null || true
docker exec vc-kafka kafka-topics --create --topic user-notifications --bootstrap-server localhost:29092 --partitions 3 --replication-factor 1 2>/dev/null || true
docker exec vc-kafka kafka-topics --create --topic audit-logs --bootstrap-server localhost:29092 --partitions 3 --replication-factor 1 2>/dev/null || true

log_success "Topics Kafka créés !"

# Configuration MinIO
log_info "Configuration de MinIO..."

# Installer le client MinIO si nécessaire
if ! command_exists mc; then
    log_info "Installation du client MinIO..."
    wget https://dl.min.io/client/mc/release/linux-amd64/mc -O /tmp/mc
    chmod +x /tmp/mc
    sudo mv /tmp/mc /usr/local/bin/
fi

# Configuration du client MinIO
mc alias set local http://localhost:9000 $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD 2>/dev/null || true

# Création des buckets
mc mb local/media 2>/dev/null || true
mc mb local/avatars 2>/dev/null || true
mc mb local/conversations 2>/dev/null || true
mc mb local/backups 2>/dev/null || true

log_success "MinIO configuré !"

# Installation des dépendances Maven pour le backend
log_info "Installation des dépendances backend..."

# Copier le pom.xml du user-service si présent
if [ -f "backend/user-service/pom.xml" ]; then
    cd backend/user-service
    mvn clean install -DskipTests
    cd ../..
    log_success "Dépendances User Service installées !"
fi

# Script de santé
log_info "Création du script de vérification de santé..."

cat > scripts/health-check.sh << 'EOF'
#!/bin/bash

echo "=== Vérification de l'état des services ==="
echo ""

# Fonction pour vérifier un service
check_service() {
    local name=$1
    local url=$2
    local auth=$3
    
    if [ -n "$auth" ]; then
        response=$(curl -s -o /dev/null -w "%{http_code}" -u "$auth" "$url" 2>/dev/null)
    else
        response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
    fi
    
    if [ "$response" = "200" ] || [ "$response" = "401" ]; then
        echo "✅ $name : OK"
    else
        echo "❌ $name : KO (HTTP $response)"
    fi
}

# Vérification des services d'infrastructure
check_service "PostgreSQL" "http://localhost:5432" ""
check_service "Redis Commander" "http://localhost:8087" "admin:$REDIS_COMMANDER_PASSWORD"
check_service "Elasticsearch" "http://localhost:9200" "elastic:$ELASTIC_PASSWORD"
check_service "Kibana" "http://localhost:5601/api/status" ""
check_service "Kafka UI" "http://localhost:8088" ""
check_service "MinIO Console" "http://localhost:9001" ""
check_service "Prometheus" "http://localhost:9090/-/healthy" ""
check_service "Grafana" "http://localhost:3001/api/health" ""
check_service "Mailhog" "http://localhost:8025" ""

echo ""
echo "=== URLs d'accès ==="
echo "📊 Grafana : http://localhost:3001 (admin/$GRAFANA_PASSWORD)"
echo "📈 Prometheus : http://localhost:9090"
echo "🔍 Kibana : http://localhost:5601"
echo "💾 MinIO : http://localhost:9001 ($MINIO_ROOT_USER/$MINIO_ROOT_PASSWORD)"
echo "📧 Mailhog : http://localhost:8025"
echo "🔄 Kafka UI : http://localhost:8088"
echo "🔴 Redis Commander : http://localhost:8087 (admin/$REDIS_COMMANDER_PASSWORD)"
EOF

chmod +x scripts/health-check.sh

# Création du script de démarrage des services
cat > scripts/start-services.sh << 'EOF'
#!/bin/bash

echo "Démarrage des microservices..."

# Fonction pour démarrer un service
start_service() {
    local service=$1
    local port=$2
    
    echo "Démarrage de $service sur le port $port..."
    cd backend/$service
    nohup mvn spring-boot:run > ../../logs/$service.log 2>&1 &
    echo $! > ../../logs/$service.pid
    cd ../..
}

# Démarrer les services
start_service "user-service" $USER_SERVICE_PORT
sleep 5
start_service "character-service" $CHARACTER_SERVICE_PORT
sleep 5
start_service "conversation-service" $CONVERSATION_SERVICE_PORT
sleep 5
start_service "media-service" $MEDIA_SERVICE_PORT
sleep 5
start_service "billing-service" $BILLING_SERVICE_PORT
sleep 5
start_service "moderation-service" $MODERATION_SERVICE_PORT
sleep 5
start_service "gateway" $GATEWAY_PORT

echo "Tous les services sont démarrés !"
echo "Logs disponibles dans le dossier logs/"
EOF

chmod +x scripts/start-services.sh

# Création du script d'arrêt
cat > scripts/stop-services.sh << 'EOF'
#!/bin/bash

echo "Arrêt des microservices..."

for pidfile in logs/*.pid; do
    if [ -f "$pidfile" ]; then
        pid=$(cat "$pidfile")
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid"
            echo "Service arrêté (PID: $pid)"
        fi
        rm "$pidfile"
    fi
done

echo "Tous les services sont arrêtés !"
EOF

chmod +x scripts/stop-services.sh

# Résumé final
echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║         🎉 Initialisation Terminée avec Succès ! 🎉       ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "📁 Structure du projet créée"
echo "🐳 Infrastructure Docker démarrée"
echo "🗄️  Base de données initialisée"
echo "📊 Services de monitoring configurés"
echo ""
echo -e "${YELLOW}Prochaines étapes :${NC}"
echo "1. Compiler les microservices : cd backend/<service> && mvn clean install"
echo "2. Démarrer les services : ./scripts/start-services.sh"
echo "3. Vérifier l'état : ./scripts/health-check.sh"
echo "4. Accéder à Swagger UI : http://localhost:8080/swagger-ui.html"
echo ""
echo -e "${BLUE}Documentation :${NC}"
echo "- README principal : ./README.md"
echo "- User Service : ./docs/modules/user-service.md"
echo ""
echo -e "${RED}⚠️  IMPORTANT :${NC}"
echo "- Modifiez TOUS les mots de passe dans .env avant la production !"
echo "- Activez HTTPS pour la production"
echo "- Configurez les sauvegardes automatiques"
echo ""

# Exécution du health check
log_info "Vérification finale de l'état des services..."
./scripts/health-check.sh