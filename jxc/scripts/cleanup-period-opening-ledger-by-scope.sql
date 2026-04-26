-- 周期期初库存重建前清理脚本。
-- 不要直接整体执行。先把下面两个占位条件替换成目标作用域，再在事务中人工执行。
-- 示例：scope_type = 'STORE' and scope_id = 1

BEGIN;

-- 1. 清理旧“仓库期初”口径数据。
DELETE FROM inventory_warehouse_opening_balance_line
WHERE header_id IN (
    SELECT id
    FROM inventory_warehouse_opening_balance
    WHERE scope_type = 'STORE'
      AND scope_id = 1
);

DELETE FROM inventory_warehouse_opening_balance
WHERE scope_type = 'STORE'
  AND scope_id = 1;

-- 2. 清理新期初草稿/已审数据，准备重新建账。
DELETE FROM inventory_period_opening_line
WHERE header_id IN (
    SELECT id
    FROM inventory_period_opening
    WHERE scope_type = 'STORE'
      AND scope_id = 1
);

DELETE FROM inventory_period_opening
WHERE scope_type = 'STORE'
  AND scope_id = 1;

-- 3. 清理库存账本和旧单据生成的脏流水。
DELETE FROM inventory_transaction
WHERE scope_type = 'STORE'
  AND scope_id = 1;

DELETE FROM inventory_balance
WHERE scope_type = 'STORE'
  AND scope_id = 1;

-- 执行后必须先录入并审核新的首个周期初，再重新跑采购入库、出库、盘点等业务链路。
-- 检查无误后执行 COMMIT；需要放弃时执行 ROLLBACK。
-- COMMIT;
-- ROLLBACK;
