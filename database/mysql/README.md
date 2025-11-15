# Ultralight Centralized Crypto Ledger - MySQL

A simple, centralized cryptocurrency ledger system with ERC20-like features built on MySQL. This ledger supports account registration with device attestation, token transfers, and token burning in a single-node environment.

## Database Name

**jcohen_ccrypto**

## Features

- **Account Registration**: Create new accounts with device information and optional initial token balance
- **Device Attestation**: Store device serial numbers, attestation blobs, and public keys
- **Device Tracking**: Track device model, brand, OS version, GPS location, and FCM tokens
- **Token Transfers**: Transfer tokens between accounts with atomicity guarantees
- **Token Burning**: Permanently destroy tokens from circulation
- **Token Minting**: Create new tokens (with max supply limits)
- **Balance Tracking**: Accurate balance tracking with audit logs
- **Transaction History**: Complete transaction history with status tracking
- **Device Analytics**: View statistics grouped by device model and brand

## Database Schema

### Tables

1. **accounts** - Stores account addresses, balances, and device information including:
   - Serial number
   - Serial hash (SHA256 hash of serial number, automatically computed)
   - Attestation blob
   - Public key
   - Device model, brand, OS version
   - GPS latitude/longitude
   - FCM token for push notifications

2. **transactions** - Records all token transactions (mints, transfers, burns)
3. **ledger_config** - Global ledger configuration (token symbol, supply, etc.)
4. **transaction_log** - Audit log for all balance changes

### Views

- **account_summary** - Summary of accounts with transaction statistics and device info
- **transaction_history** - Readable transaction history
- **ledger_stats** - Overall ledger statistics
- **recent_transactions** - 100 most recent transactions
- **top_balances** - Accounts with highest balances
- **device_stats** - Device statistics grouped by model and brand

## Installation

### Prerequisites

- MySQL 8.0 or higher
- MySQL client or any database management tool

### Setup

Run the SQL files in order:

```bash
mysql -u root -p < 01_schema.sql
mysql -u root -p < 02_initial_data.sql
mysql -u root -p < 03_procedures.sql
mysql -u root -p < 04_views.sql
```

Or in one command:

```bash
cat 01_schema.sql 02_initial_data.sql 03_procedures.sql 04_views.sql | mysql -u root -p
```

### Optional Files

- **05_example_usage.sql** - Complete examples demonstrating all features
- **06_reset_database.sql** - Reset database to initial state (purges all data)

## Usage

### Register a New Account

```sql
-- Register account with device information and zero balance
CALL register_account(
    'user123',                    -- address
    0,                            -- initial_balance
    'SN-USER123-001',             -- serial_number
    'attestation_blob_hex',       -- attestation_blob
    'public_key_pem',             -- public_key
    'Pixel 8',                    -- model
    'Google',                     -- brand
    'Android 14',                 -- os_version
    37.7749,                      -- gps_latitude
    -122.4194,                    -- gps_longitude
    'fcm_token_xyz',              -- fcm_token
    @account_id, @success, @message
);
SELECT @account_id, @success, @message;

-- Register account with initial balance (mints tokens)
CALL register_account(
    'admin001',                   -- address
    1000000.0,                    -- initial_balance
    'SN-ADMIN-001',               -- serial_number
    'attestation_blob_admin',     -- attestation_blob
    'public_key_admin',           -- public_key
    'iPhone 15 Pro',              -- model
    'Apple',                      -- brand
    'iOS 17.2',                   -- os_version
    40.7128,                      -- gps_latitude
    -74.0060,                     -- gps_longitude
    'fcm_token_admin',            -- fcm_token
    @account_id, @success, @message
);
SELECT @account_id, @success, @message;

-- Register with NULL device fields (optional)
CALL register_account(
    'user456', 100.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    @account_id, @success, @message
);
```

### Transfer Tokens

```sql
CALL transfer_tokens(
    'user123',           -- from address
    'user456',           -- to address
    100.5,               -- amount
    'Payment for goods', -- memo (optional)
    @tx_id,
    @success,
    @message
);
SELECT @tx_id, @success, @message;
```

### Burn Tokens

```sql
CALL burn_tokens(
    'user123',           -- address
    50.0,                -- amount to burn
    'Token burn event',  -- memo (optional)
    @tx_id,
    @success,
    @message
);
SELECT @tx_id, @success, @message;
```

### Mint Tokens

```sql
CALL mint_tokens(
    'user123',           -- address
    1000.0,              -- amount to mint
    'Token creation',    -- memo (optional)
    @tx_id,
    @success,
    @message
);
SELECT @tx_id, @success, @message;
```

### Get Balance

```sql
CALL get_balance('user123', @balance, @success, @message);
SELECT @balance, @success, @message;

-- Or directly query
SELECT address, balance FROM accounts WHERE address = 'user123';
```

### Update Device Information

```sql
-- Update FCM token, location, and OS version
CALL update_device_info(
    'user123',                    -- address
    'new_fcm_token_xyz',          -- fcm_token
    34.0522,                      -- gps_latitude (new location)
    -118.2437,                    -- gps_longitude
    'Android 14.1',               -- os_version (updated)
    @success, @message
);
SELECT @success, @message;

-- Update only specific fields (pass NULL for others)
CALL update_device_info('user123', 'new_fcm_token', NULL, NULL, NULL, @success, @message);
```

### View Account and Device Information

```sql
-- View specific account with all device info
SELECT address, balance, serial_number, serial_hash, model, brand, os_version,
       gps_latitude, gps_longitude, fcm_token, created_at
FROM accounts WHERE address = 'user123';

-- Verify serial number hash
SELECT serial_number, serial_hash, SHA2(serial_number, 256) as computed_hash
FROM accounts WHERE address = 'user123';

-- View device statistics
SELECT * FROM device_stats;

-- View account summary with device info
SELECT * FROM account_summary WHERE address = 'user123';
```

