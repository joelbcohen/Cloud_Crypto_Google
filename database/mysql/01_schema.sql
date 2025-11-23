-- ============================================================================
-- Ultralight Centralized Crypto Ledger - MySQL Schema
-- ============================================================================
-- This schema provides a simple centralized ledger with ERC20-like features:
-- - Account registration
-- - Token transfers
-- - Token burning
-- - Balance tracking
-- ============================================================================

-- Drop existing database if it exists (use with caution in production)
USE jcohen_ccrypto;

-- ============================================================================
-- Table: accounts
-- Stores registered accounts/addresses with device information
-- ============================================================================
CREATE TABLE accounts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    balance DECIMAL(65, 18) NOT NULL DEFAULT 0.000000000000000000,

    -- Device identification and security
    serial_number VARCHAR(255) NULL,
    serial_hash CHAR(64) NULL COMMENT 'SHA256 hash of serial_number',
    attestation_blob TEXT NULL,
    public_key TEXT NULL,

    -- Device information
    model VARCHAR(255) NULL,
    brand VARCHAR(255) NULL,
    os_version VARCHAR(100) NULL,

    -- Push notification
    fcm_token VARCHAR(500) NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY unique_serial_number (serial_number),
    INDEX idx_balance (balance),
    INDEX idx_created_at (created_at),
    INDEX idx_serial_number (serial_number),
    INDEX idx_serial_hash (serial_hash),
    INDEX idx_model_brand (model, brand)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stores account addresses, balances, and device information';

ALTER TABLE `jcohen_ccrypto`.`accounts` 
ADD COLUMN `enabled` TINYINT NOT NULL DEFAULT 1 AFTER `fcm_token`,
ADD COLUMN `apnsEnvironment` VARCHAR(45) NULL DEFAULT 'production' AFTER `enabled`,
ADD COLUMN `public_id` VARCHAR(45) NULL AFTER `apnsEnvironment`;

ALTER TABLE `jcohen_ccrypto`.`accounts` 
ADD COLUMN `deviceType` VARCHAR(45) NULL AFTER `public_id`;

-- ============================================================================
-- Table: transactions
-- Records all token transactions (transfers, burns, mints)
-- ============================================================================
CREATE TABLE transactions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tx_hash VARCHAR(66) NOT NULL,
    from_account_id BIGINT UNSIGNED NULL,
    to_account_id BIGINT UNSIGNED NULL,
    amount DECIMAL(65, 18) NOT NULL,
    tx_type ENUM('mint', 'transfer', 'burn') NOT NULL,
    status ENUM('pending', 'completed', 'failed') NOT NULL DEFAULT 'pending',
    memo VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,

    PRIMARY KEY (id),
    UNIQUE KEY unique_tx_hash (tx_hash),
    INDEX idx_from_account (from_account_id),
    INDEX idx_to_account (to_account_id),
    INDEX idx_tx_type (tx_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_composite_account_type (from_account_id, tx_type, created_at),

    CONSTRAINT fk_from_account
        FOREIGN KEY (from_account_id)
        REFERENCES accounts(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_to_account
        FOREIGN KEY (to_account_id)
        REFERENCES accounts(id)
        ON DELETE RESTRICT,

    -- Validation constraints
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_mint_no_from CHECK (
        tx_type != 'mint' OR from_account_id IS NULL
    ),
    CONSTRAINT chk_burn_no_to CHECK (
        tx_type != 'burn' OR to_account_id IS NULL
    ),
    CONSTRAINT chk_transfer_both_accounts CHECK (
        tx_type != 'transfer' OR (from_account_id IS NOT NULL AND to_account_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Records all token transactions including mints, transfers, and burns';

-- ============================================================================
-- Table: ledger_config
-- Stores global ledger configuration
-- ============================================================================
CREATE TABLE ledger_config (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    description VARCHAR(500) NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY unique_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stores global ledger configuration and metadata';

-- ============================================================================
-- Table: transaction_log
-- Audit log for all balance changes (optional but recommended)
-- ============================================================================
CREATE TABLE transaction_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NOT NULL,
    previous_balance DECIMAL(65, 18) NOT NULL,
    new_balance DECIMAL(65, 18) NOT NULL,
    change_amount DECIMAL(65, 18) NOT NULL,
    transaction_id BIGINT UNSIGNED NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_account_id (account_id),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_created_at (created_at),

    CONSTRAINT fk_log_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_log_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit log tracking all balance changes';

-- Add node_id column to accounts table
ALTER TABLE accounts 
ADD COLUMN node_id VARCHAR(255) NULL AFTER serial_number,
ADD INDEX idx_node_id (node_id);
