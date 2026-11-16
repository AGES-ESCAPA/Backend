-- Convite de colaborador (US-31, #83): o gestor cadastra o colaborador, que fica PENDING
-- até criar a própria senha pelo link enviado por e-mail.

ALTER TABLE users DROP CONSTRAINT ck_users_status;
ALTER TABLE users ADD CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'PENDING', 'INACTIVE'));

-- token_hash guarda só o SHA-256 do token (hex). O token em si existe apenas no link do
-- e-mail, então quem lê o banco não consegue usar um convite. used_at nulo = convite em aberto.
CREATE TABLE account_invites (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    company_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_account_invites_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_account_invites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_account_invites_company FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE CASCADE
);

CREATE INDEX idx_account_invites_user ON account_invites (user_id);
