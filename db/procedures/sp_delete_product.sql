SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_delete_product;
DELIMITER //
CREATE PROCEDURE sp_delete_product(IN p_product_id INT)
BEGIN
    UPDATE products
    SET is_active = FALSE
    WHERE id = p_product_id;
    SELECT ROW_COUNT() AS rows_affected;
END//
DELIMITER ;
