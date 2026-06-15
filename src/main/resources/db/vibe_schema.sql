-- Frenzo Vibe Mode production schema.
-- Local development can rely on Hibernate ddl-auto=update, but production has ddl-auto=none.

CREATE TABLE IF NOT EXISTS vibes (
  vibe_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(60) NOT NULL UNIQUE,
  name VARCHAR(80) NOT NULL,
  description VARCHAR(240) NOT NULL,
  icon VARCHAR(40) NOT NULL,
  activity_type VARCHAR(32) NOT NULL,
  default_duration_minutes INT NOT NULL DEFAULT 60,
  active BIT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  created_by VARCHAR(255),
  created_at DATETIME(6),
  updated_by VARCHAR(255),
  updated_at DATETIME(6),
  INDEX idx_vibes_activity (activity_type),
  INDEX idx_vibes_active_sort (active, sort_order)
);

CREATE TABLE IF NOT EXISTS active_vibe_sessions (
  session_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vibe_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL,
  starts_at DATETIME(6) NOT NULL,
  ends_at DATETIME(6) NOT NULL,
  created_by VARCHAR(255),
  created_at DATETIME(6),
  updated_by VARCHAR(255),
  updated_at DATETIME(6),
  INDEX idx_active_vibe_status_end (vibe_id, status, ends_at),
  INDEX idx_active_vibe_ends_at (ends_at)
);

CREATE TABLE IF NOT EXISTS user_vibe_participation (
  participation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  vibe_id BIGINT NOT NULL,
  latitude DECIMAL(9,6) NOT NULL,
  longitude DECIMAL(9,6) NOT NULL,
  radius_km INT NOT NULL,
  status VARCHAR(16) NOT NULL,
  joined_at DATETIME(6) NOT NULL,
  left_at DATETIME(6),
  expires_at DATETIME(6) NOT NULL,
  created_by VARCHAR(255),
  created_at DATETIME(6),
  updated_by VARCHAR(255),
  updated_at DATETIME(6),
  UNIQUE KEY uk_user_vibe_session (user_id, session_id),
  INDEX idx_vibe_participation_user_status (user_id, status),
  INDEX idx_vibe_participation_session_status (session_id, status),
  INDEX idx_vibe_participation_location (latitude, longitude),
  INDEX idx_vibe_participation_expiry (expires_at)
);

CREATE TABLE IF NOT EXISTS vibe_requests (
  vibe_request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vibe_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  request_message VARCHAR(160),
  response_message VARCHAR(160),
  compatibility_score INT NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_by VARCHAR(255),
  created_at DATETIME(6),
  updated_by VARCHAR(255),
  updated_at DATETIME(6),
  UNIQUE KEY uk_vibe_request_pair_session (sender_id, receiver_id, session_id),
  INDEX idx_vibe_request_sender (sender_id, status),
  INDEX idx_vibe_request_receiver (receiver_id, status),
  INDEX idx_vibe_request_session (session_id, status)
);

CREATE TABLE IF NOT EXISTS vibe_connections (
  vibe_connection_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vibe_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  user_id1 BIGINT NOT NULL,
  user_id2 BIGINT NOT NULL,
  connected_at DATETIME(6) NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_by VARCHAR(255),
  created_at DATETIME(6),
  updated_by VARCHAR(255),
  updated_at DATETIME(6),
  UNIQUE KEY uk_vibe_connection_pair (user_id1, user_id2, vibe_id),
  INDEX idx_vibe_connection_user1 (user_id1),
  INDEX idx_vibe_connection_user2 (user_id2),
  INDEX idx_vibe_connection_session (session_id)
);

-- Optional production spatial acceleration for MySQL 8+:
-- ALTER TABLE user_vibe_participation
--   ADD COLUMN geo POINT SRID 4326
--   GENERATED ALWAYS AS (ST_SRID(POINT(longitude, latitude), 4326)) STORED,
--   ADD SPATIAL INDEX idx_vibe_participation_geo (geo);
