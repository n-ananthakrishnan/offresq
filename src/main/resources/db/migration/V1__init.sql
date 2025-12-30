CREATE TABLE IF NOT EXISTS locations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id VARCHAR(64) NOT NULL,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  alert INT NOT NULL,
  rssi INT NULL,
  device_timestamp_ms BIGINT NULL,
  received_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_locations_received ON locations(received_at);
CREATE INDEX idx_locations_device_received ON locations(device_id, received_at);