-- ============================================================================
-- Example Usage - Ultralight Crypto Ledger
-- ============================================================================
-- This file demonstrates common operations with the crypto ledger
-- Run this after setting up the schema to test functionality
-- ============================================================================

USE jcohen_ccrypto;

-- ============================================================================
-- EXAMPLE 1: Register Accounts with Device Information
-- ============================================================================

-- Register Device 1001 (Alice's device) with initial balance of 10,000 tokens
CALL register_account(
    '1001',                               -- id
    10000.0,                              -- initial_balance
    'SN-ALICE-12345',                     -- serial_number
    'attestation_blob_data_alice',        -- attestation_blob
    'public_key_alice_xyz',               -- public_key
    'Pixel 8 Pro',                        -- model
    'Google',                             -- brand
    'Android 14',                         -- os_version
    'node_001',                           -- node_id
    'fcm_token_alice_abc123',             -- fcm_token
    @account_id_1001, @success, @message
);
SELECT 'Register Device 1001:' as Operation, @account_id_1001 as AccountID, @success as Success, @message as Message;

-- Register Device 1002 (Bob's device) with initial balance of 5,000 tokens
CALL register_account(
    '1002',                               -- id
    5000.0,                               -- initial_balance
    'SN-BOB-67890',                       -- serial_number
    'attestation_blob_data_bob',          -- attestation_blob
    'public_key_bob_xyz',                 -- public_key
    'iPhone 15 Pro',                      -- model
    'Apple',                              -- brand
    'iOS 17.2',                           -- os_version
    'node_002',                           -- node_id
    'fcm_token_bob_def456',               -- fcm_token
    @account_id_1002, @success, @message
);
SELECT 'Register Device 1002:' as Operation, @account_id_1002 as AccountID, @success as Success, @message as Message;

-- Register Device 1003 (Charlie's device) with zero initial balance
CALL register_account(
    '1003',                               -- id
    0,                                    -- initial_balance
    'SN-CHARLIE-11111',                   -- serial_number
    'attestation_blob_data_charlie',      -- attestation_blob
    'public_key_charlie_xyz',             -- public_key
    'Galaxy S24 Ultra',                   -- model
    'Samsung',                            -- brand
    'Android 14',                         -- os_version
    'node_003',                           -- node_id
    'fcm_token_charlie_ghi789',           -- fcm_token
    @account_id_1003, @success, @message
);
SELECT 'Register Device 1003:' as Operation, @account_id_1003 as AccountID, @success as Success, @message as Message;

-- Try to register Device 1001 again (should fail - duplicate id)
CALL register_account(
    '1001', 1000.0, 'SN-ALICE-99999', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    @account_id_dup, @success, @message
);
SELECT 'Register Device 1001 Again:' as Operation, @account_id_dup as AccountID, @success as Success, @message as Message;

-- ============================================================================
-- EXAMPLE 2: Check Initial Balances and Device Info
-- ============================================================================

SELECT 'Initial Balances and Device Info:' as Report;
SELECT id, balance, serial_number, serial_hash, model, brand, os_version, node_id
FROM accounts WHERE id IN (1001, 1002, 1003);

-- Verify SHA256 hash is correctly computed
SELECT 'Verify Serial Hash:' as Report;
SELECT
    id,
    serial_number,
    serial_hash as stored_hash,
    SHA2(serial_number, 256) as computed_hash,
    (serial_hash = SHA2(serial_number, 256)) as hash_matches
FROM accounts
WHERE id IN (1001, 1002, 1003) AND serial_number IS NOT NULL;

-- ============================================================================
-- EXAMPLE 3: Transfer Tokens
-- ============================================================================

-- Device 1001 sends 1,500 tokens to Device 1002
CALL transfer_tokens('1001', '1002', 1500.0, 'Payment for services', @tx1, @success, @message);
SELECT 'Device 1001 -> Device 1002:' as Operation, @tx1 as TransactionID, @success as Success, @message as Message;

-- Device 1002 sends 500 tokens to Device 1003
CALL transfer_tokens('1002', '1003', 500.0, 'Gift', @tx2, @success, @message);
SELECT 'Device 1002 -> Device 1003:' as Operation, @tx2 as TransactionID, @success as Success, @message as Message;

-- Try to transfer more than balance (should fail)
CALL transfer_tokens('1003', '1001', 1000.0, 'Insufficient funds test', @tx3, @success, @message);
SELECT 'Device 1003 -> Device 1001 (fail):' as Operation, @tx3 as TransactionID, @success as Success, @message as Message;

-- ============================================================================
-- EXAMPLE 4: Check Balances After Transfers
-- ============================================================================

SELECT 'Balances After Transfers:' as Report;
SELECT id, balance FROM accounts WHERE id IN (1001, 1002, 1003);

-- ============================================================================
-- EXAMPLE 5: Mint Additional Tokens
-- ============================================================================

-- Mint 2,000 tokens to Device 1003
CALL mint_tokens('1003', 2000.0, 'Token distribution', @tx4, @success, @message);
SELECT 'Mint to Device 1003:' as Operation, @tx4 as TransactionID, @success as Success, @message as Message;

SELECT 'Balance After Mint:' as Report;
CALL get_balance('1003', @balance, @success, @message);
SELECT @balance as Device1003Balance;

-- ============================================================================
-- EXAMPLE 6: Burn Tokens
-- ============================================================================

-- Device 1001 burns 1,000 tokens
CALL burn_tokens('1001', 1000.0, 'Deflationary burn', @tx5, @success, @message);
SELECT 'Burn from Device 1001:' as Operation, @tx5 as TransactionID, @success as Success, @message as Message;

-- Device 1002 burns 200 tokens
CALL burn_tokens('1002', 200.0, 'Buyback and burn', @tx6, @success, @message);
SELECT 'Burn from Device 1002:' as Operation, @tx6 as TransactionID, @success as Success, @message as Message;

-- ============================================================================
-- EXAMPLE 7: Update Device Information
-- ============================================================================

-- Device 1001 updates FCM token and node
CALL update_device_info(
    '1001',                               -- id
    'fcm_token_alice_new_xyz999',         -- fcm_token (updated)
    'Android 14.1',                       -- os_version (updated)
    'node_004',                           -- node_id (updated)
    @success, @message
);
SELECT 'Update Device 1001:' as Operation, @success as Success, @message as Message;

-- View Device 1001's updated info
SELECT 'Device 1001 Updated Info:' as Report;
SELECT id, model, brand, os_version, node_id, fcm_token
FROM accounts WHERE id = 1001;

-- ============================================================================
-- EXAMPLE 8: View Device Statistics
-- ============================================================================

SELECT 'Device Statistics:' as Report;
SELECT * FROM device_stats;

-- ============================================================================
-- EXAMPLE 9: Final Balances
-- ============================================================================

SELECT 'Final Balances:' as Report;
SELECT id, balance, serial_number, created_at, updated_at
FROM accounts
WHERE id IN (1001, 1002, 1003)
ORDER BY balance DESC;

-- ============================================================================
-- EXAMPLE 10: View Transaction History
-- ============================================================================

SELECT 'Transaction History:' as Report;
SELECT
    tx_hash,
    tx_type,
    from_id,
    to_id,
    amount,
    status,
    memo,
    created_at
FROM transaction_history
ORDER BY created_at ASC;

-- ============================================================================
-- EXAMPLE 11: View Account Summaries
-- ============================================================================

SELECT 'Account Summaries:' as Report;
SELECT
    id,
    balance,
    model,
    brand,
    total_sent_transactions,
    total_received_transactions,
    total_sent_amount,
    total_received_amount
FROM account_summary
WHERE id IN (1001, 1002, 1003)
ORDER BY balance DESC;

-- ============================================================================
-- EXAMPLE 12: View Ledger Statistics
-- ============================================================================

SELECT 'Ledger Statistics:' as Report;
SELECT * FROM ledger_stats;

-- ============================================================================
-- EXAMPLE 13: View Audit Log for Device 1001
-- ============================================================================

SELECT 'Audit Log for Device 1001:' as Report;
SELECT
    tl.id,
    a.id as account_id,
    tl.previous_balance,
    tl.new_balance,
    tl.change_amount,
    t.tx_type,
    t.memo,
    tl.created_at
FROM transaction_log tl
JOIN accounts a ON tl.account_id = a.id
LEFT JOIN transactions t ON tl.transaction_id = t.id
WHERE a.id = 1001
ORDER BY tl.created_at ASC;

-- ============================================================================
-- EXAMPLE 14: Find All Transfers Between Device 1001 and Device 1002
-- ============================================================================

SELECT 'Transfers Between Device 1001 and Device 1002:' as Report;
SELECT
    tx_hash,
    from_id,
    to_id,
    amount,
    memo,
    created_at
FROM transaction_history
WHERE
    (from_id = 1001 AND to_id = 1002)
    OR (from_id = 1002 AND to_id = 1001)
ORDER BY created_at ASC;

-- ============================================================================
-- EXAMPLE 15: Calculate Net Flow for Each Account
-- ============================================================================

SELECT 'Net Flow by Account:' as Report;
SELECT
    a.id,
    a.balance as current_balance,
    COALESCE(SUM(CASE WHEN t.tx_type = 'mint' AND t.to_account_id = a.id THEN t.amount ELSE 0 END), 0) as total_minted,
    COALESCE(SUM(CASE WHEN t.tx_type = 'transfer' AND t.to_account_id = a.id THEN t.amount ELSE 0 END), 0) as total_received,
    COALESCE(SUM(CASE WHEN t.tx_type = 'transfer' AND t.from_account_id = a.id THEN t.amount ELSE 0 END), 0) as total_sent,
    COALESCE(SUM(CASE WHEN t.tx_type = 'burn' AND t.from_account_id = a.id THEN t.amount ELSE 0 END), 0) as total_burned
FROM accounts a
LEFT JOIN transactions t ON (a.id = t.from_account_id OR a.id = t.to_account_id) AND t.status = 'completed'
WHERE a.id IN (1001, 1002, 1003)
GROUP BY a.id, a.balance
ORDER BY a.balance DESC;

-- ============================================================================
-- EXAMPLE 16: Get All Device Serial Numbers
-- ============================================================================

SELECT 'All Registered Devices:' as Report;
SELECT 
    id,
    serial_number,
    model,
    brand,
    balance,
    created_at
FROM accounts
WHERE id IN (1001, 1002, 1003)
ORDER BY id;

-- ============================================================================
-- Summary
-- ============================================================================

SELECT '=== EXAMPLE COMPLETE ===' as Status;
SELECT 'All operations completed successfully!' as Message;
SELECT 'Check the results above to verify ledger functionality' as Note;
SELECT 'Note: Devices are identified by numeric IDs (1001, 1002, 1003) instead of text addresses' as Important;
