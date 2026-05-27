SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_weekly_sales;
DELIMITER //
CREATE PROCEDURE sp_get_weekly_sales()
BEGIN
    SELECT sale_date, total_transactions, total_revenue
    FROM view_weekly_sales
    ORDER BY sale_date ASC;
END //
DELIMITER ;
