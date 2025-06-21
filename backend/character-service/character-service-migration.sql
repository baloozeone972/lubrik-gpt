-- V1__create_character_tables.sql
-- Migration initiale pour le Character Service

-- Extension pour UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Définir le schéma
SET search_path TO character_service;

-- Table principale des personnages
CREATE TABLE characters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    n VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    backstory TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    category VARCHAR(20) NOT NULL,
    access_level VARCHAR(20) NOT NULL DEFAULT 'FREE',
    created_by_user_id UUID,
    is_official BOOLEAN DEFAULT FALSE,
    age_rating INTEGER DEFAULT 18,
    popularity_score DOUBLE PRECISION DEFAULT 0.0,
    interaction_count BIGINT DEFAULT 0,
    average_rating DOUBLE PRECISION DEFAULT 0.0,
    rating_count BIGINT DEFAULT 0,
    ai_model_id VARCHAR(100),
    ai_model_version VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    published_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'PUBLISHED', 'SUSPENDED', 'DELETED')),
    CONSTRAINT chk_category CHECK (category IN ('FRIEND', 'ROMANTIC', 'MENTOR', 'COMPANION', 'FANTASY', 'HISTORICAL', 'CELEBRITY', 'CUSTOM')),
    CONSTRAINT chk_access_level CHECK (access_level IN ('FREE', 'STANDARD', 'PREMIUM', 'VIP', 'EXCLUSIVE'))
);

CREATE INDEX idx_character_n ON characters(n);
CREATE INDEX idx_character_status ON characters(status);
CREATE INDEX idx_character_category ON characters(category);
CREATE INDEX idx_character_popularity ON characters(popularity_score DESC);
CREATE INDEX idx_character_created_by ON characters(created_by_user_id);
CREATE INDEX idx_character_official ON characters(is_official);

-- Table des langues supportées
CREATE TABLE character_languages (
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (character_id, language_code)
);

-- Table des personnalités
CREATE TABLE character_personalities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    character_id UUID NOT NULL UNIQUE REFERENCES characters(id) ON DELETE CASCADE,
    -- Big Five Traits
    openness INTEGER DEFAULT 50 CHECK (openness >= 0 AND openness <= 100),
    conscientiousness INTEGER DEFAULT 50 CHECK (conscientiousness >= 0 AND conscientiousness <= 100),
    extraversion INTEGER DEFAULT 50 CHECK (extraversion >= 0 AND extraversion <= 100),
    agreeableness INTEGER DEFAULT 50 CHECK (agreeableness >= 0 AND agreeableness <= 100),
    neuroticism INTEGER DEFAULT 50 CHECK (neuroticism >= 0 AND neuroticism <= 100),
    -- Communication Style
    formality_level INTEGER DEFAULT 50 CHECK (formality_level >= 0 AND formality_level <= 100),
    humor_level INTEGER DEFAULT 50 CHECK (humor_level >= 0 AND humor_level <= 100),
    empathy_level INTEGER DEFAULT 50 CHECK (empathy_level >= 0 AND empathy_level <= 100),
    assertiveness_level INTEGER DEFAULT 50 CHECK (assertiveness_level >= 0 AND assertiveness_level <= 100),
    -- Response Patterns
    response_style TEXT,
    vocabulary_level VARCHAR(20) DEFAULT 'MEDIUM',
    sentence_complexity VARCHAR(20) DEFAULT 'MEDIUM'
);

-- Table des traits personnalisés
CREATE TABLE character_traits (
    personality_id UUID NOT NULL REFERENCES character_personalities(id) ON DELETE CASCADE,
    trait_n VARCHAR(100) NOT NULL,
    trait_value VARCHAR(500),
    PRIMARY KEY (personality_id, trait_n)
);

-- Table des intérêts
CREATE TABLE character_interests (
    personality_id UUID NOT NULL REFERENCES character_personalities(id) ON DELETE CASCADE,
    interest VARCHAR(100) NOT NULL,
    PRIMARY KEY (personality_id, interest)
);

