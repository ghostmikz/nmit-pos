SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_recent_sales;
DELIMITER //
CREATE PROCEDURE sp_get_recent_sales(
    IN p_limit INT
)
BEGIN
    SELECT
        receipt_number,
        cashier_name,
        payment_method,
        total,
        is_refunded,
        created_at
    FROM view_sales_report
    ORDER BY created_at DESC
    LIMIT p_limit;
END //
DELIMITER ;
