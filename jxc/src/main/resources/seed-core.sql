-- 默认密码：123654
-- 当前以 SHA-256 十六进制串初始化，后续接入正式认证时可替换为统一密码编码方案。

ALTER TABLE sys_role
DROP CONSTRAINT IF EXISTS uk_sys_role_tenant_code;

ALTER TABLE sys_role
ADD CONSTRAINT uk_sys_role_tenant_code UNIQUE (tenant_group_id, role_code);

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

INSERT INTO sys_group (group_code, group_name, status, remark)
VALUES ('DEFAULT_GROUP', '默认集团', 'ENABLED', '系统初始化集团')
ON CONFLICT DO NOTHING;

INSERT INTO sys_store (group_id, store_code, store_name, status, contact_name, contact_phone, address, remark)
VALUES (
    (SELECT id FROM sys_group WHERE group_code = 'DEFAULT_GROUP'),
    'MD73592041',
    '默认门店',
    'ENABLED',
    '门店联系人',
    '13800000003',
    '系统初始化门店地址',
    '系统初始化门店'
)
ON CONFLICT DO NOTHING;

UPDATE sys_store
SET store_code = 'MD73592041'
WHERE store_code = 'DEFAULT_STORE'
  AND NOT EXISTS (
    SELECT 1 FROM sys_store s WHERE s.store_code = 'MD73592041'
);

INSERT INTO sys_role (role_code, role_name, role_type, data_scope_type, description, status)
VALUES ('PLATFORM_SUPER_ADMIN', '平台超级管理员', 'PLATFORM', 'ALL', '系统初始化平台管理员角色', 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role (role_code, role_name, role_type, data_scope_type, description, status)
VALUES ('GROUP_ADMIN', '集团管理员', 'GROUP', 'GROUP', '系统初始化集团管理员角色', 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role (role_code, role_name, role_type, data_scope_type, description, status)
VALUES ('STORE_ADMIN', '门店管理员', 'STORE', 'STORE', '系统初始化门店管理员角色', 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role (tenant_group_id, role_code, role_name, role_type, data_scope_type, description, status)
VALUES (0, 'STORE_MANAGER', '店长', 'STORE', 'STORE', 'GROUP_ROLE_TEMPLATE', 'ENABLED')
ON CONFLICT (tenant_group_id, role_code) DO NOTHING;

INSERT INTO sys_role (tenant_group_id, role_code, role_name, role_type, data_scope_type, description, status)
VALUES (0, 'SALESMAN', '业务员', 'STORE', 'STORE', 'GROUP_ROLE_TEMPLATE', 'ENABLED')
ON CONFLICT (tenant_group_id, role_code) DO NOTHING;

INSERT INTO sys_role (tenant_group_id, role_code, role_name, role_type, data_scope_type, description, status)
VALUES (0, 'FINANCE', '财务', 'STORE', 'STORE', 'GROUP_ROLE_TEMPLATE', 'ENABLED')
ON CONFLICT (tenant_group_id, role_code) DO NOTHING;

INSERT INTO sys_role (tenant_group_id, role_code, role_name, role_type, data_scope_type, description, status)
VALUES (0, 'CASHIER', '收银员', 'STORE', 'STORE', 'GROUP_ROLE_TEMPLATE', 'ENABLED')
ON CONFLICT (tenant_group_id, role_code) DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES ('SYS_MGMT', '系统管理', NULL, 'DIRECTORY', '/system', NULL, NULL, 'setting', 10, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'ROLE_MGMT',
    '角色管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'SYS_MGMT'),
    'MENU',
    '/system/roles',
    'system/roles/index',
    'system:role:view',
    'team',
    11,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_MGMT', '集团管理', NULL, 'DIRECTORY', '/group', NULL, NULL, 'office-building', 40, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'GROUP_STORE_MGMT',
    '门店管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT'),
    'MENU',
    '/group/stores',
    'system/store-management/index',
    'group:store:manage',
    'shop',
    44,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'GROUP_MGMT_ADMIN',
    '集团管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'SYS_MGMT'),
    'MENU',
    '/system/groups',
    'system/groups/index',
    'system:group:manage',
    'office',
    12,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'USER_MGMT',
    '用户管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'SYS_MGMT'),
    'MENU',
    '/system/users',
    'system/user-management/index',
    'system:user:view',
    'user',
    13,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'MENU_PERMISSION_MGMT',
    '菜单权限管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'SYS_MGMT'),
    'MENU',
    '/system/menu-permissions',
    'system/menu-permissions/index',
    'system:menu:assign',
    'setting',
    14,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_WORKBENCH', '集团工作台', NULL, 'MENU', '/group/dashboard', 'group/dashboard/index', 'group:dashboard:view', 'office', 20, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_MGMT', '集团管理', NULL, 'DIRECTORY', '/group', NULL, NULL, 'office-building', 40, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_INFO', '集团信息', (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT'), 'MENU', '/group/info', 'group/info/index', 'group:info:view', 'document', 41, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_STORE_MGMT', '门店管理', (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT'), 'MENU', '/group/stores', 'group/stores/index', 'group:store:manage', 'shop', 44, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_USER_ROLE_MGMT', '用户管理', (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT'), 'MENU', '/group/user-role', 'group/user-role/index', 'group:user-role:manage', 'user', 46, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_ROLE_MGMT', '角色管理', (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT'), 'MENU', '/group/roles', 'group/roles/index', 'group:role:manage', 'team', 47, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_MENU_PERMISSION_MGMT', '菜单权限管理', (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT'), 'MENU', '/group/menu-permissions', 'group/menu-permissions/index', 'group:menu-permission:manage', 'setting', 48, TRUE, 'ENABLED')
