-- ============================================================================
-- Stored Procedures for Ledger Operations
-- ============================================================================

USE jcohen_ccrypto;

DELIMITER $$

-- ============================================================================
-- Procedure: register_account
-- Creates a new account with device information and optional initial balance (mint)
-- ============================================================================
DROP PROCEDURE IF EXISTS register_account$$
CREATE PROCEDURE register_account(
    IN p_address VARCHAR(255),
    IN p_initial_balance DECIMAL(65, 18),
    IN p_serial_number VARCHAR(255),
    IN p_attestation_blob TEXT,
    IN p_public_key TEXT,
    IN p_model VARCHAR(255),
    IN p_brand VARCHAR(255),
    IN p_os_version VARCHAR(100),
    IN p_gps_latitude DECIMAL(10, 8),
    IN p_gps_longitude DECIMAL(11, 8),
    IN p_fcm_token VARCHAR(500),
    OUT p_account_id BIGINT,
    OUT p_success BOOLEAN,
    OUT p_message VARCHAR(500)
)
BEGIN
    DECLARE v_existing_id BIGINT;
    DECLARE v_tx_hash VARCHAR(66);
    DECLARE v_total_supply DECIMAL(65, 18);
    DECLARE v_max_supply DECIMAL(65, 18);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = FALSE;
        SET p_message = 'Error: Transaction failed';
        SET p_account_id = NULL;
    END;

    START TRANSACTION;

    -- Check if account already exists
    SELECT id INTO v_existing_id FROM accounts WHERE address = p_address LIMIT 1;

    IF v_existing_id IS NOT NULL THEN
        SET p_success = FALSE;
        SET p_message = 'Error: Account already exists';
        SET p_account_id = v_existing_id;
        ROLLBACK;
    ELSE
        -- Set default balance to 0 if not provided
        IF p_initial_balance IS NULL THEN
            SET p_initial_balance = 0;
        END IF;

        -- Check max supply if minting initial balance
        IF p_initial_balance > 0 THEN
            SELECT CAST(config_value AS DECIMAL(65,18)) INTO v_max_supply
            FROM ledger_config WHERE config_key = 'max_supply';

            SELECT CAST(config_value AS DECIMAL(65,18)) INTO v_total_supply
            FROM ledger_config WHERE config_key = 'total_supply';

            IF v_max_supply > 0 AND (v_total_supply + p_initial_balance) > v_max_supply THEN
                SET p_success = FALSE;
                SET p_message = 'Error: Would exceed max supply';
                SET p_account_id = NULL;
                ROLLBACK;
            ELSE
                -- Create account with device information
                INSERT INTO accounts (
                    address, balance, serial_number, serial_hash, attestation_blob, public_key,
                    model, brand, os_version, gps_latitude, gps_longitude, fcm_token
                ) VALUES (
                    p_address, p_initial_balance, p_serial_number,
                    IF(p_serial_number IS NOT NULL, SHA2(p_serial_number, 256), NULL),
                    p_attestation_blob, p_public_key,
                    p_model, p_brand, p_os_version, p_gps_latitude, p_gps_longitude, p_fcm_token
                );
                SET p_account_id = LAST_INSERT_ID();

                -- Record mint transaction if initial balance > 0
                IF p_initial_balance > 0 THEN
                    SET v_tx_hash = CONCAT('0x', MD5(CONCAT(p_account_id, UNIX_TIMESTAMP(), 'mint')));

                    INSERT INTO transactions (tx_hash, from_account_id, to_account_id, amount, tx_type, status, completed_at)
                    VALUES (v_tx_hash, NULL, p_account_id, p_initial_balance, 'mint', 'completed', NOW());

                    -- Update total supply
                    UPDATE ledger_config
                    SET config_value = CAST((v_total_supply + p_initial_balance) AS CHAR)
                    WHERE config_key = 'total_supply';

                    -- Log balance change
                    INSERT INTO transaction_log (account_id, previous_balance, new_balance, change_amount, transaction_id)
                    VALUES (p_account_id, 0, p_initial_balance, p_initial_balance, LAST_INSERT_ID());
                END IF;

                SET p_success = TRUE;
                SET p_message = 'Account created successfully';
                COMMIT;
            END IF;
        ELSE
            -- Create account with zero balance and device information
            INSERT INTO accounts (
                address, balance, serial_number, serial_hash, attestation_blob, public_key,
                model, brand, os_version, gps_latitude, gps_longitude, fcm_token
            ) VALUES (
                p_address, 0, p_serial_number,
                IF(p_serial_number IS NOT NULL, SHA2(p_serial_number, 256), NULL),
                p_attestation_blob, p_public_key,
                p_model, p_brand, p_os_version, p_gps_latitude, p_gps_longitude, p_fcm_token
            );
            SET p_account_id = LAST_INSERT_ID();
            SET p_success = TRUE;
            SET p_message = 'Account created successfully';
            COMMIT;
        END IF;
    END IF;
