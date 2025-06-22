-- V1__create_conversation_tables.sql
-- Migration initiale pour le Conversation Service

-- Extension pour UUID et Vector
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- Définir le schéma
SET search_path TO conversation_service;

-- Table principale des conversations
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    character_id UUID NOT NULL,
    title VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    message_count INTEGER DEFAULT 0,
    user_message_count INTEGER DEFAULT 0,
    character_message_count INTEGER DEFAULT 0,
    total_tokens_used BIGINT DEFAULT 0,
    last_message_at TIMESTAMP,
    context_summary TEXT,
    emotional_state VARCHAR(50),
    relationship_score INTEGER DEFAULT 50 CHECK (relationship_score >= 0 AND relationship_score <= 100),
    language_code VARCHAR(10) DEFAULT 'en',
    is_favorite BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'PAUSED', 'ENDED', 'ARCHIVED', 'DELETED'))
);

CREATE INDEX idx_conversation_user ON conversations(user_id);
CREATE INDEX idx_conversation_character ON conversations(character_id);
CREATE INDEX idx_conversation_created ON conversations(created_at DESC);
CREATE INDEX idx_conversation_status ON conversations(status);
CREATE INDEX idx_conversation_last_message ON conversations(last_message_at DESC);
CREATE INDEX idx_conversation_favorite ON conversations(user_id) WHERE is_favorite = TRUE;

-- Table des tags de conversation
CREATE TABLE conversation_tags (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (conversation_id, tag)
);

CREATE INDEX idx_tag_conversation ON conversation_tags(conversation_id);
CREATE INDEX idx_tag_n ON conversation_tags(tag);

-- Table des mémoires de conversation (avec embeddings vectoriels)
CREATE TABLE conversation_memories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    memory_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    embedding_vector vector(1536),
    importance_score DOUBLE PRECISION DEFAULT 0.5,
    emotional_context VARCHAR(50),
    referenced_count INTEGER DEFAULT 0,
    last_referenced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_memory_conversation ON conversation_memories(conversation_id);
CREATE INDEX idx_memory_importance ON conversation_memories(importance_score DESC);
CREATE INDEX idx_memory_type ON conversation_memories(memory_type);
CREATE INDEX idx_memory_vector ON conversation_memories USING ivfflat (embedding_vector vector_cosine_ops);

-- Table des sessions de streaming (WebSocket/SSE)
CREATE TABLE streaming_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONNECTING',
    connection_type VARCHAR(20),
    client_ip VARCHAR(45),
    user_agent VARCHAR(500),
    connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMP,
    last_activity_at TIMESTAMP,
    messages_sent INTEGER DEFAULT 0,
    messages_received INTEGER DEFAULT 0,
    CONSTRAINT chk_streaming_status CHECK (status IN ('CONNECTING', 'CONNECTED', 'ACTIVE', 'IDLE', 'DISCONNECTED', 'ERROR'))
);

CREATE INDEX idx_streaming_conversation ON streaming_sessions(conversation_id);
CREATE INDEX idx_streaming_status ON streaming_sessions(status);
CREATE INDEX idx_streaming_token ON streaming_sessions(session_token);

-- Table des analytics de conversation
CREATE TABLE conversation_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    character_id UUID NOT NULL,
    -- Engagement Metrics
    total_duration_seconds BIGINT DEFAULT 0,
    average_response_time_ms BIGINT,
    user_engagement_score DOUBLE PRECISION,
    character_performance_score DOUBLE PRECISION,
    -- Sentiment Analysis
    average_sentiment_score DOUBLE PRECISION,
    positive_message_ratio DOUBLE PRECISION,
    negative_message_ratio DOUBLE PRECISION,
    -- Content Analysis
    topics_discussed JSON,
    vocabulary_diversity_score DOUBLE PRECISION,
    conversation_depth_score DOUBLE PRECISION,
    -- User Satisfaction
    user_satisfaction_score INTEGER CHECK (user_satisfaction_score >= 1 AND user_satisfaction_score <= 5),
    would_recommend BOOLEAN,
    feedback_text TEXT,
    last_analyzed_at TIMESTAMP
);

CREATE INDEX idx_analytics_conversation ON conversation_analytics(conversation_id);
CREATE INDEX idx_analytics_user ON conversation_analytics(user_id);
CREATE INDEX idx_analytics_character ON conversation_analytics(character_id);

-- Table de configuration des modèles IA
CREATE TABLE ai_model_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    character_id UUID NOT NULL,
    model_n VARCHAR(100) NOT NULL,
    model_version VARCHAR(50),
    temperature REAL DEFAULT 0.7,
    max_tokens INTEGER DEFAULT 500,
    top_p REAL DEFAULT 0.9,
    frequency_penalty REAL DEFAULT 0.0,
    presence_penalty REAL DEFAULT 0.0,
    custom_parameters JSON,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_ai_config_character ON ai_model_configs(character_id);

-- Table pour le rate limiting
CREATE TABLE rate_limits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    subscription_level VARCHAR(20) NOT NULL,
    messages_per_hour INTEGER NOT NULL,
    messages_per_day INTEGER NOT NULL,
    tokens_per_hour BIGINT NOT NULL,
    tokens_per_day BIGINT NOT NULL,
    current_hour_messages INTEGER DEFAULT 0,
    current_day_messages INTEGER DEFAULT 0,
    current_hour_tokens BIGINT DEFAULT 0,
    current_day_tokens BIGINT DEFAULT 0,
    hour_reset_at TIMESTAMP NOT NULL,
    day_reset_at TIMESTAMP NOT NULL,
    UNIQUE(user_id)
);

