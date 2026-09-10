-- Run as movie_app (or SET ROLE movie_app) in movie_booking.
CREATE TABLE IF NOT EXISTS user_registration_outbox (
    event_id varchar(36) PRIMARY KEY,
    user_id bigint NOT NULL,
    username varchar(255) NOT NULL,
    email varchar(255) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_registration_outbox_pending
    ON user_registration_outbox (created_at, event_id);