ON CONFLICT DO NOTHING;


INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES ('GROUP_WORKFLOW_HISTORY_MGMT', '流程发布历史管理', (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_WORKBENCH'), 'MENU', '/group/workflow-history', 'group/workflow/history/index', 'group:workflow:history:view', 'setting', 50, TRUE, 'ENABLED')
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
    (SELECT id FROM sys_role WHERE role_code = 'PLATFORM_SUPER_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'SYS_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE role_code = 'GROUP_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE role_code = 'GROUP_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_STORE_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE role_code = 'PLATFORM_SUPER_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'ROLE_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE role_code = 'PLATFORM_SUPER_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT_ADMIN')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE role_code = 'PLATFORM_SUPER_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'USER_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE role_code = 'PLATFORM_SUPER_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'MENU_PERMISSION_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE role_code = 'GROUP_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_WORKBENCH')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
VALUES (
    (SELECT id FROM sys_role WHERE role_code = 'GROUP_ADMIN'),
    (SELECT id FROM sys_menu WHERE menu_code = 'GROUP_MGMT')
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
SELECT
    (SELECT id FROM sys_role WHERE role_code = 'GROUP_ADMIN'),
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
VALUES ('13800000001', '默认集团管理员', '13800000001', '6460662e217c7a9f899208dd70a2c28abdea42f128666a9b78e6c0c064846493', NULL, 'ENABLED', 'SYSTEM_INIT', FALSE)
ON CONFLICT DO NOTHING;

INSERT INTO sys_user (username, real_name, phone, password_hash, password_salt, status, source_type, first_login_changed_pwd)
VALUES ('13800000002', '默认门店管理员', '13800000002', '6460662e217c7a9f899208dd70a2c28abdea42f128666a9b78e6c0c064846493', NULL, 'ENABLED', 'SYSTEM_INIT', FALSE)
ON CONFLICT DO NOTHING;

INSERT INTO sys_store_admin_rel (store_id, user_id, assigned_by, status)
VALUES (
    (SELECT id FROM sys_store WHERE store_code = 'MD73592041'),
    (SELECT id FROM sys_user WHERE phone = '13800000002'),
    (SELECT id FROM sys_user WHERE phone = '13800000000'),
    'ENABLED'
)
ON CONFLICT DO NOTHING;

-- STORE_MENU_SEED_START
-- ??????????jxc-w/src/config/jxc-menu.json??????????????/???????
INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_01',
    '订货管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    'tickets',
    101,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_01_01',
    '规则',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_01'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1101,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_01_01',
    '配送班表查看',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_01'),
    'MENU',
    '/order/1/1',
    'views/feature/index',
    'store:biz:01:01:01:view',
    NULL,
    101011,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_01_02',
    '配送费用查看',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_01'),
    'MENU',
    '/order/1/2',
    'views/feature/index',
    'store:biz:01:01:02:view',
    NULL,
    101012,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_01_02',
    '优惠券',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_01'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1102,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_02_01',
    '优惠券',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_02'),
    'MENU',
    '/order/2/1',
    'views/feature/index',
    'store:biz:01:02:01:view',
    NULL,
    101021,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_01_03',
    '单据',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_01'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1103,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_03_01',
    '订货单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_03'),
    'MENU',
    '/order/3/1',
    'views/feature/index',
    'store:biz:01:03:01:view',
    NULL,
    101031,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_03_02',
    '收货单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_03'),
    'MENU',
    '/order/3/2',
    'views/feature/index',
    'store:biz:01:03:02:view',
    NULL,
    101032,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_03_03',
    '差异单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_03'),
    'MENU',
    '/order/3/3',
    'views/feature/index',
    'store:biz:01:03:03:view',
    NULL,
    101033,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_03_04',
    '返货单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_03'),
    'MENU',
    '/order/3/4',
    'views/feature/index',
    'store:biz:01:03:04:view',
    NULL,
    101034,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_01_03_05',
    '三方调拨单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_01_03'),
    'MENU',
    '/order/3/5',
    'views/feature/index',
    'store:biz:01:03:05:view',
    NULL,
    101035,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_02',
    '采购管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    'goods',
    102,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_02_01',
    '价格管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_02'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1201,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_01_01',
    '采购单定价',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_01'),
    'MENU',
    '/purchase/1/1',
    'views/feature/index',
    'store:biz:02:01:01:view',
    NULL,
    102011,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_01_02',
    '采购定价明细调整单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_01'),
    'MENU',
    '/purchase/1/2',
    'views/feature/index',
    'store:biz:02:01:02:view',
    NULL,
    102012,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_01_03',
    '采购定价明细',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_01'),
    'MENU',
    '/purchase/1/3',
    'views/feature/index',
    'store:biz:02:01:03:view',
    NULL,
    102013,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_02_02',
    '规则',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_02'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1202,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_02_01',
    '采购模板',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_02'),
    'MENU',
    '/purchase/2/1',
    'views/feature/index',
    'store:biz:02:02:01:view',
    NULL,
    102021,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_02_02',
    '历史千元用量查询',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_02'),
    'MENU',
    '/purchase/2/2',
    'views/feature/index',
    'store:biz:02:02:02:view',
    NULL,
    102022,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_02_03',
    '供货规则',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_02'),
    'MENU',
    '/purchase/2/3',
    'views/feature/index',
    'store:biz:02:02:03:view',
    NULL,
    102023,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_02_03',
    '智能',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_02'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1203,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_03_01',
    '智能采购',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_03'),
    'MENU',
    '/purchase/3/1',
    'views/feature/index',
    'store:biz:02:03:01:view',
    NULL,
    102031,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_02_04',
    '单据',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_02'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1204,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_04_01',
    '采购单申请',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_04'),
    'MENU',
    '/purchase/4/1',
    'views/feature/index',
    'store:biz:02:04:01:view',
    NULL,
    102041,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_04_02',
    '采购单申请审核',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_04'),
    'MENU',
    '/purchase/4/2',
    'views/feature/index',
    'store:biz:02:04:02:view',
    NULL,
    102042,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_04_03',
    '采购订单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_04'),
    'MENU',
    '/purchase/4/3',
    'views/feature/index',
    'store:biz:02:04:03:view',
    NULL,
    102043,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_04_04',
    '采购收货单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_04'),
    'MENU',
    '/purchase/4/4',
    'views/feature/index',
    'store:biz:02:04:04:view',
    NULL,
    102044,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_02_04_05',
    '采购退货单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_02_04'),
    'MENU',
    '/purchase/4/5',
    'views/feature/index',
    'store:biz:02:04:05:view',
    NULL,
    102045,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_03',
    '生产管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    'collection',
    103,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_03_01',
    '引导',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_03'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1301,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_03_01_01',
    '流程引导',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_03_01'),
    'MENU',
    '/production/1/1',
    'views/feature/index',
    'store:biz:03:01:01:view',
    NULL,
    103011,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_03_02',
    '规则',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_03'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1302,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_03_02_01',
    '组合BOM',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_03_02'),
    'MENU',
    '/production/2/1',
    'views/feature/index',
    'store:biz:03:02:01:view',
    NULL,
    103021,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_03_02_02',
    '拆分BOM',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_03_02'),
    'MENU',
    '/production/2/2',
    'views/feature/index',
    'store:biz:03:02:02:view',
    NULL,
    103022,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_03_02_03',
    'BOM批量修改',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_03_02'),
    'MENU',
    '/production/2/3',
    'views/feature/index',
    'store:biz:03:02:03:view',
    NULL,
    103023,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_03_02_04',
    '生产模板',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_03_02'),
    'MENU',
    '/production/2/4',
    'views/feature/index',
    'store:biz:03:02:04:view',
    NULL,
    103024,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_03_02_05',
    '标签打印',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_03_02'),
    'MENU',
    '/production/2/5',
    'views/feature/index',
    'store:biz:03:02:05:view',
    NULL,
    103025,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_03_03',
    '单据',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_03'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1303,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_03_03_01',
    '组合加工单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_03_03'),
    'MENU',
    '/production/3/1',
    'views/feature/index',
    'store:biz:03:03:01:view',
    NULL,
    103031,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_03_03_02',
    '拆分加工单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_03_03'),
    'MENU',
    '/production/3/2',
    'views/feature/index',
    'store:biz:03:03:02:view',
    NULL,
    103032,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_04',
    '库存管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    'files',
    104,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_04_01',
    '库存单据',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_04'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1401,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_01',
    '仓库期初',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/1',
    'views/feature/index',
    'store:biz:04:01:01:view',
    NULL,
    104011,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_02',
    '采购入库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/2',
    'views/feature/index',
    'store:biz:04:01:02:view',
    NULL,
    104012,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_03',
    '采购退货出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/3',
    'views/feature/index',
    'store:biz:04:01:03:view',
    NULL,
    104013,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_04',
    '部门领料',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/4',
    'views/feature/index',
    'store:biz:04:01:04:view',
    NULL,
    104014,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_05',
    '部门退料',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/5',
    'views/feature/index',
    'store:biz:04:01:05:view',
    NULL,
    104015,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_06',
    '移库单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/6',
    'views/feature/index',
    'store:biz:04:01:06:view',
    NULL,
    104016,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_07',
    '移库入库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/7',
    'views/feature/index',
    'store:biz:04:01:07:view',
    NULL,
    104017,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_08',
    '部门调拨',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/8',
    'views/feature/index',
    'store:biz:04:01:08:view',
    NULL,
    104018,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_09',
    '店间调拨',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/9',
    'views/feature/index',
    'store:biz:04:01:09:view',
    NULL,
    104019,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_10',
    '报损出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/10',
    'views/feature/index',
    'store:biz:04:01:10:view',
    NULL,
    104020,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_11',
    '其他入库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/11',
    'views/feature/index',
    'store:biz:04:01:11:view',
    NULL,
    104021,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_12',
    '其他出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/12',
    'views/feature/index',
    'store:biz:04:01:12:view',
    NULL,
    104022,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_13',
    '生产入库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/13',
    'views/feature/index',
    'store:biz:04:01:13:view',
    NULL,
    104023,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_14',
    '客户销售出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/14',
    'views/feature/index',
    'store:biz:04:01:14:view',
    NULL,
    104024,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_15',
    '客户退货入库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/15',
    'views/feature/index',
    'store:biz:04:01:15:view',
    NULL,
    104025,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_01_16',
    '移库出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_01'),
    'MENU',
    '/inventory/1/16',
    'views/feature/index',
    'store:biz:04:01:16:view',
    NULL,
    104026,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_04_02',
    '盘点管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_04'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1402,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_02_01',
    '盘点单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_02'),
    'MENU',
    '/inventory/2/1',
    'views/feature/index',
    'store:biz:04:02:01:view',
    NULL,
    104021,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_02_02',
    '多人盘点单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_02'),
    'MENU',
    '/inventory/2/2',
    'views/feature/index',
    'store:biz:04:02:02:view',
    NULL,
    104022,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_02_03',
    '盘盈单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_02'),
    'MENU',
    '/inventory/2/3',
    'views/feature/index',
    'store:biz:04:02:03:view',
    NULL,
    104023,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_02_04',
    '盘亏单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_02'),
    'MENU',
    '/inventory/2/4',
    'views/feature/index',
    'store:biz:04:02:04:view',
    NULL,
    104024,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_04_03',
    '自动出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_04'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1403,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_03_01',
    '菜品消耗出库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_03'),
    'MENU',
    '/inventory/3/1',
    'views/feature/index',
    'store:biz:04:03:01:view',
    NULL,
    104031,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_04_04',
    '批次管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_04'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1404,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_04_01',
    '批次调整单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_04'),
    'MENU',
    '/inventory/4/1',
    'views/feature/index',
    'store:biz:04:04:01:view',
    NULL,
    104041,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_04_05',
    '库存规则',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_04'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1405,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_05_01',
    '库存模板',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_05'),
    'MENU',
    '/inventory/5/1',
    'views/feature/index',
    'store:biz:04:05:01:view',
    NULL,
    104051,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_05_02',
    '调拨分组',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_05'),
    'MENU',
    '/inventory/5/2',
    'views/feature/index',
    'store:biz:04:05:02:view',
    NULL,
    104052,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_05_03',
    '库存上下限',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_05'),
    'MENU',
    '/inventory/5/3',
    'views/feature/index',
    'store:biz:04:05:03:view',
    NULL,
    104053,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_05_04',
    '库存锁库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_05'),
    'MENU',
    '/inventory/5/4',
    'views/feature/index',
    'store:biz:04:05:04:view',
    NULL,
    104054,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_05_05',
    '锁库日志',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_05'),
    'MENU',
    '/inventory/5/5',
    'views/feature/index',
    'store:biz:04:05:05:view',
    NULL,
    104055,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_04_05_06',
    '在途规则',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_04_05'),
    'MENU',
    '/inventory/5/6',
    'views/feature/index',
    'store:biz:04:05:06:view',
    NULL,
    104056,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_05',
    '财务管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    'coin',
    105,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_05_01',
    '资金账户',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_05'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1501,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_01_01',
    '账户管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_01'),
    'MENU',
    '/finance/1/1',
    'views/feature/index',
    'store:biz:05:01:01:view',
    NULL,
    105011,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_01_02',
    '账户交易明细',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_01'),
    'MENU',
    '/finance/1/2',
    'views/feature/index',
    'store:biz:05:01:02:view',
    NULL,
    105012,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_01_03',
    '在线交易流水',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_01'),
    'MENU',
    '/finance/1/3',
    'views/feature/index',
    'store:biz:05:01:03:view',
    NULL,
    105013,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_01_04',
    '账户对账单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_01'),
    'MENU',
    '/finance/1/4',
    'views/feature/index',
    'store:biz:05:01:04:view',
    NULL,
    105014,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_05_02',
    '成本',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_05'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1502,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_02_01',
    '期末成本调整',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_02'),
    'MENU',
    '/finance/2/1',
    'views/feature/index',
    'store:biz:05:02:01:view',
    NULL,
    105021,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_02_02',
    '成本调整',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_02'),
    'MENU',
    '/finance/2/2',
    'views/feature/index',
    'store:biz:05:02:02:view',
    NULL,
    105022,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_02_03',
    '库存成本重算',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_02'),
    'MENU',
    '/finance/2/3',
    'views/feature/index',
    'store:biz:05:02:03:view',
    NULL,
    105023,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_02_04',
    '全月加权平均',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_02'),
    'MENU',
    '/finance/2/4',
    'views/feature/index',
    'store:biz:05:02:04:view',
    NULL,
    105024,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_02_05',
    '成本核算报告',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_02'),
    'MENU',
    '/finance/2/5',
    'views/feature/index',
    'store:biz:05:02:05:view',
    NULL,
    105025,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_05_03',
    '费用',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_05'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1503,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_03_01',
    '应付费用单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_03'),
    'MENU',
    '/finance/3/1',
    'views/feature/index',
    'store:biz:05:03:01:view',
    NULL,
    105031,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_03_02',
    '应付费用分摊单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_03'),
    'MENU',
    '/finance/3/2',
    'views/feature/index',
    'store:biz:05:03:02:view',
    NULL,
    105032,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_05_04',
    '应付管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_05'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1504,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_04_01',
    '供应商对账申请单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_04'),
    'MENU',
    '/finance/4/1',
    'views/feature/index',
    'store:biz:05:04:01:view',
    NULL,
    105041,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_04_02',
    '应付对账看板',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_04'),
    'MENU',
    '/finance/4/2',
    'views/feature/index',
    'store:biz:05:04:02:view',
    NULL,
    105042,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_04_03',
    '应付付账管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_04'),
    'MENU',
    '/finance/4/3',
    'views/feature/index',
    'store:biz:05:04:03:view',
    NULL,
    105043,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_04_04',
    '进项采购发票管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_04'),
    'MENU',
    '/finance/4/4',
    'views/feature/index',
    'store:biz:05:04:04:view',
    NULL,
    105044,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_04_05',
    '进项费用发票管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_04'),
    'MENU',
    '/finance/4/5',
    'views/feature/index',
    'store:biz:05:04:05:view',
    NULL,
    105045,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_04_06',
    '应(预)付付款管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_04'),
    'MENU',
    '/finance/4/6',
    'views/feature/index',
    'store:biz:05:04:06:view',
    NULL,
    105046,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_04_07',
    '预付款退款单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_04'),
    'MENU',
    '/finance/4/7',
    'views/feature/index',
    'store:biz:05:04:07:view',
    NULL,
    105047,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_05_05',
    '结转',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_05'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1505,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_05_01',
    '月结',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_05'),
    'MENU',
    '/finance/5/1',
    'views/feature/index',
    'store:biz:05:05:01:view',
    NULL,
    105051,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_05_06',
    '价格调整',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_05'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1506,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_05_06_01',
    '采购暂估调价单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_05_06'),
    'MENU',
    '/finance/6/1',
    'views/feature/index',
    'store:biz:05:06:01:view',
    NULL,
    105061,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_06',
    '质量管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    'checked',
    106,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_06_01',
    '质量管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_06'),
    'MENU',
    '/quality/1',
    'views/feature/index',
    'store:biz:06:01:view',
    NULL,
    10601,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_06_02',
    '质检项目',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_06'),
    'MENU',
    '/quality/2',
    'views/feature/index',
    'store:biz:06:02:view',
    NULL,
    10602,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_06_03',
    '质检方案',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_06'),
    'MENU',
    '/quality/3',
    'views/feature/index',
    'store:biz:06:03:view',
    NULL,
    10603,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_06_04',
    '质检单',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_06'),
    'MENU',
    '/quality/4',
    'views/feature/index',
    'store:biz:06:04:view',
    NULL,
    10604,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_06_05',
    '质检报告',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_06'),
    'MENU',
    '/quality/5',
    'views/feature/index',
    'store:biz:06:05:view',
    NULL,
    10605,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_07',
    '报表管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    'data-analysis',
    107,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_07_01',
    '报表引导助手',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_07'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1701,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_01_01',
    '报表引导助手',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_01'),
    'MENU',
    '/report/1/1',
    'views/feature/index',
    'store:biz:07:01:01:view',
    NULL,
    107011,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_07_02',
    '订配报表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_07'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1702,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_02_01',
    '配送单状态跟踪表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_02'),
    'MENU',
    '/report/2/1',
    'views/feature/index',
    'store:biz:07:02:01:view',
    NULL,
    107021,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_02_02',
    '配送返货单状态跟踪表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_02'),
    'MENU',
    '/report/2/2',
    'views/feature/index',
    'store:biz:07:02:02:view',
    NULL,
    107022,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_02_03',
    '配送汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_02'),
    'MENU',
    '/report/2/3',
    'views/feature/index',
    'store:biz:07:02:03:view',
    NULL,
    107023,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_02_04',
    '订货需求变化分析表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_02'),
    'MENU',
    '/report/2/4',
    'views/feature/index',
    'store:biz:07:02:04:view',
    NULL,
    107024,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_02_05',
    '订货单明细表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_02'),
    'MENU',
    '/report/2/5',
    'views/feature/index',
    'store:biz:07:02:05:view',
    NULL,
    107025,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_07_03',
    '采购报表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_07'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1703,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_03_01',
    '采购订单状态跟踪表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_03'),
    'MENU',
    '/report/3/1',
    'views/feature/index',
    'store:biz:07:03:01:view',
    NULL,
    107031,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_03_02',
    '退货单状态跟踪表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_03'),
    'MENU',
    '/report/3/2',
    'views/feature/index',
    'store:biz:07:03:02:view',
    NULL,
    107032,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_03_03',
    '采购物品价格分析表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_03'),
    'MENU',
    '/report/3/3',
    'views/feature/index',
    'store:biz:07:03:03:view',
    NULL,
    107033,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_03_04',
    '采购汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_03'),
    'MENU',
    '/report/3/4',
    'views/feature/index',
    'store:biz:07:03:04:view',
    NULL,
    107034,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_03_05',
    '供应商指标表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_03'),
    'MENU',
    '/report/3/5',
    'views/feature/index',
    'store:biz:07:03:05:view',
    NULL,
    107035,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_03_06',
    '采购定价分析表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_03'),
    'MENU',
    '/report/3/6',
    'views/feature/index',
    'store:biz:07:03:06:view',
    NULL,
    107036,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_03_07',
    '供应商进销存汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_03'),
    'MENU',
    '/report/3/7',
    'views/feature/index',
    'store:biz:07:03:07:view',
    NULL,
    107037,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_07_04',
    '生产报表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_07'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1704,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_04_01',
    '生产汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_04'),
    'MENU',
    '/report/4/1',
    'views/feature/index',
    'store:biz:07:04:01:view',
    NULL,
    107041,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_04_02',
    '生产明细表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_04'),
    'MENU',
    '/report/4/2',
    'views/feature/index',
    'store:biz:07:04:02:view',
    NULL,
    107042,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_04_03',
    '原料成本差异分析表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_04'),
    'MENU',
    '/report/4/3',
    'views/feature/index',
    'store:biz:07:04:03:view',
    NULL,
    107043,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_04_04',
    '应产率分析表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_04'),
    'MENU',
    '/report/4/4',
    'views/feature/index',
    'store:biz:07:04:04:view',
    NULL,
    107044,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_04_05',
    '产成品原料批次溯源表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_04'),
    'MENU',
    '/report/4/5',
    'views/feature/index',
    'store:biz:07:04:05:view',
    NULL,
    107045,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_04_06',
    '员工生产绩效表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_04'),
    'MENU',
    '/report/4/6',
    'views/feature/index',
    'store:biz:07:04:06:view',
    NULL,
    107046,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_04_07',
    '组合加工品报损统计表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_04'),
    'MENU',
    '/report/4/7',
    'views/feature/index',
    'store:biz:07:04:07:view',
    NULL,
    107047,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_07_05',
    '库存报表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_07'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1705,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_01',
    '实时库存查询表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/1',
    'views/feature/index',
    'store:biz:07:05:01:view',
    NULL,
    107051,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_02',
    '菜品消耗出库查询表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/2',
    'views/feature/index',
    'store:biz:07:05:02:view',
    NULL,
    107052,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_03',
    '盘点盈亏表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/3',
    'views/feature/index',
    'store:biz:07:05:03:view',
    NULL,
    107053,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_04',
    '出入库明细表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/4',
    'views/feature/index',
    'store:biz:07:05:04:view',
    NULL,
    107054,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_05',
    '出入库汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/5',
    'views/feature/index',
    'store:biz:07:05:05:view',
    NULL,
    107055,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_06',
    '库存进出汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/6',
    'views/feature/index',
    'store:biz:07:05:06:view',
    NULL,
    107056,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_07',
    '其他出入库汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/7',
    'views/feature/index',
    'store:biz:07:05:07:view',
    NULL,
    107057,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_08',
    '机构间调拨明细表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/8',
    'views/feature/index',
    'store:biz:07:05:08:view',
    NULL,
    107058,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_09',
    '机构间调拨汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/9',
    'views/feature/index',
    'store:biz:07:05:09:view',
    NULL,
    107059,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_10',
    '库存批次表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/10',
    'views/feature/index',
    'store:biz:07:05:10:view',
    NULL,
    107060,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_11',
    '保质期预警表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/11',
    'views/feature/index',
    'store:biz:07:05:11:view',
    NULL,
    107061,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_12',
    '物品批次全流程跟踪表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/12',
    'views/feature/index',
    'store:biz:07:05:12:view',
    NULL,
    107062,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_13',
    '库存呆滞品查询表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/13',
    'views/feature/index',
    'store:biz:07:05:13:view',
    NULL,
    107063,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_14',
    '库存周转率统计表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/14',
    'views/feature/index',
    'store:biz:07:05:14:view',
    NULL,
    107064,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_15',
    '库存预警表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/15',
    'views/feature/index',
    'store:biz:07:05:15:view',
    NULL,
    107065,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_16',
    '门店开封效期明细表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/16',
    'views/feature/index',
    'store:biz:07:05:16:view',
    NULL,
    107066,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_05_17',
    '门店开封效期汇总表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_05'),
    'MENU',
    '/report/5/17',
    'views/feature/index',
    'store:biz:07:05:17:view',
    NULL,
    107067,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_07_06',
    '财务报表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_07'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1706,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_06_01',
    '成本差异分析表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_06'),
    'MENU',
    '/report/6/1',
    'views/feature/index',
    'store:biz:07:06:01:view',
    NULL,
    107061,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_06_02',
    '门店毛利分析表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_06'),
    'MENU',
    '/report/6/2',
    'views/feature/index',
    'store:biz:07:06:02:view',
    NULL,
    107062,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_06_03',
    '部门毛利分析表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_06'),
    'MENU',
    '/report/6/3',
    'views/feature/index',
    'store:biz:07:06:03:view',
    NULL,
    107063,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_06_04',
    '菜品毛利分析表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_06'),
    'MENU',
    '/report/6/4',
    'views/feature/index',
    'store:biz:07:06:04:view',
    NULL,
    107064,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_06_05',
    '应付对账表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_06'),
    'MENU',
    '/report/6/5',
    'views/feature/index',
    'store:biz:07:06:05:view',
    NULL,
    107065,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_06_06',
    '应付对账单状态跟踪表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_06'),
    'MENU',
    '/report/6/6',
    'views/feature/index',
    'store:biz:07:06:06:view',
    NULL,
    107066,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_06_07',
    '应付明细表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_06'),
    'MENU',
    '/report/6/7',
    'views/feature/index',
    'store:biz:07:06:07:view',
    NULL,
    107067,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_07_06_08',
    '菜品报损成本统计表',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_07_06'),
    'MENU',
    '/report/6/8',
    'views/feature/index',
    'store:biz:07:06:08:view',
    NULL,
    107068,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_08',
    '档案管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    'document',
    108,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_01',
    '物品',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1801,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_01_01',
    '物品管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_01'),
    'MENU',
    '/archive/1/1',
    'views/feature/index',
    'store:biz:08:01:01:view',
    NULL,
    108011,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_01_02',
    '物品类别管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_01'),
    'MENU',
    '/archive/1/2',
    'views/feature/index',
    'store:biz:08:01:02:view',
    NULL,
    108012,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_01_03',
    '单位管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_01'),
    'MENU',
    '/archive/1/3',
    'views/feature/index',
    'store:biz:08:01:03:view',
    NULL,
    108013,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_01_04',
    '统计类型管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_01'),
    'MENU',
    '/archive/1/4',
    'views/feature/index',
    'store:biz:08:01:04:view',
    NULL,
    108014,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_01_05',
    '物品标签管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_01'),
    'MENU',
    '/archive/1/5',
    'views/feature/index',
    'store:biz:08:01:05:view',
    NULL,
    108015,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_02',
    '成本设置',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1802,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_02_01',
    '成本卡档案',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_02'),
    'MENU',
    '/archive/2/1',
    'views/feature/index',
    'store:biz:08:02:01:view',
    NULL,
    108021,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_02_02',
    '成本卡设置',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_02'),
    'MENU',
    '/archive/2/2',
    'views/feature/index',
    'store:biz:08:02:02:view',
    NULL,
    108022,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_02_03',
    '菜品关联仓库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_02'),
    'MENU',
    '/archive/2/3',
    'views/feature/index',
    'store:biz:08:02:03:view',
    NULL,
    108023,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_02_04',
    '费用关联仓库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_02'),
    'MENU',
    '/archive/2/4',
    'views/feature/index',
    'store:biz:08:02:04:view',
    NULL,
    108024,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_02_05',
    '替代料管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_02'),
    'MENU',
    '/archive/2/5',
    'views/feature/index',
    'store:biz:08:02:05:view',
    NULL,
    108025,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_02_06',
    '例外物品规则',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_02'),
    'MENU',
    '/archive/2/6',
    'views/feature/index',
    'store:biz:08:02:06:view',
    NULL,
    108026,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_02_07',
    '成本分摊例外规则',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_02'),
    'MENU',
    '/archive/2/7',
    'views/feature/index',
    'store:biz:08:02:07:view',
    NULL,
    108027,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_03',
    '供应商',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1803,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_03_01',
    '供应商档案',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_03'),
    'MENU',
    '/archive/3/1',
    'views/feature/index',
    'store:biz:08:03:01:view',
    NULL,
    108031,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_03_02',
    '供应商物品资质',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_03'),
    'MENU',
    '/archive/3/2',
    'views/feature/index',
    'store:biz:08:03:02:view',
    NULL,
    108032,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_04',
    '评价管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1804,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_04_01',
    '评价流程引导',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_04'),
    'MENU',
    '/archive/4/1',
    'views/feature/index',
    'store:biz:08:04:01:view',
    NULL,
    108041,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_04_02',
    '评价项目',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_04'),
    'MENU',
    '/archive/4/2',
    'views/feature/index',
    'store:biz:08:04:02:view',
    NULL,
    108042,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_04_03',
    '评价方案',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_04'),
    'MENU',
    '/archive/4/3',
    'views/feature/index',
    'store:biz:08:04:03:view',
    NULL,
    108043,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_04_04',
    '评价任务',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_04'),
    'MENU',
    '/archive/4/4',
    'views/feature/index',
    'store:biz:08:04:04:view',
    NULL,
    108044,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_04_05',
    '评价明细',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_04'),
    'MENU',
    '/archive/4/5',
    'views/feature/index',
    'store:biz:08:04:05:view',
    NULL,
    108045,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_04_06',
    '供应商得分设置',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_04'),
    'MENU',
    '/archive/4/6',
    'views/feature/index',
    'store:biz:08:04:06:view',
    NULL,
    108046,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_04_07',
    '供应商得分',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_04'),
    'MENU',
    '/archive/4/7',
    'views/feature/index',
    'store:biz:08:04:07:view',
    NULL,
    108047,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_05',
    '客户',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1805,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_05_01',
    '销售客户',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_05'),
    'MENU',
    '/archive/5/1',
    'views/feature/index',
    'store:biz:08:05:01:view',
    NULL,
    108051,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_06',
    '机构',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1806,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_06_01',
    '机构管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_06'),
    'MENU',
    '/archive/6/1',
    'views/feature/index',
    'store:biz:08:06:01:view',
    NULL,
    108061,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_07',
    '仓库',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1807,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_07_01',
    '仓库档案',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_07'),
    'MENU',
    '/archive/7/1',
    'views/feature/index',
    'store:biz:08:07:01:view',
    NULL,
    108071,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_07_02',
    '仓库物品规则',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_07'),
    'MENU',
    '/archive/7/2',
    'views/feature/index',
    'store:biz:08:07:02:view',
    NULL,
    108072,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_08_08',
    '扩展档案',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_08'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1808,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_08_08_01',
    '机构地址管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_08_08'),
    'MENU',
    '/archive/8/1',
    'views/feature/index',
    'store:biz:08:08:01:view',
    NULL,
    108081,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_09',
    '预警管理',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    'bell',
    109,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_09_01',
    '预警规则',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_09'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    1901,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_09_01_01',
    '创建预警',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_09_01'),
    'MENU',
    '/warning/1/1',
    'views/feature/index',
    'store:biz:09:01:01:view',
    NULL,
    109011,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_09_01_02',
    '预警管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_09_01'),
    'MENU',
    '/warning/1/2',
    'views/feature/index',
    'store:biz:09:01:02:view',
    NULL,
    109012,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MOD_10',
    '系统设置',
    NULL,
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    'setting',
    110,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_10_01',
    '设置',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_10'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    2001,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_10_01_01',
    '参数设置',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_10_01'),
    'MENU',
    '/system/1/1',
    'views/feature/index',
    'store:biz:10:01:01:view',
    NULL,
    110011,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_10_01_02',
    '业务关系',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_10_01'),
    'MENU',
    '/system/1/2',
    'views/feature/index',
    'store:biz:10:01:02:view',
    NULL,
    110012,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_10_01_03',
    '打印模板',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_10_01'),
    'MENU',
    '/system/1/3',
    'views/feature/index',
    'store:biz:10:01:03:view',
    NULL,
    110013,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_10_01_04',
    '消息设置',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_10_01'),
    'MENU',
    '/system/1/4',
    'views/feature/index',
    'store:biz:10:01:04:view',
    NULL,
    110014,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_10_01_05',
    '用户管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_10_01'),
    'MENU',
    '/system/1/5',
    'views/feature/index',
    'store:biz:10:01:05:view',
    NULL,
    110015,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_10_01_06',
    '角色管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_10_01'),
    'MENU',
    '/system/1/6',
    'views/feature/index',
    'store:biz:10:01:06:view',
    NULL,
    110016,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_10_01_07',
    '菜单权限管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_10_01'),
    'MENU',
    '/system/1/7',
    'views/feature/index',
    'store:biz:10:01:07:view',
    NULL,
    110017,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_10_02',
    '供应商协调设置',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_10'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    2002,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_10_02_01',
    '供应商打印模板',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_10_02'),
    'MENU',
    '/system/2/1',
    'views/feature/index',
    'store:biz:10:02:01:view',
    NULL,
    110021,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_GRP_10_03',
    '任务中心',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_MOD_10'),
    'DIRECTORY',
    NULL,
    NULL,
    NULL,
    NULL,
    2003,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, component_path, permission_code, icon, sort_no, visible, status)
VALUES (
    'STORE_BIZ_MENU_10_03_01',
    '任务管理',
    (SELECT id FROM sys_menu WHERE menu_code = 'STORE_BIZ_GRP_10_03'),
    'MENU',
    '/system/3/1',
    'views/feature/index',
    'store:biz:10:03:01:view',
    NULL,
    110031,
    TRUE,
    'ENABLED'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu_rel (role_id, menu_id)
SELECT
    (SELECT id FROM sys_role WHERE role_code = 'STORE_ADMIN'),
    m.id
FROM sys_menu m
WHERE m.menu_code LIKE 'STORE_BIZ_%'
ON CONFLICT DO NOTHING;
-- STORE_MENU_SEED_END
