-- =============================================================================
-- V5: Trigger para manter courses.lessons_count sincronizado.
--
-- Sempre que uma aula (content) for inserida, removida ou movida entre módulos,
-- o contador lessons_count do curso correspondente é recalculado.
--
-- Usa COUNT(*) real (em vez de incremento/decremento) para evitar drift.
-- =============================================================================

CREATE OR REPLACE FUNCTION sync_lessons_count() RETURNS TRIGGER AS $$
DECLARE
    v_course_id UUID;
BEGIN
    -- Determina o curso afetado a partir do módulo da aula
    IF TG_OP = 'DELETE' THEN
        SELECT course_id INTO v_course_id FROM modules WHERE id = OLD.module_id;
    ELSE
        SELECT course_id INTO v_course_id FROM modules WHERE id = NEW.module_id;
    END IF;

    -- Recalcula a contagem real de aulas do curso
    IF v_course_id IS NOT NULL THEN
        UPDATE courses
        SET lessons_count = (
            SELECT COUNT(*)
            FROM content c
            JOIN modules m ON c.module_id = m.id
            WHERE m.course_id = v_course_id
        )
        WHERE id = v_course_id;
    END IF;

    -- Se a aula mudou de módulo, recalcula o curso antigo também
    IF TG_OP = 'UPDATE' AND OLD.module_id IS DISTINCT FROM NEW.module_id THEN
        SELECT course_id INTO v_course_id FROM modules WHERE id = OLD.module_id;
        IF v_course_id IS NOT NULL THEN
            UPDATE courses
            SET lessons_count = (
                SELECT COUNT(*)
                FROM content c
                JOIN modules m ON c.module_id = m.id
                WHERE m.course_id = v_course_id
            )
            WHERE id = v_course_id;
        END IF;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_lessons_count
    AFTER INSERT OR UPDATE OF module_id OR DELETE ON content
    FOR EACH ROW
    EXECUTE FUNCTION sync_lessons_count();
