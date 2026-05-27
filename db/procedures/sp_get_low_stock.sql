SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_low_stock;
DELIMITER //
CREATE PROCEDURE sp_get_low_stock(IN p_limit INT)
BEGIN
    SELECT product_name, category_name, stock_quantity, unit
    FROM view_low_stock
    LIMIT p_limit;
END //
DELIMITER ;
