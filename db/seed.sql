-- ============================================================
-- Pulse — standalone seed script
--
-- The application seeds this same data on startup (DataSeeder in each
-- service); this file is the manual equivalent, for loading the demo
-- content into a database that was created some other way.
--
-- Every user shares the demo password:  Pulse2026!
-- (BCrypt hashes below, generated with BCryptPasswordEncoder.)
--
-- Usage, with the compose stack running:
--   docker exec -i pulse-postgres psql -U pulse -d socialdb < db/seed.sql
--
-- Idempotent: every statement ends in ON CONFLICT DO NOTHING.
-- ============================================================

-- ---------- Users (schema auth, owned by auth-service) ----------

INSERT INTO auth.users (id, username, password_hash, first_name, last_name, birth_date, alias) VALUES
  ('00000000-0000-0000-0000-000000000001', 'mariana', '$2a$10$2xiLPLv1h/s/aLWqizP7QO8jlGoVtCPMoMXd/0iRfMOeE3uhtb2EW', 'Mariana', 'López', '1995-04-12', 'marilo'),
  ('00000000-0000-0000-0000-000000000002', 'carlos', '$2a$10$2xiLPLv1h/s/aLWqizP7QO8jlGoVtCPMoMXd/0iRfMOeE3uhtb2EW', 'Carlos', 'Gómez', '1992-11-03', 'cgomez'),
  ('00000000-0000-0000-0000-000000000003', 'valentina', '$2a$10$2xiLPLv1h/s/aLWqizP7QO8jlGoVtCPMoMXd/0iRfMOeE3uhtb2EW', 'Valentina', 'Ruiz', '1998-07-21', 'valen'),
  ('00000000-0000-0000-0000-000000000004', 'andres', '$2a$10$2xiLPLv1h/s/aLWqizP7QO8jlGoVtCPMoMXd/0iRfMOeE3uhtb2EW', 'Andrés', 'Torres', '1990-02-14', 'atorres'),
  ('00000000-0000-0000-0000-000000000005', 'daniela', '$2a$10$2xiLPLv1h/s/aLWqizP7QO8jlGoVtCPMoMXd/0iRfMOeE3uhtb2EW', 'Daniela', 'Mora', '1996-09-30', 'danim')
ON CONFLICT (id) DO NOTHING;

-- ---------- Posts (schema posts, owned by posts-service) ----------
-- author_id matches the users above by convention: there is no cross-schema
-- foreign key, because each microservice owns its own schema. Publication
-- dates are relative to the moment the script runs, so the feed always looks
-- recent.

