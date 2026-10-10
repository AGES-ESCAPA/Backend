-- Pedidos de compra de cursos e de assentos (US-26 / US-27 / US-28).
--
-- Nenhuma coluna guarda dados de cartão (número, validade ou CVV): o pedido
-- registra só a forma de pagamento e as parcelas.
--
-- payment_method aceita CARD, PIX e BOLETO mesmo que só o cartão exista no fluxo
-- atual, para não exigir nova migration quando houver um meio de pagamento real.
-- Quem recusa os valores ainda não usados é a API de criação de pedido, não o banco.

-- Total de assentos corporativos comprados pela empresa. A coluna matricula do
-- seed (ex.: 2048) parece uma identificação, não uma quantidade de assentos.
ALTER TABLE company ADD COLUMN seats_total INTEGER NOT NULL DEFAULT 0;

ALTER TABLE company ADD CONSTRAINT ck_company_seats_total CHECK (seats_total >= 0);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    buyer_user_id UUID NOT NULL,
    company_id UUID,
    type VARCHAR(255) NOT NULL,
    item_type VARCHAR(255) NOT NULL,
    course_id UUID,
    seats_quantity INTEGER,
    amount NUMERIC(10, 2) NOT NULL,
    payment_method VARCHAR(255) NOT NULL,
    installments INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Pedido é registro financeiro: sem ON DELETE CASCADE, apagar usuário, empresa
    -- ou curso com pedidos é recusado em vez de apagar o histórico de compras.
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_company FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_orders_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT ck_orders_type CHECK (type IN ('INDIVIDUAL', 'CORPORATE')),
    CONSTRAINT ck_orders_item_type CHECK (item_type IN ('COURSE', 'SEATS')),
    CONSTRAINT ck_orders_payment_method CHECK (payment_method IN ('CARD', 'PIX', 'BOLETO')),
    CONSTRAINT ck_orders_status CHECK (status IN ('APPROVED', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT ck_orders_amount CHECK (amount >= 0),
    CONSTRAINT ck_orders_installments CHECK (installments >= 1),
    -- Compra de curso tem course_id e não tem quantidade de assentos.
    -- Compra de assentos tem seats_quantity > 0 e company_id, e não tem course_id.
    CONSTRAINT ck_orders_item CHECK (
        (item_type = 'COURSE' AND course_id IS NOT NULL AND seats_quantity IS NULL)
        OR (item_type = 'SEATS' AND course_id IS NULL AND seats_quantity IS NOT NULL AND seats_quantity > 0
            AND company_id IS NOT NULL)
    ),
    -- company_id só existe na compra corporativa (e é obrigatório nela).
    CONSTRAINT ck_orders_company CHECK (
        (type = 'CORPORATE' AND company_id IS NOT NULL)
        OR (type = 'INDIVIDUAL' AND company_id IS NULL)
    )
);

CREATE INDEX idx_orders_buyer_user ON orders (buyer_user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);

-- Histórico de mudanças de status do pedido (detalhe do pedido na US-28).
CREATE TABLE order_status_history (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    status VARCHAR(255) NOT NULL,
    changed_by UUID,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_order_status_history_status CHECK (status IN ('APPROVED', 'CANCELLED', 'REFUNDED'))
);

CREATE INDEX idx_order_status_history_order ON order_status_history (order_id, created_at);
