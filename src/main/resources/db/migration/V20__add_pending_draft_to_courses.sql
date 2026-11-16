-- Rascunho ainda não publicado de um curso que já está no ar.
-- Null quando não há alteração pendente. Mesmo tipo de learning_objectives.
ALTER TABLE courses
    ADD COLUMN pending_draft JSON;
