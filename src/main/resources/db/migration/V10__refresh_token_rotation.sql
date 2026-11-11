-- Set when a refresh token is exchanged for a new pair. Presenting a token that was already
-- rotated means it leaked (or the client lost the new pair), so every session of the account ends.
-- Other revocations (logout, password change, suspension) leave it null.
ALTER TABLE refresh_token ADD COLUMN rotated_at timestamptz;
