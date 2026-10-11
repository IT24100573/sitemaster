-- =============================================================================
-- V2: Reference data - the seven roles, every MODULE:ACTION permission and the
-- default permission matrix. Admin can change the matrix later on the
-- Roles & Permissions page; this is only the starting point.
-- =============================================================================

INSERT INTO roles (name, display_name, description) VALUES
    ('ADMIN', 'Administrator', 'Owns the system: users, roles, projects, suppliers, settings and the audit log.'),
    ('PROJECT_MANAGER', 'Project Manager', 'Plans projects, sets budgets and approves site requests and reports.'),
    ('SITE_ENGINEER', 'Site Engineer', 'Runs the site: tasks, attendance, daily reports, material and expense requests.'),
    ('WORKER', 'Worker', 'Checks in and out, updates own tasks, applies for leave and views payslips.'),
    ('HR_MANAGER', 'HR Manager', 'Manages employees, attendance corrections, leave, contracts and payroll summaries.'),
    ('ACCOUNTANT', 'Accountant', 'Records expenses, raises purchase orders, pays suppliers, runs payroll and bills clients.'),
    ('SUPPLIER', 'Supplier', 'Responds to purchase orders, uploads invoices and tracks payments.');

-- Every combination of module x action.
INSERT INTO permissions (module, action, description)
SELECT m.module, a.action, m.module || ':' || a.action
FROM (VALUES
    ('USER'),
    ('ROLE'),
    ('EMPLOYEE'),
    ('PROJECT'),
    ('TASK'),
    ('DAILY_REPORT'),
    ('MATERIAL'),
    ('MATERIAL_REQUEST'),
    ('PURCHASE_ORDER'),
    ('SUPPLIER'),
    ('SUPPLIER_INVOICE'),
    ('EQUIPMENT'),
    ('ISSUE'),
    ('DOCUMENT'),
    ('ATTENDANCE'),
    ('LEAVE'),
    ('PAYROLL'),
    ('EXPENSE'),
    ('PAYMENT'),
    ('CLIENT'),
    ('INVOICE'),
    ('REPORT'),
    ('SETTINGS'),
    ('AUDIT_LOG')
) AS m(module)
CROSS JOIN (VALUES ('VIEW'), ('CREATE'), ('EDIT'), ('APPROVE'), ('DELETE')) AS a(action);

-- ADMIN: full control of the modules it owns and read access to everything else.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.action = 'VIEW'
    OR p.module IN ('USER', 'ROLE', 'PROJECT', 'SUPPLIER', 'CLIENT', 'REPORT', 'SETTINGS', 'AUDIT_LOG')
WHERE r.name = 'ADMIN';

-- PROJECT_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN (VALUES
    ('PROJECT', 'VIEW'), ('PROJECT', 'EDIT'), ('TASK', 'VIEW'), ('TASK', 'CREATE'),
    ('TASK', 'EDIT'), ('TASK', 'DELETE'), ('DAILY_REPORT', 'VIEW'), ('DAILY_REPORT', 'APPROVE'),
    ('MATERIAL', 'VIEW'), ('MATERIAL_REQUEST', 'VIEW'), ('MATERIAL_REQUEST', 'APPROVE'), ('PURCHASE_ORDER', 'VIEW'),
    ('ATTENDANCE', 'VIEW'), ('LEAVE', 'VIEW'), ('EXPENSE', 'VIEW'), ('EXPENSE', 'EDIT'),
    ('EXPENSE', 'APPROVE'), ('REPORT', 'VIEW'), ('DOCUMENT', 'VIEW'), ('DOCUMENT', 'CREATE'),
    ('DOCUMENT', 'EDIT'), ('DOCUMENT', 'DELETE'), ('ISSUE', 'VIEW'), ('ISSUE', 'CREATE'),
    ('ISSUE', 'EDIT'), ('EQUIPMENT', 'VIEW')
) AS g(module, action) ON TRUE
JOIN permissions p ON p.module = g.module AND p.action = g.action
WHERE r.name = 'PROJECT_MANAGER';

