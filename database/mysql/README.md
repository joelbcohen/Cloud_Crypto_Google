# Ultralight Centralized Crypto Ledger - MySQL

A simple, centralized cryptocurrency ledger system with ERC20-like features built on MySQL. This ledger supports account registration, token transfers, and token burning in a single-node environment.

## Features

- **Account Registration**: Create new accounts with optional initial token balance
- **Token Transfers**: Transfer tokens between accounts with atomicity guarantees
- **Token Burning**: Permanently destroy tokens from circulation
- **Token Minting**: Create new tokens (with max supply limits)
- **Balance Tracking**: Accurate balance tracking with audit logs
- **Transaction History**: Complete transaction history with status tracking

## Database Schema

### Tables

1. **accounts** - Stores account addresses and balances
2. **transactions** - Records all token transactions (mints, transfers, burns)
3. **ledger_config** - Global ledger configuration (token symbol, supply, etc.)
4. **transaction_log** - Audit log for all balance changes

### Views

- **account_summary** - Summary of accounts with transaction statistics
- **transaction_history** - Readable transaction history
- **ledger_stats** - Overall ledger statistics
- **recent_transactions** - 100 most recent transactions
- **top_balances** - Accounts with highest balances

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

## Usage

### Register a New Account

```sql
-- Register account with zero balance
CALL register_account('user123', 0, @account_id, @success, @message);
SELECT @account_id, @success, @message;

-- Register account with initial balance (mints tokens)
CALL register_account('admin001', 1000000.0, @account_id, @success, @message);
SELECT @account_id, @success, @message;
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

- **Row-level locking** on balance updates to prevent race conditions
- **Transaction isolation** ensures atomic operations
- **Foreign key constraints** maintain referential integrity
- **Check constraints** validate transaction types and amounts
- **Audit logging** tracks all balance changes

## Data Types

- **Balances**: DECIMAL(65, 18) - supports up to 18 decimal places (like ETH)
- **Addresses**: VARCHAR(255) - can store various address formats
- **Transaction Hash**: VARCHAR(66) - supports 0x-prefixed hex strings

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
-- 1. Register accounts
CALL register_account('alice', 1000.0, @id1, @s1, @m1);
CALL register_account('bob', 500.0, @id2, @s2, @m2);

-- 2. Transfer tokens
CALL transfer_tokens('alice', 'bob', 100.0, 'Payment', @tx1, @s3, @m3);

-- 3. Check balances
SELECT address, balance FROM accounts WHERE address IN ('alice', 'bob');

-- 4. Burn tokens
CALL burn_tokens('bob', 50.0, 'Burn event', @tx2, @s4, @m4);

-- 5. View statistics
SELECT * FROM ledger_stats;

-- 6. View transaction history
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
mysqldump -u root -p crypto_ledger > backup_$(date +%Y%m%d).sql
```

### Restore Database
```bash
mysql -u root -p crypto_ledger < backup_20250115.sql
```

### Clean Old Logs (Optional)
```sql
-- Delete transaction logs older than 1 year
DELETE FROM transaction_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);
```

## License

This is a simple example implementation for educational purposes.
