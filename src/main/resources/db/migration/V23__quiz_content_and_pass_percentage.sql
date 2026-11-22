ALTER TABLE content DROP CONSTRAINT IF EXISTS ck_content_type;
ALTER TABLE content ADD CONSTRAINT ck_content_type CHECK (type IN ('VIDEO', 'TEXT', 'FILE', 'QUIZ'));

ALTER TABLE content ADD COLUMN IF NOT EXISTS quiz_json JSON;

ALTER TABLE courses ADD COLUMN IF NOT EXISTS min_quiz_pass_percentage INTEGER NOT NULL DEFAULT 70;
ALTER TABLE courses ADD CONSTRAINT ck_courses_min_quiz_pass_percentage
    CHECK (min_quiz_pass_percentage BETWEEN 0 AND 100);