-- SITE_ENGINEER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN (VALUES
    ('PROJECT', 'VIEW'), ('TASK', 'VIEW'), ('TASK', 'CREATE'), ('TASK', 'EDIT'),
    ('DAILY_REPORT', 'VIEW'), ('DAILY_REPORT', 'CREATE'), ('DAILY_REPORT', 'EDIT'), ('MATERIAL', 'VIEW'),
    ('MATERIAL_REQUEST', 'VIEW'), ('MATERIAL_REQUEST', 'CREATE'), ('MATERIAL_REQUEST', 'EDIT'), ('PURCHASE_ORDER', 'VIEW'),
    ('ATTENDANCE', 'VIEW'), ('ATTENDANCE', 'CREATE'), ('ATTENDANCE', 'EDIT'), ('ATTENDANCE', 'APPROVE'),
    ('LEAVE', 'VIEW'), ('LEAVE', 'CREATE'), ('PAYROLL', 'VIEW'), ('EXPENSE', 'VIEW'),
    ('EXPENSE', 'CREATE'), ('REPORT', 'VIEW'), ('DOCUMENT', 'VIEW'), ('ISSUE', 'VIEW'),
    ('ISSUE', 'CREATE'), ('ISSUE', 'EDIT'), ('EQUIPMENT', 'VIEW'), ('EQUIPMENT', 'EDIT')
) AS g(module, action) ON TRUE
JOIN permissions p ON p.module = g.module AND p.action = g.action
WHERE r.name = 'SITE_ENGINEER';

-- WORKER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN (VALUES
    ('PROJECT', 'VIEW'), ('TASK', 'VIEW'), ('TASK', 'EDIT'), ('ATTENDANCE', 'VIEW'),
    ('ATTENDANCE', 'CREATE'), ('LEAVE', 'VIEW'), ('LEAVE', 'CREATE'), ('PAYROLL', 'VIEW')
) AS g(module, action) ON TRUE
JOIN permissions p ON p.module = g.module AND p.action = g.action
WHERE r.name = 'WORKER';

-- HR_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN (VALUES
    ('USER', 'VIEW'), ('USER', 'CREATE'), ('EMPLOYEE', 'VIEW'), ('EMPLOYEE', 'CREATE'),
    ('EMPLOYEE', 'EDIT'), ('EMPLOYEE', 'DELETE'), ('ATTENDANCE', 'VIEW'), ('ATTENDANCE', 'CREATE'),
    ('ATTENDANCE', 'EDIT'), ('ATTENDANCE', 'APPROVE'), ('LEAVE', 'VIEW'), ('LEAVE', 'CREATE'),
    ('LEAVE', 'EDIT'), ('LEAVE', 'APPROVE'), ('PAYROLL', 'VIEW'), ('PAYROLL', 'CREATE'),
    ('REPORT', 'VIEW')
) AS g(module, action) ON TRUE
JOIN permissions p ON p.module = g.module AND p.action = g.action
WHERE r.name = 'HR_MANAGER';

-- ACCOUNTANT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN (VALUES
    ('PROJECT', 'VIEW'), ('MATERIAL', 'VIEW'), ('MATERIAL_REQUEST', 'VIEW'), ('MATERIAL_REQUEST', 'EDIT'),
    ('PURCHASE_ORDER', 'VIEW'), ('PURCHASE_ORDER', 'CREATE'), ('PURCHASE_ORDER', 'EDIT'), ('PURCHASE_ORDER', 'DELETE'),
    ('SUPPLIER', 'VIEW'), ('SUPPLIER_INVOICE', 'VIEW'), ('SUPPLIER_INVOICE', 'EDIT'), ('SUPPLIER_INVOICE', 'APPROVE'),
    ('ATTENDANCE', 'VIEW'), ('PAYROLL', 'VIEW'), ('PAYROLL', 'CREATE'), ('PAYROLL', 'EDIT'),
    ('PAYROLL', 'APPROVE'), ('EXPENSE', 'VIEW'), ('EXPENSE', 'CREATE'), ('EXPENSE', 'EDIT'),
    ('EXPENSE', 'APPROVE'), ('PAYMENT', 'VIEW'), ('PAYMENT', 'CREATE'), ('PAYMENT', 'EDIT'),
    ('CLIENT', 'VIEW'), ('CLIENT', 'CREATE'), ('CLIENT', 'EDIT'), ('INVOICE', 'VIEW'),
    ('INVOICE', 'CREATE'), ('INVOICE', 'EDIT'), ('REPORT', 'VIEW')
) AS g(module, action) ON TRUE
JOIN permissions p ON p.module = g.module AND p.action = g.action
WHERE r.name = 'ACCOUNTANT';

-- SUPPLIER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN (VALUES
    ('PURCHASE_ORDER', 'VIEW'), ('PURCHASE_ORDER', 'EDIT'), ('SUPPLIER_INVOICE', 'VIEW'), ('SUPPLIER_INVOICE', 'CREATE'),
    ('PAYMENT', 'VIEW'), ('SUPPLIER', 'VIEW'), ('SUPPLIER', 'EDIT')
) AS g(module, action) ON TRUE
JOIN permissions p ON p.module = g.module AND p.action = g.action
WHERE r.name = 'SUPPLIER';
