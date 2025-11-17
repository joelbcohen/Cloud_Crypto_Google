-- ============================================================================
-- Reset Database to Initial State
-- ============================================================================
-- This script purges all accounts, transactions, and logs from the database
-- while preserving the schema structure and resetting configuration.
--
-- WARNING: This will DELETE ALL DATA. Use with extreme caution!
-- ============================================================================

USE jcohen_ccrypto;

-- ============================================================================
-- Disable Foreign Key Checks (temporarily for deletion)
-- ============================================================================
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- Purge All Transaction Data
-- ============================================================================

-- Delete all transaction logs
DELETE FROM transaction_log;
SELECT 'Transaction logs purged' as Status;

-- Delete all transactions
DELETE FROM transactions;
SELECT 'All transactions deleted' as Status;

-- Delete all accounts
DELETE FROM accounts;
SELECT 'All accounts deleted' as Status;

-- ============================================================================
-- Reset Auto-Increment Counters
-- ============================================================================

ALTER TABLE transaction_log AUTO_INCREMENT = 1;
ALTER TABLE transactions AUTO_INCREMENT = 1;
ALTER TABLE accounts AUTO_INCREMENT = 1;
SELECT 'Auto-increment counters reset' as Status;

-- ============================================================================
-- Reset Ledger Configuration to Defaults
-- ============================================================================

-- Reset total supply to zero
UPDATE ledger_config SET config_value = '0' WHERE config_key = 'total_supply';

-- Reset other config values if needed
UPDATE ledger_config SET config_value = 'Ultralight Crypto Ledger' WHERE config_key = 'ledger_name';
UPDATE ledger_config SET config_value = 'UCL' WHERE config_key = 'token_symbol';
UPDATE ledger_config SET config_value = '18' WHERE config_key = 'token_decimals';
UPDATE ledger_config SET config_value = '1000000000' WHERE config_key = 'max_supply';
UPDATE ledger_config SET config_value = '1.0.0' WHERE config_key = 'ledger_version';

SELECT 'Ledger configuration reset to defaults' as Status;

-- ============================================================================
-- Recreate System Account (Optional)
-- ============================================================================

-- Recreate the SYSTEM account for minting/burning operations
INSERT INTO accounts (balance) VALUES (0.000000000000000000);
SELECT 'System account recreated' as Status;

-- ============================================================================
-- Re-enable Foreign Key Checks
-- ============================================================================
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- Verification: Show Current Database State
-- ============================================================================

SELECT 'Database Reset Complete!' as Status;

SELECT 'Account Count:' as Metric, COUNT(*) as Value FROM accounts
UNION ALL
SELECT 'Transaction Count:', COUNT(*) FROM transactions
UNION ALL
SELECT 'Transaction Log Count:', COUNT(*) FROM transaction_log
UNION ALL
SELECT 'Total Supply:', config_value FROM ledger_config WHERE config_key = 'total_supply';

-- ============================================================================
-- Display Remaining Accounts (should only be SYSTEM)
-- ============================================================================

SELECT 'Remaining Accounts:' as Report;
SELECT id, balance, created_at FROM accounts;

SELECT '=== DATABASE RESET COMPLETE ===' as Status;
SELECT 'The database has been reset to its initial state.' as Message;
SELECT 'All user accounts and transactions have been purged.' as Warning;
