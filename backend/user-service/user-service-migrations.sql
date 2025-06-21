-- V1__create_user_tables.sql
-- Migration initiale pour le User Service

-- Extension pour UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table principale des utilisateurs
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(100) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_n VARCHAR(50),
    last_n VARCHAR(50),
    birth_date DATE NOT NULL,
    phone_number VARCHAR(20),
    avatar_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    subscription_level VARCHAR(20) NOT NULL DEFAULT 'FREE',
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    age_verified BOOLEAN DEFAULT FALSE,
    two_fa_enabled BOOLEAN DEFAULT FALSE,
    two_fa_secret VARCHAR(255),
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'BANNED', 'DELETED')),
    CONSTRAINT chk_subscription_level CHECK (subscription_level IN ('FREE', 'STANDARD', 'PREMIUM', 'VIP'))
);

-- Index pour les recherches fréquentes
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_user_status ON users(status);
CREATE INDEX idx_user_created ON users(created_at);
CREATE INDEX idx_user_subscription ON users(subscription_level);

-- Table des rôles utilisateur
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT chk_role CHECK (role IN ('USER', 'PREMIUM_USER', 'VIP_USER', 'MODERATOR', 'ADMIN', 'SUPER_ADMIN'))
);

-- Table des sessions utilisateur
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(500) NOT NULL UNIQUE,
    refresh_token VARCHAR(500) NOT NULL,
    device_id VARCHAR(100),
    device_type VARCHAR(50),
    device_n VARCHAR(200),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    location_country VARCHAR(2),
    location_city VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_session_token ON user_sessions(session_token);
CREATE INDEX idx_session_user ON user_sessions(user_id);
CREATE INDEX idx_session_expires ON user_sessions(expires_at);

-- Table des préférences utilisateur
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    preference_key VARCHAR(100) NOT NULL,
    preference_value TEXT,
    preference_type VARCHAR(50),
    UNIQUE(user_id, preference_key)
);

CREATE INDEX idx_pref_user ON user_preferences(user_id);

-- Table de conformité utilisateur
CREATE TABLE user_compliance (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    jurisdiction VARCHAR(50),
    consent_level VARCHAR(20),
    terms_accepted_version VARCHAR(20),
    terms_accepted_at TIMESTAMP,
    privacy_accepted_version VARCHAR(20),
    privacy_accepted_at TIMESTAMP,
    marketing_consent BOOLEAN DEFAULT FALSE,
    data_processing_consent BOOLEAN DEFAULT TRUE,
    age_verification_method VARCHAR(50),
    age_verified_at TIMESTAMP,
    identity_verification_method VARCHAR(50),
    identity_verified_at TIMESTAMP,
    gdpr_data_request_count INTEGER DEFAULT 0,
    last_gdpr_request_at TIMESTAMP
);

-- Table des tokens de vérification
CREATE TABLE verification_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    token_type VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_token_type CHECK (token_type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'PHONE_VERIFICATION', 'TWO_FA_BACKUP'))
);

CREATE INDEX idx_token_value ON verification_tokens(token);
CREATE INDEX idx_token_user ON verification_tokens(user_id);
CREATE INDEX idx_token_type ON verification_tokens(token_type);

-- Table des logs d'audit
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity VARCHAR(50),
    entity_id VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);

-- Table des tentatives de connexion (pour la sécurité)
CREATE TABLE login_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(100),
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(100),
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_login_attempt_email ON login_attempts(email);
CREATE INDEX idx_login_attempt_ip ON login_attempts(ip_address);
CREATE INDEX idx_login_attempt_time ON login_attempts(attempted_at);

-- Table de blocage IP (pour la protection contre les attaques)
CREATE TABLE ip_blacklist (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ip_address VARCHAR(45) NOT NULL UNIQUE,
    reason VARCHAR(200),
    blocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    permanent BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_blacklist_ip ON ip_blacklist(ip_address);
CREATE INDEX idx_blacklist_expires ON ip_blacklist(expires_at);

-- Vue pour les statistiques utilisateur
CREATE VIEW user_statistics AS
SELECT 
    u.id,
    u.email,
    u.username,
    u.subscription_level,
    u.created_at,
    COUNT(DISTINCT us.id) as total_sessions,
    COUNT(DISTINCT CASE WHEN us.revoked = FALSE AND us.expires_at > CURRENT_TIMESTAMP THEN us.id END) as active_sessions,
    u.last_login_at,
    u.failed_login_attempts
FROM users u
LEFT JOIN user_sessions us ON u.id = us.user_id
GROUP BY u.id;

-- Fonction pour nettoyer les sessions expirées
CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS void AS $$
BEGIN
    DELETE FROM user_sessions 
    WHERE expires_at < CURRENT_TIMESTAMP 
    OR revoked = TRUE;
END;
$$ LANGUAGE plpgsql;

-- Fonction pour anonymiser les données utilisateur (GDPR)
CREATE OR REPLACE FUNCTION anonymize_user_data(user_id UUID)
RETURNS void AS $$
BEGIN
    UPDATE users SET
        email = CONCAT('deleted_', user_id, '@deleted.com'),
        username = CONCAT('deleted_', user_id),
        first_n = 'DELETED',
        last_n = 'USER',
        phone_number = NULL,
        avatar_url = NULL,
        password = 'DELETED',
        two_fa_secret = NULL,
        deleted_at = CURRENT_TIMESTAMP,
        status = 'DELETED'
    WHERE id = user_id;
    
    -- Supprimer les données sensibles
    DELETE FROM user_sessions WHERE user_id = user_id;
    DELETE FROM verification_tokens WHERE user_id = user_id;
    UPDATE audit_logs SET old_value = NULL, new_value = NULL WHERE user_id = user_id;
END;
$$ LANGUAGE plpgsql;

-- Trigger pour mettre à jour updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insertions initiales pour les tests
INSERT INTO users (email, username, password, first_n, last_n, birth_date, status, email_verified, age_verified)
VALUES 
    ('admin@virtualcompanion.app', 'admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewYpfQYHYde2keUu', 'Admin', 'User', '1990-01-01', 'ACTIVE', true, true),
    ('test@example.com', 'testuser', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewYpfQYHYde2keUu', 'Test', 'User', '1995-06-15', 'ACTIVE', true, true);

-- Assigner les rôles
INSERT INTO user_roles (user_id, role)
SELECT id, 'SUPER_ADMIN' FROM users WHERE email = 'admin@virtualcompanion.app'
UNION ALL
SELECT id, 'USER' FROM users WHERE email = 'test@example.com';

-- Ajouter les données de conformité
INSERT INTO user_compliance (user_id, terms_accepted_version, terms_accepted_at, privacy_accepted_version, privacy_accepted_at, age_verified_at)
SELECT id, '1.0', CURRENT_TIMESTAMP, '1.0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users;