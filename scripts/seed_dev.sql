-- =============================================================================
-- Seed de desenvolvimento — Escapa!
-- =============================================================================
--
-- ATENCAO: este script APAGA todo o conteudo das tabelas de dominio antes de
-- inserir. Use apenas em banco local de desenvolvimento.
--
-- Ele NAO fica em db/migration de proposito: o Flyway nao o enxerga, entao nao
-- ha risco de dado ficticio ser aplicado em homologacao ou producao.
--
-- Como rodar (com o docker-compose no ar):
--   docker exec -i escapadb psql -U escapa -d escapa_db < scripts/seed_dev.sql
--
-- Senhas em texto puro (os hashes abaixo sao BCrypt de verdade):
--   admins   -> admin12345
--   empresa  -> empresa12345
--   demais   -> senha12345
--
-- Os UUIDs sao fixos para que o script seja re-executavel e para facilitar
-- testar endpoints com ids conhecidos.
-- =============================================================================

BEGIN;

TRUNCATE TABLE
    notifications,
    course_reviews,
    company_courses,
    user_courses,
    module_prerequisites,
    content,
    modules,
    course_change_log,
    course_materials,
    course_prerequisites,
    courses,
    users_company,
    regular_users,
    admins,
    company,
    users
CASCADE;

-- -----------------------------------------------------------------------------
-- users (tabela base da heranca JOINED: todo usuario aparece aqui e em
-- exatamente uma das tabelas filhas)
-- -----------------------------------------------------------------------------
INSERT INTO users (id, name, email, password_hash, role, status, created_at) VALUES
    -- admins / instrutores
    ('a0000000-0000-4000-a000-000000000001', 'Beatriz Nunes', 'beatriz.nunes@escapa.com',
     '$2a$10$kNBr0lRO8Fn7/MSnVVgyWenVxq6wZjEhg26FN4REXA8bfZko12MTi', 'ADMIN', 'ACTIVE', '2026-01-10 09:00:00'),
    ('a0000000-0000-4000-a000-000000000002', 'Rafael Antunes', 'rafael.antunes@escapa.com',
     '$2a$10$kNBr0lRO8Fn7/MSnVVgyWenVxq6wZjEhg26FN4REXA8bfZko12MTi', 'ADMIN', 'ACTIVE', '2026-01-12 14:30:00'),

    -- alunos avulsos
    ('b0000000-0000-4000-b000-000000000001', 'Mariana Costa', 'mariana.costa@email.com',
     '$2a$10$ibpqH7THcZTbgcyqTYC0iOcvPPHi92gGYQid5BgTYsOJYzBQ/HmwS', 'STUDENT', 'ACTIVE', '2026-02-03 10:15:00'),
    ('b0000000-0000-4000-b000-000000000002', 'Joao Pedro Alves', 'joao.alves@email.com',
     '$2a$10$ibpqH7THcZTbgcyqTYC0iOcvPPHi92gGYQid5BgTYsOJYzBQ/HmwS', 'STUDENT', 'ACTIVE', '2026-02-05 16:40:00'),
    -- inativo de proposito, para exercitar filtros por status
    ('b0000000-0000-4000-b000-000000000003', 'Carla Menezes', 'carla.menezes@email.com',
     '$2a$10$ibpqH7THcZTbgcyqTYC0iOcvPPHi92gGYQid5BgTYsOJYzBQ/HmwS', 'STUDENT', 'INACTIVE', '2026-02-08 08:05:00'),

    -- a empresa tambem e um usuario (company.id referencia users.id)
    ('d0000000-0000-4000-d000-000000000001', 'Pousada Vista Mar', 'contato@vistamar.com.br',
     '$2a$10$qmo4r05zCLBtnXxsr0yw8e6ix8MvfPZqS6JBte6m1BXU1D46/mwHq', 'COMPANY', 'ACTIVE', '2026-01-20 13:00:00');

INSERT INTO admins (user_id, department, headline, bio, avatar_url) VALUES
    ('a0000000-0000-4000-a000-000000000001', 'Hospitalidade', 'Especialista em experiencia do hospede',
     'Doze anos coordenando recepcao e governanca em hoteis de praia no litoral catarinense.',
     'https://cdn.escapa.com/avatars/beatriz.png'),
    ('a0000000-0000-4000-a000-000000000002', 'Turismo', 'Consultor de receita e distribuicao',
     'Trabalhou com revenue management em redes hoteleiras e hoje presta consultoria para pousadas.',
     'https://cdn.escapa.com/avatars/rafael.png');

