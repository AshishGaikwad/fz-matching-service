-- Frenzo Lowkey Mode V2 schema.
-- Redis is intentionally deferred for the next MVP; this version uses MySQL
-- bounding-box indexes plus Haversine scoring in the service layer.

CREATE TABLE IF NOT EXISTS lowkey_sessions (
  user_id BIGINT PRIMARY KEY,
  status VARCHAR(16) NOT NULL,
  latitude DECIMAL(9,6) NOT NULL,
  longitude DECIMAL(9,6) NOT NULL,
  location_accuracy_meters INT,
  radius_km INT NOT NULL,
  duration_minutes INT NOT NULL,
  looking_for VARCHAR(256),
  left_at DATETIME(6),
  expires_at DATETIME(6) NOT NULL,
  last_seen_at DATETIME(6) NOT NULL,
  created_by VARCHAR(255),
  created_at DATETIME(6),
  updated_by VARCHAR(255),
  updated_at DATETIME(6),
  INDEX idx_lowkey_user_status (user_id, status),
  INDEX idx_lowkey_status_expiry (status, expires_at),
  INDEX idx_lowkey_location (latitude, longitude),
  INDEX idx_lowkey_last_seen (last_seen_at)
);

CREATE TABLE IF NOT EXISTS user_locations (
  user_id BIGINT PRIMARY KEY,
  latitude DECIMAL(9,6) NOT NULL,
  longitude DECIMAL(9,6) NOT NULL,
  accuracy_meters INT,
  source VARCHAR(32),
  last_updated_at DATETIME(6) NOT NULL,
  created_by VARCHAR(255),
  created_at DATETIME(6),
  updated_by VARCHAR(255),
  updated_at DATETIME(6),
  INDEX idx_user_locations_updated (updated_at),
  INDEX idx_user_locations_geo (latitude, longitude)
);

CREATE TABLE IF NOT EXISTS lowkey_discovery_history (
  viewer_user_id BIGINT NOT NULL,
  candidate_user_id BIGINT NOT NULL,
  exposure_count INT NOT NULL DEFAULT 0,
  last_score INT,
  last_seen_at DATETIME(6) NOT NULL,
  created_by VARCHAR(255),
  created_at DATETIME(6),
  updated_by VARCHAR(255),
  updated_at DATETIME(6),
  PRIMARY KEY (viewer_user_id, candidate_user_id),
  INDEX idx_lowkey_history_viewer_seen (viewer_user_id, last_seen_at),
  INDEX idx_lowkey_history_candidate (candidate_user_id)
);

CREATE TABLE IF NOT EXISTS match_score_cache (
  viewer_user_id BIGINT NOT NULL,
  candidate_user_id BIGINT NOT NULL,
  score INT NOT NULL,
  match_grade VARCHAR(4) NOT NULL,
  match_explanation VARCHAR(512),
  score_breakdown TEXT,
  expires_at DATETIME(6) NOT NULL,
  created_by VARCHAR(255),
  created_at DATETIME(6),
  updated_by VARCHAR(255),
  updated_at DATETIME(6),
  PRIMARY KEY (viewer_user_id, candidate_user_id),
  INDEX idx_match_score_viewer (viewer_user_id, expires_at),
  INDEX idx_match_score_candidate (candidate_user_id)
);
