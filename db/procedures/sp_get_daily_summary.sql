SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_daily_summary;
DELIMITER //
CREATE PROCEDURE sp_get_daily_summary()
BEGIN
    SELECT sale_date, total_transactions, total_revenue
    FROM view_daily_sales;
END//
DELIMITER ;