END$$

-- ============================================================================
-- Procedure: transfer_tokens
-- Transfers tokens from one account to another
-- ============================================================================
DROP PROCEDURE IF EXISTS transfer_tokens$$
CREATE PROCEDURE transfer_tokens(
    IN p_from_address VARCHAR(255),
    IN p_to_address VARCHAR(255),
    IN p_amount DECIMAL(65, 18),
    IN p_memo VARCHAR(500),
    OUT p_tx_id BIGINT,
    OUT p_success BOOLEAN,
    OUT p_message VARCHAR(500)
)
BEGIN
    DECLARE v_from_id BIGINT;
    DECLARE v_to_id BIGINT;
    DECLARE v_from_balance DECIMAL(65, 18);
    DECLARE v_to_balance DECIMAL(65, 18);
    DECLARE v_tx_hash VARCHAR(66);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = FALSE;
        SET p_message = 'Error: Transaction failed';
        SET p_tx_id = NULL;
    END;

    START TRANSACTION;

    -- Validate amount
    IF p_amount <= 0 THEN
        SET p_success = FALSE;
        SET p_message = 'Error: Amount must be positive';
        SET p_tx_id = NULL;
        ROLLBACK;
    ELSE
        -- Get account IDs and balances with row locks
        SELECT id, balance INTO v_from_id, v_from_balance
        FROM accounts WHERE address = p_from_address FOR UPDATE;

        SELECT id, balance INTO v_to_id, v_to_balance
        FROM accounts WHERE address = p_to_address FOR UPDATE;

        -- Validate accounts exist
        IF v_from_id IS NULL THEN
            SET p_success = FALSE;
            SET p_message = 'Error: From account does not exist';
            SET p_tx_id = NULL;
            ROLLBACK;
        ELSEIF v_to_id IS NULL THEN
            SET p_success = FALSE;
            SET p_message = 'Error: To account does not exist';
            SET p_tx_id = NULL;
            ROLLBACK;
        ELSEIF v_from_id = v_to_id THEN
            SET p_success = FALSE;
            SET p_message = 'Error: Cannot transfer to same account';
            SET p_tx_id = NULL;
            ROLLBACK;
        ELSEIF v_from_balance < p_amount THEN
            SET p_success = FALSE;
            SET p_message = 'Error: Insufficient balance';
            SET p_tx_id = NULL;
            ROLLBACK;
        ELSE
            -- Generate transaction hash
            SET v_tx_hash = CONCAT('0x', MD5(CONCAT(v_from_id, v_to_id, p_amount, UNIX_TIMESTAMP())));

            -- Update balances
            UPDATE accounts SET balance = balance - p_amount WHERE id = v_from_id;
            UPDATE accounts SET balance = balance + p_amount WHERE id = v_to_id;

            -- Record transaction
            INSERT INTO transactions (tx_hash, from_account_id, to_account_id, amount, tx_type, status, memo, completed_at)
            VALUES (v_tx_hash, v_from_id, v_to_id, p_amount, 'transfer', 'completed', p_memo, NOW());
            SET p_tx_id = LAST_INSERT_ID();

            -- Log balance changes
            INSERT INTO transaction_log (account_id, previous_balance, new_balance, change_amount, transaction_id)
            VALUES
                (v_from_id, v_from_balance, v_from_balance - p_amount, -p_amount, p_tx_id),
                (v_to_id, v_to_balance, v_to_balance + p_amount, p_amount, p_tx_id);

            SET p_success = TRUE;
            SET p_message = 'Transfer completed successfully';
            COMMIT;
        END IF;
    END IF;
