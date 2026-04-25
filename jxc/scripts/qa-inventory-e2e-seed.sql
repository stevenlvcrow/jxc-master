-- QA inventory E2E seed data.
-- Run with psql after schema.sql and seed-core.sql have completed.
-- This script is idempotent and intentionally does not delete existing business data.

\set ON_ERROR_STOP on

SET search_path TO dev;

DO $$
BEGIN
    IF to_regclass('dev.sys_group') IS NULL
        OR to_regclass('dev.sys_store') IS NULL
        OR to_regclass('dev.item_profile') IS NULL
        OR to_regclass('dev.inventory_balance') IS NULL
        OR to_regclass('dev.inventory_transaction') IS NULL THEN
        RAISE EXCEPTION 'Required schema is missing. Run application schema initialization first.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'PLATFORM_ADMIN') THEN
        RAISE EXCEPTION 'Builtin role PLATFORM_ADMIN is missing.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'GROUP_ADMIN') THEN
        RAISE EXCEPTION 'Builtin role GROUP_ADMIN is missing.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'STORE_ADMIN') THEN
        RAISE EXCEPTION 'Builtin role STORE_ADMIN is missing.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'SALESMAN') THEN
        RAISE EXCEPTION 'Builtin role SALESMAN is missing.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM sys_role WHERE tenant_group_id = 0 AND role_code = 'FINANCE') THEN
        RAISE EXCEPTION 'Builtin role FINANCE is missing.';
    END IF;
END $$;

INSERT INTO sys_group (group_code, group_name, status, remark)
VALUES
    ('QA-GRP-A', 'QA集团A', 'ENABLED', 'QA_INV_E2E'),
    ('QA-GRP-B', 'QA集团B', 'ENABLED', 'QA_INV_E2E')
ON CONFLICT (group_code) DO UPDATE
SET group_name = EXCLUDED.group_name,
    status = EXCLUDED.status,
    remark = EXCLUDED.remark,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_store (group_id, store_code, store_name, status, contact_name, contact_phone, address, remark)
SELECT g.id, store_code, store_name, 'ENABLED', contact_name, contact_phone, address, 'QA_INV_E2E'
FROM (
    VALUES
        ('QA-GRP-A', 'QA-A1', 'QA门店A1', 'QA店长A1', '13990001001', 'QA集团A-门店A1'),
        ('QA-GRP-A', 'QA-A2', 'QA门店A2', 'QA店长A2', '13990001002', 'QA集团A-门店A2'),
        ('QA-GRP-B', 'QA-B1', 'QA门店B1', 'QA店长B1', '13990002001', 'QA集团B-门店B1')
) AS seed(group_code, store_code, store_name, contact_name, contact_phone, address)
JOIN sys_group g ON g.group_code = seed.group_code
ON CONFLICT (store_code) DO UPDATE
SET group_id = EXCLUDED.group_id,
    store_name = EXCLUDED.store_name,
    status = EXCLUDED.status,
    contact_name = EXCLUDED.contact_name,
    contact_phone = EXCLUDED.contact_phone,
    address = EXCLUDED.address,
    remark = EXCLUDED.remark,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_role (tenant_group_id, role_code, role_name, builtin, role_type, data_scope_type, description, status)
SELECT g.id, role_code, role_name, FALSE, role_type, data_scope_type, 'QA_INV_E2E', 'ENABLED'
FROM sys_group g
CROSS JOIN (
    VALUES
        ('QA_GROUP_AUDITOR', 'QA集团审核员', 'GROUP', 'GROUP'),
        ('SALESMAN', 'QA门店仅自己业务员', 'STORE', 'SELF'),
        ('FINANCE', 'QA门店审核员', 'STORE', 'STORE')
) AS seed(role_code, role_name, role_type, data_scope_type)
WHERE g.group_code IN ('QA-GRP-A', 'QA-GRP-B')
ON CONFLICT (tenant_group_id, role_code) DO UPDATE
SET role_name = EXCLUDED.role_name,
    role_type = EXCLUDED.role_type,
    data_scope_type = EXCLUDED.data_scope_type,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

