-- user_courses.progress nunca era atualizado ao concluir aulas: recalcula a partir
-- de user_content_progress, com a mesma regra da aplicacao (arredonda para baixo).
WITH course_lessons AS (
    SELECT m.course_id, COUNT(*) AS total
    FROM content c
    JOIN modules m ON m.id = c.module_id
    GROUP BY m.course_id
),
completed_lessons AS (
    SELECT p.user_id, m.course_id, COUNT(*) AS completed, MAX(p.completed_at) AS last_completed_at
    FROM user_content_progress p
    JOIN content c ON c.id = p.content_id
    JOIN modules m ON m.id = c.module_id
    GROUP BY p.user_id, m.course_id
)
UPDATE user_courses uc
SET progress = LEAST(100, cl.completed * 100 / l.total),
    conclusion_date = CASE
        WHEN cl.completed >= l.total AND uc.conclusion_date IS NULL THEN CAST(cl.last_completed_at AS DATE)
        ELSE uc.conclusion_date
    END
FROM completed_lessons cl
JOIN course_lessons l ON l.course_id = cl.course_id
WHERE uc.user_id = cl.user_id
  AND uc.course_id = cl.course_id;