END$$

-- ============================================================================
-- Procedure: burn_tokens
-- Burns (destroys) tokens from an account
-- ============================================================================
DROP PROCEDURE IF EXISTS burn_tokens$$
CREATE PROCEDURE burn_tokens(
    IN p_address VARCHAR(255),
    IN p_amount DECIMAL(65, 18),
    IN p_memo VARCHAR(500),
    OUT p_tx_id BIGINT,
    OUT p_success BOOLEAN,
    OUT p_message VARCHAR(500)
)
BEGIN
    DECLARE v_account_id BIGINT;
    DECLARE v_balance DECIMAL(65, 18);
    DECLARE v_tx_hash VARCHAR(66);
    DECLARE v_total_supply DECIMAL(65, 18);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = FALSE;
        SET p_message = 'Error: Transaction failed';
        SET p_tx_id = NULL;
    END;

    START TRANSACTION;

    -- Validate amount
    IF p_amount <= 0 THEN
        SET p_success = FALSE;
        SET p_message = 'Error: Amount must be positive';
        SET p_tx_id = NULL;
        ROLLBACK;
    ELSE
        -- Get account ID and balance with row lock
        SELECT id, balance INTO v_account_id, v_balance
        FROM accounts WHERE address = p_address FOR UPDATE;

        -- Validate account exists and has sufficient balance
        IF v_account_id IS NULL THEN
            SET p_success = FALSE;
            SET p_message = 'Error: Account does not exist';
            SET p_tx_id = NULL;
            ROLLBACK;
        ELSEIF v_balance < p_amount THEN
            SET p_success = FALSE;
            SET p_message = 'Error: Insufficient balance';
            SET p_tx_id = NULL;
            ROLLBACK;
        ELSE
            -- Generate transaction hash
            SET v_tx_hash = CONCAT('0x', MD5(CONCAT(v_account_id, p_amount, UNIX_TIMESTAMP(), 'burn')));

            -- Update balance
            UPDATE accounts SET balance = balance - p_amount WHERE id = v_account_id;

            -- Record burn transaction
            INSERT INTO transactions (tx_hash, from_account_id, to_account_id, amount, tx_type, status, memo, completed_at)
            VALUES (v_tx_hash, v_account_id, NULL, p_amount, 'burn', 'completed', p_memo, NOW());
            SET p_tx_id = LAST_INSERT_ID();

            -- Update total supply
            SELECT CAST(config_value AS DECIMAL(65,18)) INTO v_total_supply
            FROM ledger_config WHERE config_key = 'total_supply';

            UPDATE ledger_config
            SET config_value = CAST((v_total_supply - p_amount) AS CHAR)
            WHERE config_key = 'total_supply';

            -- Log balance change
            INSERT INTO transaction_log (account_id, previous_balance, new_balance, change_amount, transaction_id)
            VALUES (v_account_id, v_balance, v_balance - p_amount, -p_amount, p_tx_id);

            SET p_success = TRUE;
            SET p_message = 'Tokens burned successfully';
            COMMIT;
        END IF;
    END IF;
END$$

