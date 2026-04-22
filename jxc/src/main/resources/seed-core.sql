-- 默认密码：123654
-- 当前以 SHA-256 十六进制串初始化，后续接入正式认证时可替换为统一密码编码方案。

ALTER TABLE sys_role
DROP CONSTRAINT IF EXISTS uk_sys_role_tenant_code;

ALTER TABLE sys_role
ADD CONSTRAINT uk_sys_role_tenant_code UNIQUE (tenant_group_id, role_code);

ALTER TABLE sys_role
ADD COLUMN IF NOT EXISTS builtin BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE sys_role
SET builtin = TRUE
WHERE role_code = 'PLATFORM_SUPER_ADMIN';

UPDATE sys_role role
SET builtin = TRUE
WHERE role.tenant_group_id = 0
  AND role.role_type IN ('GROUP', 'STORE');

UPDATE sys_role role
SET builtin = TRUE
WHERE role.tenant_group_id > 0
  AND role.role_type IN ('GROUP', 'STORE')
  AND EXISTS (
      SELECT 1
      FROM sys_role template
      WHERE template.tenant_group_id = 0
        AND template.builtin = TRUE
        AND template.role_code = role.role_code
        AND template.role_type = role.role_type
  );

WITH ranked AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY user_id, role_id, scope_type, scope_id
               ORDER BY CASE WHEN status = 'ENABLED' THEN 0 ELSE 1 END,
                        assigned_at DESC NULLS LAST,
                        id DESC
           ) AS rn
    FROM sys_user_role_rel
)
DELETE FROM sys_user_role_rel rel
USING ranked
WHERE rel.id = ranked.id
  AND ranked.rn > 1;

WITH store_ranked AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY user_id, scope_id
               ORDER BY CASE WHEN status = 'ENABLED' THEN 0 ELSE 1 END,
                        assigned_at DESC NULLS LAST,
                        id DESC
           ) AS rn
    FROM sys_user_role_rel
    WHERE scope_type = 'STORE'
)
DELETE FROM sys_user_role_rel rel
USING store_ranked
WHERE rel.id = store_ranked.id
  AND store_ranked.rn > 1;

ALTER TABLE sys_user_role_rel
DROP CONSTRAINT IF EXISTS uk_sys_user_role_scope;

ALTER TABLE sys_user_role_rel
ADD CONSTRAINT uk_sys_user_role_scope UNIQUE (user_id, role_id, scope_type, scope_id);

DROP INDEX IF EXISTS uk_sys_user_role_store_scope;

CREATE UNIQUE INDEX uk_sys_user_role_store_scope
    ON sys_user_role_rel (user_id, scope_id)
    WHERE scope_type = 'STORE';

WITH dict_type_seed(dict_code, dict_name, category, sort_no, remark) AS (
    VALUES
        ('common.enabled_status', '通用启停状态', 'COMMON', 10, '平台全局启停状态'),
        ('common.yes_no', '通用是否', 'COMMON', 20, '通用是否枚举'),
        ('identity.role_type', '角色类型', 'IDENTITY', 100, '平台/集团/门店角色类型'),
        ('identity.data_scope_type', '角色数据范围', 'IDENTITY', 110, '角色数据权限范围'),
        ('identity.menu_type', '菜单资源类型', 'IDENTITY', 120, '菜单/按钮/API资源类型'),
        ('unit.type', '单位类型', 'ARCHIVE', 200, '标准单位/辅助单位'),
        ('warehouse.type', '仓库类型', 'ARCHIVE', 210, '仓库业务类型'),
        ('supplier.status', '供应商启停状态', 'ARCHIVE', 300, '供应商启停状态'),
        ('supplier.bind_status', '供应商绑定状态', 'ARCHIVE', 310, '供应商绑定状态'),
        ('supplier.source', '供应商来源', 'ARCHIVE', 320, '供应商来源'),
        ('supplier.supply_relation', '供应商供货关系', 'ARCHIVE', 330, '供应商供货关系'),
        ('supplier.scope_control', '供应商范围管控', 'ARCHIVE', 340, '供应商范围管控'),
        ('item.status', '物品启停状态', 'ARCHIVE', 400, '物品档案启停状态'),
        ('inventory.document_status', '库存单据状态', 'INVENTORY', 500, '库存与采购单据状态'),
        ('inventory.workflow_status', '库存流程状态', 'INVENTORY', 510, '库存单据流程状态'),
        ('inventory.check_range_type', '盘点范围类型', 'INVENTORY', 520, '盘点范围类型'),
        ('inventory.check_generation_status', '盘点生成状态', 'INVENTORY', 530, '多人盘点单生成状态'),
        ('workflow.definition_status', '流程定义状态', 'WORKFLOW', 600, '流程模板发布状态'),
        ('workflow.node_type', '流程节点类型', 'WORKFLOW', 610, '流程配置节点类型'),
        ('workflow.sign_mode', '流程会签方式', 'WORKFLOW', 620, '流程节点会签方式'),
        ('purchase.reconciliation_status', '采购对账状态', 'PURCHASE', 700, '采购单据对账状态'),
        ('purchase.invoice_status', '采购开票状态', 'PURCHASE', 710, '采购单据开票状态'),
        ('purchase.split_status', '采购分账状态', 'PURCHASE', 720, '采购单据分账状态'),
        ('document.print_status', '单据打印状态', 'COMMON', 800, '单据打印状态')
)
INSERT INTO sys_dict_type (dict_code, dict_name, category, status, builtin, sort_no, remark)
SELECT dict_code, dict_name, category, 'ENABLED', TRUE, sort_no, remark
FROM dict_type_seed
ON CONFLICT (dict_code) DO NOTHING;

