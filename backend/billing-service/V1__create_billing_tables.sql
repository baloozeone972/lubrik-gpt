
-- V1__create_billing_tables.sql
-- Migration initiale pour le Billing Service

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET search_path TO billing_service;

-- Table des abonnements
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE,
    plan_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    stripe_subscription_id VARCHAR(255) UNIQUE,
    stripe_customer_id VARCHAR(255),
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    trial_start TIMESTAMP,
    trial_end TIMESTAMP,
    cancel_at_period_end BOOLEAN DEFAULT FALSE,
    canceled_at TIMESTAMP,
    expires_at TIMESTAMP,
    billing_cycle VARCHAR(20) DEFAULT 'MONTHLY',
    amount DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'EUR',
    tax_rate DECIMAL(5,2),
    discount_percentage DECIMAL(5,2),
    promo_code VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_plan_type CHECK (plan_type IN ('FREE', 'STANDARD', 'PREMIUM', 'VIP', 'ENTERPRISE')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'TRIALING', 'PAST_DUE', 'CANCELED', 'INCOMPLETE', 'INCOMPLETE_EXPIRED', 'UNPAID', 'PAUSED')),
    CONSTRAINT chk_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'QUARTERLY', 'YEARLY'))
);

CREATE INDEX idx_subscription_user ON subscriptions(user_id);
CREATE INDEX idx_subscription_status ON subscriptions(status);
CREATE INDEX idx_subscription_expires ON subscriptions(expires_at);

-- Table des limites d'abonnement
CREATE TABLE subscription_limits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subscription_id UUID NOT NULL UNIQUE REFERENCES subscriptions(id) ON DELETE CASCADE,
    max_characters INTEGER,
    max_custom_characters INTEGER,
    messages_per_hour INTEGER,
    messages_per_day INTEGER,
    messages_per_month INTEGER,
    tokens_per_hour BIGINT,
    tokens_per_day BIGINT,
    tokens_per_month BIGINT,
    video_chat_enabled BOOLEAN DEFAULT FALSE,
    voice_chat_enabled BOOLEAN DEFAULT FALSE,
    premium_voices_enabled BOOLEAN DEFAULT FALSE,
    custom_avatars_enabled BOOLEAN DEFAULT FALSE,
    api_access_enabled BOOLEAN DEFAULT FALSE,
    max_storage_gb INTEGER,
    max_conversation_history_days INTEGER
);

-- Table des méthodes de paiement
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    stripe_payment_method_id VARCHAR(255) UNIQUE,
    method_type VARCHAR(20) NOT NULL,
    card_brand VARCHAR(20),
    card_last4 VARCHAR(4),
    card_exp_month INTEGER,
    card_exp_year INTEGER,
    billing_email VARCHAR(255),
    billing_n VARCHAR(255),
    billing_address JSON,
    is_default BOOLEAN DEFAULT FALSE,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_method_type CHECK (method_type IN ('CARD', 'BANK_ACCOUNT', 'PAYPAL', 'APPLE_PAY', 'GOOGLE_PAY'))
);

CREATE INDEX idx_payment_method_user ON payment_methods(user_id);
CREATE INDEX idx_payment_method_default ON payment_methods(user_id, is_default);

-- Table des paiements
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    subscription_id UUID REFERENCES subscriptions(id),
    payment_method_id UUID REFERENCES payment_methods(id),
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    stripe_charge_id VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    tax_amount DECIMAL(10,2),
    total_amount DECIMAL(10,2),
    description VARCHAR(500),
    failure_reason VARCHAR(500),
    refunded_amount DECIMAL(10,2),
    metadata JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELED', 'REFUNDED', 'PARTIALLY_REFUNDED')),
    CONSTRAINT chk_payment_type CHECK (payment_type IN ('SUBSCRIPTION', 'ONE_TIME', 'TOP_UP', 'REFUND'))
);

CREATE INDEX idx_payment_user ON payments(user_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_created ON payments(created_at);

-- Table des factures
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    payment_id UUID UNIQUE REFERENCES payments(id),
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    stripe_invoice_id VARCHAR(255) UNIQUE,
    status VARCHAR(20) NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    due_date TIMESTAMP,
    period_start TIMESTAMP,
    period_end TIMESTAMP,
    subtotal DECIMAL(10,2),
    tax_amount DECIMAL(10,2),
    tax_rate DECIMAL(5,2),
    discount_amount DECIMAL(10,2),
    total_amount DECIMAL(10,2),
    currency VARCHAR(3),
    billing_n VARCHAR(255),
    billing_email VARCHAR(255),
    billing_address JSON,
    tax_id VARCHAR(50),
    pdf_url TEXT,
    pdf_generated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_invoice_status CHECK (status IN ('DRAFT', 'OPEN', 'PAID', 'VOID', 'UNCOLLECTIBLE'))
);

CREATE INDEX idx_invoice_user ON invoices(user_id);
CREATE INDEX idx_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoice_date ON invoices(invoice_date);

-- Table des lignes de facture
CREATE TABLE invoice_line_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity INTEGER DEFAULT 1,
    unit_price DECIMAL(10,2),
    amount DECIMAL(10,2),
    tax_amount DECIMAL(10,2),
    total_amount DECIMAL(10,2),
    metadata JSON
);

-- Table d'historique des abonnements
CREATE TABLE subscription_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    old_value VARCHAR(500),
    new_value VARCHAR(500),
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table des enregistrements d'utilisation
CREATE TABLE usage_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    usage_type VARCHAR(50) NOT NULL,
    quantity BIGINT NOT NULL,
    unit VARCHAR(20),
    resource_id VARCHAR(255),
    description VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usage_type CHECK (usage_type IN ('MESSAGE_SENT', 'TOKEN_USED', 'VIDEO_MINUTE', 'VOICE_MINUTE', 'STORAGE_GB', 'API_CALL', 'CHARACTER_CREATED'))
);

CREATE INDEX idx_usage_user ON usage_records(user_id);
CREATE INDEX idx_usage_type ON usage_records(usage_type);
CREATE INDEX idx_usage_timestamp ON usage_records(timestamp);

-- Vue pour les statistiques de revenus
CREATE VIEW revenue_statistics AS
SELECT 
    DATE_TRUNC('month', p.created_at) as month,
    COUNT(DISTINCT p.user_id) as unique_customers,
    COUNT(p.id) as total_payments,
    SUM(CASE WHEN p.status = 'COMPLETED' THEN p.total_amount ELSE 0 END) as revenue,
    AVG(CASE WHEN p.status = 'COMPLETED' THEN p.total_amount ELSE NULL END) as avg_payment,
    SUM(CASE WHEN p.status = 'REFUNDED' THEN p.refunded_amount ELSE 0 END) as refunds
FROM payments p
GROUP BY DATE_TRUNC('month', p.created_at)
ORDER BY month DESC;