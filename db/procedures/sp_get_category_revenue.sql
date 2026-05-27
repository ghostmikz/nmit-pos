SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_category_revenue;
DELIMITER //
CREATE PROCEDURE sp_get_category_revenue()
BEGIN
    SELECT category_name, SUM(total_revenue) AS totalRevenue
    FROM view_top_products
    GROUP BY category_name
    ORDER BY totalRevenue DESC;
END //
DELIMITER ;
