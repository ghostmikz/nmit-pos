DELIMITER //
CREATE PROCEDURE sp_delete_payment_method(IN p_id INT)
BEGIN
    IF EXISTS (SELECT 1 FROM sale_payments WHERE payment_method_id = p_id LIMIT 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot delete: this payment method has associated sales';
    END IF;
    DELETE FROM payment_methods WHERE id = p_id;
    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Payment method not found';
    END IF;
END //
DELIMITER ;
