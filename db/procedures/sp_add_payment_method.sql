DELIMITER //
CREATE PROCEDURE sp_add_payment_method(IN p_name VARCHAR(50))
BEGIN
    IF p_name IS NULL OR TRIM(p_name) = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Method name cannot be empty';
    END IF;
    IF EXISTS (SELECT 1 FROM payment_methods WHERE LOWER(name) = LOWER(TRIM(p_name))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A payment method with this name already exists';
    END IF;
    INSERT INTO payment_methods (name) VALUES (TRIM(p_name));
    SELECT LAST_INSERT_ID() AS id;
END //
DELIMITER ;
