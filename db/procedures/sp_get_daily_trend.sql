SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_daily_trend;
DELIMITER //
CREATE PROCEDURE sp_get_daily_trend(
    IN p_start_date DATE,
    IN p_end_date   DATE
)
BEGIN
    SELECT
        DATE(created_at)            AS sale_date,
        COUNT(*)                    AS total_transactions,
        COALESCE(SUM(total), 0)     AS total_revenue
    FROM view_sales_report
    WHERE (p_start_date IS NULL OR created_at >= p_start_date)
      AND (p_end_date   IS NULL OR created_at <  DATE_ADD(p_end_date, INTERVAL 1 DAY))
    GROUP BY DATE(created_at)
    ORDER BY sale_date ASC;
END //
DELIMITER ;
