-- Function to validate new user registration
CREATE OR REPLACE FUNCTION validate_new_user_registration()
RETURNS TRIGGER AS $$
BEGIN
    -- Only check constraints on INSERT (new records)
    IF (TG_OP = 'INSERT') THEN
        -- Password Length Check (Assume plain text length check or hashed length)
        -- Note: If passwords are hashed before DB, this length might need adjustment 
        -- or we check length in Java before hashing.
        IF LENGTH(NEW.password) < 8 THEN
            RAISE EXCEPTION 'Password must be at least 8 characters long';
        END IF;

        -- Phone Number Format Check (Simplified Turkish format check)
        -- Starts with 05 or 5 or +905 and followed by digits
        IF NOT (NEW.phone_number ~ '^(05|5)\d{9}$|^\+905\d{9}$') THEN
            RAISE EXCEPTION 'Invalid phone number format for new registration: %', NEW.phone_number;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to run before INSERT on users table
-- We drop if exists to allow re-running this script
DROP TRIGGER IF EXISTS trg_validate_user_registration ON users;

CREATE TRIGGER trg_validate_user_registration
BEFORE INSERT ON users
FOR EACH ROW
EXECUTE FUNCTION validate_new_user_registration();
