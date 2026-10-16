-- Bugfix #72: courses tinha duas colunas com o mesmo papel (deadline e
-- access_duration_days). A tela do admin so preenche deadline, mas
-- PublishCourseUseCase exigia as duas, entao nenhum curso criado pela
-- aplicacao conseguia ser publicado. Fica so courses.deadline.
ALTER TABLE courses DROP COLUMN access_duration_days;

COMMENT ON COLUMN courses.deadline IS
    'Dias de acesso concedidos ao aluno a partir da matricula.';
