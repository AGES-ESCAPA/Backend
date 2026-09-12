-- Espelha uk_modules_course_order: o /reorder de conteudos depende desta
-- constraint para garantir uma ordem unica por modulo.
ALTER TABLE content
    ADD CONSTRAINT uk_content_module_order UNIQUE (module_id, "order");
