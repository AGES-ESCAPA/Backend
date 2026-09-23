-- =============================================================================
-- Migration Secundária de Seed - Usuários V1 para Testes
-- Executa após o R__seed_dev.sql (ordem alfabética).
-- =============================================================================

-- Remove os dados caso esse script rode repetidamente para evitar PK duplicada
DELETE FROM user_content_progress WHERE user_id = 'b0000000-0000-4000-c000-000000000004';
DELETE FROM user_courses WHERE user_id = 'b0000000-0000-4000-c000-000000000004';
DELETE FROM users_company WHERE user_id = 'b0000000-0000-4000-c000-000000000004';
DELETE FROM admins WHERE user_id = 'b0000000-0000-4000-c000-000000000001';
DELETE FROM company WHERE id = 'b0000000-0000-4000-c000-000000000002';
DELETE FROM regular_users WHERE user_id IN ('b0000000-0000-4000-c000-000000000003', 'b0000000-0000-4000-c000-000000000004');
DELETE FROM users WHERE id IN (
    'b0000000-0000-4000-c000-000000000001',
    'b0000000-0000-4000-c000-000000000002',
    'b0000000-0000-4000-c000-000000000003',
    'b0000000-0000-4000-c000-000000000004'
);

-- ==========================================
-- 1. Inserindo na tabela base (users)
-- Senha de todos: 123
-- ==========================================
INSERT INTO users (id, name, email, password_hash, role, status, created_at) VALUES
    ('b0000000-0000-4000-c000-000000000001', 'Admin V1 Teste', 'admin_v1@escapa.com', '$2b$10$x1Lr6X2h7LbTZWbbZHsjIOZQUc2C3k7pWwVrNRbuU0L/RW4HytA5C', 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP),
    ('b0000000-0000-4000-c000-000000000002', 'Empresa V1 S.A.', 'company_v1@escapa.com', '$2b$10$x1Lr6X2h7LbTZWbbZHsjIOZQUc2C3k7pWwVrNRbuU0L/RW4HytA5C', 'COMPANY', 'ACTIVE', CURRENT_TIMESTAMP),
    ('b0000000-0000-4000-c000-000000000003', 'Aluno Comum V1', 'student_v1@escapa.com', '$2b$10$x1Lr6X2h7LbTZWbbZHsjIOZQUc2C3k7pWwVrNRbuU0L/RW4HytA5C', 'STUDENT', 'ACTIVE', CURRENT_TIMESTAMP),
    ('b0000000-0000-4000-c000-000000000004', 'Aluno Vinculado V1', 'linked_student_v1@escapa.com', '$2b$10$x1Lr6X2h7LbTZWbbZHsjIOZQUc2C3k7pWwVrNRbuU0L/RW4HytA5C', 'STUDENT', 'ACTIVE', CURRENT_TIMESTAMP);

-- ==========================================
-- 2. Perfis Específicos
-- ==========================================
INSERT INTO admins (user_id, department, headline, bio) VALUES
    ('b0000000-0000-4000-c000-000000000001', 'Testes QA', 'Coordenador de Testes', 'Bio do Admin de Testes');

INSERT INTO company (id, company_name, cnpj_id, company_email, matricula) VALUES
    ('b0000000-0000-4000-c000-000000000002', 'Empresa V1 S.A.', '00000000000199', 'contato@v1.com', 9999);

INSERT INTO regular_users (user_id, cpf, phone) VALUES
    ('b0000000-0000-4000-c000-000000000003', '11111111111', '51999999999'),
    ('b0000000-0000-4000-c000-000000000004', '22222222222', '51888888888');

-- ==========================================
-- 3. Vínculos do Usuário 4 (O "Super Aluno")
-- ==========================================

-- A) Vínculo B2B com a Empresa (users_company)
INSERT INTO users_company (user_id, company_id, role) VALUES
    ('b0000000-0000-4000-c000-000000000004', 'b0000000-0000-4000-c000-000000000002', 'EMPLOYEE');

-- B) Vínculo com Cursos (Matrículas em user_courses)
-- Usando cursos que já existem no R__seed_dev.sql original
INSERT INTO user_courses (user_id, course_id, dt_inicio, dt_expiracao, progress, certificate_issued) VALUES
    ('b0000000-0000-4000-c000-000000000004', 'e0000000-0000-4000-e000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 year', 50, FALSE),
    ('b0000000-0000-4000-c000-000000000004', 'e0000000-0000-4000-e000-000000000002', CURRENT_TIMESTAMP - INTERVAL '1 month', CURRENT_TIMESTAMP + INTERVAL '11 months', 100, TRUE);

-- C) Vínculo com Progresso de Aulas (user_content_progress)
-- Marcando 2 aulas como concluídas
INSERT INTO user_content_progress (user_id, content_id, completed_at) VALUES
    ('b0000000-0000-4000-c000-000000000004', '02000000-0000-4000-9000-000000000001', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    ('b0000000-0000-4000-c000-000000000004', '02000000-0000-4000-9000-000000000002', CURRENT_TIMESTAMP - INTERVAL '2 days');


-- ==========================================
-- 4. Matrículas para o Admin (para testar a tela "Meus Cursos")
-- Como a RBAC agora permite ADMIN nas rotas de STUDENT, o admin
-- pode ver sua própria listagem. 1 curso para cada status:
-- ==========================================
INSERT INTO user_courses (user_id, course_id, dt_inicio, dt_expiracao, progress, certificate_issued) VALUES
    -- Em Andamento (IN_PROGRESS)
    ('b0000000-0000-4000-c000-000000000001', 'e0000000-0000-4000-e000-000000000001', CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE + INTERVAL '1 year', 50, FALSE),
    
    -- Concluído (COMPLETED)
    ('b0000000-0000-4000-c000-000000000001', 'e0000000-0000-4000-e000-000000000002', CURRENT_DATE - INTERVAL '2 months', CURRENT_DATE + INTERVAL '10 months', 100, TRUE),
    
    -- Aguardando (PENDING) - dt_inicio no futuro
    ('b0000000-0000-4000-c000-000000000001', 'e0000000-0000-4000-e000-000000000003', CURRENT_DATE + INTERVAL '10 days', CURRENT_DATE + INTERVAL '1 year', 0, FALSE),
    
    -- Expirado (EXPIRED) - dt_expiracao no passado
    ('b0000000-0000-4000-c000-000000000001', 'e0000000-0000-4000-e000-000000000004', CURRENT_DATE - INTERVAL '2 years', CURRENT_DATE - INTERVAL '1 year', 20, FALSE);