-- Table des apparences
CREATE TABLE character_appearances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    character_id UUID NOT NULL UNIQUE REFERENCES characters(id) ON DELETE CASCADE,
    age_appearance INTEGER,
    gender VARCHAR(50),
    ethnicity VARCHAR(100),
    height_cm INTEGER,
    body_type VARCHAR(50),
    hair_color VARCHAR(50),
    hair_style VARCHAR(100),
    eye_color VARCHAR(50),
    skin_tone VARCHAR(50),
    clothing_style VARCHAR(100),
    distinctive_features TEXT,
    model_file_url TEXT,
    texture_file_url TEXT,
    animation_set_id VARCHAR(100),
    avatar_preset_id VARCHAR(100),
    avatar_config JSON
);

-- Table des voix
CREATE TABLE character_voices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    voice_id VARCHAR(100),
    voice_provider VARCHAR(50),
    pitch REAL DEFAULT 0.0,
    speed REAL DEFAULT 1.0,
    tone VARCHAR(50),
    accent VARCHAR(50),
    emotion_range VARCHAR(20) DEFAULT 'NORMAL',
    sample_audio_url TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    is_premium BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_voice_character ON character_voices(character_id);
CREATE INDEX idx_voice_language ON character_voices(language_code);

-- Table des images
CREATE TABLE character_images (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    thumbnail_url TEXT,
    image_type VARCHAR(20) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    width INTEGER,
    height INTEGER,
    file_size BIGINT,
    mime_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_image_type CHECK (image_type IN ('PROFILE', 'FULL_BODY', 'EXPRESSION', 'OUTFIT', 'SCENE', 'PROMOTIONAL'))
);

CREATE INDEX idx_image_character ON character_images(character_id);
CREATE INDEX idx_image_type ON character_images(image_type);

-- Table des tags
CREATE TABLE character_tags (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    tag_n VARCHAR(50) NOT NULL,
    tag_category VARCHAR(50),
    UNIQUE(character_id, tag_n)
);

CREATE INDEX idx_tag_character ON character_tags(character_id);
CREATE INDEX idx_tag_n ON character_tags(tag_n);

-- Table des dialogues d'exemple
CREATE TABLE character_dialogues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    context TEXT,
    user_message TEXT,
    character_response TEXT NOT NULL,
    emotion VARCHAR(50),
    dialogue_type VARCHAR(50),
    order_index INTEGER
);

CREATE INDEX idx_dialogue_character ON character_dialogues(character_id);

-- Table de relation utilisateur-personnage
CREATE TABLE user_characters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    is_favorite BOOLEAN DEFAULT FALSE,
    is_unlocked BOOLEAN DEFAULT TRUE,
    unlock_method VARCHAR(50),
    relationship_level INTEGER DEFAULT 0,
    interaction_count BIGINT DEFAULT 0,
    last_interaction_at TIMESTAMP,
    custom_n VARCHAR(100),
    memory_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(user_id, character_id)
);

CREATE INDEX idx_user_character ON user_characters(user_id, character_id);
CREATE INDEX idx_user_characters ON user_characters(user_id);
CREATE INDEX idx_character_users ON user_characters(character_id);
CREATE INDEX idx_favorites ON user_characters(user_id) WHERE is_favorite = TRUE;

-- Table des évaluations
CREATE TABLE character_ratings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review TEXT,
    is_verified_purchase BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, character_id)
);

CREATE INDEX idx_rating_character ON character_ratings(character_id);
CREATE INDEX idx_rating_user ON character_ratings(user_id);

-- Table de personnalisation utilisateur
CREATE TABLE user_character_customizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_character_id UUID NOT NULL REFERENCES user_characters(id) ON DELETE CASCADE,
    customization_type VARCHAR(50) NOT NULL,
    customization_value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Table d'historique des interactions (pour analytics)
