-- Troca de e-mail confirmada por link (token distinto do reset de senha).
-- Guarda só o hash; o token bruto existe apenas no e-mail enviado ao endereço NOVO.
-- users.email continua sendo o e-mail ativo até a confirmação.

CREATE TABLE email_change_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    new_email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    requested_by UUID,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_change_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_change_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_email_change_tokens_requested_by FOREIGN KEY (requested_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_email_change_tokens_user ON email_change_tokens (user_id);

-- Histórico de edições administrativas (inclui colaboradores e demais perfis).
CREATE TABLE user_profile_change_log (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    changed_by UUID,
    field_name VARCHAR(64) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_profile_change_log_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_profile_change_log_actor FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_user_profile_change_log_user ON user_profile_change_log (user_id);
CREATE INDEX idx_user_profile_change_log_created ON user_profile_change_log (created_at);
