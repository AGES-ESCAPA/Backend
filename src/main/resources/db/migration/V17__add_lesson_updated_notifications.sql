ALTER TABLE notifications ADD COLUMN content_id UUID;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_content FOREIGN KEY (content_id) REFERENCES content (id) ON DELETE CASCADE;

ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;

ALTER TABLE notifications ADD CONSTRAINT ck_notifications_type CHECK (
    type IN ('COURSE_PUBLISHED', 'COURSE_UPDATED', 'COURSE_EXPIRING', 'CERTIFICATE_ISSUED', 'GENERAL', 'LESSON_UPDATED')
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