-- ============================================================================
-- Procedure: mint_tokens
-- Mints (creates) new tokens to an account
-- ============================================================================
DROP PROCEDURE IF EXISTS mint_tokens$$
CREATE PROCEDURE mint_tokens(
    IN p_address VARCHAR(255),
    IN p_amount DECIMAL(65, 18),
    IN p_memo VARCHAR(500),
    OUT p_tx_id BIGINT,
    OUT p_success BOOLEAN,
    OUT p_message VARCHAR(500)
)
BEGIN
    DECLARE v_account_id BIGINT;
    DECLARE v_balance DECIMAL(65, 18);
    DECLARE v_tx_hash VARCHAR(66);
    DECLARE v_total_supply DECIMAL(65, 18);
    DECLARE v_max_supply DECIMAL(65, 18);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = FALSE;
        SET p_message = 'Error: Transaction failed';
        SET p_tx_id = NULL;
    END;

    START TRANSACTION;

    -- Validate amount
    IF p_amount <= 0 THEN
        SET p_success = FALSE;
        SET p_message = 'Error: Amount must be positive';
        SET p_tx_id = NULL;
        ROLLBACK;
    ELSE
        -- Get account ID and balance with row lock
        SELECT id, balance INTO v_account_id, v_balance
        FROM accounts WHERE address = p_address FOR UPDATE;

        -- Validate account exists
        IF v_account_id IS NULL THEN
            SET p_success = FALSE;
            SET p_message = 'Error: Account does not exist';
            SET p_tx_id = NULL;
            ROLLBACK;
        ELSE
            -- Check max supply
            SELECT CAST(config_value AS DECIMAL(65,18)) INTO v_max_supply
            FROM ledger_config WHERE config_key = 'max_supply';

            SELECT CAST(config_value AS DECIMAL(65,18)) INTO v_total_supply
            FROM ledger_config WHERE config_key = 'total_supply';

            IF v_max_supply > 0 AND (v_total_supply + p_amount) > v_max_supply THEN
                SET p_success = FALSE;
                SET p_message = 'Error: Would exceed max supply';
                SET p_tx_id = NULL;
                ROLLBACK;
            ELSE
                -- Generate transaction hash
                SET v_tx_hash = CONCAT('0x', MD5(CONCAT(v_account_id, p_amount, UNIX_TIMESTAMP(), 'mint')));

                -- Update balance
                UPDATE accounts SET balance = balance + p_amount WHERE id = v_account_id;

                -- Record mint transaction
                INSERT INTO transactions (tx_hash, from_account_id, to_account_id, amount, tx_type, status, memo, completed_at)
                VALUES (v_tx_hash, NULL, v_account_id, p_amount, 'mint', 'completed', p_memo, NOW());
                SET p_tx_id = LAST_INSERT_ID();

                -- Update total supply
                UPDATE ledger_config
                SET config_value = CAST((v_total_supply + p_amount) AS CHAR)
                WHERE config_key = 'total_supply';

                -- Log balance change
                INSERT INTO transaction_log (account_id, previous_balance, new_balance, change_amount, transaction_id)
                VALUES (v_account_id, v_balance, v_balance + p_amount, p_amount, p_tx_id);

                SET p_success = TRUE;
                SET p_message = 'Tokens minted successfully';
                COMMIT;
            END IF;
        END IF;
    END IF;
END$$

-- ============================================================================
-- Procedure: get_balance
-- Retrieves the balance for an account
-- ============================================================================
DROP PROCEDURE IF EXISTS get_balance$$
CREATE PROCEDURE get_balance(
    IN p_address VARCHAR(255),
    OUT p_balance DECIMAL(65, 18),
    OUT p_success BOOLEAN,
    OUT p_message VARCHAR(500)
)
BEGIN
    SELECT balance INTO p_balance FROM accounts WHERE address = p_address;

    IF p_balance IS NULL THEN
        SET p_success = FALSE;
        SET p_message = 'Error: Account not found';
        SET p_balance = 0;
    ELSE
        SET p_success = TRUE;
        SET p_message = 'Balance retrieved successfully';
    END IF;
END$$

-- ============================================================================
-- Procedure: update_device_info
-- Updates device information for an existing account
-- ============================================================================
DROP PROCEDURE IF EXISTS update_device_info$$
CREATE PROCEDURE update_device_info(
    IN p_address VARCHAR(255),
    IN p_fcm_token VARCHAR(500),
    IN p_gps_latitude DECIMAL(10, 8),
    IN p_gps_longitude DECIMAL(11, 8),
    IN p_os_version VARCHAR(100),
    OUT p_success BOOLEAN,
    OUT p_message VARCHAR(500)
)
BEGIN
    DECLARE v_account_id BIGINT;

    -- Check if account exists
    SELECT id INTO v_account_id FROM accounts WHERE address = p_address;

    IF v_account_id IS NULL THEN
        SET p_success = FALSE;
        SET p_message = 'Error: Account not found';
    ELSE
        -- Update device information
        UPDATE accounts SET
            fcm_token = COALESCE(p_fcm_token, fcm_token),
            gps_latitude = COALESCE(p_gps_latitude, gps_latitude),
            gps_longitude = COALESCE(p_gps_longitude, gps_longitude),
            os_version = COALESCE(p_os_version, os_version)
        WHERE id = v_account_id;

        SET p_success = TRUE;
        SET p_message = 'Device information updated successfully';
    END IF;
END$$

DELIMITER ;
