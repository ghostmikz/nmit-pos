SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_create_sale;
DELIMITER //
CREATE PROCEDURE sp_create_sale(
    IN p_receipt_number VARCHAR(30),
    IN p_user_id        INT,
    IN p_subtotal       DECIMAL(10,2),
    IN p_total          DECIMAL(10,2),
    IN p_items          JSON,
    IN p_payments       JSON
)
BEGIN
    DECLARE v_sale_id           INT;
    DECLARE v_product_id        INT;
    DECLARE v_product_name      VARCHAR(150);
    DECLARE v_quantity          INT;
    DECLARE v_unit_price        DECIMAL(10,2);
    DECLARE v_item_sub          DECIMAL(10,2);
    DECLARE v_stock             INT;
    DECLARE v_pay_method_id     INT;
    DECLARE v_pay_amount        DECIMAL(10,2);
    DECLARE v_i                 INT DEFAULT 0;
    DECLARE v_item_count        INT;
    DECLARE v_pay_count         INT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;

    SET v_pay_method_id = JSON_UNQUOTE(JSON_EXTRACT(p_payments, '$[0].payment_method_id'));

    INSERT INTO sales (receipt_number, user_id, payment_method_id, subtotal, total)
    VALUES (p_receipt_number, p_user_id, v_pay_method_id, p_subtotal, p_total);

    SET v_sale_id    = LAST_INSERT_ID();
    SET v_item_count = JSON_LENGTH(p_items);

    WHILE v_i < v_item_count DO
        SET v_product_id   = JSON_UNQUOTE(JSON_EXTRACT(p_items, CONCAT('$[', v_i, '].product_id')));
        SET v_product_name = JSON_UNQUOTE(JSON_EXTRACT(p_items, CONCAT('$[', v_i, '].product_name')));
        SET v_quantity     = JSON_UNQUOTE(JSON_EXTRACT(p_items, CONCAT('$[', v_i, '].quantity')));
        SET v_unit_price   = JSON_UNQUOTE(JSON_EXTRACT(p_items, CONCAT('$[', v_i, '].unit_price')));
        SET v_item_sub     = JSON_UNQUOTE(JSON_EXTRACT(p_items, CONCAT('$[', v_i, '].subtotal')));

        SELECT stock_quantity INTO v_stock FROM products WHERE id = v_product_id FOR UPDATE;
        IF v_stock < v_quantity THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient stock';
        END IF;

        INSERT INTO sale_items (sale_id, product_id, product_name, quantity, unit_price, subtotal)
        VALUES (v_sale_id, v_product_id, v_product_name, v_quantity, v_unit_price, v_item_sub);

        UPDATE products SET stock_quantity = stock_quantity - v_quantity WHERE id = v_product_id;
        SET v_i = v_i + 1;
    END WHILE;

    SET v_i = 0;
    SET v_pay_count = JSON_LENGTH(p_payments);
    WHILE v_i < v_pay_count DO
        SET v_pay_method_id = JSON_UNQUOTE(JSON_EXTRACT(p_payments, CONCAT('$[', v_i, '].payment_method_id')));
        SET v_pay_amount    = JSON_UNQUOTE(JSON_EXTRACT(p_payments, CONCAT('$[', v_i, '].amount')));
        INSERT INTO sale_payments (sale_id, payment_method_id, amount)
        VALUES (v_sale_id, v_pay_method_id, v_pay_amount);
        SET v_i = v_i + 1;
    END WHILE;

    COMMIT;
    SELECT v_sale_id AS sale_id;
END//
DELIMITER ;