WITH dict_item_seed(dict_code, item_key, item_code, item_label, sort_no) AS (
    VALUES
        ('common.enabled_status', 'ENABLED', 'ENABLED', '启用', 10),
        ('common.enabled_status', 'DISABLED', 'DISABLED', '停用', 20),
        ('common.yes_no', 'YES', '是', '是', 10),
        ('common.yes_no', 'NO', '否', '否', 20),
        ('identity.role_type', 'PLATFORM', 'PLATFORM', '平台', 10),
        ('identity.role_type', 'GROUP', 'GROUP', '集团', 20),
        ('identity.role_type', 'STORE', 'STORE', '门店', 30),
        ('identity.data_scope_type', 'ALL', 'ALL', '全部数据', 10),
        ('identity.data_scope_type', 'GROUP', 'GROUP', '集团数据', 20),
        ('identity.data_scope_type', 'STORE', 'STORE', '门店数据', 30),
        ('identity.data_scope_type', 'SELF', 'SELF', '本人数据', 40),
        ('identity.data_scope_type', 'CUSTOM', 'CUSTOM', '自定义数据', 50),
        ('identity.menu_type', 'DIRECTORY', 'DIRECTORY', '目录', 10),
        ('identity.menu_type', 'MENU', 'MENU', '菜单', 20),
        ('identity.menu_type', 'BUTTON', 'BUTTON', '按钮', 30),
        ('identity.menu_type', 'API', 'API', '接口', 40),
        ('unit.type', 'STANDARD', 'STANDARD', '标准单位', 10),
        ('unit.type', 'AUXILIARY', 'AUXILIARY', '辅助单位', 20),
        ('warehouse.type', 'PRODUCTION_DEPARTMENT', '出品及生产部门', '出品及生产部门', 10),
        ('warehouse.type', 'ADMIN_DEPARTMENT', '行政部门', '行政部门', 20),
        ('warehouse.type', 'NORMAL_WAREHOUSE', '普通仓库', '普通仓库', 30),
        ('supplier.status', 'ENABLED', '启用', '启用', 10),
        ('supplier.status', 'DISABLED', '停用', '停用', 20),
        ('supplier.bind_status', 'BOUND', '已绑定', '已绑定', 10),
        ('supplier.bind_status', 'UNBOUND', '未绑定', '未绑定', 20),
        ('supplier.source', 'GROUP', '集团', '集团', 10),
        ('supplier.source', 'STORE', '门店', '门店', 20),
        ('supplier.supply_relation', 'YES', '有', '有', 10),
        ('supplier.supply_relation', 'NO', '无', '无', 20),
        ('supplier.scope_control', 'ENABLED', '开启', '开启', 10),
        ('supplier.scope_control', 'DISABLED', '关闭', '关闭', 20),
        ('item.status', 'ENABLED', '启用', '启用', 10),
        ('item.status', 'DISABLED', '停用', '停用', 20),
        ('inventory.document_status', 'DRAFT', '草稿', '草稿', 10),
        ('inventory.document_status', 'SUBMITTED', '已提交', '已提交', 20),
        ('inventory.document_status', 'APPROVED', '已审核', '已审核', 30),
        ('inventory.document_status', 'CLOSED', '已关闭', '已关闭', 40),
        ('inventory.workflow_status', 'NONE', 'NONE', '无流程', 10),
        ('inventory.workflow_status', 'RUNNING', 'RUNNING', '流程中', 20),
        ('inventory.workflow_status', 'COMPLETED', 'COMPLETED', '已完成', 30),
        ('inventory.workflow_status', 'REVOKED', 'REVOKED', '已撤回', 40),
        ('inventory.check_range_type', 'FULL_WAREHOUSE', 'FULL_WAREHOUSE', '全仓盘点', 10),
        ('inventory.check_range_type', 'PARTITION', 'PARTITION', '分区盘点', 20),
        ('inventory.check_range_type', 'SPECIFIC_ITEM', 'SPECIFIC_ITEM', '指定食材盘点', 30),
        ('inventory.check_generation_status', 'UNGENERATED', 'UNGENERATED', '未生成', 10),
        ('inventory.check_generation_status', 'GENERATED', 'GENERATED', '已生成', 20),
        ('workflow.definition_status', 'DRAFT', 'DRAFT', '草稿', 10),
        ('workflow.definition_status', 'PUBLISHED', 'PUBLISHED', '已发布', 20),
        ('workflow.node_type', 'START', 'START', '开始', 10),
        ('workflow.node_type', 'NORMAL', 'NORMAL', '审批节点', 20),
        ('workflow.node_type', 'CONDITION', 'CONDITION', '条件节点', 30),
        ('workflow.node_type', 'SUCCESS', 'SUCCESS', '通过结束', 40),
        ('workflow.node_type', 'FAIL', 'FAIL', '拒绝结束', 50),
        ('workflow.node_type', 'END', 'END', '结束', 60),
        ('workflow.sign_mode', 'OR', 'OR', '任一通过', 10),
        ('workflow.sign_mode', 'AND', 'AND', '全部通过', 20),
        ('purchase.reconciliation_status', 'UNRECONCILED', '未对账', '未对账', 10),
        ('purchase.reconciliation_status', 'PARTIAL_RECONCILED', '部分对账', '部分对账', 20),
        ('purchase.reconciliation_status', 'RECONCILED', '已对账', '已对账', 30),
        ('purchase.invoice_status', 'UNINVOICED', '未开票', '未开票', 10),
        ('purchase.invoice_status', 'PARTIAL_INVOICED', '部分开票', '部分开票', 20),
        ('purchase.invoice_status', 'INVOICED', '已开票', '已开票', 30),
        ('purchase.split_status', 'UNSPLIT', '未分账', '未分账', 10),
        ('purchase.split_status', 'SPLIT', '已分账', '已分账', 20),
        ('document.print_status', 'UNPRINTED', '未打印', '未打印', 10),
        ('document.print_status', 'PRINTED', '已打印', '已打印', 20)
)
INSERT INTO sys_dict_item (dict_type_id, parent_id, item_key, item_code, item_label, status, builtin, sort_no)
SELECT dict_type.id, NULL, seed.item_key, seed.item_code, seed.item_label, 'ENABLED', TRUE, seed.sort_no
FROM dict_item_seed seed
         JOIN sys_dict_type dict_type ON dict_type.dict_code = seed.dict_code
ON CONFLICT (dict_type_id, item_key) DO NOTHING;

