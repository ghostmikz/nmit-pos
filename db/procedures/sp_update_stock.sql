SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_update_stock;
DELIMITER //
CREATE PROCEDURE sp_update_stock(
    IN p_product_id INT,
    IN p_quantity   INT
)
BEGIN
    DECLARE v_current_stock INT DEFAULT NULL;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT stock_quantity INTO v_current_stock
    FROM products
    WHERE id = p_product_id
    FOR UPDATE;

    IF v_current_stock IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Product not found';
    END IF;

    IF v_current_stock + p_quantity < 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Insufficient stock for adjustment';
    END IF;

    UPDATE products
    SET stock_quantity = stock_quantity + p_quantity
    WHERE id = p_product_id;

    COMMIT;

    SELECT stock_quantity AS new_stock
    FROM products
    WHERE id = p_product_id;
END//
DELIMITER ;
