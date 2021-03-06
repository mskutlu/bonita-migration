SELECT
    c.CONSTRAINT_NAME
FROM
    information_schema.TABLE_CONSTRAINTS c
WHERE
    LOWER( c.TABLE_NAME ) = LOWER( ? )
    AND c.CONSTRAINT_TYPE = 'UNIQUE'