INSERT INTO posts.posts (id, author_id, author_alias, message, published_at) VALUES
  ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'marilo', 'Estrenando Pulse. Aviso: voy a publicar demasiadas fotos de atardeceres.', now() - INTERVAL '7 minutes'),
  ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 'cgomez', 'Opinion impopular: los stored procedures estan infravalorados.', now() - INTERVAL '20 minutes'),
  ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003', 'valen', 'Plan de fin de semana: cafe, codigo y una buena playlist.', now() - INTERVAL '45 minutes'),
  ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000004', 'atorres', 'Esta noche subo el proyecto que llevo tres meses puliendo. Deseenme suerte.', now() - INTERVAL '79 minutes'),
  ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000005', 'danim', 'Ver los contadores moverse solos es sorprendentemente satisfactorio.', now() - INTERVAL '133 minutes'),
  ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'cgomez', 'Tres horas depurando. Era un punto y coma. Siempre es un punto y coma.', now() - INTERVAL '191 minutes'),
  ('20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003', 'valen', 'Terminando el rediseno del dashboard. Los degradados quedaron mejor de lo que esperaba.', now() - INTERVAL '241 minutes'),
  ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'marilo', 'Consejo del dia: si tu README no explica por que, no explica nada.', now() - INTERVAL '306 minutes'),
  ('20000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000004', 'atorres', 'Cambie las consultas del feed a paginacion por cursor y la diferencia se nota.', now() - INTERVAL '381 minutes'),
  ('20000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000005', 'danim', 'Hoy aprendi que PostgreSQL distingue PROCEDURE de FUNCTION. Tiene todo el sentido.', now() - INTERVAL '461 minutes'),
  ('20000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000002', 'cgomez', 'El mejor refactor de la semana fue borrar codigo que ya nadie usaba.', now() - INTERVAL '621 minutes'),
  ('20000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001', 'marilo', 'Cafe numero tres y todavia es martes.', now() - INTERVAL '781 minutes'),
  ('20000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000003', 'valen', 'Un test de integracion que levanta la base real vale por diez con mocks.', now() - INTERVAL '911 minutes'),
  ('20000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000004', 'atorres', 'Migre todo a Docker Compose y ahora arranca con un solo comando. Paz mental.', now() - INTERVAL '1081 minutes'),
  ('20000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000005', 'danim', 'Recordatorio: el WebSocket es una optimizacion, no la fuente de verdad.', now() - INTERVAL '1261 minutes'),
  ('20000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000001', 'marilo', 'Nada como cerrar el viernes con la suite en verde.', now() - INTERVAL '1501 minutes'),
  ('20000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000002', 'cgomez', 'Leyendo sobre indices compuestos. El orden de las columnas importa mas de lo que parece.', now() - INTERVAL '1801 minutes'),
  ('20000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000003', 'valen', 'Domingo de documentar decisiones tecnicas. Mi yo del futuro lo agradecera.', now() - INTERVAL '2201 minutes'),
  ('20000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000004', 'atorres', 'El diseno tambien es una funcionalidad. Una app fea se siente rota.', now() - INTERVAL '2601 minutes'),
  ('20000000-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000005', 'danim', 'Empece a escribir los mensajes de commit como si alguien fuera a leerlos. Alguien lo hace.', now() - INTERVAL '3101 minutes'),
  ('20000000-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000001', 'marilo', 'La parte dificil nunca es el codigo, es decidir que no construir.', now() - INTERVAL '3801 minutes'),
  ('20000000-0000-0000-0000-000000000017', '00000000-0000-0000-0000-000000000002', 'cgomez', 'Primer dia usando atajos de teclado en serio. No hay vuelta atras.', now() - INTERVAL '4601 minutes'),
  ('20000000-0000-0000-0000-000000000018', '00000000-0000-0000-0000-000000000003', 'valen', 'Pequena victoria: el bundle bajo de 400 a 97 kilobytes comprimidos.', now() - INTERVAL '5601 minutes'),
  ('20000000-0000-0000-0000-000000000019', '00000000-0000-0000-0000-000000000004', 'atorres', 'Hola mundo. Estrenando cuenta por aqui.', now() - INTERVAL '7201 minutes')
ON CONFLICT (id) DO NOTHING;

-- ---------- Likes ----------
-- The composite primary key (post_id, user_id) is what makes a like
-- idempotent: one user counts once, however many times they click.

INSERT INTO posts.likes (post_id, user_id) VALUES
  ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001'),
  ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002'),
  ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000005'),
  ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001'),
  ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000003'),
  ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),
  ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003'),
  ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000004'),
  ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000005'),
  ('20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001'),
  ('20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'),
  ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002'),
  ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003'),
  ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000004'),
  ('20000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000002'),
  ('20000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000005'),
  ('20000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001'),
  ('20000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000002'),
  ('20000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000003'),
  ('20000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000004'),
  ('20000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000003'),
  ('20000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000005'),
  ('20000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000001'),
  ('20000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000002'),
  ('20000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000004'),
  ('20000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000005'),
  ('20000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002'),
  ('20000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000001'),
  ('20000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000004'),
  ('20000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000002'),
  ('20000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000003'),
  ('20000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000004'),
  ('20000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000005'),
  ('20000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000003'),
  ('20000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000001'),
  ('20000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000005'),
  ('20000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000001'),
  ('20000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000002'),
  ('20000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000003'),
  ('20000000-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000004'),
  ('20000000-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000002'),
  ('20000000-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000003'),
  ('20000000-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000004'),
  ('20000000-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000005'),
  ('20000000-0000-0000-0000-000000000017', '00000000-0000-0000-0000-000000000001'),
  ('20000000-0000-0000-0000-000000000018', '00000000-0000-0000-0000-000000000002'),
  ('20000000-0000-0000-0000-000000000018', '00000000-0000-0000-0000-000000000004'),
  ('20000000-0000-0000-0000-000000000018', '00000000-0000-0000-0000-000000000005'),
  ('20000000-0000-0000-0000-000000000019', '00000000-0000-0000-0000-000000000001'),
  ('20000000-0000-0000-0000-000000000019', '00000000-0000-0000-0000-000000000002')
ON CONFLICT (post_id, user_id) DO NOTHING;
