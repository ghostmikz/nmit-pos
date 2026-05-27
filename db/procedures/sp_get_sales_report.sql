SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_sales_report;
DELIMITER //
CREATE PROCEDURE sp_get_sales_report(
    IN p_start_date DATE,
    IN p_end_date   DATE
)
BEGIN
    SELECT s.id,
           s.receipt_number,
           u.full_name AS cashier_name,
           (SELECT GROUP_CONCAT(pm.name ORDER BY sp.id SEPARATOR ' + ')
            FROM sale_payments sp
            JOIN payment_methods pm ON pm.id = sp.payment_method_id
            WHERE sp.sale_id = s.id) AS payment_method,
           s.subtotal,
           s.total,
           s.is_refunded,
           s.created_at,
           s.notes
    FROM sales s
    JOIN users u ON u.id = s.user_id
    WHERE (p_start_date IS NULL OR s.created_at >= p_start_date)
      AND (p_end_date   IS NULL OR s.created_at <  DATE_ADD(p_end_date, INTERVAL 1 DAY))
    ORDER BY s.created_at DESC;
END//
DELIMITER ;
