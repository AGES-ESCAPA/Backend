-- =============================================================================
-- V6: redes de seguranca para regras que o Java checa antes, e os contadores
--     desnormalizados que ainda nao tinham mecanismo de atualizacao.
--
-- Principio (ver AGENTS.md, "Banco de dados"): o banco guarda integridade
-- (enum, faixa, unicidade, FK) e contador derivado por trigger. Decisao de fluxo
-- fica no service. Toda constraint abaixo tem, ou tera, checagem anterior no
-- service ou no DTO; aqui ela so impede que dado invalido entre por outro caminho
-- (seed, script, condicao de corrida).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Enum sem gemeo no banco: users.role. Gemeo em Java: user.entity.UserRole.
-- -----------------------------------------------------------------------------
ALTER TABLE users
    ADD CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'ADMIN', 'COMPANY'));

-- -----------------------------------------------------------------------------
-- 2. Faixas numericas
-- -----------------------------------------------------------------------------
ALTER TABLE user_courses
    ADD CONSTRAINT ck_user_courses_progress CHECK (progress IS NULL OR progress BETWEEN 0 AND 100);

ALTER TABLE courses
    ADD CONSTRAINT ck_courses_price CHECK (price IS NULL OR price >= 0),
    ADD CONSTRAINT ck_courses_duration CHECK (duration_time IS NULL OR duration_time >= 0),
    ADD CONSTRAINT ck_courses_access_days CHECK (access_duration_days IS NULL OR access_duration_days > 0),
    ADD CONSTRAINT ck_courses_counters CHECK (
        lessons_count >= 0 AND materials_count >= 0 AND students_count >= 0 AND reviews_count >= 0
    ),
    ADD CONSTRAINT ck_courses_rating CHECK (rating_average IS NULL OR rating_average BETWEEN 0 AND 5);

ALTER TABLE modules
    ADD CONSTRAINT ck_modules_order CHECK ("order" > 0);

ALTER TABLE content
    ADD CONSTRAINT ck_content_order CHECK ("order" > 0);

ALTER TABLE course_materials
    ADD CONSTRAINT ck_course_materials_order CHECK ("order" IS NULL OR "order" > 0);

-- -----------------------------------------------------------------------------
-- 3. Datas coerentes
-- -----------------------------------------------------------------------------
ALTER TABLE user_courses
    ADD CONSTRAINT ck_user_courses_dates CHECK (
        dt_inicio IS NULL OR dt_expiracao IS NULL OR dt_expiracao >= dt_inicio
    );

ALTER TABLE company_courses
    ADD CONSTRAINT ck_company_courses_dates CHECK (
        data_inicio IS NULL OR data_expiracao IS NULL OR data_expiracao >= data_inicio
    );

-- -----------------------------------------------------------------------------
-- 4. Contadores derivados: reviews_count, rating_average e materials_count.
--    Mesmo padrao da V5 (lessons_count): recalcula com COUNT/AVG em vez de
--    incrementar, para nao acumular drift. students_count fica de fora ate a
--    US de matricula definir o que conta (expirado? empresa?).
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION recalc_review_counters(p_course_id UUID) RETURNS VOID AS $$
BEGIN
    IF p_course_id IS NULL THEN
        RETURN;
    END IF;
    UPDATE courses
    SET reviews_count  = (SELECT COUNT(*) FROM course_reviews WHERE course_id = p_course_id),
        rating_average = (SELECT AVG(rating)::double precision FROM course_reviews WHERE course_id = p_course_id)
    WHERE id = p_course_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sync_review_counters() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM recalc_review_counters(OLD.course_id);
        RETURN OLD;
    END IF;

    PERFORM recalc_review_counters(NEW.course_id);
    IF TG_OP = 'UPDATE' AND OLD.course_id IS DISTINCT FROM NEW.course_id THEN
        PERFORM recalc_review_counters(OLD.course_id);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_review_counters
    AFTER INSERT OR UPDATE OF rating, course_id OR DELETE ON course_reviews
    FOR EACH ROW
    EXECUTE FUNCTION sync_review_counters();

CREATE OR REPLACE FUNCTION recalc_materials_count(p_course_id UUID) RETURNS VOID AS $$
BEGIN
    IF p_course_id IS NULL THEN
        RETURN;
    END IF;
    UPDATE courses
    SET materials_count = (SELECT COUNT(*) FROM course_materials WHERE course_id = p_course_id)
    WHERE id = p_course_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sync_materials_count() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM recalc_materials_count(OLD.course_id);
        RETURN OLD;
    END IF;

    PERFORM recalc_materials_count(NEW.course_id);
    IF TG_OP = 'UPDATE' AND OLD.course_id IS DISTINCT FROM NEW.course_id THEN
        PERFORM recalc_materials_count(OLD.course_id);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_materials_count
    AFTER INSERT OR UPDATE OF course_id OR DELETE ON course_materials
    FOR EACH ROW
    EXECUTE FUNCTION sync_materials_count();

-- -----------------------------------------------------------------------------
-- 5. Reconcilia o que ja existe (seed, dados anteriores a esta migration).
-- -----------------------------------------------------------------------------
UPDATE courses c
SET reviews_count   = (SELECT COUNT(*) FROM course_reviews r WHERE r.course_id = c.id),
    rating_average  = (SELECT AVG(r.rating)::double precision FROM course_reviews r WHERE r.course_id = c.id),
    materials_count = (SELECT COUNT(*) FROM course_materials m WHERE m.course_id = c.id);