### View Transaction History

```sql
-- All transactions
SELECT * FROM transaction_history;

-- Transactions for specific account
SELECT * FROM transaction_history
WHERE from_address = 'user123' OR to_address = 'user123'
ORDER BY created_at DESC;

-- Recent transactions only
SELECT * FROM recent_transactions;
```

### View Ledger Statistics

```sql
SELECT * FROM ledger_stats;
```

### View Top Balances

```sql
SELECT * FROM top_balances;
```

### View Account Summary

```sql
SELECT * FROM account_summary WHERE address = 'user123';
```

## Configuration

Modify ledger configuration:

```sql
-- Update token symbol
UPDATE ledger_config SET config_value = 'MYT' WHERE config_key = 'token_symbol';

-- Update max supply (0 = unlimited)
UPDATE ledger_config SET config_value = '1000000000' WHERE config_key = 'max_supply';

-- View current configuration
SELECT * FROM ledger_config;
```

## Security Features

- **Device Attestation**: Store attestation blobs and public keys for device verification
- **Serial Number Tracking**: Unique device serial numbers prevent duplicate registrations
- **SHA256 Hashing**: Automatic SHA256 hash computation of serial numbers for privacy and verification
- **Row-level locking** on balance updates to prevent race conditions
- **Transaction isolation** ensures atomic operations
- **Foreign key constraints** maintain referential integrity
- **Check constraints** validate transaction types and amounts
- **Audit logging** tracks all balance changes
- **GPS Tracking**: Location data for compliance and fraud detection

## Data Types

- **Balances**: DECIMAL(65, 18) - supports up to 18 decimal places (like ETH)
- **Addresses**: VARCHAR(255) - can store various address formats
- **Transaction Hash**: VARCHAR(66) - supports 0x-prefixed hex strings
- **GPS Coordinates**: DECIMAL(10, 8) for latitude, DECIMAL(11, 8) for longitude
- **Attestation/Public Key**: TEXT - supports large cryptographic data
- **Serial Number**: VARCHAR(255) - unique device identifier
- **Serial Hash**: CHAR(64) - SHA256 hash of serial number (automatically computed)
- **FCM Token**: VARCHAR(500) - Firebase Cloud Messaging token

## Limitations

This is a centralized, single-node ledger system:

- No consensus mechanism
- No blockchain or distributed ledger features
- No cryptographic signature verification
- Single point of failure
- Requires trust in the database operator

## Performance Considerations

- Indexes are created on frequently queried columns
- Use transactions for atomicity
- Row-level locking prevents concurrent balance conflicts
- InnoDB engine provides ACID compliance

## Example Workflow

```sql
-- 1. Register accounts with device information
CALL register_account('alice', 1000.0, 'SN-001', 'attestation_alice', 'pk_alice',
    'Pixel 8', 'Google', 'Android 14', 37.7749, -122.4194, 'fcm_alice',
    @id1, @s1, @m1);
CALL register_account('bob', 500.0, 'SN-002', 'attestation_bob', 'pk_bob',
    'iPhone 15', 'Apple', 'iOS 17', 40.7128, -74.0060, 'fcm_bob',
    @id2, @s2, @m2);

-- 2. Transfer tokens
CALL transfer_tokens('alice', 'bob', 100.0, 'Payment', @tx1, @s3, @m3);

-- 3. Check balances and device info
SELECT address, balance, model, brand, gps_latitude, gps_longitude
FROM accounts WHERE address IN ('alice', 'bob');

-- 4. Update device location
CALL update_device_info('alice', NULL, 34.0522, -118.2437, NULL, @s, @m);

-- 5. Burn tokens
CALL burn_tokens('bob', 50.0, 'Burn event', @tx2, @s4, @m4);

-- 6. View device statistics
SELECT * FROM device_stats;

-- 7. View ledger statistics
SELECT * FROM ledger_stats;

-- 8. View transaction history
SELECT * FROM transaction_history WHERE from_address = 'alice' OR to_address = 'alice';
```

## Troubleshooting

### Account Already Exists
```
Error: Account already exists
```
Check if the address is already registered:
```sql
SELECT * FROM accounts WHERE address = 'your_address';
```

### Insufficient Balance
```
Error: Insufficient balance
```
Check current balance:
```sql
CALL get_balance('your_address', @balance, @success, @message);
SELECT @balance;
```

### Max Supply Exceeded
```
Error: Would exceed max supply
```
Check current and max supply:
```sql
SELECT * FROM ledger_config WHERE config_key IN ('total_supply', 'max_supply');
```

## Maintenance

### Backup Database
```bash
mysqldump -u root -p jcohen_ccrypto > backup_$(date +%Y%m%d).sql
```

### Restore Database
```bash
mysql -u root -p jcohen_ccrypto < backup_20250115.sql
```

### Reset Database to Initial State

**WARNING: This will delete ALL accounts and transactions!**

To reset the database and purge all data while keeping the schema:

```bash
mysql -u root -p < 06_reset_database.sql
```

This will:
- Delete all accounts (except SYSTEM)
- Delete all transactions and transaction logs
- Reset auto-increment counters to 1
- Reset total supply to 0
- Recreate the SYSTEM account

Use this for:
- Development and testing
- Clearing test data
- Starting fresh after data corruption

### Clean Old Logs (Optional)
```sql
-- Delete transaction logs older than 1 year
DELETE FROM transaction_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);
```

## License

This is a simple example implementation for educational purposes.
