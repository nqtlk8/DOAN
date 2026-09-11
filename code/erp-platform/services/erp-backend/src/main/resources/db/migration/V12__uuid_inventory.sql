-- 1. Add UUID column
ALTER TABLE stock_movement ADD COLUMN uuid_id UUID;
ALTER TABLE cost_layer ADD COLUMN uuid_id UUID;

-- 2. Generate UUID cho existing rows
UPDATE stock_movement SET uuid_id = gen_random_uuid() WHERE uuid_id IS NULL;
UPDATE cost_layer SET uuid_id = gen_random_uuid() WHERE uuid_id IS NULL;

-- 3. Add new FK column tại cost_layer
ALTER TABLE cost_layer ADD COLUMN inbound_movement_uuid UUID;

-- 4. Backfill mapping từ old movement id -> new movement UUID
UPDATE cost_layer cl
SET inbound_movement_uuid = sm.uuid_id
FROM stock_movement sm
WHERE cl.inbound_movement_id = sm.id;

-- 5. Validate mapping
-- If any cost_layer had inbound_movement_id but inbound_movement_uuid is null, fail.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM cost_layer
        WHERE inbound_movement_id IS NOT NULL AND inbound_movement_uuid IS NULL
    ) THEN
        RAISE EXCEPTION 'Backfill failed: Some cost_layer records missing inbound_movement_uuid';
    END IF;
END $$;

-- 6. Drop old FK
ALTER TABLE cost_layer DROP CONSTRAINT IF EXISTS cost_layer_inbound_movement_id_fkey;

-- 7. Switch primary key
ALTER TABLE stock_movement DROP CONSTRAINT IF EXISTS stock_movement_pkey CASCADE;
ALTER TABLE cost_layer DROP CONSTRAINT IF EXISTS cost_layer_pkey CASCADE;

-- 8. Switch foreign key
-- (Will add the actual constraint in step 11)

-- 9. Drop old integer columns
ALTER TABLE cost_layer DROP COLUMN inbound_movement_id;
ALTER TABLE stock_movement DROP COLUMN id CASCADE;
ALTER TABLE cost_layer DROP COLUMN id CASCADE;

-- 10. Rename UUID columns
ALTER TABLE stock_movement RENAME COLUMN uuid_id TO id;
ALTER TABLE cost_layer RENAME COLUMN uuid_id TO id;
ALTER TABLE cost_layer RENAME COLUMN inbound_movement_uuid TO inbound_movement_id;

-- 11. Add NOT NULL / PK / FK constraints
ALTER TABLE stock_movement ALTER COLUMN id SET NOT NULL;
ALTER TABLE cost_layer ALTER COLUMN id SET NOT NULL;

ALTER TABLE stock_movement ADD PRIMARY KEY (id);
ALTER TABLE cost_layer ADD PRIMARY KEY (id);

ALTER TABLE cost_layer ADD CONSTRAINT fk_cost_layer_movement FOREIGN KEY (inbound_movement_id) REFERENCES stock_movement(id);