INSERT INTO regular_users (user_id, cpf, phone) VALUES
    ('b0000000-0000-4000-b000-000000000001', '11122233344', '+55 48 99120-3344'),
    ('b0000000-0000-4000-b000-000000000002', '22233344455', '+55 48 99887-1122'),
    ('b0000000-0000-4000-b000-000000000003', '33344455566', '+55 51 98765-4321');

INSERT INTO company (id, company_name, cnpj_id, company_email, matricula) VALUES
    ('d0000000-0000-4000-d000-000000000001', 'Pousada Vista Mar LTDA', '12345678000199',
     'rh@vistamar.com.br', 2048);

INSERT INTO users_company (user_id, company_id, role) VALUES
    ('c0000000-0000-4000-c000-000000000001', 'd0000000-0000-4000-d000-000000000001', 'RECEPCAO'),
    ('c0000000-0000-4000-c000-000000000002', 'd0000000-0000-4000-d000-000000000001', 'GERENCIA');

-- -----------------------------------------------------------------------------
-- courses
-- Os contadores (lessons_count, materials_count, students_count, reviews_count,
-- rating_average) sao desnormalizados: os valores abaixo batem exatamente com as
-- linhas inseridas mais adiante neste script.
-- -----------------------------------------------------------------------------
INSERT INTO courses (
    id, title, description, short_description, thumbnail_url, teaser_video_url, status,
    created_by, instructor_id, category, level, duration_time, deadline, access_duration_days,
    price, learning_objectives, require_sequential_progress, enforce_deadline_block,
    major_version, minor_version, lessons_count, materials_count, students_count,
    reviews_count, rating_average, created_at, updated_at
) VALUES
    ('e0000000-0000-4000-e000-000000000001',
     'Atendimento de Excelencia em Hospedagem',
     'Da recepcao ao check-out: como conduzir a jornada do hospede com consistencia, resolver conflitos e transformar reclamacao em fidelizacao.',
     'A jornada do hospede, do check-in ao pos-estadia.',
     'https://cdn.escapa.com/courses/atendimento.jpg',
     'https://cdn.escapa.com/teasers/atendimento.mp4',
     'PUBLISHED',
     'a0000000-0000-4000-a000-000000000001', 'a0000000-0000-4000-a000-000000000001',
     'Hospitalidade', 'INICIANTE', 480, 60, 365, 249.90,
     '["Conduzir check-in e check-out sem atrito", "Aplicar escuta ativa em reclamacoes", "Padronizar a comunicacao da equipe de recepcao"]',
     TRUE, FALSE, 1, 2, 5, 2, 3, 2, 4.5,
     '2026-02-01 09:00:00', '2026-03-15 17:20:00'),

    ('e0000000-0000-4000-e000-000000000002',
     'Gestao de Reservas e Overbooking',
     'Como operar o motor de reservas, calcular no-show e decidir quando o overbooking compensa o risco.',
     'Motor de reservas, no-show e decisao de overbooking.',
     'https://cdn.escapa.com/courses/reservas.jpg',
     NULL,
     'PUBLISHED',
     'a0000000-0000-4000-a000-000000000002', 'a0000000-0000-4000-a000-000000000002',
     'Turismo', 'INTERMEDIARIO', 360, 45, 365, 329.90,
     '["Configurar politicas de cancelamento", "Estimar taxa de no-show", "Decidir limite seguro de overbooking"]',
     TRUE, TRUE, 2, 0, 2, 1, 2, 1, 5.0,
     '2026-02-14 10:30:00', '2026-04-02 11:00:00'),

    ('e0000000-0000-4000-e000-000000000003',
     'Ingles para Recepcao',
     'Vocabulario e situacoes praticas de atendimento em ingles para equipes de linha de frente.',
     'Ingles pratico para linha de frente.',
     NULL, NULL,
     'DRAFT',
     'a0000000-0000-4000-a000-000000000001', 'a0000000-0000-4000-a000-000000000001',
     'Idiomas', 'INICIANTE', 240, NULL, 180, 189.90,
     '["Receber o hospede em ingles", "Resolver pedidos comuns por telefone"]',
     FALSE, FALSE, 0, 1, 1, 0, 0, 0, NULL,
     '2026-04-10 15:45:00', '2026-04-10 15:45:00');

