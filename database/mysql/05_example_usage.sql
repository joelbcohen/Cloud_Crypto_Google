-- ============================================================================
-- Example Usage - Ultralight Crypto Ledger
-- ============================================================================
-- This file demonstrates common operations with the crypto ledger
-- Run this after setting up the schema to test functionality
-- ============================================================================

USE crypto_ledger;

-- ============================================================================
-- EXAMPLE 1: Register Accounts
-- ============================================================================

-- Register Alice with initial balance of 10,000 tokens
CALL register_account('alice', 10000.0, @alice_id, @success, @message);
SELECT 'Register Alice:' as Operation, @alice_id as AccountID, @success as Success, @message as Message;

-- Register Bob with initial balance of 5,000 tokens
CALL register_account('bob', 5000.0, @bob_id, @success, @message);
SELECT 'Register Bob:' as Operation, @bob_id as AccountID, @success as Success, @message as Message;

-- Register Charlie with zero initial balance
CALL register_account('charlie', 0, @charlie_id, @success, @message);
SELECT 'Register Charlie:' as Operation, @charlie_id as AccountID, @success as Success, @message as Message;

-- Try to register Alice again (should fail - duplicate)
CALL register_account('alice', 1000.0, @alice_id2, @success, @message);
SELECT 'Register Alice Again:' as Operation, @alice_id2 as AccountID, @success as Success, @message as Message;

-- ============================================================================
-- EXAMPLE 2: Check Initial Balances
-- ============================================================================

SELECT 'Initial Balances:' as Report;
SELECT address, balance FROM accounts WHERE address IN ('alice', 'bob', 'charlie');

-- ============================================================================
-- EXAMPLE 3: Transfer Tokens
-- ============================================================================

-- Alice sends 1,500 tokens to Bob
CALL transfer_tokens('alice', 'bob', 1500.0, 'Payment for services', @tx1, @success, @message);
SELECT 'Alice -> Bob:' as Operation, @tx1 as TransactionID, @success as Success, @message as Message;

-- Bob sends 500 tokens to Charlie
CALL transfer_tokens('bob', 'charlie', 500.0, 'Gift', @tx2, @success, @message);
SELECT 'Bob -> Charlie:' as Operation, @tx2 as TransactionID, @success as Success, @message as Message;

-- Try to transfer more than balance (should fail)
CALL transfer_tokens('charlie', 'alice', 1000.0, 'Insufficient funds test', @tx3, @success, @message);
SELECT 'Charlie -> Alice (fail):' as Operation, @tx3 as TransactionID, @success as Success, @message as Message;

-- ============================================================================
-- EXAMPLE 4: Check Balances After Transfers
-- ============================================================================

SELECT 'Balances After Transfers:' as Report;
SELECT address, balance FROM accounts WHERE address IN ('alice', 'bob', 'charlie');

-- ============================================================================
-- EXAMPLE 5: Mint Additional Tokens
-- ============================================================================

-- Mint 2,000 tokens to Charlie
CALL mint_tokens('charlie', 2000.0, 'Token distribution', @tx4, @success, @message);
SELECT 'Mint to Charlie:' as Operation, @tx4 as TransactionID, @success as Success, @message as Message;

SELECT 'Balance After Mint:' as Report;
CALL get_balance('charlie', @balance, @success, @message);
SELECT @balance as CharlieBalance;

-- ============================================================================
-- EXAMPLE 6: Burn Tokens
-- ============================================================================

-- Alice burns 1,000 tokens
CALL burn_tokens('alice', 1000.0, 'Deflationary burn', @tx5, @success, @message);
SELECT 'Burn from Alice:' as Operation, @tx5 as TransactionID, @success as Success, @message as Message;

-- Bob burns 200 tokens
CALL burn_tokens('bob', 200.0, 'Buyback and burn', @tx6, @success, @message);
SELECT 'Burn from Bob:' as Operation, @tx6 as TransactionID, @success as Success, @message as Message;

-- ============================================================================
-- EXAMPLE 7: Final Balances
-- ============================================================================

SELECT 'Final Balances:' as Report;
SELECT address, balance, created_at, updated_at
FROM accounts
WHERE address IN ('alice', 'bob', 'charlie')
ORDER BY balance DESC;

-- ============================================================================
-- EXAMPLE 8: View Transaction History
-- ============================================================================

SELECT 'Transaction History:' as Report;
SELECT
    tx_hash,
    tx_type,
    from_address,
    to_address,
    amount,
    status,
    memo,
    created_at
FROM transaction_history
ORDER BY created_at ASC;

-- ============================================================================
-- EXAMPLE 9: View Account Summaries
-- ============================================================================

SELECT 'Account Summaries:' as Report;
SELECT
    address,
    balance,
    total_sent_transactions,
    total_received_transactions,
    total_sent_amount,
    total_received_amount
FROM account_summary
WHERE address IN ('alice', 'bob', 'charlie')
ORDER BY balance DESC;

-- ============================================================================
-- EXAMPLE 10: View Ledger Statistics
-- ============================================================================

SELECT 'Ledger Statistics:' as Report;
SELECT * FROM ledger_stats;

-- ============================================================================
-- EXAMPLE 11: View Audit Log for Alice
-- ============================================================================

SELECT 'Audit Log for Alice:' as Report;
SELECT
    tl.id,
    a.address,
    tl.previous_balance,
    tl.new_balance,
    tl.change_amount,
    t.tx_type,
    t.memo,
    tl.created_at
FROM transaction_log tl
JOIN accounts a ON tl.account_id = a.id
LEFT JOIN transactions t ON tl.transaction_id = t.id
WHERE a.address = 'alice'
ORDER BY tl.created_at ASC;

-- ============================================================================
-- EXAMPLE 12: Find All Transfers Between Alice and Bob
-- ============================================================================

SELECT 'Transfers Between Alice and Bob:' as Report;
SELECT
    tx_hash,
    from_address,
    to_address,
    amount,
    memo,
    created_at
FROM transaction_history
WHERE
    (from_address = 'alice' AND to_address = 'bob')
    OR (from_address = 'bob' AND to_address = 'alice')
ORDER BY created_at ASC;

-- ============================================================================
-- EXAMPLE 13: Calculate Net Flow for Each Account
-- ============================================================================

SELECT 'Net Flow by Account:' as Report;
SELECT
    a.address,
    a.balance as current_balance,
    COALESCE(SUM(CASE WHEN t.tx_type = 'mint' AND t.to_account_id = a.id THEN t.amount ELSE 0 END), 0) as total_minted,
    COALESCE(SUM(CASE WHEN t.tx_type = 'transfer' AND t.to_account_id = a.id THEN t.amount ELSE 0 END), 0) as total_received,
    COALESCE(SUM(CASE WHEN t.tx_type = 'transfer' AND t.from_account_id = a.id THEN t.amount ELSE 0 END), 0) as total_sent,
    COALESCE(SUM(CASE WHEN t.tx_type = 'burn' AND t.from_account_id = a.id THEN t.amount ELSE 0 END), 0) as total_burned
FROM accounts a
LEFT JOIN transactions t ON (a.id = t.from_account_id OR a.id = t.to_account_id) AND t.status = 'completed'
WHERE a.address IN ('alice', 'bob', 'charlie')
GROUP BY a.id, a.address, a.balance
ORDER BY a.balance DESC;

-- ============================================================================
-- Summary
-- ============================================================================

SELECT '=== EXAMPLE COMPLETE ===' as Status;
SELECT 'All operations completed successfully!' as Message;
SELECT 'Check the results above to verify ledger functionality' as Note;
