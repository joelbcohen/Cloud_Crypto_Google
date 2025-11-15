-- ============================================================================
-- Initial Configuration Data
-- ============================================================================

USE jcohen_ccrypto;

-- Insert initial ledger configuration
INSERT INTO ledger_config (config_key, config_value, description) VALUES
('ledger_name', 'Ultralight Crypto Ledger', 'Name of the crypto ledger'),
('token_symbol', 'UCL', 'Token symbol (like ETH, BTC, etc)'),
('token_decimals', '18', 'Number of decimal places for the token'),
('total_supply', '0', 'Total supply of tokens minted'),
('max_supply', '1000000000', 'Maximum supply cap (0 = unlimited)'),
('ledger_version', '1.0.0', 'Version of the ledger schema'),
('created_at', NOW(), 'Ledger creation timestamp');

-- Optional: Create a system account for minting/burning
INSERT INTO accounts (address, balance) VALUES
('SYSTEM', 0.000000000000000000);

-- Optional: Create an initial admin account
-- INSERT INTO accounts (address, balance) VALUES
-- ('ADMIN_ADDRESS_HERE', 1000000.000000000000000000);
