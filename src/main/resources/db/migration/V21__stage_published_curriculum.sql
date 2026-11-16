-- Currículo de um curso já publicado. order é a posição do construtor;
-- published_order é a que o aluno lê. Null em published_order esconde a linha
-- até a próxima publicação. pending_* guarda edição e exclusão ainda não publicadas.

ALTER TABLE modules
    ADD COLUMN published_order INT,
    ADD COLUMN pending_title VARCHAR(255),
    ADD COLUMN pending_deletion BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE content
    ADD COLUMN published_order INT,
    ADD COLUMN pending_draft JSON,
    ADD COLUMN pending_deletion BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE modules SET published_order = "order";
UPDATE content SET published_order = "order";

CREATE OR REPLACE FUNCTION sync_lessons_count() RETURNS TRIGGER AS $$
DECLARE
    v_course_id UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        SELECT course_id INTO v_course_id FROM modules WHERE id = OLD.module_id;
    ELSE
        SELECT course_id INTO v_course_id FROM modules WHERE id = NEW.module_id;
    END IF;

    PERFORM refresh_lessons_count(v_course_id);

    IF TG_OP = 'UPDATE' AND OLD.module_id IS DISTINCT FROM NEW.module_id THEN
        SELECT course_id INTO v_course_id FROM modules WHERE id = OLD.module_id;
        PERFORM refresh_lessons_count(v_course_id);
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION refresh_lessons_count(v_course_id UUID) RETURNS VOID AS $$
BEGIN
    IF v_course_id IS NULL THEN
        RETURN;
    END IF;

    -- Curso ainda não publicado conta todas as aulas. No publicado, só as que
    -- o aluno consegue abrir: módulo e aula com published_order preenchido.
    UPDATE courses
    SET lessons_count = (
        SELECT COUNT(*)
        FROM content c
        JOIN modules m ON c.module_id = m.id
        JOIN courses co ON m.course_id = co.id
        WHERE m.course_id = v_course_id
          AND (
            co.status IS DISTINCT FROM 'PUBLISHED'
            OR (c.published_order IS NOT NULL AND m.published_order IS NOT NULL)
          )
    )
    WHERE id = v_course_id;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_lessons_count ON content;

CREATE TRIGGER trg_sync_lessons_count
    AFTER INSERT OR UPDATE OF module_id, published_order OR DELETE ON content
    FOR EACH ROW
    EXECUTE FUNCTION sync_lessons_count();

CREATE OR REPLACE FUNCTION sync_lessons_count_from_module() RETURNS TRIGGER AS $$
BEGIN
    PERFORM refresh_lessons_count(NEW.course_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_lessons_count_module ON modules;

CREATE TRIGGER trg_sync_lessons_count_module
    AFTER UPDATE OF published_order ON modules
    FOR EACH ROW
    EXECUTE FUNCTION sync_lessons_count_from_module();
