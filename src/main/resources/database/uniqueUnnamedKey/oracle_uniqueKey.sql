SELECT
    c.CONSTRAINT_NAME
FROM
    user_CONSTRAINTS C
WHERE
    LOWER(c.TABLE_NAME) = LOWER( ? )
    AND c.CONSTRAINT_TYPE = 'U'