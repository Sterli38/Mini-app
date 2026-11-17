CREATE INDEX idx_transactions_account_id_transaction_date
    ON transactions (account_id, transaction_date);
