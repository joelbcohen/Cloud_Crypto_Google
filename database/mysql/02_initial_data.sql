-- ============================================================================
-- Initial Configuration Data
-- ============================================================================

USE jcohen_ccrypto;

-- Insert initial ledger configuration
INSERT INTO ledger_config (config_key, config_value, description) VALUES
('ledger_name', 'Mobile Cloud Crypto', 'Name of the crypto ledger'),
('token_symbol', 'MCC', 'Token symbol (like ETH, BTC, etc)'),
('token_decimals', '18', 'Number of decimal places for the token'),
('total_supply', '0', 'Total supply of tokens minted'),
('max_supply', '1000000000', 'Maximum supply cap (0 = unlimited)'),
('ledger_version', '1.0.0', 'Version of the ledger schema'),
('created_at', NOW(), 'Ledger creation timestamp');

-- Create a system account for minting/burning
INSERT INTO accounts (balance, node_id) VALUES
(0.000000000000000000, 1);

-- Optional: Create an initial admin account
-- INSERT INTO accounts (id, balance) VALUES
-- ('ADMIN_ID_HERE', 1000000.000000000000000000);

