
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    account_number VARCHAR(34) UNIQUE NOT NULL,
    balance NUMERIC(19,4) NOT NULL DEFAULT 0,
    FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    type VARCHAR(20) NOT NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT now(),
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);
