CREATE TABLE user_content_progress (
    user_id UUID NOT NULL,
    content_id UUID NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, content_id),
    CONSTRAINT fk_user_content_progress_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_content_progress_content FOREIGN KEY (content_id) REFERENCES content (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_content_progress_user ON user_content_progress (user_id);