WITH field_binding_seed(dict_code, table_name, column_name, remark) AS (
    VALUES
        ('common.enabled_status', 'sys_group', 'status', '集团状态'),
        ('common.enabled_status', 'sys_store', 'status', '门店状态'),
        ('common.enabled_status', 'sys_user', 'status', '用户状态'),
        ('common.enabled_status', 'sys_role', 'status', '角色状态'),
        ('common.enabled_status', 'sys_menu', 'status', '菜单状态'),
        ('common.enabled_status', 'sys_user_role_rel', 'status', '用户角色关系状态'),
        ('common.enabled_status', 'sys_store_admin_rel', 'status', '门店管理员关系状态'),
        ('common.enabled_status', 'sys_unit', 'status', '单位状态'),
        ('common.enabled_status', 'sys_warehouse', 'status', '仓库状态'),
        ('common.enabled_status', 'warehouse_item_rule', 'status', '仓库物品规则状态'),
        ('unit.type', 'sys_unit', 'unit_type', '单位类型'),
        ('warehouse.type', 'sys_warehouse', 'warehouse_type', '仓库类型'),
        ('supplier.status', 'supplier_profile', 'status', '供应商启停状态'),
        ('supplier.bind_status', 'supplier_profile', 'bind_status', '供应商绑定状态'),
        ('supplier.source', 'supplier_profile', 'source', '供应商来源'),
        ('supplier.supply_relation', 'supplier_profile', 'supply_relation', '供应商供货关系'),
        ('supplier.scope_control', 'supplier_profile', 'scope_control', '供应商范围管控'),
        ('item.status', 'item_category', 'status', '物品类别状态'),
        ('item.status', 'item_tag', 'status', '物品标签状态'),
        ('workflow.definition_status', 'workflow_definition_config', 'status', '流程定义状态'),
        ('inventory.document_status', 'inventory_purchase_inbound', 'status', '采购入库状态'),
        ('inventory.document_status', 'inventory_purchase_return_outbound', 'status', '采购退货出库状态'),
        ('inventory.document_status', 'inventory_department_picking', 'status', '部门领料状态'),
        ('inventory.document_status', 'inventory_department_return', 'status', '部门退料状态'),
        ('inventory.document_status', 'inventory_stock_transfer', 'status', '移库单状态'),
        ('inventory.document_status', 'inventory_stock_transfer_inbound', 'status', '移库入库状态'),
        ('inventory.document_status', 'inventory_department_transfer', 'status', '部门调拨状态'),
        ('inventory.document_status', 'inventory_damage_outbound', 'status', '报损出库状态'),
        ('inventory.document_status', 'inventory_other_inbound', 'status', '其他入库状态'),
        ('inventory.document_status', 'inventory_other_outbound', 'status', '其他出库状态'),
        ('inventory.document_status', 'inventory_production_inbound', 'status', '生产入库状态'),
        ('inventory.document_status', 'inventory_customer_sales_outbound', 'status', '客户销售出库状态'),
        ('inventory.document_status', 'inventory_customer_return_inbound', 'status', '客户退货入库状态'),
        ('inventory.document_status', 'inventory_warehouse_opening_balance', 'status', '期初库存状态'),
        ('inventory.document_status', 'inventory_store_transfer', 'status', '门店调拨状态'),
        ('inventory.document_status', 'inventory_stock_transfer_outbound', 'status', '移库出库状态'),
        ('inventory.workflow_status', 'inventory_purchase_inbound', 'workflow_status', '采购入库流程状态'),
        ('inventory.workflow_status', 'inventory_purchase_return_outbound', 'workflow_status', '采购退货出库流程状态'),
        ('inventory.workflow_status', 'inventory_department_picking', 'workflow_status', '部门领料流程状态'),
        ('inventory.workflow_status', 'inventory_department_return', 'workflow_status', '部门退料流程状态'),
        ('inventory.workflow_status', 'inventory_stock_transfer', 'workflow_status', '移库单流程状态'),
        ('inventory.workflow_status', 'inventory_stock_transfer_inbound', 'workflow_status', '移库入库流程状态'),
        ('inventory.workflow_status', 'inventory_department_transfer', 'workflow_status', '部门调拨流程状态'),
        ('inventory.workflow_status', 'inventory_damage_outbound', 'workflow_status', '报损出库流程状态'),
        ('inventory.workflow_status', 'inventory_other_inbound', 'workflow_status', '其他入库流程状态'),
        ('inventory.workflow_status', 'inventory_other_outbound', 'workflow_status', '其他出库流程状态'),
        ('inventory.workflow_status', 'inventory_production_inbound', 'workflow_status', '生产入库流程状态'),
        ('inventory.workflow_status', 'inventory_customer_sales_outbound', 'workflow_status', '客户销售出库流程状态'),
        ('inventory.workflow_status', 'inventory_customer_return_inbound', 'workflow_status', '客户退货入库流程状态'),
        ('inventory.workflow_status', 'inventory_warehouse_opening_balance', 'workflow_status', '期初库存流程状态'),
        ('inventory.workflow_status', 'inventory_store_transfer', 'workflow_status', '门店调拨流程状态'),
        ('inventory.workflow_status', 'inventory_stock_transfer_outbound', 'workflow_status', '移库出库流程状态')
)
INSERT INTO sys_dict_field_binding (dict_code, table_name, column_name, remark)
SELECT dict_code, table_name, column_name, remark
FROM field_binding_seed
ON CONFLICT (dict_code, table_name, column_name) DO NOTHING;

INSERT INTO sys_group (group_code, group_name, status, remark)
VALUES ('GP00001', '默认集团', 'ENABLED', '系统初始化集团')
ON CONFLICT DO NOTHING;

INSERT INTO sys_store (group_id, store_code, store_name, status, contact_name, contact_phone, address, remark)
VALUES (
    (SELECT id FROM sys_group WHERE group_code = 'GP00001'),
    'MD00001',
    '默认门店',
    'ENABLED',
    '门店联系人',
    '13800000003',
    '系统初始化门店地址',
    '系统初始化门店'
)
ON CONFLICT DO NOTHING;

UPDATE sys_store
SET store_code = 'MD00001'
WHERE store_code = 'DEFAULT_STORE'
  AND NOT EXISTS (
    SELECT 1 FROM sys_store s WHERE s.store_code = 'MD00001'
);

WITH workflow_process_seed(process_code, business_name) AS (
    VALUES
        ('PURCHASE_INBOUND', '采购入库流程'),
        ('PURCHASE_RETURN_OUTBOUND', '采购退货出库流程'),
        ('DEPARTMENT_PICKING', '部门领料流程'),
        ('DEPARTMENT_RETURN', '部门退料流程'),
        ('STOCK_TRANSFER', '移库单流程'),
        ('STOCK_TRANSFER_INBOUND', '移库入库流程'),
        ('DEPARTMENT_TRANSFER', '部门调拨流程'),
        ('DAMAGE_OUTBOUND', '报损出库流程'),
        ('OTHER_INBOUND', '其他入库流程'),
        ('OTHER_OUTBOUND', '其他出库流程'),
        ('PRODUCTION_INBOUND', '生产入库流程'),
        ('CUSTOMER_SALES_OUTBOUND', '客户销售出库流程'),
        ('CUSTOMER_RETURN_INBOUND', '客户退货入库流程')
)
INSERT INTO workflow_process_registry (scope_type, scope_id, process_code, business_name, created_by, updated_by)
SELECT 'GROUP',
       g.id,
       seed.process_code,
       seed.business_name,
       NULL,
       NULL
FROM workflow_process_seed seed
         CROSS JOIN (SELECT id FROM sys_group WHERE group_code = 'GP00001') g
ON CONFLICT (scope_type, scope_id, process_code) DO NOTHING;

