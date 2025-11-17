-- ============================================================================
-- Views and Helper Queries
-- ============================================================================

USE jcohen_ccrypto;

-- ============================================================================
-- View: account_summary
-- Provides a summary of all accounts with their balances and device info
-- ============================================================================
CREATE OR REPLACE VIEW account_summary AS
SELECT
    a.id,
    a.balance,
    a.serial_number,
    a.serial_hash,
    a.model,
    a.brand,
    a.os_version,
    a.node_id,
    COUNT(DISTINCT t1.id) as total_sent_transactions,
    COUNT(DISTINCT t2.id) as total_received_transactions,
    COALESCE(SUM(DISTINCT t1.amount), 0) as total_sent_amount,
    COALESCE(SUM(DISTINCT t2.amount), 0) as total_received_amount,
    a.created_at as account_created_at,
    a.updated_at as last_activity
FROM accounts a
LEFT JOIN transactions t1 ON a.id = t1.from_account_id AND t1.status = 'completed'
LEFT JOIN transactions t2 ON a.id = t2.to_account_id AND t2.status = 'completed'
GROUP BY a.id, a.balance, a.serial_number, a.serial_hash, a.model, a.brand, a.os_version,
         a.node_id, a.created_at, a.updated_at;

-- ============================================================================
-- View: transaction_history
-- Provides a readable transaction history
-- ============================================================================
CREATE OR REPLACE VIEW transaction_history AS
SELECT
    t.id,
    t.tx_hash,
    t.tx_type,
    from_acc.id as from_id,
    to_acc.id as to_id,
    t.amount,
    t.status,
    t.memo,
    t.created_at,
    t.completed_at
FROM transactions t
LEFT JOIN accounts from_acc ON t.from_account_id = from_acc.id
LEFT JOIN accounts to_acc ON t.to_account_id = to_acc.id
ORDER BY t.created_at DESC;

-- ============================================================================
-- View: ledger_stats
-- Provides overall ledger statistics
-- ============================================================================
CREATE OR REPLACE VIEW ledger_stats AS
SELECT
    (SELECT COUNT(*) FROM accounts) as total_accounts,
    (SELECT COUNT(*) FROM transactions WHERE status = 'completed') as total_transactions,
    (SELECT COUNT(*) FROM transactions WHERE tx_type = 'mint') as total_mints,
    (SELECT COUNT(*) FROM transactions WHERE tx_type = 'transfer') as total_transfers,
    (SELECT COUNT(*) FROM transactions WHERE tx_type = 'burn') as total_burns,
    (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE tx_type = 'mint' AND status = 'completed') as total_minted,
    (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE tx_type = 'burn' AND status = 'completed') as total_burned,
    (SELECT COALESCE(SUM(balance), 0) FROM accounts) as total_balance,
    (SELECT config_value FROM ledger_config WHERE config_key = 'total_supply') as total_supply,
    (SELECT config_value FROM ledger_config WHERE config_key = 'max_supply') as max_supply;

-- ============================================================================
-- View: recent_transactions
-- Shows the 100 most recent transactions
-- ============================================================================
CREATE OR REPLACE VIEW recent_transactions AS
SELECT
    t.tx_hash,
    t.tx_type,
    from_acc.id as from_id,
    to_acc.id as to_id,
    t.amount,
    t.status,
    t.created_at
FROM transactions t
LEFT JOIN accounts from_acc ON t.from_account_id = from_acc.id
LEFT JOIN accounts to_acc ON t.to_account_id = to_acc.id
ORDER BY t.created_at DESC
LIMIT 100;

-- ============================================================================
-- View: top_balances
-- Shows accounts with highest balances
-- ============================================================================
CREATE OR REPLACE VIEW top_balances AS
SELECT
    id,
    balance,
    model,
    brand,
    created_at
FROM accounts
WHERE id != 'SYSTEM'
ORDER BY balance DESC
LIMIT 100;

-- ============================================================================
-- View: device_stats
-- Shows device statistics grouped by model and brand
-- ============================================================================
CREATE OR REPLACE VIEW device_stats AS
SELECT
    brand,
    model,
    os_version,
    COUNT(*) as device_count,
    SUM(balance) as total_balance,
    AVG(balance) as avg_balance
FROM accounts
WHERE brand IS NOT NULL AND model IS NOT NULL
GROUP BY brand, model, os_version
ORDER BY device_count DESC;
