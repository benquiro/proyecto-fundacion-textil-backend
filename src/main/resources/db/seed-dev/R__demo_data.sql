-- Demo data for the dev profile, taken from the mobile app seed (assets/seed/*.json).
-- Repeatable and idempotent: Flyway re-runs it when it changes and existing rows are kept.
-- Every demo account logs in with the password Demo1234 (BCrypt hash below).

INSERT INTO user_account (user_id, email, password_hash, role, alias, status, consent_at, created_at, updated_at, deactivated_at, deleted_at) VALUES
    (1, 'admin1@textilcircular.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ADMINISTRATOR', 'Admin Textil', 'ACTIVE', '2026-08-01T12:00:00Z', '2026-08-01T12:00:00Z', '2026-08-01T12:00:00Z', NULL, NULL),
    (2, 'admin2@textilcircular.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ADMINISTRATOR', 'Admin Circular', 'ACTIVE', '2026-08-02T12:00:00Z', '2026-08-02T12:00:00Z', '2026-08-02T12:00:00Z', NULL, NULL),
    (3, 'valentina.mora@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-03T12:00:00Z', '2026-08-03T12:00:00Z', '2026-08-03T12:00:00Z', NULL, NULL),
    (4, 'camila.soto@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-04T12:00:00Z', '2026-08-04T12:00:00Z', '2026-08-04T12:00:00Z', NULL, NULL),
    (5, 'javiera.pinto@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-05T12:00:00Z', '2026-08-05T12:00:00Z', '2026-08-05T12:00:00Z', NULL, NULL),
    (6, 'daniela.rios@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-06T12:00:00Z', '2026-08-06T12:00:00Z', '2026-08-06T12:00:00Z', NULL, NULL),
    (7, 'fernanda.lagos@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-07T12:00:00Z', '2026-08-07T12:00:00Z', '2026-08-07T12:00:00Z', NULL, NULL),
    (8, 'martin.diaz@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'CUSTOMER', 'Martín', 'ACTIVE', '2026-08-08T12:00:00Z', '2026-08-08T12:00:00Z', '2026-08-08T12:00:00Z', NULL, NULL),
    (9, 'sofia.reyes@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'CUSTOMER', 'Sofi', 'ACTIVE', '2026-08-09T12:00:00Z', '2026-08-09T12:00:00Z', '2026-08-09T12:00:00Z', NULL, NULL),
    (10, 'pablo.vega@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'CUSTOMER', 'Pablo V.', 'ACTIVE', '2026-08-10T12:00:00Z', '2026-08-10T12:00:00Z', '2026-08-10T12:00:00Z', NULL, NULL),
    (11, 'ana.castro@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'CUSTOMER', 'Ana', 'ACTIVE', '2026-08-11T12:00:00Z', '2026-08-11T12:00:00Z', '2026-08-11T12:00:00Z', NULL, NULL)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO entrepreneur (user_id, age_range_id, gender_id, rut, first_name, last_name, anonymized_at) VALUES
    (3, 4, 1, '16482905-7', 'Valentina', 'Mora', NULL),
    (4, 3, 1, '17934521-8', 'Camila', 'Soto', NULL),
    (5, 5, 1, '15208736-5', 'Javiera', 'Pinto', NULL),
    (6, 3, 1, '18345092-1', 'Daniela', 'Ríos', NULL),
    (7, 4, 3, '14762318-6', 'Fernanda', 'Lagos', NULL)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO business (business_id, user_id, commune_id, name, description, photo_url, status, review_round, approved_at, hidden_at, created_at, updated_at, deleted_at) VALUES
    (1, 3, 102, 'Hilos de Barrio', 'Arreglos y ajustes de ropa en Ñuñoa. Alargamos la vida de tus prendas con reparaciones visibles y bordados que cuentan una historia.', 'https://images.unsplash.com/photo-1528459801416-a9e53bbf4e17?auto=format&fit=crop&w=800&q=80', 'APPROVED', 1, '2026-09-03T12:00:00Z', NULL, '2026-09-01T12:00:00Z', '2026-09-03T12:00:00Z', NULL),
    (2, 4, 45, 'Retazos Circulares', 'Bolsos, cojines y chaquetas hechos con retazos de tela y ropa en desuso. Cada pieza es única y nace de la costura local de Valparaíso.', 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=800&q=80', 'APPROVED', 1, '2026-09-05T12:00:00Z', NULL, '2026-09-02T12:00:00Z', '2026-09-05T12:00:00Z', NULL),
    (3, 5, 219, 'Ropero Compartido', 'Arriendo de ropa de fiesta y venta de ropa de segunda mano seleccionada en Concepción. Vestir bien sin comprar nuevo.', 'https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?auto=format&fit=crop&w=800&q=80', 'APPROVED', 1, '2026-09-08T12:00:00Z', NULL, '2026-09-04T12:00:00Z', '2026-09-08T12:00:00Z', NULL),
    (4, 6, 252, 'Taller Telar Sur', 'Talleres de telar y bordado en Temuco para aprender a darle una segunda vida a las telas.', 'https://images.unsplash.com/photo-1606760227091-3dd870d97f1d?auto=format&fit=crop&w=800&q=80', 'PENDING', 1, NULL, NULL, '2026-09-20T12:00:00Z', '2026-09-20T12:00:00Z', NULL),
    (5, 7, 296, 'Moda Sin Fin', 'Ropa nueva a partir de telas recicladas.', 'https://images.unsplash.com/photo-1558769132-cb1aea458c5e?auto=format&fit=crop&w=800&q=80', 'REJECTED', 1, NULL, NULL, '2026-09-10T12:00:00Z', '2026-09-10T12:00:00Z', NULL)
ON CONFLICT (business_id) DO NOTHING;

INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (1, 4, 1), (1, 5, 2)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 1);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (2, 2, 1), (2, 6, 2)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 2);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (3, 1, 1), (3, 7, 2)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 3);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (4, 3, 1)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 4);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (5, 6, 1)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 5);

INSERT INTO business_review (review_id, business_id, reviewer_id, review_round, decision, rejection_reason, comment, reviewed_at) VALUES
    (1, 1, 1, 1, 'APPROVED', NULL, NULL, '2026-09-02T12:00:00Z'),
    (2, 1, 2, 1, 'APPROVED', NULL, 'Muy buena propuesta', '2026-09-03T12:00:00Z'),
    (3, 2, 1, 1, 'APPROVED', NULL, NULL, '2026-09-04T12:00:00Z'),
    (4, 2, 2, 1, 'APPROVED', NULL, NULL, '2026-09-05T12:00:00Z'),
    (5, 3, 2, 1, 'APPROVED', NULL, NULL, '2026-09-07T12:00:00Z'),
    (6, 3, 1, 1, 'APPROVED', NULL, 'Linda ficha', '2026-09-08T12:00:00Z'),
    (7, 4, 1, 1, 'APPROVED', NULL, 'Falta que el segundo voto confirme', '2026-09-21T12:00:00Z'),
    (8, 5, 2, 1, 'REJECTED', 'UNCLEAR_DESCRIPTION', 'La descripción no explica cómo reutilizan las telas. Agrega más detalle y vuelve a enviarla.', '2026-09-11T12:00:00Z')
ON CONFLICT (review_id) DO NOTHING;

INSERT INTO product (product_id, business_id, title, description, price, status, created_at, updated_at, deleted_at) VALUES
    (1, 1, 'Reparación de jeans', 'Cambiamos cierres, cosemos rotos y reforzamos rodillas. Entrega en 5 días hábiles.', 8000, 'ACTIVE', '2026-09-04T12:00:00Z', '2026-09-04T12:00:00Z', NULL),
    (2, 1, 'Parche bordado a mano', 'Parches bordados para cubrir manchas o desgastes. Elige el diseño.', 3500, 'ACTIVE', '2026-09-06T12:00:00Z', '2026-09-06T12:00:00Z', NULL),
    (3, 1, 'Ajuste de pantalón', 'Largo, basta o cintura. Cotizamos según la prenda.', NULL, 'ACTIVE', '2026-09-07T12:00:00Z', '2026-09-07T12:00:00Z', NULL),
    (4, 2, 'Tote bag de retazos', 'Bolso de tela con asas reforzadas, hecho con retazos de tapicería.', 12000, 'ACTIVE', '2026-09-06T12:00:00Z', '2026-09-06T12:00:00Z', NULL),
    (5, 2, 'Chaqueta patchwork', 'Chaqueta única hecha con retazos de mezclilla y lino. Talla M.', 45000, 'ACTIVE', '2026-09-09T12:00:00Z', '2026-09-09T12:00:00Z', NULL),
    (6, 2, 'Cojín de retazos', 'Cojín de 40x40 cm. Por ahora sin stock.', 9000, 'HIDDEN', '2026-09-10T12:00:00Z', '2026-09-10T12:00:00Z', NULL),
    (7, 3, 'Arriendo vestido de fiesta', 'Vestido largo verde, talla 38. Arriendo por 3 días.', 25000, 'ACTIVE', '2026-09-09T12:00:00Z', '2026-09-09T12:00:00Z', NULL),
    (8, 3, 'Abrigo de segunda mano', 'Abrigo de lana en excelente estado, talla L.', 18000, 'ACTIVE', '2026-09-11T12:00:00Z', '2026-09-11T12:00:00Z', NULL),
    (9, 4, 'Taller de bordado (4 clases)', 'Aprende puntos básicos y crea tu propio parche. Incluye materiales.', 15000, 'ACTIVE', '2026-09-22T12:00:00Z', '2026-09-22T12:00:00Z', NULL)
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO product_photo (photo_id, product_id, url, sort_order, created_at) VALUES
    (1, 1, 'https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=800&q=80', 1, '2026-09-04T12:00:00Z'),
    (2, 1, 'https://images.unsplash.com/photo-1582552938357-32b906df40cb?auto=format&fit=crop&w=800&q=80', 2, '2026-09-04T12:00:00Z'),
    (3, 2, 'https://images.unsplash.com/photo-1604014237800-1c9102c219da?auto=format&fit=crop&w=800&q=80', 1, '2026-09-06T12:00:00Z'),
    (4, 3, 'https://images.unsplash.com/photo-1593030761757-71fae45fa0e7?auto=format&fit=crop&w=800&q=80', 1, '2026-09-07T12:00:00Z'),
    (5, 4, 'https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=800&q=80', 1, '2026-09-06T12:00:00Z'),
    (6, 4, 'https://images.unsplash.com/photo-1597484661643-2f5fef640dd1?auto=format&fit=crop&w=800&q=80', 2, '2026-09-06T12:00:00Z'),
    (7, 4, 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=800&q=80', 3, '2026-09-06T12:00:00Z'),
    (8, 5, 'https://images.unsplash.com/photo-1576995853123-5a10305d93c0?auto=format&fit=crop&w=800&q=80', 1, '2026-09-09T12:00:00Z'),
    (9, 5, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=800&q=80', 2, '2026-09-09T12:00:00Z'),
    (10, 5, 'https://images.unsplash.com/photo-1604176354204-9268737828e4?auto=format&fit=crop&w=800&q=80', 3, '2026-09-09T12:00:00Z'),
    (11, 6, 'https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?auto=format&fit=crop&w=800&q=80', 1, '2026-09-10T12:00:00Z'),
    (12, 7, 'https://images.unsplash.com/photo-1566174053879-31528523f8ae?auto=format&fit=crop&w=800&q=80', 1, '2026-09-09T12:00:00Z'),
    (13, 7, 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=800&q=80', 2, '2026-09-09T12:00:00Z'),
    (14, 8, 'https://images.unsplash.com/photo-1544441893-675973e31985?auto=format&fit=crop&w=800&q=80', 1, '2026-09-11T12:00:00Z'),
    (15, 8, 'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=800&q=80', 2, '2026-09-11T12:00:00Z'),
    (16, 9, 'https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?auto=format&fit=crop&w=800&q=80', 1, '2026-09-22T12:00:00Z')
ON CONFLICT (photo_id) DO NOTHING;

INSERT INTO conversation (conversation_id, initiator_id, business_id, created_at) VALUES
    (1, 8, 1, '2026-09-12T15:00:00Z'),
    (2, 9, 2, '2026-09-14T10:00:00Z'),
    (3, 8, 3, '2026-09-15T10:00:00Z')
ON CONFLICT (conversation_id) DO NOTHING;

INSERT INTO message (message_id, conversation_id, sender_id, client_uuid, content, sent_at, read_at) VALUES
    (1, 1, 8, '624ceaa7-be04-5633-8dd8-fdc3d41a9c4b', 'Hola, ¿arreglan cierres de chaquetas de cuero?', '2026-09-12T15:00:00Z', '2026-09-12T16:00:00Z'),
    (2, 1, 3, '28ccb839-7539-5a85-bed6-54b80b24e0c7', '¡Hola Martín! Sí, los cambiamos. Depende del largo, ¿me mandas una foto?', '2026-09-12T16:00:00Z', '2026-09-12T17:00:00Z'),
    (3, 1, 8, 'cd9f912b-8299-5e7b-9773-801542e53ab6', 'Claro, te la envío mañana. ¿Cuánto demora?', '2026-09-12T18:00:00Z', '2026-09-12T19:00:00Z'),
    (4, 1, 3, '02ade0c3-68e3-5d85-bca1-a9a60947cda6', 'Entre 5 y 7 días hábiles. Te espero.', '2026-09-13T09:00:00Z', NULL),
    (5, 2, 9, 'df09fc60-f738-56c9-a751-270bd510a56b', 'Hola! ¿La chaqueta patchwork sigue disponible?', '2026-09-14T10:00:00Z', '2026-09-14T11:00:00Z'),
    (6, 2, 4, 'c0ead8f5-86ef-5ca8-ada7-32bac3d567fc', 'Hola Sofi, sí, está disponible. Es talla M.', '2026-09-14T11:00:00Z', '2026-09-14T12:00:00Z'),
    (7, 2, 9, '60bf6484-1724-502b-a570-946f91a40650', 'Perfecto, ¿puedo pasar a verla este fin de semana?', '2026-09-14T13:00:00Z', NULL),
    (8, 3, 8, 'f84ce730-be3f-5674-85b0-af0c5871ed97', 'Buenas, ¿tienen el vestido verde para el 20 de octubre?', '2026-09-15T10:00:00Z', '2026-09-15T11:00:00Z'),
    (9, 3, 5, 'c502b0a1-7c7a-5d82-8d96-b35594106aca', 'Hola Martín, está libre esa fecha. Te lo reservo.', '2026-09-15T12:00:00Z', '2026-09-15T13:00:00Z')
ON CONFLICT (message_id) DO NOTHING;

INSERT INTO outreach_content (content_id, author_id, type, title, body, image_url, event_at, event_location, created_at, updated_at, deleted_at) VALUES
    (1, 1, 'TEXT', NULL, 'Bienvenidas a Textil Circular, el directorio de emprendimientos que le dan una segunda vida a la ropa y a las telas.', NULL, NULL, NULL, '2026-09-01T12:00:00Z', '2026-09-01T12:00:00Z', NULL),
    (2, 1, 'ARTICLE', '5 formas de alargar la vida de tu ropa', '1. Lava en frío. 2. Repara antes de que se agrande el rasgado. 3. Intercambia con amigas. 4. Dona lo que no uses. 5. Compra de segunda mano antes que nuevo.', 'https://images.unsplash.com/photo-1558769132-cb1aea458c5e?auto=format&fit=crop&w=1000&q=80', NULL, NULL, '2026-09-08T12:00:00Z', '2026-09-08T12:00:00Z', NULL),
    (3, 1, 'IMAGE', NULL, NULL, 'https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=1000&q=80', NULL, NULL, '2026-09-15T12:00:00Z', '2026-09-15T12:00:00Z', NULL),
    (4, 1, 'EVENT', 'Feria de segunda mano', 'Más de 20 emprendimientos de la región metropolitana.', 'https://images.unsplash.com/photo-1517677208171-0bc6725a3e60?auto=format&fit=crop&w=1000&q=80', '2026-11-14T14:00:00Z', 'Parque Bustamante, Providencia', '2026-09-18T12:00:00Z', '2026-09-18T12:00:00Z', NULL),
    (5, 1, 'TEXT', NULL, 'Este mes sumamos 3 emprendimientos nuevos al directorio. ¡Descúbrelos!', NULL, NULL, NULL, '2026-09-25T12:00:00Z', '2026-09-25T12:00:00Z', NULL)
ON CONFLICT (content_id) DO NOTHING;
