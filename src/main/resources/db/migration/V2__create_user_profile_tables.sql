ALTER TABLE users ADD COLUMN status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE users ADD CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE'));

CREATE TABLE admins (
    user_id VARCHAR(255) PRIMARY KEY,
    department VARCHAR(255),
    headline VARCHAR(255),
    bio TEXT,
    avatar_url VARCHAR(255),
    CONSTRAINT fk_admins_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE company (
    id VARCHAR(255) PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    cnpj_id VARCHAR(255) UNIQUE,
    company_email VARCHAR(255),
    matricula INTEGER,
    CONSTRAINT fk_company_user FOREIGN KEY (id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE employees (
    user_id VARCHAR(255) PRIMARY KEY,
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE regular_users (
    user_id VARCHAR(255) PRIMARY KEY,
    cpf VARCHAR(255) UNIQUE,
    phone VARCHAR(255),
    CONSTRAINT fk_regular_users_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE users_company (
    user_id VARCHAR(255) NOT NULL,
    company_id VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, company_id),
    CONSTRAINT fk_users_company_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_users_company_company FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE CASCADE
);

CREATE INDEX idx_users_company_company ON users_company (company_id);