CREATE TABLE character_interaction_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    interaction_type VARCHAR(50) NOT NULL,
    duration_seconds INTEGER,
    message_count INTEGER,
    satisfaction_score INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interaction_date ON character_interaction_logs(created_at);
CREATE INDEX idx_interaction_character ON character_interaction_logs(character_id);
CREATE INDEX idx_interaction_user ON character_interaction_logs(user_id);

-- Vue pour les statistiques des personnages
CREATE VIEW character_statistics AS
SELECT 
    c.id,
    c.n,
    c.category,
    c.status,
    c.access_level,
    c.popularity_score,
    c.interaction_count,
    c.average_rating,
    c.rating_count,
    COUNT(DISTINCT uc.user_id) as unique_users,
    COUNT(DISTINCT CASE WHEN uc.is_favorite THEN uc.user_id END) as favorite_count,
    MAX(uc.last_interaction_at) as last_used_at
FROM characters c
LEFT JOIN user_characters uc ON c.id = uc.character_id
GROUP BY c.id;

-- Fonction pour mettre à jour les statistiques d'un personnage
CREATE OR REPLACE FUNCTION update_character_statistics(character_id UUID)
RETURNS void AS $$
BEGIN
    UPDATE characters c
    SET 
        average_rating = COALESCE((
            SELECT AVG(rating) FROM character_ratings 
            WHERE character_id = c.id
        ), 0),
        rating_count = (
            SELECT COUNT(*) FROM character_ratings 
            WHERE character_id = c.id
        ),
        interaction_count = (
            SELECT SUM(interaction_count) FROM user_characters 
            WHERE character_id = c.id
        )
    WHERE c.id = character_id;
END;
$$ LANGUAGE plpgsql;

-- Trigger pour mettre à jour les statistiques après une évaluation
CREATE OR REPLACE FUNCTION update_rating_statistics()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM update_character_statistics(NEW.character_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER after_rating_insert_or_update
AFTER INSERT OR UPDATE ON character_ratings
FOR EACH ROW EXECUTE FUNCTION update_rating_statistics();

-- Trigger pour mettre à jour updated_at
CREATE TRIGGER update_characters_updated_at BEFORE UPDATE ON characters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_characters_updated_at BEFORE UPDATE ON user_characters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insertion de personnages par défaut pour les tests
INSERT INTO characters (n, description, category, status, is_official, age_rating) VALUES
('Sophia', 'Une amie bienveillante et empathique', 'FRIEND', 'PUBLISHED', true, 13),
('Marcus', 'Un mentor sage et inspirant', 'MENTOR', 'PUBLISHED', true, 13),
('Luna', 'Une compagne mystérieuse et fascinante', 'COMPANION', 'PUBLISHED', true, 16),
('Alexander', 'Un partenaire romantique attentionné', 'ROMANTIC', 'PUBLISHED', true, 18),
('Aria', 'Une guerrière elfe courageuse', 'FANTASY', 'PUBLISHED', true, 16);

-- Ajouter des personnalités aux personnages
INSERT INTO character_personalities (character_id, openness, conscientiousness, extraversion, agreeableness, neuroticism)
SELECT id, 
    70 + FLOOR(RANDOM() * 20)::INTEGER,  -- openness
    60 + FLOOR(RANDOM() * 30)::INTEGER,  -- conscientiousness
    50 + FLOOR(RANDOM() * 40)::INTEGER,  -- extraversion
    70 + FLOOR(RANDOM() * 25)::INTEGER,  -- agreeableness
    20 + FLOOR(RANDOM() * 30)::INTEGER   -- neuroticism
FROM characters;

-- Ajouter des tags aux personnages
INSERT INTO character_tags (character_id, tag_n, tag_category)
SELECT id, tag, 'personality'
FROM characters,
LATERAL (VALUES ('friendly'), ('supportive'), ('intelligent'), ('creative')) AS tags(tag)
WHERE is_official = true;