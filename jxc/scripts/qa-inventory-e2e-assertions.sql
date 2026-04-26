-- QA inventory E2E assertions.
-- Run after qa-inventory-e2e-seed.sql and after each E2E test batch.

\set ON_ERROR_STOP on

SET search_path TO dev;

WITH schema_checks AS (
    SELECT 'schema.required_tables' AS check_name,
           to_regclass('dev.sys_group') IS NOT NULL
           AND to_regclass('dev.item_profile') IS NOT NULL
           AND to_regclass('dev.inventory_balance') IS NOT NULL
           AND to_regclass('dev.inventory_transaction') IS NOT NULL AS passed,
           'sys_group,item_profile,inventory_balance,inventory_transaction' AS details
)
SELECT CASE WHEN passed THEN 'PASS' ELSE 'FAIL' END AS status,
       check_name,
       details
FROM schema_checks;

DROP VIEW IF EXISTS qa_inventory_docs;
CREATE TEMP VIEW qa_inventory_docs AS
SELECT 'PURCHASE_INBOUND' AS biz_type, id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_purchase_inbound
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%'
   OR COALESCE(document_code, '') LIKE 'QA-%'
   OR COALESCE(upstream_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'PURCHASE_RETURN_OUTBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_purchase_return_outbound
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'DEPARTMENT_PICKING', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_department_picking
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'STOCK_TRANSFER', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_stock_transfer
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'STOCK_TRANSFER_INBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_stock_transfer_inbound
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'DEPARTMENT_TRANSFER', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_department_transfer
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'DAMAGE_OUTBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_damage_outbound
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'OTHER_INBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_other_inbound
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'OTHER_OUTBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_other_outbound
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'PRODUCTION_INBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_production_inbound
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'CUSTOMER_SALES_OUTBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_customer_sales_outbound
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'CUSTOMER_RETURN_INBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_customer_return_inbound
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'DISH_CONSUMPTION_OUTBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_dish_consumption_outbound
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'STORE_TRANSFER', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_store_transfer
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
UNION ALL
SELECT 'STOCK_TRANSFER_OUTBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id, upstream_code
FROM inventory_stock_transfer_outbound
WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%';

DROP VIEW IF EXISTS qa_inventory_doc_lines;
CREATE TEMP VIEW qa_inventory_doc_lines AS
SELECT 'PURCHASE_INBOUND' AS biz_type, inbound_id AS header_id, id AS line_id, item_code, category, quantity
FROM inventory_purchase_inbound_line
UNION ALL
SELECT 'PURCHASE_RETURN_OUTBOUND', header_id, id, item_code, category, quantity FROM inventory_purchase_return_outbound_line
UNION ALL
SELECT 'DEPARTMENT_PICKING', header_id, id, item_code, category, quantity FROM inventory_department_picking_line
UNION ALL
SELECT 'STOCK_TRANSFER', header_id, id, item_code, category, quantity FROM inventory_stock_transfer_line
UNION ALL
SELECT 'STOCK_TRANSFER_INBOUND', header_id, id, item_code, category, quantity FROM inventory_stock_transfer_inbound_line
UNION ALL
SELECT 'DEPARTMENT_TRANSFER', header_id, id, item_code, category, quantity FROM inventory_department_transfer_line
UNION ALL
SELECT 'DAMAGE_OUTBOUND', header_id, id, item_code, category, quantity FROM inventory_damage_outbound_line
UNION ALL
SELECT 'OTHER_INBOUND', header_id, id, item_code, category, quantity FROM inventory_other_inbound_line
UNION ALL
SELECT 'OTHER_OUTBOUND', header_id, id, item_code, category, quantity FROM inventory_other_outbound_line
UNION ALL
SELECT 'PRODUCTION_INBOUND', header_id, id, item_code, category, quantity FROM inventory_production_inbound_line
UNION ALL
SELECT 'CUSTOMER_SALES_OUTBOUND', header_id, id, item_code, category, quantity FROM inventory_customer_sales_outbound_line
UNION ALL
SELECT 'CUSTOMER_RETURN_INBOUND', header_id, id, item_code, category, quantity FROM inventory_customer_return_inbound_line
UNION ALL
SELECT 'DISH_CONSUMPTION_OUTBOUND', header_id, id, item_code, category, quantity FROM inventory_dish_consumption_outbound_line
UNION ALL
SELECT 'STORE_TRANSFER', header_id, id, item_code, category, quantity FROM inventory_store_transfer_line
UNION ALL
SELECT 'STOCK_TRANSFER_OUTBOUND', header_id, id, item_code, category, quantity FROM inventory_stock_transfer_outbound_line;

DROP VIEW IF EXISTS qa_expected_inventory_business;
CREATE TEMP VIEW qa_expected_inventory_business AS
SELECT *
FROM (VALUES
    ('PURCHASE_INBOUND', 'INBOUND'),
    ('STOCK_TRANSFER_INBOUND', 'INBOUND'),
    ('PRODUCTION_INBOUND', 'INBOUND'),
    ('OTHER_INBOUND', 'INBOUND'),
    ('CUSTOMER_RETURN_INBOUND', 'INBOUND'),
    ('PURCHASE_RETURN_OUTBOUND', 'OUTBOUND'),
    ('DEPARTMENT_PICKING', 'OUTBOUND'),
    ('STOCK_TRANSFER', 'OUTBOUND'),
    ('DEPARTMENT_TRANSFER', 'OUTBOUND'),
    ('DAMAGE_OUTBOUND', 'OUTBOUND'),
    ('OTHER_OUTBOUND', 'OUTBOUND'),
    ('CUSTOMER_SALES_OUTBOUND', 'OUTBOUND'),
    ('DISH_CONSUMPTION_OUTBOUND', 'OUTBOUND'),
    ('STORE_TRANSFER', 'OUTBOUND'),
    ('STOCK_TRANSFER_OUTBOUND', 'OUTBOUND')
) AS expected(biz_type, expected_direction);

WITH checks AS (
    SELECT 'master.groups' AS check_name,
           COUNT(*) >= 2 AS passed,
           COUNT(*)::TEXT AS details
    FROM sys_group
    WHERE group_code IN ('QA-GRP-A', 'QA-GRP-B')

    UNION ALL
    SELECT 'master.stores',
           COUNT(*) = 3,
           COUNT(*)::TEXT
    FROM sys_store
    WHERE store_code IN ('QA-A1', 'QA-A2', 'QA-B1')

    UNION ALL
    SELECT 'permissions.qa_a1_salesman_menu_count',
           COUNT(DISTINCT m.id) >= 40,
           COUNT(DISTINCT m.id)::TEXT
    FROM sys_user u
    JOIN sys_user_role_rel ur ON ur.user_id = u.id AND ur.status = 'ENABLED'
    JOIN sys_role_menu_rel rm ON rm.role_id = ur.role_id
    JOIN sys_menu m ON m.id = rm.menu_id AND m.status = 'ENABLED'
    WHERE u.phone = '13990000003'

    UNION ALL
    SELECT 'permissions.qa_a1_auditor_menu_count',
           COUNT(DISTINCT m.id) >= 45,
           COUNT(DISTINCT m.id)::TEXT
    FROM sys_user u
    JOIN sys_user_role_rel ur ON ur.user_id = u.id AND ur.status = 'ENABLED'
    JOIN sys_role_menu_rel rm ON rm.role_id = ur.role_id
    JOIN sys_menu m ON m.id = rm.menu_id AND m.status = 'ENABLED'
    WHERE u.phone = '13990000004'

    UNION ALL
    SELECT 'permissions.qa_a1_auditor_has_purchase_review_menu',
           COUNT(*) > 0,
           CASE WHEN COUNT(*) > 0 THEN 'present' ELSE 'missing' END
    FROM sys_user u
    JOIN sys_user_role_rel ur ON ur.user_id = u.id AND ur.status = 'ENABLED'
    JOIN sys_role_menu_rel rm ON rm.role_id = ur.role_id
    JOIN sys_menu m ON m.id = rm.menu_id
    WHERE u.phone = '13990000004'
      AND m.menu_code = 'STORE_BIZ_MENU_PURCHASE_APPLICATION_REVIEW'

    UNION ALL
    SELECT 'permissions.qa_a1_salesman_no_purchase_review_menu',
           COUNT(*) = 0,
           COUNT(*)::TEXT
    FROM sys_user u
    JOIN sys_user_role_rel ur ON ur.user_id = u.id AND ur.status = 'ENABLED'
    JOIN sys_role_menu_rel rm ON rm.role_id = ur.role_id
    JOIN sys_menu m ON m.id = rm.menu_id
    WHERE u.phone = '13990000003'
      AND m.menu_code = 'STORE_BIZ_MENU_PURCHASE_APPLICATION_REVIEW'

    UNION ALL
    SELECT 'workflow.qa_a1_purchase_application_published',
           COUNT(*) > 0,
           CASE WHEN COUNT(*) > 0 THEN 'published' ELSE 'missing' END
    FROM workflow_definition_config config
    JOIN sys_store s ON s.store_code = 'QA-A1'
    WHERE config.scope_type = 'GROUP'
      AND config.scope_id = s.group_id
      AND config.business_code = 'PURCHASE_APPLICATION'
      AND config.workflow_code = 'BUILTIN_DEFAULT_APPROVAL'
      AND config.status = 'PUBLISHED'
      AND COALESCE(config.process_definition_key, '') <> ''
      AND COALESCE(config.process_definition_id, '') <> ''

    UNION ALL
    SELECT 'workflow.qa_a1_purchase_application_finance_review_node',
           COUNT(*) > 0,
           CASE WHEN COUNT(*) > 0 THEN 'present' ELSE 'missing' END
    FROM workflow_definition_config config
    JOIN sys_store s ON s.store_code = 'QA-A1'
    CROSS JOIN LATERAL jsonb_array_elements(config.node_config_json::JSONB) node
    WHERE config.scope_type = 'GROUP'
      AND config.scope_id = s.group_id
      AND config.business_code = 'PURCHASE_APPLICATION'
      AND config.workflow_code = 'BUILTIN_DEFAULT_APPROVAL'
      AND config.status = 'PUBLISHED'
      AND node ->> 'approverRoleCode' = 'FINANCE'
      AND COALESCE(node ->> 'triggerActions', '[]') = '[]'

    UNION ALL
    SELECT 'master.units',
           COUNT(DISTINCT unit_name) >= 9,
           COUNT(DISTINCT unit_name)::TEXT
    FROM sys_unit
    WHERE unit_name IN ('斤', '公斤', '克', '个', '箱', '袋', '瓶', '包', '份')
      AND status = 'ENABLED'

    UNION ALL
    SELECT 'master.categories.store_scope',
           MIN(category_count) >= 12,
           string_agg(store_code || '=' || category_count, ', ' ORDER BY store_code)
    FROM (
        SELECT s.store_code, COUNT(c.id) AS category_count
        FROM sys_store s
        LEFT JOIN item_category c ON c.scope_type = 'STORE'
                                 AND c.scope_id = s.id
                                 AND c.status = '启用'
                                 AND c.category_name IN (
                                     '蔬菜', '肉类', '水产', '调料', '面点', '预制菜',
                                     '酒水', '一次性用品', '前厅类', '日用百货', '半成品', '成品'
                                 )
        WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
        GROUP BY s.store_code
    ) t

    UNION ALL
    SELECT 'master.suppliers.group_a',
           COUNT(*) >= 8,
           COUNT(*)::TEXT
    FROM supplier_profile p
    JOIN sys_group g ON p.scope_type = 'GROUP' AND p.scope_id = g.id
    WHERE g.group_code = 'QA-GRP-A'
      AND p.status = '启用'
      AND p.remark = 'QA_INV_E2E'

    UNION ALL
    SELECT 'master.warehouses.per_store',
           MIN(warehouse_count) >= 4,
           string_agg(store_code || '=' || warehouse_count, ', ' ORDER BY store_code)
    FROM (
        SELECT s.store_code, COUNT(w.id) AS warehouse_count
        FROM sys_store s
        LEFT JOIN sys_warehouse w ON w.store_id = s.id AND w.warehouse_code LIKE 'QA-WH-%'
        WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
        GROUP BY s.store_code
    ) t

    UNION ALL
    SELECT 'master.items.group_scope',
           COUNT(*) >= 20,
           COUNT(*)::TEXT
    FROM item_profile p
    JOIN sys_group g ON p.scope_type = 'GROUP' AND p.scope_id = g.id
    WHERE g.group_code = 'QA-GRP-A'
      AND p.item_code LIKE 'QA-G-%'
      AND p.is_draft = FALSE

    UNION ALL
    SELECT 'master.items.store_enabled',
           MIN(item_count) >= 39,
           string_agg(store_code || '=' || item_count, ', ' ORDER BY store_code)
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
    ) t

    UNION ALL
    SELECT 'master.items.batch',
           MIN(batch_count) >= 8,
           string_agg(store_code || '=' || batch_count, ', ' ORDER BY store_code)
    FROM (
        SELECT s.store_code, COUNT(p.id) AS batch_count
        FROM sys_store s
        LEFT JOIN item_profile p ON p.scope_type = 'STORE'
                                AND p.scope_id = s.id
                                AND p.item_code LIKE 'QA-' || s.store_code || '-%'
                                AND (p.detail_json::JSONB ->> 'batchManagement')::BOOLEAN = TRUE
        WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
        GROUP BY s.store_code
    ) t

    UNION ALL
    SELECT 'master.balances.per_warehouse',
           MIN(stock_count) >= 10,
           string_agg(store_code || '/' || warehouse_name || '=' || stock_count, ', ' ORDER BY store_code, warehouse_name)
    FROM (
        SELECT s.store_code, w.warehouse_name, COUNT(b.id) AS stock_count
        FROM sys_store s
        JOIN sys_warehouse w ON w.store_id = s.id AND w.warehouse_code LIKE 'QA-WH-%'
        LEFT JOIN inventory_balance b ON b.scope_type = 'STORE'
                                    AND b.scope_id = s.id
                                    AND b.warehouse_name = w.warehouse_name
                                    AND b.item_code LIKE 'QA-%'
                                    AND b.quantity > 0
        WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
        GROUP BY s.store_code, w.warehouse_name
    ) t
)
SELECT CASE WHEN passed THEN 'PASS' ELSE 'FAIL' END AS status,
       check_name,
       details
