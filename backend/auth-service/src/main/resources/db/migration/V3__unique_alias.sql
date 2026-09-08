-- The alias is the handle other users see and mention, so it has to be unique.
-- A functional index on lower(alias) makes that uniqueness case insensitive:
-- "Marilo" and "marilo" are the same handle, which is what people expect.
--
-- Enforcing it here rather than only in the service closes the race between two
-- concurrent renames picking the same alias.

CREATE UNIQUE INDEX uk_users_alias_lower ON users (lower(alias));