WITH qa_role_menu(role_code, menu_code) AS (
    VALUES
        ('SALESMAN', 'STORE_BIZ_MOD_PURCHASE'),
        ('SALESMAN', 'STORE_BIZ_GRP_PURCHASE_DOCUMENT'),
        ('SALESMAN', 'STORE_BIZ_MENU_PURCHASE_APPLICATION'),
        ('SALESMAN', 'STORE_BIZ_MENU_PURCHASE_ORDER'),
        ('SALESMAN', 'STORE_BIZ_MENU_PURCHASE_RECEIPT'),
        ('SALESMAN', 'STORE_BIZ_MENU_PURCHASE_RETURN'),
        ('SALESMAN', 'STORE_BIZ_MOD_INVENTORY'),
        ('SALESMAN', 'STORE_BIZ_GRP_INVENTORY_DOCUMENT'),
        ('SALESMAN', 'STORE_BIZ_MENU_WAREHOUSE_OPENING_BALANCE'),
        ('SALESMAN', 'STORE_BIZ_MENU_STOCK_TRANSFER_INBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_PRODUCTION_INBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_OTHER_INBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_PURCHASE_INBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_CUSTOMER_RETURN_INBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_CUSTOMER_SALES_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_DAMAGE_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_OTHER_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_STOCK_TRANSFER_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_PURCHASE_RETURN_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_DEPARTMENT_PICKING'),
        ('SALESMAN', 'STORE_BIZ_MENU_DEPARTMENT_RETURN'),
        ('SALESMAN', 'STORE_BIZ_MENU_STOCK_TRANSFER'),
        ('SALESMAN', 'STORE_BIZ_MENU_DEPARTMENT_TRANSFER'),
        ('SALESMAN', 'STORE_BIZ_MENU_STORE_TRANSFER'),
        ('SALESMAN', 'STORE_BIZ_GRP_INVENTORY_AUTO_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_DISH_CONSUMPTION_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_GRP_INVENTORY_RULE'),
        ('SALESMAN', 'STORE_BIZ_MENU_TRANSFER_GROUP'),
        ('SALESMAN', 'STORE_BIZ_GRP_INVENTORY_CHECK'),
        ('SALESMAN', 'STORE_BIZ_MENU_INVENTORY_CHECK'),
        ('SALESMAN', 'STORE_BIZ_MENU_MULTI_INVENTORY_CHECK'),
        ('SALESMAN', 'STORE_BIZ_MENU_PROFIT_INBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_LOSS_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MOD_REPORT'),
        ('SALESMAN', 'STORE_BIZ_GRP_REPORT_INVENTORY'),
        ('SALESMAN', 'STORE_BIZ_MENU_REALTIME_STOCK_QUERY'),
        ('SALESMAN', 'STORE_BIZ_MENU_INVENTORY_INOUT_DETAIL'),
        ('SALESMAN', 'STORE_BIZ_MENU_INVENTORY_INOUT_SUMMARY'),
        ('SALESMAN', 'STORE_BIZ_MENU_STOCK_INOUT_SUMMARY'),
        ('SALESMAN', 'STORE_BIZ_MOD_ARCHIVE'),
        ('SALESMAN', 'STORE_BIZ_GRP_ARCHIVE_ITEM'),
        ('SALESMAN', 'STORE_BIZ_MENU_ITEM'),
        ('SALESMAN', 'STORE_BIZ_MENU_ITEM_CATEGORY'),
        ('SALESMAN', 'STORE_BIZ_MENU_UNIT'),
        ('SALESMAN', 'STORE_BIZ_GRP_ARCHIVE_SUPPLIER'),
        ('SALESMAN', 'STORE_BIZ_MENU_SUPPLIER'),
        ('SALESMAN', 'STORE_BIZ_GRP_ARCHIVE_WAREHOUSE'),
        ('SALESMAN', 'STORE_BIZ_MENU_WAREHOUSE'),

        ('FINANCE', 'STORE_BIZ_MOD_PURCHASE'),
        ('FINANCE', 'STORE_BIZ_GRP_PURCHASE_DOCUMENT'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_APPLICATION'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_APPLICATION_REVIEW'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_ORDER'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_RECEIPT'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_RETURN'),
        ('FINANCE', 'STORE_BIZ_MOD_INVENTORY'),
        ('FINANCE', 'STORE_BIZ_GRP_INVENTORY_DOCUMENT'),
        ('FINANCE', 'STORE_BIZ_MENU_WAREHOUSE_OPENING_BALANCE'),
        ('FINANCE', 'STORE_BIZ_MENU_STOCK_TRANSFER_INBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_PRODUCTION_INBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_OTHER_INBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_INBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_CUSTOMER_RETURN_INBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_CUSTOMER_SALES_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_DAMAGE_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_OTHER_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_STOCK_TRANSFER_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_RETURN_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_DEPARTMENT_PICKING'),
        ('FINANCE', 'STORE_BIZ_MENU_DEPARTMENT_RETURN'),
        ('FINANCE', 'STORE_BIZ_MENU_STOCK_TRANSFER'),
        ('FINANCE', 'STORE_BIZ_MENU_DEPARTMENT_TRANSFER'),
        ('FINANCE', 'STORE_BIZ_MENU_STORE_TRANSFER'),
        ('FINANCE', 'STORE_BIZ_GRP_INVENTORY_AUTO_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_DISH_CONSUMPTION_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_GRP_INVENTORY_RULE'),
        ('FINANCE', 'STORE_BIZ_MENU_TRANSFER_GROUP'),
        ('FINANCE', 'STORE_BIZ_GRP_INVENTORY_CHECK'),
        ('FINANCE', 'STORE_BIZ_MENU_INVENTORY_CHECK'),
        ('FINANCE', 'STORE_BIZ_MENU_MULTI_INVENTORY_CHECK'),
        ('FINANCE', 'STORE_BIZ_MENU_PROFIT_INBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_LOSS_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MOD_REPORT'),
        ('FINANCE', 'STORE_BIZ_GRP_REPORT_INVENTORY'),
        ('FINANCE', 'STORE_BIZ_MENU_REALTIME_STOCK_QUERY'),
        ('FINANCE', 'STORE_BIZ_MENU_INVENTORY_INOUT_DETAIL'),
        ('FINANCE', 'STORE_BIZ_MENU_INVENTORY_INOUT_SUMMARY'),
        ('FINANCE', 'STORE_BIZ_MENU_STOCK_INOUT_SUMMARY'),
        ('FINANCE', 'STORE_BIZ_MOD_ARCHIVE'),
        ('FINANCE', 'STORE_BIZ_GRP_ARCHIVE_ITEM'),
        ('FINANCE', 'STORE_BIZ_MENU_ITEM'),
        ('FINANCE', 'STORE_BIZ_MENU_ITEM_CATEGORY'),
        ('FINANCE', 'STORE_BIZ_MENU_UNIT'),
        ('FINANCE', 'STORE_BIZ_GRP_ARCHIVE_SUPPLIER'),
        ('FINANCE', 'STORE_BIZ_MENU_SUPPLIER'),
        ('FINANCE', 'STORE_BIZ_GRP_ARCHIVE_WAREHOUSE'),
        ('FINANCE', 'STORE_BIZ_MENU_WAREHOUSE'),

        ('QA_GROUP_AUDITOR', 'STORE_BIZ_MOD_REPORT'),
        ('QA_GROUP_AUDITOR', 'STORE_BIZ_GRP_REPORT_INVENTORY'),
        ('QA_GROUP_AUDITOR', 'STORE_BIZ_MENU_REALTIME_STOCK_QUERY'),
        ('QA_GROUP_AUDITOR', 'STORE_BIZ_MENU_INVENTORY_INOUT_DETAIL'),
        ('QA_GROUP_AUDITOR', 'STORE_BIZ_MENU_INVENTORY_INOUT_SUMMARY'),
        ('QA_GROUP_AUDITOR', 'STORE_BIZ_MENU_STOCK_INOUT_SUMMARY')
),
missing_menu AS (
    SELECT DISTINCT q.menu_code
    FROM qa_role_menu q
    LEFT JOIN sys_menu m ON m.menu_code = q.menu_code
    WHERE m.id IS NULL
)
SELECT 1 / CASE WHEN COUNT(*) = 0 THEN 1 ELSE 0 END
FROM missing_menu;

WITH qa_role_menu(role_code, menu_code) AS (
    VALUES
        ('SALESMAN', 'STORE_BIZ_MOD_PURCHASE'),
        ('SALESMAN', 'STORE_BIZ_GRP_PURCHASE_DOCUMENT'),
        ('SALESMAN', 'STORE_BIZ_MENU_PURCHASE_APPLICATION'),
        ('SALESMAN', 'STORE_BIZ_MENU_PURCHASE_ORDER'),
        ('SALESMAN', 'STORE_BIZ_MENU_PURCHASE_RECEIPT'),
        ('SALESMAN', 'STORE_BIZ_MENU_PURCHASE_RETURN'),
        ('SALESMAN', 'STORE_BIZ_MOD_INVENTORY'),
        ('SALESMAN', 'STORE_BIZ_GRP_INVENTORY_DOCUMENT'),
        ('SALESMAN', 'STORE_BIZ_MENU_WAREHOUSE_OPENING_BALANCE'),
        ('SALESMAN', 'STORE_BIZ_MENU_STOCK_TRANSFER_INBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_PRODUCTION_INBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_OTHER_INBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_PURCHASE_INBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_CUSTOMER_RETURN_INBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_CUSTOMER_SALES_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_DAMAGE_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_OTHER_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_STOCK_TRANSFER_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_PURCHASE_RETURN_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_DEPARTMENT_PICKING'),
        ('SALESMAN', 'STORE_BIZ_MENU_DEPARTMENT_RETURN'),
        ('SALESMAN', 'STORE_BIZ_MENU_STOCK_TRANSFER'),
        ('SALESMAN', 'STORE_BIZ_MENU_DEPARTMENT_TRANSFER'),
        ('SALESMAN', 'STORE_BIZ_MENU_STORE_TRANSFER'),
        ('SALESMAN', 'STORE_BIZ_GRP_INVENTORY_AUTO_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_DISH_CONSUMPTION_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_GRP_INVENTORY_RULE'),
        ('SALESMAN', 'STORE_BIZ_MENU_TRANSFER_GROUP'),
        ('SALESMAN', 'STORE_BIZ_GRP_INVENTORY_CHECK'),
        ('SALESMAN', 'STORE_BIZ_MENU_INVENTORY_CHECK'),
        ('SALESMAN', 'STORE_BIZ_MENU_MULTI_INVENTORY_CHECK'),
        ('SALESMAN', 'STORE_BIZ_MENU_PROFIT_INBOUND'),
        ('SALESMAN', 'STORE_BIZ_MENU_LOSS_OUTBOUND'),
        ('SALESMAN', 'STORE_BIZ_MOD_REPORT'),
        ('SALESMAN', 'STORE_BIZ_GRP_REPORT_INVENTORY'),
        ('SALESMAN', 'STORE_BIZ_MENU_REALTIME_STOCK_QUERY'),
        ('SALESMAN', 'STORE_BIZ_MENU_INVENTORY_INOUT_DETAIL'),
        ('SALESMAN', 'STORE_BIZ_MENU_INVENTORY_INOUT_SUMMARY'),
        ('SALESMAN', 'STORE_BIZ_MENU_STOCK_INOUT_SUMMARY'),
        ('SALESMAN', 'STORE_BIZ_MOD_ARCHIVE'),
        ('SALESMAN', 'STORE_BIZ_GRP_ARCHIVE_ITEM'),
        ('SALESMAN', 'STORE_BIZ_MENU_ITEM'),
        ('SALESMAN', 'STORE_BIZ_MENU_ITEM_CATEGORY'),
        ('SALESMAN', 'STORE_BIZ_MENU_UNIT'),
        ('SALESMAN', 'STORE_BIZ_GRP_ARCHIVE_SUPPLIER'),
        ('SALESMAN', 'STORE_BIZ_MENU_SUPPLIER'),
        ('SALESMAN', 'STORE_BIZ_GRP_ARCHIVE_WAREHOUSE'),
        ('SALESMAN', 'STORE_BIZ_MENU_WAREHOUSE'),

        ('FINANCE', 'STORE_BIZ_MOD_PURCHASE'),
        ('FINANCE', 'STORE_BIZ_GRP_PURCHASE_DOCUMENT'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_APPLICATION'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_APPLICATION_REVIEW'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_ORDER'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_RECEIPT'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_RETURN'),
        ('FINANCE', 'STORE_BIZ_MOD_INVENTORY'),
        ('FINANCE', 'STORE_BIZ_GRP_INVENTORY_DOCUMENT'),
        ('FINANCE', 'STORE_BIZ_MENU_WAREHOUSE_OPENING_BALANCE'),
        ('FINANCE', 'STORE_BIZ_MENU_STOCK_TRANSFER_INBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_PRODUCTION_INBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_OTHER_INBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_INBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_CUSTOMER_RETURN_INBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_CUSTOMER_SALES_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_DAMAGE_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_OTHER_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_STOCK_TRANSFER_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_PURCHASE_RETURN_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_DEPARTMENT_PICKING'),
        ('FINANCE', 'STORE_BIZ_MENU_DEPARTMENT_RETURN'),
        ('FINANCE', 'STORE_BIZ_MENU_STOCK_TRANSFER'),
        ('FINANCE', 'STORE_BIZ_MENU_DEPARTMENT_TRANSFER'),
        ('FINANCE', 'STORE_BIZ_MENU_STORE_TRANSFER'),
        ('FINANCE', 'STORE_BIZ_GRP_INVENTORY_AUTO_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_DISH_CONSUMPTION_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_GRP_INVENTORY_RULE'),
        ('FINANCE', 'STORE_BIZ_MENU_TRANSFER_GROUP'),
        ('FINANCE', 'STORE_BIZ_GRP_INVENTORY_CHECK'),
        ('FINANCE', 'STORE_BIZ_MENU_INVENTORY_CHECK'),
        ('FINANCE', 'STORE_BIZ_MENU_MULTI_INVENTORY_CHECK'),
        ('FINANCE', 'STORE_BIZ_MENU_PROFIT_INBOUND'),
        ('FINANCE', 'STORE_BIZ_MENU_LOSS_OUTBOUND'),
        ('FINANCE', 'STORE_BIZ_MOD_REPORT'),
        ('FINANCE', 'STORE_BIZ_GRP_REPORT_INVENTORY'),
        ('FINANCE', 'STORE_BIZ_MENU_REALTIME_STOCK_QUERY'),
        ('FINANCE', 'STORE_BIZ_MENU_INVENTORY_INOUT_DETAIL'),
        ('FINANCE', 'STORE_BIZ_MENU_INVENTORY_INOUT_SUMMARY'),
        ('FINANCE', 'STORE_BIZ_MENU_STOCK_INOUT_SUMMARY'),
        ('FINANCE', 'STORE_BIZ_MOD_ARCHIVE'),
        ('FINANCE', 'STORE_BIZ_GRP_ARCHIVE_ITEM'),
        ('FINANCE', 'STORE_BIZ_MENU_ITEM'),
        ('FINANCE', 'STORE_BIZ_MENU_ITEM_CATEGORY'),
        ('FINANCE', 'STORE_BIZ_MENU_UNIT'),
        ('FINANCE', 'STORE_BIZ_GRP_ARCHIVE_SUPPLIER'),
        ('FINANCE', 'STORE_BIZ_MENU_SUPPLIER'),
        ('FINANCE', 'STORE_BIZ_GRP_ARCHIVE_WAREHOUSE'),
        ('FINANCE', 'STORE_BIZ_MENU_WAREHOUSE'),

        ('QA_GROUP_AUDITOR', 'STORE_BIZ_MOD_REPORT'),
        ('QA_GROUP_AUDITOR', 'STORE_BIZ_GRP_REPORT_INVENTORY'),
        ('QA_GROUP_AUDITOR', 'STORE_BIZ_MENU_REALTIME_STOCK_QUERY'),
        ('QA_GROUP_AUDITOR', 'STORE_BIZ_MENU_INVENTORY_INOUT_DETAIL'),
        ('QA_GROUP_AUDITOR', 'STORE_BIZ_MENU_INVENTORY_INOUT_SUMMARY'),
        ('QA_GROUP_AUDITOR', 'STORE_BIZ_MENU_STOCK_INOUT_SUMMARY')
)
INSERT INTO sys_role_menu_rel (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN qa_role_menu q ON q.role_code = r.role_code
JOIN sys_menu m ON m.menu_code = q.menu_code
WHERE r.tenant_group_id IN (SELECT id FROM sys_group WHERE group_code IN ('QA-GRP-A', 'QA-GRP-B'))
ON CONFLICT DO NOTHING;

INSERT INTO sys_user (
    username, real_name, phone, password_hash, password_salt, status, source_type,
    created_scope_type, created_scope_id, first_login_changed_pwd
)
SELECT username, real_name, phone,
       '6460662e217c7a9f899208dd70a2c28abdea42f128666a9b78e6c0c064846493',
       NULL, 'ENABLED', 'SYSTEM_INIT', created_scope_type, created_scope_id, FALSE
FROM (
    SELECT 'qa_platform_admin' AS username, 'QA平台管理员' AS real_name, '13990000000' AS phone, 'PLATFORM' AS created_scope_type, 0::BIGINT AS created_scope_id
    UNION ALL SELECT 'qa_group_a_admin', 'QA集团A管理员', '13990000001', 'GROUP', (SELECT id FROM sys_group WHERE group_code = 'QA-GRP-A')
    UNION ALL SELECT 'qa_store_a1_admin', 'QA门店A1管理员', '13990000002', 'STORE', (SELECT id FROM sys_store WHERE store_code = 'QA-A1')
    UNION ALL SELECT 'qa_store_a1_self', 'QA门店A1仅自己业务员', '13990000003', 'STORE', (SELECT id FROM sys_store WHERE store_code = 'QA-A1')
    UNION ALL SELECT 'qa_store_a1_auditor', 'QA门店A1审核员', '13990000004', 'STORE', (SELECT id FROM sys_store WHERE store_code = 'QA-A1')
    UNION ALL SELECT 'qa_store_a2_operator', 'QA门店A2业务员', '13990000005', 'STORE', (SELECT id FROM sys_store WHERE store_code = 'QA-A2')
    UNION ALL SELECT 'qa_group_b_operator', 'QA集团B业务员', '13990000006', 'STORE', (SELECT id FROM sys_store WHERE store_code = 'QA-B1')
) seed
ON CONFLICT (phone) DO UPDATE
SET username = EXCLUDED.username,
    real_name = EXCLUDED.real_name,
    password_hash = EXCLUDED.password_hash,
    status = EXCLUDED.status,
    source_type = EXCLUDED.source_type,
    created_scope_type = EXCLUDED.created_scope_type,
    created_scope_id = EXCLUDED.created_scope_id,
    first_login_changed_pwd = EXCLUDED.first_login_changed_pwd,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_user_role_rel (user_id, role_id, scope_type, scope_id, assigned_by, status)
SELECT u.id, r.id, seed.scope_type, seed.scope_id, admin_user.id, 'ENABLED'
FROM (
    SELECT '13990000000' AS phone, 'PLATFORM_SUPER_ADMIN' AS role_code, 0::BIGINT AS tenant_group_id, 'PLATFORM' AS scope_type, NULL::BIGINT AS scope_id
    UNION ALL SELECT '13990000001', 'GROUP_ADMIN', 0, 'GROUP', (SELECT id FROM sys_group WHERE group_code = 'QA-GRP-A')
    UNION ALL SELECT '13990000002', 'STORE_ADMIN', 0, 'STORE', (SELECT id FROM sys_store WHERE store_code = 'QA-A1')
    UNION ALL SELECT '13990000003', 'SALESMAN', (SELECT id FROM sys_group WHERE group_code = 'QA-GRP-A'), 'STORE', (SELECT id FROM sys_store WHERE store_code = 'QA-A1')
    UNION ALL SELECT '13990000004', 'FINANCE', (SELECT id FROM sys_group WHERE group_code = 'QA-GRP-A'), 'STORE', (SELECT id FROM sys_store WHERE store_code = 'QA-A1')
    UNION ALL SELECT '13990000005', 'SALESMAN', (SELECT id FROM sys_group WHERE group_code = 'QA-GRP-A'), 'STORE', (SELECT id FROM sys_store WHERE store_code = 'QA-A2')
    UNION ALL SELECT '13990000006', 'SALESMAN', (SELECT id FROM sys_group WHERE group_code = 'QA-GRP-B'), 'STORE', (SELECT id FROM sys_store WHERE store_code = 'QA-B1')
) seed
JOIN sys_user u ON u.phone = seed.phone
JOIN sys_role r ON r.tenant_group_id = seed.tenant_group_id AND r.role_code = seed.role_code
JOIN sys_user admin_user ON admin_user.phone = '13990000000'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_role_rel existing
    WHERE existing.user_id = u.id
      AND existing.role_id = r.id
      AND existing.scope_type = seed.scope_type
      AND existing.scope_id IS NOT DISTINCT FROM seed.scope_id
);

INSERT INTO sys_store_admin_rel (store_id, user_id, assigned_by, status)
SELECT s.id, u.id, admin_user.id, 'ENABLED'
FROM sys_store s
JOIN sys_user u ON u.phone = '13990000002'
JOIN sys_user admin_user ON admin_user.phone = '13990000000'
WHERE s.store_code = 'QA-A1'
ON CONFLICT DO NOTHING;

INSERT INTO sys_unit (scope_type, scope_id, unit_code, unit_name, unit_type, status, remark)
SELECT 'PLATFORM', 0, unit_code, unit_name, unit_type, 'ENABLED', 'QA_INV_E2E'
FROM (
    VALUES
        ('QA-U-JIN', '斤', 'STANDARD'),
        ('QA-U-KG-CN', '公斤', 'STANDARD'),
        ('QA-U-G', '克', 'STANDARD'),
        ('QA-U-EA', '个', 'STANDARD'),
        ('QA-U-BOX', '箱', 'AUXILIARY'),
        ('QA-U-BAG', '袋', 'AUXILIARY'),
        ('QA-U-BOTTLE', '瓶', 'AUXILIARY'),
        ('QA-U-PACK', '包', 'AUXILIARY'),
        ('QA-U-SERVE', '份', 'STANDARD')
) seed(unit_code, unit_name, unit_type)
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_unit u
    WHERE u.scope_type = 'PLATFORM'
      AND u.scope_id = 0
      AND u.unit_name = seed.unit_name
)
ON CONFLICT (scope_type, scope_id, unit_code) DO UPDATE
SET unit_name = EXCLUDED.unit_name,
    unit_type = EXCLUDED.unit_type,
    status = EXCLUDED.status,
    remark = EXCLUDED.remark,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO item_category (scope_type, scope_id, category_code, category_name, parent_category, status, remark)
SELECT scope_type, scope_id, category_code, category_name, '物品类别', '启用', 'QA_INV_E2E'
FROM (
    SELECT 'GROUP' AS scope_type, g.id AS scope_id, category_code, category_name
    FROM sys_group g
    CROSS JOIN (
        VALUES
            ('QA-CAT-VEG', '蔬菜'),
            ('QA-CAT-MEAT', '肉类'),
            ('QA-CAT-SEAFOOD', '水产'),
            ('QA-CAT-SEASONING', '调料'),
            ('QA-CAT-PASTRY', '面点'),
            ('QA-CAT-PREPARED', '预制菜'),
            ('QA-CAT-DRINK', '酒水'),
            ('QA-CAT-DISPOSABLE', '一次性用品'),
            ('QA-CAT-FRONT', '前厅类'),
            ('QA-CAT-DAILY', '日用百货'),
            ('QA-CAT-SEMI', '半成品'),
            ('QA-CAT-FINISHED', '成品')
    ) seed(category_code, category_name)
    WHERE g.group_code IN ('QA-GRP-A', 'QA-GRP-B')
    UNION ALL
    SELECT 'STORE', s.id, category_code, category_name
    FROM sys_store s
    CROSS JOIN (
        VALUES
            ('QA-CAT-VEG', '蔬菜'),
            ('QA-CAT-MEAT', '肉类'),
            ('QA-CAT-SEAFOOD', '水产'),
            ('QA-CAT-SEASONING', '调料'),
            ('QA-CAT-PASTRY', '面点'),
            ('QA-CAT-PREPARED', '预制菜'),
            ('QA-CAT-DRINK', '酒水'),
            ('QA-CAT-DISPOSABLE', '一次性用品'),
            ('QA-CAT-FRONT', '前厅类'),
            ('QA-CAT-DAILY', '日用百货'),
            ('QA-CAT-SEMI', '半成品'),
            ('QA-CAT-FINISHED', '成品')
    ) seed(category_code, category_name)
    WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM item_category c
    WHERE c.scope_type = seed.scope_type
      AND c.scope_id = seed.scope_id
      AND c.category_name = seed.category_name
)
ON CONFLICT (scope_type, scope_id, category_code) DO UPDATE
SET category_name = EXCLUDED.category_name,
    parent_category = EXCLUDED.parent_category,
    status = EXCLUDED.status,
    remark = EXCLUDED.remark,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO supplier_category (scope_type, scope_id, category_code, category_name, parent_category)
SELECT scope_type, scope_id, category_code, category_name, '供应商类别'
FROM (
    SELECT 'GROUP' AS scope_type, g.id AS scope_id, 'QA-SUP-CAT-GROUP' AS category_code, 'QA集团供应商' AS category_name
    FROM sys_group g WHERE g.group_code IN ('QA-GRP-A', 'QA-GRP-B')
    UNION ALL
    SELECT 'STORE', s.id, 'QA-SUP-CAT-STORE', 'QA门店供应商'
    FROM sys_store s WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM supplier_category c
    WHERE c.scope_type = seed.scope_type
      AND c.scope_id = seed.scope_id
      AND c.category_name = seed.category_name
)
ON CONFLICT (scope_type, scope_id, category_code) DO UPDATE
SET category_name = EXCLUDED.category_name,
    parent_category = EXCLUDED.parent_category,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO supplier_profile (
    scope_type, scope_id, supplier_code, supplier_name, supplier_short_name, supplier_mnemonic,
    supplier_category, tax_rate, status, contact_person, contact_phone, email, contact_address,
    remark, settlement_method, order_summary_rule, input_batch_when_delivery, sync_receipt_data,
    purchase_receipt_depend_shipping, delivery_depend_shipping, supplier_manage_inventory,
    control_order_time, allow_close_order, reconciliation_mode, scope_control, source,
    supply_relation, bind_status, invoice_company_name, taxpayer_id, invoice_phone, invoice_address
)
SELECT scope_type, scope_id, supplier_code, supplier_name, supplier_short_name, supplier_mnemonic,
       supplier_category, tax_rate, '启用', contact_person, contact_phone, email, contact_address,
       'QA_INV_E2E', '月结', '按机构', input_batch_when_delivery, TRUE,
       '不依赖', '不依赖', supplier_manage_inventory,
       FALSE, TRUE, '按单对账', '开启', source,
       supply_relation, bind_status, supplier_name, taxpayer_id, contact_phone, contact_address
FROM (
    SELECT 'GROUP' AS scope_type,
           g.id AS scope_id,
           'QA-SUP-G' || lpad(n::TEXT, 2, '0') AS supplier_code,
           'QA集团供应商' || n AS supplier_name,
           'QA集供' || n AS supplier_short_name,
           'QAJG' || n AS supplier_mnemonic,
           'QA集团供应商' AS supplier_category,
           CASE n % 4 WHEN 0 THEN 0 WHEN 1 THEN 6 WHEN 2 THEN 9 ELSE 13 END::NUMERIC(8,2) AS tax_rate,
           'QA联系人G' || n AS contact_person,
           '1399010' || lpad(n::TEXT, 4, '0') AS contact_phone,
           'qa-supplier-g' || n || '@example.test' AS email,
           'QA集团供应商地址' || n AS contact_address,
           n % 2 = 0 AS input_batch_when_delivery,
           n IN (2, 4, 6, 8) AS supplier_manage_inventory,
           '集团' AS source,
           CASE WHEN n IN (3, 7) THEN '无' ELSE '有' END AS supply_relation,
           CASE WHEN n <= 4 THEN '已绑定' ELSE '未绑定' END AS bind_status,
           'QA-TAX-G' || lpad(n::TEXT, 2, '0') AS taxpayer_id
    FROM sys_group g
    CROSS JOIN generate_series(1, 8) AS n
    WHERE g.group_code = 'QA-GRP-A'
    UNION ALL
    SELECT 'STORE',
           s.id,
           'QA-SUP-' || s.store_code || '-' || lpad(n::TEXT, 2, '0'),
           'QA' || s.store_name || '供应商' || n,
           'QA门供' || n,
           'QAMG' || n,
           'QA门店供应商',
           CASE n % 3 WHEN 0 THEN 0 WHEN 1 THEN 6 ELSE 13 END::NUMERIC(8,2),
           'QA联系人S' || n,
           '1399020' || lpad((s.id % 100)::TEXT, 2, '0') || lpad(n::TEXT, 2, '0'),
           'qa-supplier-' || lower(s.store_code) || '-' || n || '@example.test',
           s.store_name || '供应商地址' || n,
           n % 2 = 1,
           n IN (1, 3),
           '门店',
           CASE WHEN n = 4 THEN '无' ELSE '有' END,
           CASE WHEN n <= 2 THEN '已绑定' ELSE '未绑定' END,
           'QA-TAX-S' || s.store_code || '-' || lpad(n::TEXT, 2, '0')
    FROM sys_store s
    CROSS JOIN generate_series(1, 4) AS n
    WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
) seed
ON CONFLICT (scope_type, scope_id, supplier_code) DO UPDATE
SET supplier_name = EXCLUDED.supplier_name,
    supplier_short_name = EXCLUDED.supplier_short_name,
    supplier_mnemonic = EXCLUDED.supplier_mnemonic,
    supplier_category = EXCLUDED.supplier_category,
    tax_rate = EXCLUDED.tax_rate,
    status = EXCLUDED.status,
    contact_person = EXCLUDED.contact_person,
    contact_phone = EXCLUDED.contact_phone,
    email = EXCLUDED.email,
    contact_address = EXCLUDED.contact_address,
    remark = EXCLUDED.remark,
    settlement_method = EXCLUDED.settlement_method,
    order_summary_rule = EXCLUDED.order_summary_rule,
    input_batch_when_delivery = EXCLUDED.input_batch_when_delivery,
    sync_receipt_data = EXCLUDED.sync_receipt_data,
    purchase_receipt_depend_shipping = EXCLUDED.purchase_receipt_depend_shipping,
    delivery_depend_shipping = EXCLUDED.delivery_depend_shipping,
    supplier_manage_inventory = EXCLUDED.supplier_manage_inventory,
    control_order_time = EXCLUDED.control_order_time,
    allow_close_order = EXCLUDED.allow_close_order,
    reconciliation_mode = EXCLUDED.reconciliation_mode,
    scope_control = EXCLUDED.scope_control,
    source = EXCLUDED.source,
    supply_relation = EXCLUDED.supply_relation,
    bind_status = EXCLUDED.bind_status,
    invoice_company_name = EXCLUDED.invoice_company_name,
    taxpayer_id = EXCLUDED.taxpayer_id,
    invoice_phone = EXCLUDED.invoice_phone,
    invoice_address = EXCLUDED.invoice_address,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_warehouse (
    store_id, group_id, warehouse_code, warehouse_name, department, status, warehouse_type,
    contact_name, contact_phone, region_path, address, target_gross_margin,
    ideal_purchase_sale_ratio, is_default, remark
)
SELECT s.id, s.group_id, payload.warehouse_code, payload.warehouse_name, payload.warehouse_department, 'ENABLED', payload.warehouse_type,
       'QA仓管', '13990300000', 'QA省/QA市/QA区', s.store_name || '-' || payload.warehouse_name,
       '35%', '1:1', payload.is_default, 'QA_INV_E2E'
FROM sys_store s
CROSS JOIN LATERAL (
    VALUES
        ('MAIN', '主仓', '仓储部', '普通仓库', TRUE),
        ('PROD', '生产仓', '生产部', '出品及生产部门', FALSE),
        ('ADMIN', '行政仓', '行政部', '行政部门', FALSE),
        ('RETURN', '退货调拨仓', '仓储部', '普通仓库', FALSE)
) wh(code_suffix, warehouse_suffix, department, warehouse_type, is_default)
CROSS JOIN LATERAL (
    SELECT 'QA-WH-' || s.store_code || '-' || wh.code_suffix AS warehouse_code,
           s.store_name || wh.warehouse_suffix AS warehouse_name,
           wh.department AS warehouse_department,
           wh.warehouse_type AS warehouse_type,
           wh.is_default AS is_default
) payload
WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
ON CONFLICT (warehouse_code) DO UPDATE
SET store_id = EXCLUDED.store_id,
    group_id = EXCLUDED.group_id,
    warehouse_name = EXCLUDED.warehouse_name,
    department = EXCLUDED.department,
    status = EXCLUDED.status,
    warehouse_type = EXCLUDED.warehouse_type,
    contact_name = EXCLUDED.contact_name,
    contact_phone = EXCLUDED.contact_phone,
    region_path = EXCLUDED.region_path,
    address = EXCLUDED.address,
    target_gross_margin = EXCLUDED.target_gross_margin,
    ideal_purchase_sale_ratio = EXCLUDED.ideal_purchase_sale_ratio,
    is_default = EXCLUDED.is_default,
    remark = EXCLUDED.remark,
    updated_at = CURRENT_TIMESTAMP;

WITH group_items AS (
    SELECT g.id AS scope_id,
           n,
           'QA-G-' || lpad(n::TEXT, 3, '0') AS item_code,
           'qa-group-' || g.id || '-' || lpad(n::TEXT, 3, '0') AS item_id,
           CASE ((n - 1) % 12)
               WHEN 0 THEN '蔬菜'
               WHEN 1 THEN '肉类'
               WHEN 2 THEN '水产'
               WHEN 3 THEN '调料'
               WHEN 4 THEN '面点'
               WHEN 5 THEN '预制菜'
               WHEN 6 THEN '酒水'
               WHEN 7 THEN '一次性用品'
               WHEN 8 THEN '前厅类'
               WHEN 9 THEN '日用百货'
               WHEN 10 THEN '半成品'
               ELSE '成品'
           END AS category,
           CASE
               WHEN n <= 10 THEN 'BASE'
               WHEN n <= 20 THEN 'MULTI'
               ELSE 'GROUP'
           END AS kind
    FROM sys_group g
    CROSS JOIN generate_series(1, 20) AS n
    WHERE g.group_code = 'QA-GRP-A'
),
store_items AS (
    SELECT s.id AS scope_id,
           s.store_code,
           n,
           'QA-' || s.store_code || '-' || lpad(n::TEXT, 3, '0') AS item_code,
           'qa-store-' || s.id || '-' || lpad(n::TEXT, 3, '0') AS item_id,
           CASE ((n - 1) % 12)
               WHEN 0 THEN '蔬菜'
               WHEN 1 THEN '肉类'
               WHEN 2 THEN '水产'
               WHEN 3 THEN '调料'
               WHEN 4 THEN '面点'
               WHEN 5 THEN '预制菜'
               WHEN 6 THEN '酒水'
               WHEN 7 THEN '一次性用品'
               WHEN 8 THEN '前厅类'
               WHEN 9 THEN '日用百货'
               WHEN 10 THEN '半成品'
               ELSE '成品'
           END AS category,
           CASE
               WHEN n <= 10 THEN 'BASE'
               WHEN n <= 20 THEN 'MULTI'
               WHEN n <= 28 THEN 'BATCH'
               WHEN n <= 34 THEN 'RAW'
               WHEN n <= 38 THEN 'SEMI'
               ELSE 'FINISHED'
           END AS kind
    FROM sys_store s
    CROSS JOIN generate_series(1, 40) AS n
    WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
),
items AS (
    SELECT 'GROUP' AS scope_type,
           scope_id,
           item_id,
           item_code,
           'QA集团共用物品' || n AS item_name,
           category,
           n,
           kind,
           'QA集团供应商' || (((n - 1) % 8) + 1) AS supplier_name
    FROM group_items
    UNION ALL
    SELECT 'STORE',
           scope_id,
           item_id,
           item_code,
           'QA' || store_code || '测试物品' || n,
           category,
           n,
           kind,
           'QA' || (SELECT store_name FROM sys_store WHERE id = scope_id) || '供应商' || (((n - 1) % 4) + 1)
    FROM store_items
)
INSERT INTO item_profile (scope_type, scope_id, item_id, item_code, detail_json, is_draft)
SELECT scope_type,
       scope_id,
       item_id,
       item_code,
       (
       jsonb_build_object(
           'name', item_name,
           'code', item_code,
           'category', category,
           'spec', CASE kind WHEN 'MULTI' THEN '多单位规格' WHEN 'BATCH' THEN '批次规格' ELSE '标准规格' END,
           'status', CASE WHEN n = 40 THEN '停用' ELSE '启用' END,
           'brand', 'QA品牌',
           'mnemonicCode', 'QA' || n,
           'barcode', 'QA-BAR-' || item_code,
           'thirdPartyCode', 'QA-TP-' || item_code,
           'defaultPurchaseUnit', CASE WHEN kind = 'MULTI' THEN '箱' WHEN kind = 'BATCH' THEN '袋' ELSE '斤' END,
           'defaultOrderUnit', CASE WHEN kind = 'MULTI' THEN '箱' WHEN kind = 'BATCH' THEN '袋' ELSE '斤' END,
           'defaultStockUnit', CASE WHEN kind = 'MULTI' THEN '袋' WHEN kind = 'BATCH' THEN '斤' ELSE '斤' END,
           'defaultCostUnit', CASE WHEN kind IN ('MULTI', 'BATCH') THEN '克' ELSE '斤' END,
           'stocktakeUnits', CASE WHEN n IN (35, 36) THEN jsonb_build_array('份') ELSE jsonb_build_array('斤', '袋') END,
           'assistUnitEnabled', kind IN ('MULTI', 'BATCH'),
           'productionRefCost', to_char((2 + n * 0.35)::NUMERIC, 'FM999999990.00'),
           'monthEndUpdate', false,
           'suggestPurchasePrice', to_char((3 + n * 0.45)::NUMERIC, 'FM999999990.00'),
           'storageMode', '常温',
           'statType', CASE WHEN kind IN ('RAW', 'SEMI', 'FINISHED') THEN '生产物料' ELSE '采购物料' END,
           'stockMin', CASE WHEN n = 39 THEN '50' ELSE '5' END,
           'stockMax', '500',
           'safeStock', CASE WHEN n = 39 THEN '80' ELSE '20' END,
           'taxCode', 'QA-TAX',
           'taxName', '增值税',
           'taxRate', CASE n % 4 WHEN 0 THEN '0' WHEN 1 THEN '6' WHEN 2 THEN '9' ELSE '13' END,
           'taxBenefit', '无',
           'consumeOnInbound', CASE WHEN kind = 'RAW' THEN '是' ELSE '否' END,
           'disableStocktake', CASE WHEN n = 37 THEN '是' ELSE '否' END,
           'defaultNoStocktake', CASE WHEN n = 38 THEN '是' ELSE '否' END,
           'stocktakeTypes', jsonb_build_array('日盘点', '周盘点'),
           'purchaseReceiptRule', '按订单收货',
           'purchaseRuleMaxRatio', '120',
           'purchaseRuleMinRatio', '80',
           'requireAssemblyProcess', kind = 'FINISHED',
           'requireSplitProcess', kind = 'SEMI',
           'requireBatchReport', kind = 'BATCH',
           'allowLossReport', true,
           'allowTransfer', true,
           'enablePrepare', kind IN ('RAW', 'SEMI', 'FINISHED')
       )
       ||
       jsonb_build_object(
           'productCategory', kind,
           'netContent', '1',
           'ingredients', 'QA测试成分',
           'itemDescription', 'QA_INV_E2E',
           'remark', 'QA_INV_E2E',
           'alias', item_name || '别名',
           'abcClass', CASE WHEN n % 3 = 0 THEN 'A' WHEN n % 3 = 1 THEN 'B' ELSE 'C' END,
           'batchManagement', kind = 'BATCH',
           'shelfLifeEnabled', kind = 'BATCH',
           'shelfLifeDays', CASE WHEN kind = 'BATCH' THEN 180 ELSE NULL END,
           'warningDays', CASE WHEN kind = 'BATCH' THEN 15 ELSE NULL END,
           'stagnantDays', 30,
           'tag', 'QA_INV_E2E',
           'unitSettingRows', CASE
               WHEN kind = 'MULTI' THEN jsonb_build_array(
                   jsonb_build_object('unit', '袋', 'convertFrom', '1', 'convertTo', '1', 'volume', '0.010', 'volumeUnit', 'dm³', 'weight', '5.000', 'weightUnit', '斤', 'barcode', 'QA-U-' || item_code || '-BAG'),
                   jsonb_build_object('unit', '箱', 'convertFrom', '1', 'convertTo', '10', 'volume', '0.100', 'volumeUnit', 'dm³', 'weight', '50.000', 'weightUnit', '斤', 'barcode', 'QA-U-' || item_code || '-BOX'),
                   jsonb_build_object('unit', '克', 'convertFrom', '1', 'convertTo', '500', 'volume', '0.001', 'volumeUnit', 'dm³', 'weight', '1.000', 'weightUnit', '克', 'barcode', 'QA-U-' || item_code || '-G')
               )
               WHEN kind = 'BATCH' THEN jsonb_build_array(
                   jsonb_build_object('unit', '斤', 'convertFrom', '1', 'convertTo', '1', 'volume', '0.002', 'volumeUnit', 'dm³', 'weight', '1.000', 'weightUnit', '斤', 'barcode', 'QA-U-' || item_code || '-JIN'),
                   jsonb_build_object('unit', '袋', 'convertFrom', '1', 'convertTo', '20', 'volume', '0.040', 'volumeUnit', 'dm³', 'weight', '20.000', 'weightUnit', '斤', 'barcode', 'QA-U-' || item_code || '-BAG')
               )
               ELSE jsonb_build_array(
                   jsonb_build_object('unit', '斤', 'convertFrom', '1', 'convertTo', '1', 'volume', '0.002', 'volumeUnit', 'dm³', 'weight', '1.000', 'weightUnit', '斤', 'barcode', 'QA-U-' || item_code || '-JIN')
               )
           END,
           'supplierRelationRows', CASE
               WHEN n = 36 THEN jsonb_build_array()
               ELSE jsonb_build_array(jsonb_build_object('key', 1, 'supplier', supplier_name, 'contact', 'QA联系人', 'phone', '13990009999'))
           END,
           'defaultSupplierRowKey', CASE WHEN n = 36 THEN NULL ELSE 1 END,
           'nutritionHeaders', jsonb_build_object('item', '项目', 'per100g', '每100克', 'nrv', 'NRV%'),
           'nutritionRows', jsonb_build_array(jsonb_build_object('id', 1, 'item', '能量', 'per100g', '100', 'nrv', '5%')),
           'extensionInfoRows', jsonb_build_array(jsonb_build_object('id', 1, 'name', 'QA标记', 'value', 'QA_INV_E2E'))
       )
       )::TEXT,
       FALSE
FROM items
ON CONFLICT (item_id) DO UPDATE
SET item_code = EXCLUDED.item_code,
    scope_type = EXCLUDED.scope_type,
    scope_id = EXCLUDED.scope_id,
    detail_json = EXCLUDED.detail_json,
    is_draft = EXCLUDED.is_draft,
    updated_at = CURRENT_TIMESTAMP;

WITH stock_items AS (
    SELECT s.id AS store_id,
           w.warehouse_name,
           p.item_code,
           p.detail_json::JSONB ->> 'name' AS item_name,
           ((substring(p.item_code FROM '([0-9]{3})$'))::INTEGER) AS item_no
    FROM sys_store s
    JOIN sys_warehouse w ON w.store_id = s.id AND w.warehouse_code LIKE 'QA-WH-%'
    JOIN item_profile p ON p.scope_type = 'STORE' AND p.scope_id = s.id
    WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
      AND p.item_code LIKE 'QA-%'
      AND (p.detail_json::JSONB ->> 'status') = '启用'
      AND (
          ((substring(p.item_code FROM '([0-9]{3})$'))::INTEGER) IN (1, 2, 3, 4, 5, 11, 12, 13, 21, 22)
          OR (
              w.warehouse_code LIKE 'QA-WH-%-MAIN'
              AND ((substring(p.item_code FROM '([0-9]{3})$'))::INTEGER) <= 34
          )
      )
)
INSERT INTO inventory_balance (scope_type, scope_id, warehouse_name, item_code, item_name, quantity)
SELECT 'STORE',
       store_id,
       warehouse_name,
       item_code,
       item_name,
       CASE
           WHEN item_no <= 10 THEN 100
           WHEN item_no <= 20 THEN 80
           WHEN item_no <= 28 THEN 60
           ELSE 40
       END::NUMERIC(18,4)
       + CASE
           WHEN warehouse_name LIKE '%生产仓' THEN 5
           WHEN warehouse_name LIKE '%行政仓' THEN 2
           WHEN warehouse_name LIKE '%退货调拨仓' THEN 1
           ELSE 0
       END::NUMERIC(18,4)
FROM stock_items
ON CONFLICT (scope_type, scope_id, warehouse_name, item_code) DO UPDATE
SET item_name = EXCLUDED.item_name,
    quantity = EXCLUDED.quantity,
    updated_at = CURRENT_TIMESTAMP;

WITH balance_rows AS (
    SELECT b.*,
           row_number() OVER (PARTITION BY b.scope_type, b.scope_id, b.warehouse_name, b.item_code ORDER BY b.id) AS rn
    FROM inventory_balance b
    JOIN sys_store s ON b.scope_type = 'STORE' AND b.scope_id = s.id
    WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
      AND b.item_code LIKE 'QA-%'
)
INSERT INTO inventory_transaction (
    scope_type, scope_id, biz_type, biz_id, biz_line_id, warehouse_name,
    item_code, item_name, quantity_delta, before_qty, after_qty, operator_id, created_at
)
SELECT b.scope_type,
       b.scope_id,
       'QA_OPENING_BALANCE',
       b.id,
       NULL,
       b.warehouse_name,
       b.item_code,
       b.item_name,
       b.quantity,
       0,
       b.quantity,
       (SELECT id FROM sys_user WHERE phone = '13990000000'),
       CURRENT_TIMESTAMP
FROM balance_rows b
WHERE b.rn = 1
  AND NOT EXISTS (
      SELECT 1
      FROM inventory_transaction t
      WHERE t.biz_type = 'QA_OPENING_BALANCE'
        AND t.biz_id = b.id
        AND t.item_code = b.item_code
        AND t.warehouse_name = b.warehouse_name
  );

DO $$
DECLARE
    missing_units INTEGER;
    missing_store_items INTEGER;
    missing_warehouses INTEGER;
BEGIN
    SELECT 9 - COUNT(DISTINCT unit_name)
    INTO missing_units
    FROM sys_unit
    WHERE unit_name IN ('斤', '公斤', '克', '个', '箱', '袋', '瓶', '包', '份')
      AND status = 'ENABLED';

    IF missing_units > 0 THEN
        RAISE EXCEPTION 'QA unit seed incomplete. Missing count: %', missing_units;
    END IF;

    SELECT COUNT(*)
    INTO missing_store_items
    FROM (
        SELECT s.store_code, COUNT(p.id) AS item_count
        FROM sys_store s
        LEFT JOIN item_profile p ON p.scope_type = 'STORE'
                                AND p.scope_id = s.id
                                AND p.item_code LIKE 'QA-' || s.store_code || '-%'
                                AND p.is_draft = FALSE
                                AND (p.detail_json::JSONB ->> 'status') = '启用'
        WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
        GROUP BY s.store_code
        HAVING COUNT(p.id) < 39
    ) invalid;

    IF missing_store_items > 0 THEN
        RAISE EXCEPTION 'QA store item seed incomplete. Some stores have less than 39 enabled QA items.';
    END IF;

    SELECT COUNT(*)
    INTO missing_warehouses
    FROM (
        SELECT s.store_code, COUNT(w.id) AS warehouse_count
        FROM sys_store s
        LEFT JOIN sys_warehouse w ON w.store_id = s.id AND w.warehouse_code LIKE 'QA-WH-%'
        WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
        GROUP BY s.store_code
        HAVING COUNT(w.id) < 4
    ) invalid;

    IF missing_warehouses > 0 THEN
        RAISE EXCEPTION 'QA warehouse seed incomplete. Some stores have less than 4 QA warehouses.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM sys_store s
        JOIN sys_warehouse w ON w.store_id = s.id AND w.warehouse_code LIKE 'QA-WH-%'
        LEFT JOIN inventory_balance b ON b.scope_type = 'STORE'
                                    AND b.scope_id = s.id
                                    AND b.warehouse_name = w.warehouse_name
                                    AND b.item_code LIKE 'QA-%'
                                    AND b.quantity > 0
        WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
        GROUP BY s.store_code, w.warehouse_name
        HAVING COUNT(b.id) < 10
    ) THEN
        RAISE EXCEPTION 'QA inventory balance seed incomplete. Each QA warehouse must have at least 10 stock items.';
    END IF;
END $$;

SELECT 'QA inventory E2E seed completed' AS result;