FROM checks
ORDER BY status, check_name;

WITH doc_checks AS (
    SELECT 'docs.required_business_types_approved' AS check_name,
           COUNT(*) = 0 AS passed,
           COALESCE(string_agg(e.biz_type, ', ' ORDER BY e.biz_type), 'none') AS details
    FROM qa_expected_inventory_business e
    WHERE NOT EXISTS (
        SELECT 1
        FROM qa_inventory_docs d
        WHERE d.biz_type = e.biz_type
          AND d.status = '已审核'
    )

    UNION ALL
    SELECT 'docs.approved_rows_have_report_source',
           COUNT(*) = 0,
           COALESCE(string_agg(d.biz_type || ':' || d.document_code, ', ' ORDER BY d.biz_type, d.document_code), 'none')
    FROM qa_inventory_docs d
    WHERE d.status = '已审核'
      AND NOT EXISTS (
          SELECT 1
          FROM qa_inventory_doc_lines line
          WHERE line.biz_type = d.biz_type
            AND line.header_id = d.id
      )

    UNION ALL
    SELECT 'docs.report_line_category_matches_item_profile',
           COUNT(*) = 0,
           COALESCE(string_agg(d.biz_type || ':' || d.document_code || ':' || line.item_code
               || ':line=' || COALESCE(line.category, '')
               || ':profile=' || COALESCE(item.detail_json::JSONB ->> 'category', ''),
               ', ' ORDER BY d.biz_type, d.document_code, line.item_code), 'none')
    FROM qa_inventory_docs d
    JOIN qa_inventory_doc_lines line ON line.biz_type = d.biz_type AND line.header_id = d.id
    JOIN item_profile item ON item.scope_type = d.scope_type
                          AND item.scope_id = d.scope_id
                          AND item.item_code = line.item_code
                          AND item.is_draft = FALSE
    WHERE d.status = '已审核'
      AND COALESCE(line.category, '') <> COALESCE(item.detail_json::JSONB ->> 'category', '')

    UNION ALL
    SELECT 'docs.approved_have_transactions' AS check_name,
           NOT EXISTS (
               SELECT 1
               FROM qa_inventory_docs d
               WHERE d.status = '已审核'
                 AND NOT EXISTS (
                     SELECT 1
                     FROM inventory_transaction t
                     WHERE t.biz_type IN (d.biz_type, d.biz_type || '_APPROVE', d.biz_type || '_CONFIRM')
                       AND t.biz_id = d.id
                 )
           ) AS passed,
           COALESCE(string_agg(d.biz_type || ':' || d.document_code, ', '), 'none') AS details
    FROM qa_inventory_docs d
    WHERE d.status = '已审核'
      AND NOT EXISTS (
          SELECT 1
          FROM inventory_transaction t
          WHERE t.biz_type IN (d.biz_type, d.biz_type || '_APPROVE', d.biz_type || '_CONFIRM')
            AND t.biz_id = d.id
      )

    UNION ALL
    SELECT 'docs.approved_line_has_transaction',
           COUNT(*) = 0,
           COALESCE(string_agg(d.biz_type || ':' || d.document_code || ':line=' || line.line_id, ', ' ORDER BY d.biz_type, d.document_code, line.line_id), 'none')
    FROM qa_inventory_docs d
    JOIN qa_inventory_doc_lines line ON line.biz_type = d.biz_type AND line.header_id = d.id
    WHERE d.status = '已审核'
      AND NOT EXISTS (
          SELECT 1
          FROM inventory_transaction t
          WHERE t.biz_type IN (d.biz_type, d.biz_type || '_APPROVE', d.biz_type || '_CONFIRM')
            AND t.biz_id = d.id
            AND t.biz_line_id = line.line_id
      )

    UNION ALL
    SELECT 'docs.transaction_direction_matches_business',
           COUNT(*) = 0,
           COALESCE(string_agg(d.biz_type || ':' || d.document_code || ':delta=' || t.quantity_delta, ', ' ORDER BY d.biz_type, d.document_code), 'none')
    FROM qa_inventory_docs d
    JOIN qa_expected_inventory_business e ON e.biz_type = d.biz_type
    JOIN inventory_transaction t ON t.biz_type IN (d.biz_type, d.biz_type || '_APPROVE', d.biz_type || '_CONFIRM')
                                AND t.biz_id = d.id
    WHERE d.status = '已审核'
      AND (
          (e.expected_direction = 'INBOUND' AND t.quantity_delta <= 0)
          OR (e.expected_direction = 'OUTBOUND' AND t.quantity_delta >= 0)
      )

    UNION ALL
    SELECT 'docs.unapproved_have_no_transactions',
           NOT EXISTS (
               SELECT 1
               FROM qa_inventory_docs d
               WHERE d.status <> '已审核'
                 AND EXISTS (
                     SELECT 1
                     FROM inventory_transaction t
                     WHERE t.biz_type IN (d.biz_type, d.biz_type || '_APPROVE', d.biz_type || '_CONFIRM')
                       AND t.biz_id = d.id
                 )
           ),
           COALESCE(string_agg(d.biz_type || ':' || d.document_code || ':' || d.status, ', '), 'none')
    FROM qa_inventory_docs d
    WHERE d.status <> '已审核'
      AND EXISTS (
          SELECT 1
          FROM inventory_transaction t
          WHERE t.biz_type IN (d.biz_type, d.biz_type || '_APPROVE', d.biz_type || '_CONFIRM')
            AND t.biz_id = d.id
      )

    UNION ALL
    SELECT 'docs.approved_workflow_completed',
           NOT EXISTS (
               SELECT 1
               FROM qa_inventory_docs d
               WHERE d.status = '已审核'
                 AND COALESCE(d.workflow_status, '') <> 'COMPLETED'
           ),
           COALESCE(string_agg(d.biz_type || ':' || d.document_code || ':' || COALESCE(d.workflow_status, 'NULL'), ', '), 'none')
    FROM qa_inventory_docs d
    WHERE d.status = '已审核'
      AND COALESCE(d.workflow_status, '') <> 'COMPLETED'

    UNION ALL
    SELECT 'docs.store_transfer_requires_source_and_target_warehouse',
           COUNT(*) = 0,
           COALESCE(string_agg(document_code, ', ' ORDER BY document_code), 'none')
    FROM inventory_store_transfer
    WHERE status = '已审核'
      AND (COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%')
      AND (
          COALESCE(extra_json::JSONB ->> 'sourceWarehouse', '') = ''
          OR COALESCE(extra_json::JSONB ->> 'targetWarehouse', '') = ''
      )
)
SELECT CASE WHEN passed THEN 'PASS' ELSE 'FAIL' END AS status,
       check_name,
       details
FROM doc_checks
ORDER BY status, check_name;

WITH purchase_chain AS (
    SELECT document_type, document_code, source_document_code, downstream_document_code,
           document_status, review_status, workflow_status, created_by, scope_type, scope_id
    FROM purchase_document
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%'
       OR COALESCE(document_code, '') LIKE 'QA-%'
       OR COALESCE(source_document_code, '') LIKE 'QA-%'
       OR COALESCE(downstream_document_code, '') LIKE 'QA-%'
),
purchase_checks AS (
    SELECT 'purchase.chain.application_order_receipt_present' AS check_name,
           COUNT(DISTINCT document_type) FILTER (WHERE document_type IN ('APPLICATION', 'ORDER', 'RECEIPT')) = 3 AS passed,
           string_agg(DISTINCT document_type, ', ' ORDER BY document_type) AS details
    FROM purchase_chain

    UNION ALL
    SELECT 'purchase.approved_workflow_completed',
           NOT EXISTS (
               SELECT 1
               FROM purchase_chain
               WHERE review_status = '已审核'
                 AND COALESCE(workflow_status, '') <> 'COMPLETED'
           ),
           COALESCE(string_agg(document_type || ':' || document_code || ':' || COALESCE(workflow_status, 'NULL'), ', '), 'none')
    FROM purchase_chain
    WHERE review_status = '已审核'
      AND COALESCE(workflow_status, '') <> 'COMPLETED'

    UNION ALL
    SELECT 'purchase.downstream_links_present',
           NOT EXISTS (
               SELECT 1
               FROM purchase_chain
               WHERE document_type IN ('ORDER', 'RECEIPT')
                 AND COALESCE(source_document_code, '') = ''
           ),
           COALESCE(string_agg(document_type || ':' || document_code, ', '), 'none')
    FROM purchase_chain
    WHERE document_type IN ('ORDER', 'RECEIPT')
      AND COALESCE(source_document_code, '') = ''

    UNION ALL
    SELECT 'purchase.inbound_links_receipt_order_application',
           COUNT(*) = 0,
           COALESCE(string_agg(pi.document_code || ':upstream=' || COALESCE(pi.upstream_code, ''), ', ' ORDER BY pi.document_code), 'none')
    FROM inventory_purchase_inbound pi
    LEFT JOIN purchase_document receipt ON receipt.document_type = 'RECEIPT'
                                       AND receipt.document_code = pi.upstream_code
    LEFT JOIN purchase_document purchase_order ON purchase_order.document_type = 'ORDER'
                                              AND purchase_order.document_code = receipt.source_document_code
    LEFT JOIN purchase_document application ON application.document_type = 'APPLICATION'
                                           AND application.document_code = purchase_order.source_document_code
    WHERE pi.status = '已审核'
      AND (COALESCE(pi.remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(pi.document_code, '') LIKE 'QA-%')
      AND (
          receipt.id IS NULL
          OR purchase_order.id IS NULL
          OR application.id IS NULL
          OR receipt.review_status <> '已审核'
          OR purchase_order.review_status <> '已审核'
          OR application.review_status <> '已审核'
      )
)
SELECT CASE WHEN passed THEN 'PASS' ELSE 'FAIL' END AS status,
       check_name,
       details
FROM purchase_checks
ORDER BY status, check_name;

WITH inventory_check_docs AS (
    SELECT 'INVENTORY_CHECK' AS check_type,
           id,
           document_code,
           status,
           diff_status,
           warehouse_name,
           created_by,
           scope_type,
           scope_id
    FROM inventory_inventory_check
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%'
       OR COALESCE(document_code, '') LIKE 'PD-%'
    UNION ALL
    SELECT 'MULTI_INVENTORY_CHECK',
           id,
           document_code,
           status,
           '' AS diff_status,
           warehouse_name,
           created_by,
           scope_type,
           scope_id
    FROM inventory_multi_inventory_check
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%'
       OR COALESCE(document_code, '') LIKE 'MPD-%'
),
inventory_check_lines AS (
    SELECT 'INVENTORY_CHECK' AS check_type, header_id, item_code, profit_qty, loss_qty
    FROM inventory_inventory_check_line
    UNION ALL
    SELECT 'MULTI_INVENTORY_CHECK', header_id, item_code, profit_qty, loss_qty
    FROM inventory_multi_inventory_check_line
),
inventory_check_assertions AS (
    SELECT 'check.approved_profit_and_loss_source_exists' AS check_name,
           EXISTS (
               SELECT 1
               FROM inventory_check_docs h
               JOIN inventory_check_lines l ON l.check_type = h.check_type AND l.header_id = h.id
               WHERE h.status = '已审核'
                 AND l.profit_qty > 0
           )
           AND EXISTS (
               SELECT 1
               FROM inventory_check_docs h
               JOIN inventory_check_lines l ON l.check_type = h.check_type AND l.header_id = h.id
               WHERE h.status = '已审核'
                 AND l.loss_qty > 0
           ) AS passed,
           COALESCE(string_agg(DISTINCT h.document_code || ':' || h.status || ':' || h.diff_status, ', ' ORDER BY h.document_code || ':' || h.status || ':' || h.diff_status), 'none') AS details
    FROM inventory_check_docs h

    UNION ALL
    SELECT 'check.profit_lines_generated_profit_inbound',
           COUNT(*) = 0,
           COALESCE(string_agg(h.document_code || ':' || l.item_code, ', ' ORDER BY h.document_code, l.item_code), 'none')
    FROM inventory_check_docs h
    JOIN inventory_check_lines l ON l.check_type = h.check_type AND l.header_id = h.id
    WHERE h.status = '已审核'
      AND l.profit_qty > 0
      AND NOT EXISTS (
          SELECT 1
          FROM inventory_profit_inbound p
          JOIN inventory_profit_inbound_line pl ON pl.header_id = p.id
          WHERE p.upstream_code = h.document_code
            AND p.status = '已审核'
            AND pl.item_code = l.item_code
            AND pl.quantity = l.profit_qty
      )

    UNION ALL
    SELECT 'check.loss_lines_generated_loss_outbound',
           COUNT(*) = 0,
           COALESCE(string_agg(h.document_code || ':' || l.item_code, ', ' ORDER BY h.document_code, l.item_code), 'none')
    FROM inventory_check_docs h
    JOIN inventory_check_lines l ON l.check_type = h.check_type AND l.header_id = h.id
    WHERE h.status = '已审核'
      AND l.loss_qty > 0
      AND NOT EXISTS (
          SELECT 1
          FROM inventory_loss_outbound lo
          JOIN inventory_loss_outbound_line ll ON ll.header_id = lo.id
          WHERE lo.upstream_code = h.document_code
            AND lo.status = '已审核'
            AND ll.item_code = l.item_code
            AND ll.quantity = l.loss_qty
      )
)
SELECT CASE WHEN passed THEN 'PASS' ELSE 'FAIL' END AS status,
       check_name,
       details
FROM inventory_check_assertions
ORDER BY status, check_name;

WITH balance_recon AS (
    SELECT b.scope_type, b.scope_id, b.warehouse_name, b.item_code, b.quantity,
           latest.after_qty,
           latest.id AS last_transaction_id
    FROM inventory_balance b
    JOIN sys_store s ON b.scope_type = 'STORE' AND b.scope_id = s.id
    LEFT JOIN LATERAL (
        SELECT t.id, t.after_qty
        FROM inventory_transaction t
        WHERE t.scope_type = b.scope_type
          AND t.scope_id = b.scope_id
          AND t.warehouse_name = b.warehouse_name
          AND t.item_code = b.item_code
        ORDER BY t.created_at DESC, t.id DESC
        LIMIT 1
    ) latest ON TRUE
    WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
      AND b.item_code LIKE 'QA-%'
)
SELECT CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END AS status,
       'inventory.balance_matches_latest_transaction' AS check_name,
       COALESCE(string_agg(item_code || '/' || warehouse_name || ':balance=' || quantity || ',after=' || COALESCE(after_qty::TEXT, 'NULL'), ', '), 'none') AS details
FROM balance_recon
WHERE last_transaction_id IS NULL
   OR quantity <> after_qty;

WITH qa_scope AS (
    SELECT g.id AS group_id,
           s.id AS store_id,
           g.group_code,
           s.store_code
    FROM sys_group g
    JOIN sys_store s ON s.group_id = g.id
    WHERE g.group_code IN ('QA-GRP-A', 'QA-GRP-B')
),
permission_checks AS (
    SELECT 'permission.self_user_role_scope' AS check_name,
           EXISTS (
               SELECT 1
               FROM sys_user u
               JOIN sys_user_role_rel rel ON rel.user_id = u.id AND rel.status = 'ENABLED'
               JOIN sys_role r ON r.id = rel.role_id
               JOIN sys_store s ON rel.scope_type = 'STORE' AND rel.scope_id = s.id
               WHERE u.phone = '13990000003'
                 AND s.store_code = 'QA-A1'
                 AND r.role_code = 'SALESMAN'
                 AND r.data_scope_type = 'SELF'
           ) AS passed,
           '13990000003 should be SALESMAN/SELF in QA-A1' AS details

    UNION ALL
    SELECT 'permission.store_auditor_role_scope',
           EXISTS (
               SELECT 1
               FROM sys_user u
               JOIN sys_user_role_rel rel ON rel.user_id = u.id AND rel.status = 'ENABLED'
               JOIN sys_role r ON r.id = rel.role_id
               JOIN sys_store s ON rel.scope_type = 'STORE' AND rel.scope_id = s.id
               WHERE u.phone = '13990000004'
                 AND s.store_code = 'QA-A1'
                 AND r.role_code = 'FINANCE'
                 AND r.data_scope_type = 'STORE'
           ),
           '13990000004 should be FINANCE/STORE auditor in QA-A1'

    UNION ALL
    SELECT 'permission.cross_group_data_exists_for_negative_case',
           EXISTS (SELECT 1 FROM qa_scope WHERE group_code = 'QA-GRP-A')
           AND EXISTS (SELECT 1 FROM qa_scope WHERE group_code = 'QA-GRP-B'),
           'QA-GRP-A and QA-GRP-B must both exist'

    UNION ALL
    SELECT 'permission.store_roles_require_group_membership',
           COUNT(*) = 0,
           COALESCE(string_agg(u.phone || ':' || g.group_code, ', ' ORDER BY u.phone, g.group_code), 'none')
    FROM sys_user_role_rel store_rel
    JOIN sys_role store_role ON store_role.id = store_rel.role_id
    JOIN sys_store s ON store_rel.scope_type = 'STORE' AND store_rel.scope_id = s.id
    JOIN sys_group g ON g.id = s.group_id
    JOIN sys_user u ON u.id = store_rel.user_id
    WHERE store_rel.status = 'ENABLED'
      AND store_role.role_type = 'STORE'
      AND u.phone LIKE '139900000%'
      AND NOT EXISTS (
          SELECT 1
          FROM sys_user_role_rel group_rel
          JOIN sys_role group_role ON group_role.id = group_rel.role_id
          WHERE group_rel.user_id = store_rel.user_id
            AND group_rel.status = 'ENABLED'
            AND group_rel.scope_type = 'GROUP'
            AND group_rel.scope_id = s.group_id
            AND group_role.role_type = 'GROUP'
            AND group_role.role_code = 'GROUP_MEMBER'
            AND group_role.data_scope_type = 'SELF'
      )
)
SELECT CASE WHEN passed THEN 'PASS' ELSE 'FAIL' END AS status,
       check_name,
       details
FROM permission_checks
ORDER BY status, check_name;

SELECT s.store_code,
       w.warehouse_name,
       b.item_code,
       b.item_name,
       b.quantity AS book_quantity,
       COALESCE(latest.id, 0) AS last_transaction_id,
       COALESCE(latest.biz_type, 'NONE') AS last_biz_type,
       COALESCE(latest.created_at::TEXT, '') AS last_transaction_at
FROM sys_store s
JOIN sys_warehouse w ON w.store_id = s.id AND w.warehouse_code LIKE 'QA-WH-%'
JOIN inventory_balance b ON b.scope_type = 'STORE'
                        AND b.scope_id = s.id
                        AND b.warehouse_name = w.warehouse_name
                        AND b.item_code LIKE 'QA-%'
LEFT JOIN LATERAL (
    SELECT t.id, t.biz_type, t.created_at
    FROM inventory_transaction t
    WHERE t.scope_type = b.scope_type
      AND t.scope_id = b.scope_id
      AND t.warehouse_name = b.warehouse_name
      AND t.item_code = b.item_code
    ORDER BY t.created_at DESC, t.id DESC
    LIMIT 1
) latest ON TRUE
WHERE s.store_code IN ('QA-A1', 'QA-A2', 'QA-B1')
ORDER BY s.store_code, w.warehouse_name, b.item_code;
