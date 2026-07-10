-- ============================================================
-- Pulse — seed script (deliverable)
-- Predefined users and one post per user.
--
-- NOTE: the application seeds this same data automatically on
-- startup (DataSeeder in each service), so this script is the
-- standalone/manual alternative required by the test statement.
-- It is idempotent: ON CONFLICT DO NOTHING.
--
-- All users share the demo password:  Pulse2026!
-- (BCrypt hash below; generated with BCryptPasswordEncoder)
--
-- Usage (with the compose stack running):
--   docker exec -i pulse-postgres psql -U pulse -d socialdb < db/seed.sql
-- ============================================================

-- ---------- Users (schema auth, owned by auth-service) ----------

INSERT INTO auth.users (id, username, password_hash, first_name, last_name, birth_date, alias) VALUES
  ('00000000-0000-0000-0000-000000000001', 'mariana',   '$2a$10$Kf0SMBGodGYIkcBKNZ2v.eI5ynbBEZ.UuCGBKWFcpJrZzELDEBNcC', 'Mariana',   'López',  '1995-04-12', 'marilo'),
  ('00000000-0000-0000-0000-000000000002', 'carlos',    '$2a$10$Kf0SMBGodGYIkcBKNZ2v.eI5ynbBEZ.UuCGBKWFcpJrZzELDEBNcC', 'Carlos',    'Gómez',  '1992-11-03', 'cgomez'),
  ('00000000-0000-0000-0000-000000000003', 'valentina', '$2a$10$Kf0SMBGodGYIkcBKNZ2v.eI5ynbBEZ.UuCGBKWFcpJrZzELDEBNcC', 'Valentina', 'Ruiz',   '1998-07-21', 'valen'),
  ('00000000-0000-0000-0000-000000000004', 'andres',    '$2a$10$Kf0SMBGodGYIkcBKNZ2v.eI5ynbBEZ.UuCGBKWFcpJrZzELDEBNcC', 'Andrés',    'Torres', '1990-02-14', 'atorres'),
  ('00000000-0000-0000-0000-000000000005', 'daniela',   '$2a$10$Kf0SMBGodGYIkcBKNZ2v.eI5ynbBEZ.UuCGBKWFcpJrZzELDEBNcC', 'Daniela',   'Mora',   '1996-09-30', 'danim')
ON CONFLICT (id) DO NOTHING;

-- ---------- Posts (schema posts, owned by posts-service) ----------
-- author_id matches the users above by convention (no cross-schema FK:
-- each microservice owns its schema). published_at uses the DB default.

INSERT INTO posts.posts (id, author_id, author_alias, message) VALUES
  ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'marilo',  'Just joined Pulse — excited to share things here! 🚀'),
  ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 'cgomez',  'Hot take: PostgreSQL stored procedures are underrated.'),
  ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003', 'valen',   'Weekend plan: coffee, code and a good playlist ☕'),
  ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000004', 'atorres', 'Shipping a side project tonight. Wish me luck!'),
  ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000005', 'danim',   'Watching likes update in real time is oddly satisfying ✨')
ON CONFLICT (id) DO NOTHING;
