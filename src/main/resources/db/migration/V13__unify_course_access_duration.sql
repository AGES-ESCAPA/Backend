-- Bugfix #72: courses tinha duas colunas com o mesmo papel (deadline e
-- access_duration_days). A tela do admin so preenche deadline, mas
-- PublishCourseUseCase exigia as duas, entao nenhum curso criado pela
-- aplicacao conseguia ser publicado. Fica so courses.deadline.

-- Antes de remover a coluna, preserva o prazo dos cursos que so tinham
-- access_duration_days: sem isso eles ficariam sem prazo e nao publicariam mais.
-- Quando os dois existem, vale deadline, que e o que a tela do admin preenche.
UPDATE courses
SET deadline = access_duration_days
WHERE deadline IS NULL
  AND access_duration_days IS NOT NULL;

ALTER TABLE courses DROP COLUMN access_duration_days;

COMMENT ON COLUMN courses.deadline IS
    'Dias de acesso concedidos ao aluno a partir da matricula.';