-- ITEM_MASTER_SEED_START
-- 以下为模板数据源：平台内置角色模板、单位模板、类别模板、统计类型模板、标签模板；
-- 这些数据会被复制到新建的集团/门店真实业务数据中。
WITH item_scope_seed AS (
    SELECT 'PLATFORM'::VARCHAR(16) AS scope_type, 0::BIGINT AS scope_id
    UNION ALL
    SELECT 'GROUP'::VARCHAR(16), g.id
    FROM sys_group g
    UNION ALL
    SELECT 'STORE'::VARCHAR(16), s.id
    FROM sys_store s
),
unit_seed(unit_code, unit_name, unit_type, status, remark) AS (
    VALUES
        ('U001', '件', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U002', '箱', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U003', '袋', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U004', '个', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U005', '斤', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U006', '公斤', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U007', '克', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U008', '千克', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U009', '瓶', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U010', '包', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U011', '盒', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U012', '桶', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U013', '罐', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U014', '听', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U015', '板', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U016', '提', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U017', '卷', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U018', '张', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U019', '只', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U020', '根', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U021', '把', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U022', '支', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U023', '套', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U024', '双', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U025', '台', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U026', '米', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U027', '厘米', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U028', '升', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U029', '毫升', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U030', 'L', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U031', 'mL', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U032', '次', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U033', '杯', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U034', '串', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U035', '捆', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U036', '条', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U037', '块', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U038', '本', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U039', '组', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U040', '坛', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U041', 'kg', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U042', '份', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U043', '盘', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U044', '碗', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U045', '片', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U046', '颗', 'STANDARD', 'ENABLED', '系统初始化业务默认单位'),
        ('U047', '束', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U048', '扎', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位'),
        ('U049', '打', 'AUXILIARY', 'ENABLED', '系统初始化业务默认单位')
)
INSERT INTO sys_unit (scope_type, scope_id, unit_code, unit_name, unit_type, status, remark)
SELECT scope.scope_type,
       scope.scope_id,
       unit.unit_code,
       unit.unit_name,
       unit.unit_type,
       unit.status,
       unit.remark
FROM item_scope_seed scope
         CROSS JOIN unit_seed unit
ON CONFLICT DO NOTHING;

WITH item_scope_seed AS (
    SELECT 'PLATFORM'::VARCHAR(16) AS scope_type, 0::BIGINT AS scope_id
    UNION ALL
    SELECT 'GROUP'::VARCHAR(16), g.id
    FROM sys_group g
    UNION ALL
    SELECT 'STORE'::VARCHAR(16), s.id
    FROM sys_store s
),
category_seed(category_code, category_name, parent_category, status, remark) AS (
    VALUES
        ('WPLB000001', '生鲜食材', '物品类别', '启用', '系统初始化业务默认类别'),
        ('WPLB000002', '蔬菜', '生鲜食材', '启用', '系统初始化业务默认类别'),
        ('WPLB000003', '水果', '生鲜食材', '启用', '系统初始化业务默认类别'),
        ('WPLB000004', '肉类', '生鲜食材', '启用', '系统初始化业务默认类别'),
        ('WPLB000005', '水产', '生鲜食材', '启用', '系统初始化业务默认类别'),
        ('WPLB000006', '冻品半成品', '物品类别', '启用', '系统初始化业务默认类别'),
        ('WPLB000007', '冻品', '冻品半成品', '启用', '系统初始化业务默认类别'),
        ('WPLB000008', '半成品', '冻品半成品', '启用', '系统初始化业务默认类别'),
        ('WPLB000009', '预制菜', '冻品半成品', '启用', '系统初始化业务默认类别'),
        ('WPLB000010', '米面主食', '物品类别', '启用', '系统初始化业务默认类别'),
        ('WPLB000011', '河粉', '米面主食', '启用', '系统初始化业务默认类别'),
        ('WPLB000012', '面食', '米面主食', '启用', '系统初始化业务默认类别'),
        ('WPLB000013', '面点', '米面主食', '启用', '系统初始化业务默认类别'),
        ('WPLB000014', '酒水饮品', '物品类别', '启用', '系统初始化业务默认类别'),
        ('WPLB000015', '酒水', '酒水饮品', '启用', '系统初始化业务默认类别'),
        ('WPLB000016', '奶茶', '酒水饮品', '启用', '系统初始化业务默认类别'),
        ('WPLB000017', '干货调料', '物品类别', '启用', '系统初始化业务默认类别'),
        ('WPLB000018', '调料', '干货调料', '启用', '系统初始化业务默认类别'),
        ('WPLB000019', '豆制品', '干货调料', '启用', '系统初始化业务默认类别'),
        ('WPLB000020', '熟食卤味', '物品类别', '启用', '系统初始化业务默认类别'),
        ('WPLB000021', '熟食', '熟食卤味', '启用', '系统初始化业务默认类别'),
        ('WPLB000022', '日杂包材', '物品类别', '启用', '系统初始化业务默认类别'),
        ('WPLB000023', '一次性用品', '日杂包材', '启用', '系统初始化业务默认类别'),
        ('WPLB000024', '前厅类', '日杂包材', '启用', '系统初始化业务默认类别'),
        ('WPLB000025', '日用百货', '日杂包材', '启用', '系统初始化业务默认类别')
)
INSERT INTO item_category (scope_type, scope_id, category_code, category_name, parent_category, status, remark)
SELECT scope.scope_type,
       scope.scope_id,
       category.category_code,
       category.category_name,
       category.parent_category,
       category.status,
       category.remark
FROM item_scope_seed scope
         CROSS JOIN category_seed category
ON CONFLICT DO NOTHING;

WITH item_scope_seed AS (
    SELECT 'PLATFORM'::VARCHAR(16) AS scope_type, 0::BIGINT AS scope_id
    UNION ALL
    SELECT 'GROUP'::VARCHAR(16), g.id
    FROM sys_group g
    UNION ALL
    SELECT 'STORE'::VARCHAR(16), s.id
    FROM sys_store s
),
statistics_type_seed(code, name, statistics_category, create_type) AS (
    VALUES
        ('TJLX000001', '原料类', '成本类', 'SYSTEM_BUILTIN'),
        ('TJLX000002', '酒水类', '成本类', 'SYSTEM_BUILTIN'),
        ('TJLX000003', '调料类', '成本类', 'SYSTEM_BUILTIN'),
        ('TJLX000004', '半成品类', '成本类', 'SYSTEM_BUILTIN'),
        ('TJLX000005', '成品类', '成本类', 'SYSTEM_BUILTIN'),
        ('TJLX000006', '包材类', '费用类', 'SYSTEM_BUILTIN'),
        ('TJLX000007', '低值易耗品类', '费用类', 'SYSTEM_BUILTIN'),
        ('TJLX000008', '固定资产类', '费用类', 'SYSTEM_BUILTIN')
)
INSERT INTO item_statistics_type (scope_type, scope_id, code, name, statistics_category, create_type)
SELECT scope.scope_type,
       scope.scope_id,
       statistics_type.code,
       statistics_type.name,
       statistics_type.statistics_category,
       statistics_type.create_type
FROM item_scope_seed scope
         CROSS JOIN statistics_type_seed statistics_type
ON CONFLICT DO NOTHING;

WITH item_scope_seed AS (
    SELECT 'PLATFORM'::VARCHAR(16) AS scope_type, 0::BIGINT AS scope_id
    UNION ALL
    SELECT 'GROUP'::VARCHAR(16), g.id
    FROM sys_group g
    UNION ALL
    SELECT 'STORE'::VARCHAR(16), s.id
    FROM sys_store s
),
tag_seed(tag_code, tag_name, status, remark) AS (
    VALUES
        ('BQBM000001', '生鲜', '启用', CAST(NULL AS VARCHAR(500))),
        ('BQBM000002', '冻品', '启用', CAST(NULL AS VARCHAR(500))),
        ('BQBM000003', '称重', '启用', CAST(NULL AS VARCHAR(500))),
        ('BQBM000004', '散装', '启用', CAST(NULL AS VARCHAR(500))),
        ('BQBM000005', '高值', '启用', CAST(NULL AS VARCHAR(500))),
        ('BQBM000006', '易耗', '启用', CAST(NULL AS VARCHAR(500))),
        ('BQBM000007', '易损', '启用', CAST(NULL AS VARCHAR(500))),
        ('BQBM000008', '促销', '启用', CAST(NULL AS VARCHAR(500))),
        ('BQBM000009', '赠品', '启用', CAST(NULL AS VARCHAR(500))),
        ('BQBM000010', '自制', '启用', CAST(NULL AS VARCHAR(500)))
)
INSERT INTO item_tag (scope_type, scope_id, tag_code, tag_name, status, remark)
SELECT scope.scope_type,
       scope.scope_id,
       tag.tag_code,
       tag.tag_name,
       tag.status,
       tag.remark
FROM item_scope_seed scope
         CROSS JOIN tag_seed tag
ON CONFLICT DO NOTHING;
-- ITEM_MASTER_SEED_END

-- 以下为角色模板数据，平台角色模板和集团/门店角色模板均属于模板数据源。
INSERT INTO sys_role (role_code, role_name, builtin, role_type, data_scope_type, description, status)
VALUES ('PLATFORM_SUPER_ADMIN', '平台超级管理员', TRUE, 'PLATFORM', 'ALL', '系统初始化平台管理员角色', 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role (role_code, role_name, builtin, role_type, data_scope_type, description, status)
VALUES ('GROUP_ADMIN', '集团管理员', TRUE, 'GROUP', 'GROUP', '系统初始化集团管理员角色', 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role (role_code, role_name, builtin, role_type, data_scope_type, description, status)
VALUES ('STORE_ADMIN', '门店管理员', TRUE, 'STORE', 'STORE', '系统初始化门店管理员角色', 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role (tenant_group_id, role_code, role_name, builtin, role_type, data_scope_type, description, status)
VALUES (0, 'STORE_MANAGER', '店长', TRUE, 'STORE', 'STORE', 'GROUP_ROLE_TEMPLATE', 'ENABLED')
ON CONFLICT (tenant_group_id, role_code) DO NOTHING;

INSERT INTO sys_role (tenant_group_id, role_code, role_name, builtin, role_type, data_scope_type, description, status)
VALUES (0, 'SALESMAN', '业务员', TRUE, 'STORE', 'STORE', 'GROUP_ROLE_TEMPLATE', 'ENABLED')
ON CONFLICT (tenant_group_id, role_code) DO NOTHING;

INSERT INTO sys_role (tenant_group_id, role_code, role_name, builtin, role_type, data_scope_type, description, status)
VALUES (0, 'FINANCE', '财务', TRUE, 'STORE', 'STORE', 'GROUP_ROLE_TEMPLATE', 'ENABLED')
ON CONFLICT (tenant_group_id, role_code) DO NOTHING;

INSERT INTO sys_role (tenant_group_id, role_code, role_name, builtin, role_type, data_scope_type, description, status)
VALUES (0, 'CASHIER', '收银员', TRUE, 'STORE', 'STORE', 'GROUP_ROLE_TEMPLATE', 'ENABLED')
ON CONFLICT (tenant_group_id, role_code) DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES ('SYS_MGMT', '系统管理', NULL, 'DIRECTORY', '/system', NULL, 'setting', 10, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'ROLE_MGMT',
    '角色管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'SYS_MGMT'),
    'MENU',
    '/system/roles',
    'system:role:view',
    'team',
    11,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_MGMT', '集团管理', NULL, 'DIRECTORY', '/group', NULL, 'office-building', 40, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'GROUP_STORE_MGMT',
    '门店管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT'),
    'MENU',
    '/group/stores',
    'group:store:manage',
    'shop',
    44,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'GROUP_MGMT_ADMIN',
    '集团管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'SYS_MGMT'),
    'MENU',
    '/system/groups',
    'system:group:manage',
    'office',
    12,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'USER_MGMT',
    '用户管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'SYS_MGMT'),
    'MENU',
    '/system/users',
    'system:user:view',
    'user',
    13,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'MENU_PERMISSION_MGMT',
    '菜单权限管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'SYS_MGMT'),
    'MENU',
    '/system/menu-permissions',
    'system:menu:assign',
    'setting',
    14,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'DICT_MGMT',
    '字典管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'SYS_MGMT'),
    'MENU',
    '/system/dictionaries',
    'system:dictionary:manage',
    'setting',
    15,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_WORKBENCH', '集团工作台', NULL, 'MENU', '/group/dashboard', 'group:dashboard:view', 'office', 20, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_MGMT', '集团管理', NULL, 'DIRECTORY', '/group', NULL, 'office-building', 40, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_INFO', '集团信息', (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT'), 'MENU', '/group/info', 'group:info:view', 'document', 41, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_STORE_MGMT', '门店管理', (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT'), 'MENU', '/group/stores', 'group:store:manage', 'shop', 44, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_USER_ROLE_MGMT', '用户管理', (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT'), 'MENU', '/group/user-role', 'group:user-role:manage', 'user', 46, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_ROLE_MGMT', '角色管理', (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT'), 'MENU', '/group/roles', 'group:role:manage', 'team', 47, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_MENU_PERMISSION_MGMT', '菜单权限管理', (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT'), 'MENU', '/group/menu-permissions', 'group:menu-permission:manage', 'setting', 48, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;


INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_WORKFLOW_HISTORY_MGMT', '流程发布历史管理', (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_WORKBENCH'), 'MENU', '/group/workflow-history', 'group:workflow:history:view', 'setting', 50, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

UPDATE sys_menu
SET menu_name = '用户管理'
WHERE menu_code = 'GROUP_USER_ROLE_MGMT';

DELETE FROM sys_role_menu_rel
WHERE menu_id IN (
    SELECT id
    FROM sys_menu
    WHERE menu_code IN ('GROUP_STORE_SELECTOR', 'GROUP_STORE_OVERVIEW', 'GROUP_DATA_STAT')
);

DELETE FROM sys_menu
WHERE menu_code IN ('GROUP_STORE_SELECTOR', 'GROUP_STORE_OVERVIEW', 'GROUP_DATA_STAT');

DELETE FROM sys_role_menu_rel
WHERE menu_id IN (
    SELECT id
    FROM sys_menu
    WHERE menu_code = 'GROUP_WORKFLOW_CONFIG'
);

DELETE FROM sys_menu
WHERE menu_code = 'GROUP_WORKFLOW_CONFIG';

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'PLATFORM_SUPER_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'SYS_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'GROUP_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'GROUP_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_STORE_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'PLATFORM_SUPER_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'ROLE_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'PLATFORM_SUPER_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT_ADMIN')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'PLATFORM_SUPER_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'USER_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'PLATFORM_SUPER_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'MENU_PERMISSION_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'PLATFORM_SUPER_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'DICT_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
SELECT
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'PLATFORM_SUPER_ADMIN'),
    m.id
