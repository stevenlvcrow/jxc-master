-- QA inventory E2E assertions.
-- Run after qa-inventory-e2e-seed.sql and after each E2E test batch.

\set ON_ERROR_STOP on

SET search_path TO dev;

DO $$
BEGIN
    IF to_regclass('dev.sys_group') IS NULL
        OR to_regclass('dev.item_profile') IS NULL
        OR to_regclass('dev.inventory_balance') IS NULL
        OR to_regclass('dev.inventory_transaction') IS NULL THEN
        RAISE EXCEPTION 'Required schema is missing. Run schema and QA seed first.';
    END IF;
END $$;

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

WITH inventory_docs AS (
    SELECT 'PURCHASE_INBOUND' AS biz_type, id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_purchase_inbound
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%'
       OR COALESCE(document_code, '') LIKE 'QA-%'
       OR COALESCE(upstream_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'PURCHASE_RETURN_OUTBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_purchase_return_outbound
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'DEPARTMENT_PICKING', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_department_picking
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'STOCK_TRANSFER', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_stock_transfer
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'STOCK_TRANSFER_INBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_stock_transfer_inbound
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'DEPARTMENT_TRANSFER', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_department_transfer
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'DAMAGE_OUTBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_damage_outbound
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'OTHER_INBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_other_inbound
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'OTHER_OUTBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_other_outbound
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'PRODUCTION_INBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_production_inbound
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'CUSTOMER_SALES_OUTBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_customer_sales_outbound
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'CUSTOMER_RETURN_INBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_customer_return_inbound
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'DISH_CONSUMPTION_OUTBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_dish_consumption_outbound
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'STORE_TRANSFER', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_store_transfer
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
    UNION ALL
    SELECT 'STOCK_TRANSFER_OUTBOUND', id, document_code, status, workflow_status, created_by, scope_type, scope_id
    FROM inventory_stock_transfer_outbound
    WHERE COALESCE(remark, '') LIKE '%QA_INV_E2E%' OR COALESCE(document_code, '') LIKE 'QA-%'
),
doc_checks AS (
    SELECT 'docs.approved_have_transactions' AS check_name,
           NOT EXISTS (
               SELECT 1
               FROM inventory_docs d
               WHERE d.status = '已审核'
                 AND NOT EXISTS (
                     SELECT 1
                     FROM inventory_transaction t
                     WHERE t.biz_type = d.biz_type
                       AND t.biz_id = d.id
                 )
           ) AS passed,
           COALESCE(string_agg(d.biz_type || ':' || d.document_code, ', '), 'none') AS details
    FROM inventory_docs d
    WHERE d.status = '已审核'
      AND NOT EXISTS (
          SELECT 1
          FROM inventory_transaction t
          WHERE t.biz_type = d.biz_type
            AND t.biz_id = d.id
      )

    UNION ALL
    SELECT 'docs.unapproved_have_no_transactions',
           NOT EXISTS (
               SELECT 1
               FROM inventory_docs d
               WHERE d.status <> '已审核'
                 AND EXISTS (
                     SELECT 1
                     FROM inventory_transaction t
                     WHERE t.biz_type = d.biz_type
                       AND t.biz_id = d.id
                 )
           ),
           COALESCE(string_agg(d.biz_type || ':' || d.document_code || ':' || d.status, ', '), 'none')
    FROM inventory_docs d
    WHERE d.status <> '已审核'
      AND EXISTS (
          SELECT 1
          FROM inventory_transaction t
          WHERE t.biz_type = d.biz_type
            AND t.biz_id = d.id
      )

    UNION ALL
    SELECT 'docs.approved_workflow_completed',
           NOT EXISTS (
               SELECT 1
               FROM inventory_docs d
               WHERE d.status = '已审核'
                 AND d.biz_type NOT IN ('STORE_TRANSFER', 'STOCK_TRANSFER_OUTBOUND')
                 AND COALESCE(d.workflow_status, '') <> 'COMPLETED'
           ),
           COALESCE(string_agg(d.biz_type || ':' || d.document_code || ':' || COALESCE(d.workflow_status, 'NULL'), ', '), 'none')
    FROM inventory_docs d
    WHERE d.status = '已审核'
      AND d.biz_type NOT IN ('STORE_TRANSFER', 'STOCK_TRANSFER_OUTBOUND')
      AND COALESCE(d.workflow_status, '') <> 'COMPLETED'
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
)
SELECT CASE WHEN passed THEN 'PASS' ELSE 'FAIL' END AS status,
       check_name,
       details
FROM purchase_checks
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