-- "Gestao de Reservas" so libera depois de "Atendimento de Excelencia"
INSERT INTO course_prerequisites (course_id, prerequisite_course_id) VALUES
    ('e0000000-0000-4000-e000-000000000002', 'e0000000-0000-4000-e000-000000000001');

INSERT INTO course_materials (id, course_id, title, file_url, file_type, file_size_bytes, "order", created_at) VALUES
    ('f1000000-0000-4000-f000-000000000001', 'e0000000-0000-4000-e000-000000000001',
     'Checklist de check-in', 'https://cdn.escapa.com/materials/checklist-checkin.pdf', 'PDF', 184320, 1,
     '2026-02-01 09:30:00'),
    ('f1000000-0000-4000-f000-000000000002', 'e0000000-0000-4000-e000-000000000001',
     'Roteiro de contorno de reclamacao', 'https://cdn.escapa.com/materials/roteiro-reclamacao.pdf', 'PDF', 96256, 2,
     '2026-02-01 09:32:00'),
    ('f1000000-0000-4000-f000-000000000003', 'e0000000-0000-4000-e000-000000000002',
     'Planilha de calculo de no-show', 'https://cdn.escapa.com/materials/no-show.xlsx', 'XLSX', 51200, 1,
     '2026-02-14 11:00:00');

INSERT INTO course_change_log (id, course_id, changed_by, description, major_version, minor_version, created_at) VALUES
    ('f2000000-0000-4000-f000-000000000001', 'e0000000-0000-4000-e000-000000000001',
     'a0000000-0000-4000-a000-000000000001', 'Publicacao inicial do curso.', 1, 0, '2026-02-01 09:00:00'),
    ('f2000000-0000-4000-f000-000000000002', 'e0000000-0000-4000-e000-000000000001',
     'a0000000-0000-4000-a000-000000000001', 'Reescrita do modulo de situacoes criticas apos feedback dos alunos.', 1, 2,
     '2026-03-15 17:20:00'),
    ('f2000000-0000-4000-f000-000000000003', 'e0000000-0000-4000-e000-000000000002',
     'a0000000-0000-4000-a000-000000000002', 'Inclusao da politica de cancelamento flexivel.', 2, 0,
     '2026-04-02 11:00:00');

-- -----------------------------------------------------------------------------
-- modules e content
-- -----------------------------------------------------------------------------
INSERT INTO modules (id, course_id, title, "order") VALUES
    ('01000000-0000-4000-9000-000000000001', 'e0000000-0000-4000-e000-000000000001', 'Fundamentos do Atendimento', 1),
    ('01000000-0000-4000-9000-000000000002', 'e0000000-0000-4000-e000-000000000001', 'Situacoes Criticas', 2),
    ('01000000-0000-4000-9000-000000000003', 'e0000000-0000-4000-e000-000000000002', 'Motor de Reservas', 1),
    ('01000000-0000-4000-9000-000000000004', 'e0000000-0000-4000-e000-000000000003', 'Primeiros Contatos', 1);

-- "Situacoes Criticas" exige "Fundamentos do Atendimento"
INSERT INTO module_prerequisites (module_id, prerequisite_module_id) VALUES
    ('01000000-0000-4000-9000-000000000002', '01000000-0000-4000-9000-000000000001');