FROM sys_menu m
WHERE m.menu_code IN (
    'STORE_BIZ_MOD_08',
    'STORE_BIZ_GRP_08_01',
    'STORE_BIZ_MENU_08_01_02',
    'STORE_BIZ_MENU_08_01_03',
    'STORE_BIZ_MENU_08_01_04',
    'STORE_BIZ_MENU_08_01_05'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'GROUP_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_WORKBENCH')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'GROUP_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
SELECT
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'GROUP_ADMIN'),
    m.id
FROM sys_menu m
WHERE m.menu_code IN (
    'GROUP_INFO',
    'GROUP_STORE_MGMT',
    'GROUP_USER_ROLE_MGMT',
    'GROUP_ROLE_MGMT',
    'GROUP_MENU_PERMISSION_MGMT',
    'GROUP_WORKFLOW_HISTORY_MGMT'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_user (username, real_name, phone, password_hash, password_salt, status, source_type, first_login_changed_pwd)
VALUES ('13800000000', '系统管理员', '13800000000', '6460662e217c7a9f899208dd70a2c28abdea42f128666a9b78e6c0c064846493', NULL, 'ENABLED', 'SYSTEM_INIT', FALSE)
ON CONFLICT DO NOTHING;

UPDATE sys_user
SET username = 'admin'
WHERE phone = '13800000000'
  AND username = '13800000000'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user u WHERE u.username = 'admin'
);

INSERT INTO sys_user (username, real_name, phone, password_hash, password_salt, status, source_type, first_login_changed_pwd)
VALUES ('mrjtgly0001', '默认集团管理员', '13800000001', '6460662e217c7a9f899208dd70a2c28abdea42f128666a9b78e6c0c064846493', NULL, 'ENABLED', 'SYSTEM_INIT', FALSE)
ON CONFLICT DO NOTHING;

INSERT INTO sys_user (username, real_name, phone, password_hash, password_salt, status, source_type, first_login_changed_pwd)
VALUES ('mrmdgly0002', '默认门店管理员', '13800000002', '6460662e217c7a9f899208dd70a2c28abdea42f128666a9b78e6c0c064846493', NULL, 'ENABLED', 'SYSTEM_INIT', FALSE)
ON CONFLICT DO NOTHING;

INSERT INTO sys_user_role_rel (user_id, role_id, scope_type, scope_id, assigned_by, status)
VALUES (
    (SELECT id FROM sys_user WHERE phone = '13800000000'),
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'PLATFORM_SUPER_ADMIN'),
    'PLATFORM',
    NULL,
    (SELECT id FROM sys_user WHERE phone = '13800000000'),
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_user_role_rel (user_id, role_id, scope_type, scope_id, assigned_by, status)
VALUES (
    (SELECT id FROM sys_user WHERE phone = '13800000001'),
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'GROUP_ADMIN'),
    'GROUP',
    (SELECT id FROM sys_group WHERE group_code = 'GP00001'),
    (SELECT id FROM sys_user WHERE phone = '13800000000'),
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_user_role_rel (user_id, role_id, scope_type, scope_id, assigned_by, status)
VALUES (
    (SELECT id FROM sys_user WHERE phone = '13800000002'),
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'STORE_ADMIN'),
    'STORE',
    (SELECT id FROM sys_store WHERE store_code = 'MD00001'),
    (SELECT id FROM sys_user WHERE phone = '13800000000'),
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_store_admin_rel (store_id, user_id, assigned_by, status)
VALUES (
    (SELECT id FROM sys_store WHERE store_code = 'MD00001'),
    (SELECT id FROM sys_user WHERE phone = '13800000002'),
    (SELECT id FROM sys_user WHERE phone = '13800000000'),
    'ENABLED'
)
ON CONFLICT DO NOTHING;

-- STORE_MENU_SEED_START
-- ??????????jxc-w/src/config/jxc-menu.json??????????????/???????
INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_01',
    '订货管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    'tickets',
    101,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;






INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_01_03',
    '单据',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_01'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1103,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_03_01',
    '订货单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_03'),
    'MENU',
    '/order/order-documents',
    'store:order:order-documents:view',
    NULL,
    101031,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_03_02',
    '收货单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_03'),
    'MENU',
    '/order/receipt-documents',
    'store:order:receipt-documents:view',
    NULL,
    101032,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_03_03',
    '差异单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_03'),
    'MENU',
    '/order/difference-documents',
    'store:order:difference-documents:view',
    NULL,
    101033,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_03_04',
    '返货单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_03'),
    'MENU',
    '/order/return-documents',
    'store:order:return-documents:view',
    NULL,
    101034,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_03_05',
    '三方调拨单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_03'),
    'MENU',
    '/order/third-party-transfer-documents',
    'store:order:third-party-transfer-documents:view',
    NULL,
    101035,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_02',
    '采购管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    'goods',
    102,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_02_01',
    '价格管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_02'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1201,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_01_01',
    '采购单定价',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_01'),
    'MENU',
    '/purchase/pricing',
    'store:purchase:pricing:view',
    NULL,
    102011,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_01_02',
    '采购定价明细调整单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_01'),
    'MENU',
    '/purchase/pricing-adjustments',
    'store:purchase:pricing-adjustments:view',
    NULL,
    102012,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_01_03',
    '采购定价明细',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_01'),
    'MENU',
    '/purchase/pricing-details',
    'store:purchase:pricing-details:view',
    NULL,
    102013,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;







INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_02_04',
    '单据',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_02'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1204,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_04_01',
    '采购单申请',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_04'),
    'MENU',
    '/purchase/applications',
    'store:purchase:applications:view',
    NULL,
    102041,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_04_02',
    '采购单申请审核',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_04'),
    'MENU',
    '/purchase/application-reviews',
    'store:purchase:application-reviews:view',
    NULL,
    102042,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_04_03',
    '采购订单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_04'),
    'MENU',
    '/purchase/orders',
    'store:purchase:orders:view',
    NULL,
    102043,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_04_04',
    '采购收货单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_04'),
    'MENU',
    '/purchase/receipts',
    'store:purchase:receipts:view',
    NULL,
    102044,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_04_05',
    '采购退货单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_04'),
    'MENU',
    '/purchase/returns',
    'store:purchase:returns:view',
    NULL,
    102045,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;













INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_04',
    '库存管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    'files',
    104,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_04_01',
    '库存单据',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_04'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1401,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_01',
    '仓库期初',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/warehouse-opening-balances',
    'store:inventory:warehouse-opening-balances:view',
    NULL,
    104011,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_02',
    '采购入库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/purchase-inbounds',
    'store:inventory:purchase-inbounds:view',
    NULL,
    104012,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_03',
    '采购退货出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/purchase-return-outbounds',
    'store:inventory:purchase-return-outbounds:view',
    NULL,
    104013,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_04',
    '部门领料',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/department-pickings',
    'store:inventory:department-pickings:view',
    NULL,
    104014,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_05',
    '部门退料',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/department-returns',
    'store:inventory:department-returns:view',
    NULL,
    104015,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_06',
    '移库单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/stock-transfers',
    'store:inventory:stock-transfers:view',
    NULL,
    104016,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_07',
    '移库入库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/stock-transfer-inbounds',
    'store:inventory:stock-transfer-inbounds:view',
    NULL,
    104017,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_08',
    '部门调拨',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/department-transfers',
    'store:inventory:department-transfers:view',
    NULL,
    104018,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_09',
    '店间调拨',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/store-transfers',
    'store:inventory:store-transfers:view',
    NULL,
    104019,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_10',
    '报损出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/damage-outbounds',
    'store:inventory:damage-outbounds:view',
    NULL,
    104020,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_11',
    '其他入库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/other-inbounds',
    'store:inventory:other-inbounds:view',
    NULL,
    104021,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_12',
    '其他出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/other-outbounds',
    'store:inventory:other-outbounds:view',
    NULL,
    104022,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_13',
    '生产入库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/production-inbounds',
    'store:inventory:production-inbounds:view',
    NULL,
    104023,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_14',
    '客户销售出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/customer-sales-outbounds',
    'store:inventory:customer-sales-outbounds:view',
    NULL,
    104024,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_15',
    '客户退货入库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/customer-return-inbounds',
    'store:inventory:customer-return-inbounds:view',
    NULL,
    104025,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_16',
    '移库出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/stock-transfer-outbounds',
    'store:inventory:stock-transfer-outbounds:view',
    NULL,
    104026,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_04_02',
    '盘点管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_04'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1402,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_02_01',
    '盘点单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_02'),
    'MENU',
    '/inventory/inventory-checks',
    'store:inventory:inventory-checks:view',
    NULL,
    104021,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_02_02',
    '多人盘点单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_02'),
    'MENU',
    '/inventory/multi-inventory-checks',
    'store:inventory:multi-inventory-checks:view',
    NULL,
    104022,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_02_03',
    '盘盈单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_02'),
    'MENU',
    '/inventory/profit-inbounds',
    'store:inventory:profit-inbounds:view',
    NULL,
    104023,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_02_04',
    '盘亏单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_02'),
    'MENU',
    '/inventory/loss-outbounds',
    'store:inventory:loss-outbounds:view',
    NULL,
    104024,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_04_03',
    '自动出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_04'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1403,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_03_01',
    '菜品消耗出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_03'),
    'MENU',
    '/inventory/dish-consumption-outbounds',
    'store:inventory:dish-consumption-outbounds:view',
    NULL,
    104031,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_04_04',
    '批次管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_04'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1404,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_04_01',
    '批次调整单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_04'),
    'MENU',
    '/inventory/batch-adjustments',
    'store:inventory:batch-adjustments:view',
    NULL,
    104041,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_04_05',
    '库存规则',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_04'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1405,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_05_01',
    '库存模板',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_05'),
    'MENU',
    '/inventory/inventory-templates',
    'store:inventory:inventory-templates:view',
    NULL,
    104051,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_05_02',
    '调拨分组',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_05'),
    'MENU',
    '/inventory/transfer-groups',
    'store:inventory:transfer-groups:view',
    NULL,
    104052,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_05_03',
    '库存上下限',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_05'),
    'MENU',
    '/inventory/stock-limits',
    'store:inventory:stock-limits:view',
    NULL,
    104053,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_05_04',
    '库存锁库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_05'),
    'MENU',
    '/inventory/stock-locks',
    'store:inventory:stock-locks:view',
    NULL,
    104054,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_05_05',
    '锁库日志',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_05'),
    'MENU',
    '/inventory/stock-lock-logs',
    'store:inventory:stock-lock-logs:view',
    NULL,
    104055,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;






























INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_07',
    '报表管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    'data-analysis',
    107,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;









INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_07_03',
    '采购报表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_07'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1703,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_03_01',
    '采购订单状态跟踪表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_03'),
    'MENU',
    '/report/purchase-order-status-tracking',
    'store:report:purchase-order-status-tracking:view',
    NULL,
    107031,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_03_02',
    '退货单状态跟踪表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_03'),
    'MENU',
    '/report/purchase-return-status-tracking',
    'store:report:purchase-return-status-tracking:view',
    NULL,
    107032,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_03_03',
    '采购物品价格分析表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_03'),
    'MENU',
    '/report/purchase-item-price-analysis',
    'store:report:purchase-item-price-analysis:view',
    NULL,
    107033,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;













INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_07_05',
    '库存报表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_07'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1705,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_01',
    '实时库存查询表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/realtime-stock-query',
    'store:report:realtime-stock-query:view',
    NULL,
    107051,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_02',
    '菜品消耗出库查询表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/dish-consumption-outbound-query',
    'store:report:dish-consumption-outbound-query:view',
    NULL,
    107052,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_03',
    '盘点盈亏表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/inventory-profit-loss',
    'store:report:inventory-profit-loss:view',
    NULL,
    107053,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_04',
    '出入库明细表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/inventory-inout-details',
    'store:report:inventory-inout-details:view',
    NULL,
    107054,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_05',
    '出入库汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/inventory-inout-summary',
    'store:report:inventory-inout-summary:view',
    NULL,
    107055,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_06',
    '库存进出汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/stock-inout-summary',
    'store:report:stock-inout-summary:view',
    NULL,
    107056,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_07',
    '其他出入库汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/other-inout-summary',
    'store:report:other-inout-summary:view',
    NULL,
    107057,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_08',
    '机构间调拨明细表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/inter-org-transfer-detail',
    'store:report:inter-org-transfer-detail:view',
    NULL,
    107058,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_09',
    '机构间调拨汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/inter-org-transfer-summary',
    'store:report:inter-org-transfer-summary:view',
    NULL,
    107059,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;



INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_12',
    '物品批次全流程跟踪表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/item-batch-trace',
    'store:report:item-batch-trace:view',
    NULL,
    107062,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_13',
    '库存呆滞品查询表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/stagnant-stock',
    'store:report:stagnant-stock:view',
    NULL,
    107063,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_14',
    '库存周转率统计表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/stock-turnover-rate',
    'store:report:stock-turnover-rate:view',
    NULL,
    107064,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_15',
    '库存预警表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/stock-warning',
    'store:report:stock-warning:view',
    NULL,
    107065,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;












INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_08',
    '档案管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    'document',
    108,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_01',
    '物品',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1801,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_01_01',
    '物品管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_01'),
    'MENU',
    '/archive/items',
    'store:archive:items:view',
    NULL,
    108011,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_01_02',
    '物品类别管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_01'),
    'MENU',
    '/archive/item-categories',
    'store:archive:item-categories:view',
    NULL,
    108012,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_01_03',
    '单位管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_01'),
    'MENU',
    '/archive/units',
    'store:archive:units:view',
    NULL,
    108013,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_01_04',
    '统计类型管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_01'),
    'MENU',
    '/archive/statistics-types',
    'store:archive:statistics-types:view',
    NULL,
    108014,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_01_05',
    '物品标签管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_01'),
    'MENU',
    '/archive/item-tags',
    'store:archive:item-tags:view',
    NULL,
    108015,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_02',
    '成本设置',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1802,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_02_01',
    '成本卡档案',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_02'),
    'MENU',
    '/archive/cost-card-archives',
    'store:archive:cost-card-archives:view',
    NULL,
    108021,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_02_02',
    '成本卡设置',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_02'),
    'MENU',
    '/archive/cost-card-settings',
    'store:archive:cost-card-settings:view',
    NULL,
    108022,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_02_03',
    '菜品关联仓库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_02'),
    'MENU',
    '/archive/dish-warehouse-links',
    'store:archive:dish-warehouse-links:view',
    NULL,
    108023,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_02_04',
    '费用关联仓库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_02'),
    'MENU',
    '/archive/expense-warehouse-links',
    'store:archive:expense-warehouse-links:view',
    NULL,
    108024,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;




INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_03',
    '供应商',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1803,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_03_01',
    '供应商档案',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_03'),
    'MENU',
    '/archive/suppliers',
    'store:archive:suppliers:view',
    NULL,
    108031,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;












INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_06',
    '机构',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1806,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_06_01',
    '机构管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_06'),
    'MENU',
    '/archive/organizations',
    'store:archive:organizations:view',
    NULL,
    108061,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_07',
    '仓库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    1807,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_07_01',
    '仓库档案',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_07'),
    'MENU',
    '/archive/warehouses',
    'store:archive:warehouses:view',
    NULL,
    108071,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_07_02',
    '仓库物品规则',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_07'),
    'MENU',
    '/archive/warehouse-item-rules',
    'store:archive:warehouse-item-rules:view',
    NULL,
    108072,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

















INSERT INTO sys_role_menu_rel (role_id, menu_id)
SELECT
    (SELECT id FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'STORE_ADMIN'),
    m.id
FROM sys_menu m
WHERE m.menu_code LIKE 'STORE_BIZ_%'
ON CONFLICT DO NOTHING;

-- 删除当前无前端页面承接的遗留数字菜单路由，避免初始化后再次回潮。
DELETE FROM sys_role_menu_rel
WHERE menu_id IN (
    SELECT id
    FROM sys_menu
    WHERE COALESCE(component_key, '') = ''
      AND route_path ~ '^/(order|purchase|inventory|archive|report|finance|warning|system|production|quality)/[0-9]+(/[0-9]+)?$'
);

DELETE FROM sys_menu
WHERE COALESCE(component_key, '') = ''
  AND route_path ~ '^/(order|purchase|inventory|archive|report|finance|warning|system|production|quality)/[0-9]+(/[0-9]+)?$';

-- 删除没有任何非目录后代承接的空目录树，避免菜单树残留无效分组。
WITH RECURSIVE menu_descendants AS (
    SELECT
        p.id AS directory_id,
        c.id AS descendant_id,
        c.menu_type AS descendant_type
    FROM sys_menu p
    JOIN sys_menu c ON c.parent_id = p.id
    WHERE p.menu_type = 'DIRECTORY'

    UNION ALL

    SELECT
        d.directory_id,
        c.id AS descendant_id,
        c.menu_type AS descendant_type
    FROM menu_descendants d
    JOIN sys_menu c ON c.parent_id = d.descendant_id
),
empty_directories AS (
    SELECT m.id
    FROM sys_menu m
    WHERE m.menu_type = 'DIRECTORY'
      AND NOT EXISTS (
          SELECT 1
          FROM menu_descendants d
          WHERE d.directory_id = m.id
            AND d.descendant_type <> 'DIRECTORY'
      )
)
DELETE FROM sys_role_menu_rel
WHERE menu_id IN (SELECT id FROM empty_directories);

WITH RECURSIVE menu_descendants AS (
    SELECT
        p.id AS directory_id,
        c.id AS descendant_id,
        c.menu_type AS descendant_type
    FROM sys_menu p
    JOIN sys_menu c ON c.parent_id = p.id
    WHERE p.menu_type = 'DIRECTORY'

    UNION ALL

    SELECT
        d.directory_id,
        c.id AS descendant_id,
        c.menu_type AS descendant_type
    FROM menu_descendants d
    JOIN sys_menu c ON c.parent_id = d.descendant_id
),
empty_directories AS (
    SELECT m.id
    FROM sys_menu m
    WHERE m.menu_type = 'DIRECTORY'
      AND NOT EXISTS (
          SELECT 1
          FROM menu_descendants d
          WHERE d.directory_id = m.id
            AND d.descendant_type <> 'DIRECTORY'
      )
)
DELETE FROM sys_menu
WHERE id IN (SELECT id FROM empty_directories);

-- 将已落地真实页面的门店业务菜单权限编码收敛为语义格式。
UPDATE sys_menu
SET permission_code = 'store:' || split_part(route_path, '/', 2) || ':' || split_part(route_path, '/', 3) || ':view'
WHERE menu_code LIKE 'STORE_BIZ_MENU_%'
  AND COALESCE(component_key, '') <> ''
  AND route_path ~ '^/[^/]+/[^/]+$';
-- STORE_MENU_SEED_END
