-- Cancelamento de pedido (US-28): o acesso é retirado sem apagar matrícula, progresso
-- nem certificado. revoked_at nulo significa acesso ativo; preenchido, o acesso foi
-- revogado naquele momento.
ALTER TABLE user_courses ADD COLUMN revoked_at TIMESTAMP;

ALTER TABLE company_courses ADD COLUMN revoked_at TIMESTAMP;
