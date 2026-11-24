--liquibase formatted sql

INSERT INTO clients (name, email)
SELECT 'Client ' || i, 'client' || i || '@bank.ru'
FROM generate_series(1, 1000) AS i;

INSERT INTO accounts (client_id, account_number, balance)
SELECT i, 'ACC' || i, round((random() * 100000)::numeric, 2)
FROM generate_series(1, 1000) AS i;

INSERT INTO transactions (account_id, amount, type, transaction_date)
SELECT (random() * 999 + 1)::bigint,
       round((random() * 10000)::numeric, 2),
       'DEPOSIT',
       now() - random() * interval '365 days'
FROM generate_series(1, 100000);

ANALYZE transactions;