INSERT INTO content (id, module_id, title, description, type, url, duration_minutes, is_free, "order", recursos, created_at) VALUES
    -- Fundamentos do Atendimento
    ('02000000-0000-4000-9000-000000000001', '01000000-0000-4000-9000-000000000001',
     'A jornada do hospede', 'Panorama das etapas de contato, do primeiro e-mail ao pos-estadia.',
     'VIDEO', 'https://cdn.escapa.com/lessons/jornada-hospede.mp4', 18, TRUE, 1,
     '{"legendas": ["pt-BR"], "transcricao": "https://cdn.escapa.com/lessons/jornada-hospede.txt"}',
     '2026-02-01 09:10:00'),
    ('02000000-0000-4000-9000-000000000002', '01000000-0000-4000-9000-000000000001',
     'Padroes de comunicacao', 'Tom de voz, saudacoes e o que nunca dizer ao hospede.',
     'TEXT', NULL, 12, FALSE, 2,
     '{"leitura_complementar": ["https://cdn.escapa.com/lessons/tom-de-voz.pdf"]}',
     '2026-02-01 09:12:00'),
    ('02000000-0000-4000-9000-000000000003', '01000000-0000-4000-9000-000000000001',
     'Check-in sem atrito', 'Roteiro pratico de recepcao em alta temporada.',
     'VIDEO', 'https://cdn.escapa.com/lessons/check-in.mp4', 22, FALSE, 3,
     NULL, '2026-02-01 09:15:00'),

    -- Situacoes Criticas
    ('02000000-0000-4000-9000-000000000004', '01000000-0000-4000-9000-000000000002',
     'Contornando uma reclamacao', 'Escuta ativa e recuperacao de servico na pratica.',
     'VIDEO', 'https://cdn.escapa.com/lessons/reclamacao.mp4', 26, FALSE, 1,
     '{"estudos_de_caso": 3}', '2026-02-01 09:20:00'),
    ('02000000-0000-4000-9000-000000000005', '01000000-0000-4000-9000-000000000002',
     'Modelo de carta de desculpas', 'Documento editavel para follow-up formal.',
     'FILE', 'https://cdn.escapa.com/lessons/carta-desculpas.docx', NULL, FALSE, 2,
     NULL, '2026-02-01 09:22:00'),

    -- Motor de Reservas
    ('02000000-0000-4000-9000-000000000006', '01000000-0000-4000-9000-000000000003',
     'Anatomia de um motor de reservas', 'Como as tarifas e a disponibilidade se conectam.',
     'VIDEO', 'https://cdn.escapa.com/lessons/motor-reservas.mp4', 30, TRUE, 1,
     '{"legendas": ["pt-BR", "en"]}', '2026-02-14 10:40:00'),
    ('02000000-0000-4000-9000-000000000007', '01000000-0000-4000-9000-000000000003',
     'Calculando o risco de overbooking', 'Passo a passo do calculo com dados historicos.',
     'TEXT', NULL, 20, FALSE, 2,
     '{"planilha": "https://cdn.escapa.com/materials/no-show.xlsx"}', '2026-02-14 10:45:00'),

    -- Primeiros Contatos (curso em rascunho)
    ('02000000-0000-4000-9000-000000000008', '01000000-0000-4000-9000-000000000004',
     'Greeting and small talk', 'Saudacoes e conversa breve na recepcao.',
     'VIDEO', 'https://cdn.escapa.com/lessons/greeting.mp4', 15, TRUE, 1,
     NULL, '2026-04-10 15:50:00');

-- -----------------------------------------------------------------------------
-- matriculas, licencas, avaliacoes e notificacoes
-- -----------------------------------------------------------------------------
INSERT INTO user_courses (
    user_id, course_id, dt_inicio, dt_expiracao, progress, conclusion_date, last_access_date,
    certificate_issued, certificate_code, certificate_issued_at
) VALUES
    -- concluiu e tem certificado
    ('b0000000-0000-4000-b000-000000000001', 'e0000000-0000-4000-e000-000000000001',
     '2026-02-10', '2027-02-10', 100, '2026-03-08', '2026-03-08 19:42:00',
     TRUE, 'ESC-2026-ATD-0001', '2026-03-08 19:45:00'),
    -- em andamento
    ('b0000000-0000-4000-b000-000000000002', 'e0000000-0000-4000-e000-000000000001',
     '2026-02-18', '2027-02-18', 45, NULL, '2026-04-20 21:05:00',
     FALSE, NULL, NULL),
    -- mal comecou
    ('c0000000-0000-4000-c000-000000000001', 'e0000000-0000-4000-e000-000000000001',
     '2026-03-01', '2027-03-01', 10, NULL, '2026-03-03 08:30:00',
     FALSE, NULL, NULL),
    -- concluiu o segundo curso
    ('b0000000-0000-4000-b000-000000000002', 'e0000000-0000-4000-e000-000000000002',
     '2026-03-05', '2027-03-05', 100, '2026-04-18', '2026-04-18 20:10:00',
     TRUE, 'ESC-2026-RES-0002', '2026-04-18 20:15:00'),
    -- matriculada mas ainda nao acessou
    ('c0000000-0000-4000-c000-000000000002', 'e0000000-0000-4000-e000-000000000002',
     '2026-04-01', '2027-04-01', 0, NULL, NULL,
     FALSE, NULL, NULL);

