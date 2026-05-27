SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_top_products;
DELIMITER //
CREATE PROCEDURE sp_get_top_products(IN p_limit INT)
BEGIN
    SELECT product_name, category_name, total_sold, total_revenue
    FROM view_top_products
    LIMIT p_limit;
END //
DELIMITER ;
