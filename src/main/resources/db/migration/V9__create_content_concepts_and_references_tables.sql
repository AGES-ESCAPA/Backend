CREATE TABLE content_concepts (
    content_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    "order" INTEGER NOT NULL,
    CONSTRAINT fk_content_concepts_content FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    CONSTRAINT uq_content_concepts_order UNIQUE (content_id, "order")
);

CREATE TABLE content_references (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    "order" INTEGER NOT NULL,
    CONSTRAINT fk_content_references_content FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    CONSTRAINT uq_content_references_order UNIQUE (content_id, "order")
);
