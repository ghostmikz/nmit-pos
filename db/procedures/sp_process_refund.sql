SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_process_refund;
DELIMITER //
CREATE PROCEDURE sp_process_refund(
    IN p_sale_id      INT,
    IN p_user_id      INT,
    IN p_reason       TEXT,
    IN p_refund_amount DECIMAL(10,2)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    IF NOT EXISTS (
        SELECT 1 FROM sales
        WHERE id = p_sale_id AND is_refunded = FALSE
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Sale not found or already refunded';
    END IF;

    UPDATE sales
    SET is_refunded = TRUE
    WHERE id = p_sale_id;

    UPDATE products p
    JOIN sale_items si ON p.id = si.product_id
    SET p.stock_quantity = p.stock_quantity + si.quantity
    WHERE si.sale_id = p_sale_id;

    INSERT INTO refunds (sale_id, user_id, reason, refund_amount)
    VALUES (p_sale_id, p_user_id, p_reason, p_refund_amount);

    COMMIT;
    SELECT 'Refund processed successfully' AS message;
END//
DELIMITER ;
