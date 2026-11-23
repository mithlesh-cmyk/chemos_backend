-- Manual deployment prerequisite: adds file_no to physical_stocks.
-- Production runs spring.jpa.hibernate.ddl-auto=validate, so Hibernate never adds this column.
-- Run by hand, once per database, BEFORE deploying the code that maps PhysicalStock.fileNo.
BEGIN;

ALTER TABLE physical_stocks
    ADD COLUMN IF NOT EXISTS file_no varchar(255);

-- Backfill existing rows from the purchase they belong to.
UPDATE physical_stocks ps
SET file_no = p.file_no
FROM purchases p
WHERE p.id = ps.purchase_id
  AND ps.file_no IS NULL;

COMMIT;
