SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_dashboard_summary;
DELIMITER //
CREATE PROCEDURE sp_get_dashboard_summary(
    IN p_start_date DATE,
    IN p_end_date   DATE
)
BEGIN
    SELECT
        COUNT(*)                    AS total_transactions,
        COALESCE(SUM(total), 0)     AS total_revenue,
        COALESCE(AVG(total), 0)     AS avg_sale_value
    FROM view_sales_report
    WHERE is_refunded = 0
      AND (p_start_date IS NULL OR created_at >= p_start_date)
      AND (p_end_date   IS NULL OR created_at <  DATE_ADD(p_end_date, INTERVAL 1 DAY));
END //
DELIMITER ;
