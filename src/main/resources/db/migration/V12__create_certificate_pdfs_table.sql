-- Cache do PDF do certificado por tema e versao do template (US-19, template fiel
-- a tela). Substitui a coluna user_courses.certificate_pdf (V8), que guardava um
-- unico PDF por matricula, sem tema nem versao. Os PDFs do template antigo saem
-- junto com a coluna, entao nao sobra arquivo velho para ser servido.
CREATE TABLE certificate_pdfs (
    certificate_code VARCHAR(255) NOT NULL REFERENCES user_courses (certificate_code) ON DELETE CASCADE,
    theme VARCHAR(10) NOT NULL,
    template_version VARCHAR(50) NOT NULL,
    pdf BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (certificate_code, theme, template_version),
    CONSTRAINT ck_certificate_pdfs_theme CHECK (theme IN ('LIGHT', 'DARK'))
);

ALTER TABLE user_courses DROP COLUMN certificate_pdf;