-- Vue pour les conversations actives
CREATE VIEW active_conversations AS
SELECT 
    c.id,
    c.user_id,
    c.character_id,
    c.title,
    c.message_count,
    c.last_message_at,
    c.emotional_state,
    c.relationship_score,
    COUNT(DISTINCT DATE(c.last_message_at)) as active_days,
    EXTRACT(EPOCH FROM (NOW() - c.last_message_at)) / 3600 as hours_since_last_message
FROM conversations c
WHERE c.status = 'ACTIVE'
  AND c.is_archived = FALSE
  AND c.last_message_at > NOW() - INTERVAL '30 days'
GROUP BY c.id;

-- Fonction pour nettoyer les anciennes sessions
CREATE OR REPLACE FUNCTION cleanup_old_streaming_sessions()
RETURNS void AS $$
BEGIN
    UPDATE streaming_sessions
    SET status = 'DISCONNECTED',
        disconnected_at = CURRENT_TIMESTAMP
    WHERE status IN ('CONNECTING', 'CONNECTED', 'ACTIVE', 'IDLE')
      AND last_activity_at < NOW() - INTERVAL '30 minutes';
    
    DELETE FROM streaming_sessions
    WHERE disconnected_at < NOW() - INTERVAL '7 days';
END;
$$ LANGUAGE plpgsql;

-- Fonction pour calculer les analytics d'une conversation
CREATE OR REPLACE FUNCTION calculate_conversation_analytics(conv_id UUID)
RETURNS void AS $$
DECLARE
    v_user_id UUID;
    v_character_id UUID;
    v_total_duration BIGINT;
    v_message_count INTEGER;
BEGIN
    -- Récupérer les infos de base
    SELECT user_id, character_id, message_count
    INTO v_user_id, v_character_id, v_message_count
    FROM conversations
    WHERE id = conv_id;
    
    -- Calculer la durée totale (estimée)
    SELECT EXTRACT(EPOCH FROM (MAX(last_message_at) - MIN(created_at)))::BIGINT
    INTO v_total_duration
    FROM conversations
    WHERE id = conv_id;
    
    -- Insérer ou mettre à jour les analytics
    INSERT INTO conversation_analytics (
        conversation_id, user_id, character_id, 
        total_duration_seconds, last_analyzed_at
    ) VALUES (
        conv_id, v_user_id, v_character_id,
        v_total_duration, CURRENT_TIMESTAMP
    )
    ON CONFLICT (conversation_id) DO UPDATE
    SET total_duration_seconds = v_total_duration,
        last_analyzed_at = CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- Fonction pour mettre à jour les compteurs de messages
CREATE OR REPLACE FUNCTION update_message_counters()
RETURNS TRIGGER AS $$
BEGIN
    -- Cette fonction sera appelée depuis MongoDB via événement Kafka
    -- Pour maintenir la cohérence entre PostgreSQL et MongoDB
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger pour mettre à jour updated_at
CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Données de configuration par défaut pour les limites
INSERT INTO rate_limits (user_id, subscription_level, messages_per_hour, messages_per_day, 
                        tokens_per_hour, tokens_per_day, hour_reset_at, day_reset_at)
VALUES 
    ('00000000-0000-0000-0000-000000000000', 'FREE', 10, 50, 10000, 50000, 
     CURRENT_TIMESTAMP + INTERVAL '1 hour', CURRENT_TIMESTAMP + INTERVAL '1 day'),
    ('00000000-0000-0000-0000-000000000001', 'STANDARD', 50, 500, 50000, 500000,
     CURRENT_TIMESTAMP + INTERVAL '1 hour', CURRENT_TIMESTAMP + INTERVAL '1 day'),
    ('00000000-0000-0000-0000-000000000002', 'PREMIUM', 200, 2000, 200000, 2000000,
     CURRENT_TIMESTAMP + INTERVAL '1 hour', CURRENT_TIMESTAMP + INTERVAL '1 day'),
    ('00000000-0000-0000-0000-000000000003', 'VIP', 999999, 999999, 999999999, 999999999,
     CURRENT_TIMESTAMP + INTERVAL '1 hour', CURRENT_TIMESTAMP + INTERVAL '1 day');

-- Index pour la recherche vectorielle (similarité sémantique)
CREATE INDEX idx_memory_embedding ON conversation_memories 
USING ivfflat (embedding_vector vector_cosine_ops)
WITH (lists = 100);

-- Vue pour les statistiques de performance
CREATE VIEW conversation_performance_stats AS
SELECT 
    DATE_TRUNC('hour', c.created_at) as hour,
    COUNT(DISTINCT c.id) as conversations_started,
    AVG(c.message_count) as avg_messages_per_conversation,
    AVG(c.total_tokens_used) as avg_tokens_per_conversation,
    COUNT(DISTINCT c.user_id) as unique_users,
    COUNT(DISTINCT c.character_id) as unique_characters
FROM conversations c
WHERE c.created_at > NOW() - INTERVAL '7 days'
GROUP BY DATE_TRUNC('hour', c.created_at)
ORDER BY hour DESC;