-- Cache do PDF do certificado (US-19): gerado sob demanda no primeiro download
-- e reaproveitado nos seguintes, em vez de reprocessar o template a cada requisicao.
ALTER TABLE user_courses ADD COLUMN certificate_pdf BYTEA;
