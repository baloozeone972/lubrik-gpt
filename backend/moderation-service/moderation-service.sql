
-- V1__create_moderation_tables.sql
-- Migration initiale pour le Moderation Service

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- Pour la recherche fuzzy
SET search_path TO moderation_service;

-- Table des demandes de modération
CREATE TABLE moderation_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    content_id VARCHAR(255),
    content_type VARCHAR(50) NOT NULL,
    content_text TEXT,
    content_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(20) DEFAULT 'NORMAL',
    final_decision VARCHAR(50),
    final_score DECIMAL(3,2),
    requires_human_review BOOLEAN DEFAULT FALSE,
    reviewed_by UUID,
    review_notes TEXT,
    auto_action_taken VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT chk_content_type CHECK (content_type IN ('TEXT_MESSAGE', 'IMAGE', 'VIDEO', 'AUDIO', 'CHARACTER_PROFILE', 'USER_PROFILE', 'VOICE_SAMPLE')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REQUIRES_REVIEW')),
    CONSTRAINT chk_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_moderation_user ON moderation_requests(user_id);
CREATE INDEX idx_moderation_status ON moderation_requests(status);
CREATE INDEX idx_moderation_type ON moderation_requests(content_type);
CREATE INDEX idx_moderation_created ON moderation_requests(created_at);
CREATE INDEX idx_moderation_priority ON moderation_requests(priority) WHERE status = 'PENDING';

-- Table des labels détectés
CREATE TABLE moderation_labels (
    moderation_request_id UUID NOT NULL REFERENCES moderation_requests(id) ON DELETE CASCADE,
    label VARCHAR(100) NOT NULL,
    PRIMARY KEY (moderation_request_id, label)
);

-- Table des résultats de modération
CREATE TABLE moderation_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    moderation_request_id UUID NOT NULL REFERENCES moderation_requests(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    model_version VARCHAR(50),
    is_safe BOOLEAN,
    confidence_score DECIMAL(3,2),
    categories JSON,
    detailed_scores JSON,
    processing_time_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_result_request ON moderation_results(moderation_request_id);

-- Table des règles de modération
CREATE TABLE moderation_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_n VARCHAR(100) NOT NULL,
    description TEXT,
    rule_type VARCHAR(20) NOT NULL,
    pattern TEXT,
    category VARCHAR(50),
    severity INTEGER CHECK (severity >= 1 AND severity <= 10),
    action VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    applies_to_content_types TEXT,
    jurisdiction VARCHAR(50),
    CONSTRAINT chk_rule_type CHECK (rule_type IN ('KEYWORD', 'REGEX', 'AI_THRESHOLD', 'BLACKLIST', 'WHITELIST', 'CUSTOM'))
);

CREATE INDEX idx_rule_active ON moderation_rules(is_active);
CREATE INDEX idx_rule_type ON moderation_rules(rule_type);

-- Table du contenu bloqué
CREATE TABLE blocked_content (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    content_hash VARCHAR(256) UNIQUE,
    content_type VARCHAR(50),
    blocked_reason VARCHAR(200) NOT NULL,
    severity_level INTEGER,
    categories JSON,
    action_taken VARCHAR(100),
    appeal_status VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX idx_blocked_user ON blocked_content(user_id);
CREATE INDEX idx_blocked_hash ON blocked_content(content_hash);
CREATE INDEX idx_blocked_created ON blocked_content(created_at);

-- Table de l'historique de modération des utilisateurs
CREATE TABLE user_moderation_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    reason VARCHAR(200) NOT NULL,
    severity INTEGER,
    content_reference VARCHAR(500),
    duration_hours INTEGER,
    moderator_id UUID,
    auto_moderated BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mod_history_user ON user_moderation_history(user_id);
CREATE INDEX idx_mod_history_created ON user_moderation_history(created_at);

-- Table de vérification d'âge
CREATE TABLE age_verifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verification_method VARCHAR(50),
    document_type VARCHAR(50),
    document_hash VARCHAR(256),
    estimated_age INTEGER,
    birth_date DATE,
    is_adult BOOLEAN,
    confidence_score DECIMAL(3,2),
    verification_provider VARCHAR(50),
    failure_reason TEXT,
    attempts_count INTEGER DEFAULT 0,
    verified_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_verification_status CHECK (status IN ('PENDING', 'PROCESSING', 'VERIFIED', 'FAILED', 'EXPIRED'))
);

CREATE INDEX idx_age_verification_user ON age_verifications(user_id);
CREATE INDEX idx_age_verification_status ON age_verifications(status);

-- Table des scores de risque utilisateur
CREATE TABLE user_risk_scores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE,
    overall_risk_score DECIMAL(3,2) DEFAULT 0.0,
    toxicity_score DECIMAL(3,2) DEFAULT 0.0,
    spam_score DECIMAL(3,2) DEFAULT 0.0,
    violation_count INTEGER DEFAULT 0,
    warning_count INTEGER DEFAULT 0,
    content_blocked_count INTEGER DEFAULT 0,
    last_violation_at TIMESTAMP,
    risk_factors JSON,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_risk_user ON user_risk_scores(user_id);
CREATE INDEX idx_risk_score ON user_risk_scores(overall_risk_score);

-- Vue pour les statistiques de modération
CREATE VIEW moderation_statistics AS
SELECT 
    DATE_TRUNC('hour', created_at) as hour,
    content_type,
    COUNT(*) as total_requests,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed,
    COUNT(CASE WHEN requires_human_review THEN 1 END) as human_review,
    AVG(CASE WHEN final_score IS NOT NULL THEN final_score END) as avg_risk_score,
    COUNT(CASE WHEN auto_action_taken IS NOT NULL THEN 1 END) as auto_actions
FROM moderation_requests
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY DATE_TRUNC('hour', created_at), content_type
ORDER BY hour DESC;

-- Fonction pour calculer le score de risque d'un utilisateur
CREATE OR REPLACE FUNCTION calculate_user_risk_score(p_user_id UUID)
RETURNS DECIMAL AS $$
DECLARE
    v_risk_score DECIMAL(3,2);
    v_violation_count INTEGER;
    v_recent_violations INTEGER;
BEGIN
    -- Compter les violations
    SELECT COUNT(*) INTO v_violation_count
    FROM user_moderation_history
    WHERE user_id = p_user_id
    AND severity >= 5;
    
    -- Violations récentes (30 derniers jours)
    SELECT COUNT(*) INTO v_recent_violations
    FROM user_moderation_history
    WHERE user_id = p_user_id
    AND created_at > NOW() - INTERVAL '30 days';
    
    -- Calcul du score (0.0 = safe, 1.0 = high risk)
    v_risk_score := LEAST(1.0, 
        (v_violation_count * 0.1) + 
        (v_recent_violations * 0.2)
    );
    
    -- Mettre à jour la table
    INSERT INTO user_risk_scores (user_id, overall_risk_score, violation_count)
    VALUES (p_user_id, v_risk_score, v_violation_count)
    ON CONFLICT (user_id) DO UPDATE
    SET overall_risk_score = v_risk_score,
        violation_count = v_violation_count,
        calculated_at = CURRENT_TIMESTAMP;
    
    RETURN v_risk_score;
END;
$$ LANGUAGE plpgsql;

-- Trigger pour auto-calculer le score après une action
CREATE OR REPLACE FUNCTION update_risk_score_trigger()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM calculate_user_risk_score(NEW.user_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER after_moderation_action
AFTER INSERT ON user_moderation_history
FOR EACH ROW EXECUTE FUNCTION update_risk_score_trigger();

-- Données initiales : règles de base
INSERT INTO moderation_rules (rule_n, rule_type, pattern, category, severity, action) VALUES
('profanity-filter', 'KEYWORD', 'badwords.txt', 'profanity', 3, 'WARN'),
('spam-detection', 'REGEX', '(https?://[^\s]+){3,}', 'spam', 4, 'BLOCK'),
('personal-info', 'REGEX', '\b\d{3}-\d{2}-\d{4}\b', 'privacy', 8, 'BLOCK'),
('self-harm-keywords', 'KEYWORD', 'selfharm-keywords.txt', 'self-harm', 10, 'BLOCK_AND_SUPPORT');