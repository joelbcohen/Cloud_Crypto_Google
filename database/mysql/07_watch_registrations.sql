-- Create watch_registrations table for Cloud Crypto Watch Registration
-- This table stores device registration information for smartwatches

CREATE TABLE IF NOT EXISTS watch_registrations (
    id VARCHAR(255) PRIMARY KEY COMMENT 'Android device ID',
    serial_number VARCHAR(255) NOT NULL UNIQUE COMMENT 'Watch serial number',
    fcm_token TEXT COMMENT 'Firebase Cloud Messaging token for push notifications',
    public_key TEXT COMMENT 'Device public key for encryption',
    attestation_blob TEXT COMMENT 'Device attestation data',
    device_model VARCHAR(100) COMMENT 'Device model name',
    device_brand VARCHAR(100) COMMENT 'Device brand/manufacturer',
    os_version VARCHAR(50) COMMENT 'Operating system version',
    node_id VARCHAR(100) COMMENT 'Wear OS node ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Registration timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    
    INDEX idx_serial_number (serial_number),
    INDEX idx_fcm_token (fcm_token(255)),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Watch device registrations for Cloud Crypto';