INSERT INTO company_courses (company_id, course_id, data_inicio, data_expiracao) VALUES
    ('d0000000-0000-4000-d000-000000000001', 'e0000000-0000-4000-e000-000000000001', '2026-02-25', '2027-02-25'),
    ('d0000000-0000-4000-d000-000000000001', 'e0000000-0000-4000-e000-000000000002', '2026-03-30', '2027-03-30');

INSERT INTO course_reviews (id, course_id, user_id, rating, comment, created_at, updated_at) VALUES
    ('03000000-0000-4000-9000-000000000001', 'e0000000-0000-4000-e000-000000000001',
     'b0000000-0000-4000-b000-000000000001', 5,
     'O roteiro de contorno de reclamacao ja me salvou duas vezes no balcao.',
     '2026-03-09 10:00:00', '2026-03-09 10:00:00'),
    ('03000000-0000-4000-9000-000000000002', 'e0000000-0000-4000-e000-000000000001',
     'b0000000-0000-4000-b000-000000000002', 4,
     'Muito bom, mas senti falta de exemplos de hostel.',
     '2026-04-21 09:15:00', '2026-04-22 14:00:00'),
    ('03000000-0000-4000-9000-000000000003', 'e0000000-0000-4000-e000-000000000002',
     'b0000000-0000-4000-b000-000000000002', 5,
     'A planilha de no-show virou ferramenta fixa da minha rotina.',
     '2026-04-19 08:40:00', '2026-04-19 08:40:00');

INSERT INTO notifications (id, user_id, type, title, message, course_id, is_read, read_at, created_at) VALUES
    ('04000000-0000-4000-9000-000000000001', 'b0000000-0000-4000-b000-000000000001',
     'CERTIFICATE_ISSUED', 'Seu certificado esta pronto',
     'O certificado de "Atendimento de Excelencia em Hospedagem" ja pode ser baixado.',
     'e0000000-0000-4000-e000-000000000001', TRUE, '2026-03-08 20:00:00', '2026-03-08 19:45:00'),
    ('04000000-0000-4000-9000-000000000002', 'b0000000-0000-4000-b000-000000000002',
     'COURSE_UPDATED', 'Um curso que voce faz foi atualizado',
     'O modulo "Situacoes Criticas" foi reescrito com novos estudos de caso.',
     'e0000000-0000-4000-e000-000000000001', FALSE, NULL, '2026-03-15 17:25:00'),
    ('04000000-0000-4000-9000-000000000003', 'c0000000-0000-4000-c000-000000000001',
     'COURSE_EXPIRING', 'Seu acesso expira em breve',
     'Faltam 30 dias para o fim do seu acesso a "Atendimento de Excelencia em Hospedagem".',
     'e0000000-0000-4000-e000-000000000001', FALSE, NULL, '2026-04-25 07:00:00'),
    ('04000000-0000-4000-9000-000000000004', 'c0000000-0000-4000-c000-000000000002',
     'COURSE_PUBLISHED', 'Novo curso disponivel para voce',
     'A Pousada Vista Mar liberou "Gestao de Reservas e Overbooking" para a sua equipe.',
     'e0000000-0000-4000-e000-000000000002', FALSE, NULL, '2026-03-30 09:00:00'),
    ('04000000-0000-4000-9000-000000000005', 'b0000000-0000-4000-b000-000000000001',
     'GENERAL', 'Bem-vinda a Escapa!',
     'Complete seu perfil para receber recomendacoes de cursos.',
     NULL, TRUE, '2026-02-03 11:00:00', '2026-02-03 10:20:00');

COMMIT;
