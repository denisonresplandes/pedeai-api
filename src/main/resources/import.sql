-- INSERTS KITCHEN
INSERT INTO tb_kitchen(id, name) VALUES (1, 'Indiana');
INSERT INTO tb_kitchen(id, name) VALUES (2, 'Tailandesa');

-- INSERTS RESTAURANT
INSERT INTO tb_restaurant(id, name, kitchen_id) VALUES (1, 'Ilha Verde', 1);
INSERT INTO tb_restaurant(id, name, kitchen_id) VALUES (2, 'Quero Mais', 1);
INSERT INTO tb_restaurant(id, name, kitchen_id) VALUES (3, 'Tuk Tuk Comida Tailandesa', 2);

-- INSERTS RESTAURANT PAYMENT METHOD
INSERT INTO tb_restaurant_payment_method(restaurant_id, payment_method) VALUES (1, 'CASH');
INSERT INTO tb_restaurant_payment_method(restaurant_id, payment_method) VALUES (2, 'CASH');
INSERT INTO tb_restaurant_payment_method(restaurant_id, payment_method) VALUES (3, 'CASH');