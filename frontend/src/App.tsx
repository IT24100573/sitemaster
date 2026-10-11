import type { ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { ROLE_ROUTES } from './config/roles';
import { AppLayout } from './components/layout/AppLayout';
import { AuthLayout } from './components/layout/AuthLayout';
import { PublicLayout } from './components/layout/PublicLayout';
import { GuestOnly } from './routes/GuestOnly';
import { RequireAuth } from './routes/RequireAuth';
import { RequirePermission } from './routes/RequirePermission';
import HomePage from './pages/public/HomePage';
import SitesMapPage from './pages/public/SitesMapPage';
import AboutPage from './pages/public/AboutPage';
import SupplierApplicationPage from './pages/public/SupplierApplicationPage';
import LoginPage from './pages/auth/LoginPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ResetPasswordPage from './pages/auth/ResetPasswordPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import ForbiddenPage from './pages/errors/ForbiddenPage';
import NotFoundPage from './pages/errors/NotFoundPage';
import UsersPage from './pages/admin/UsersPage';
import UserFormPage from './pages/admin/UserFormPage';
import RolesPermissionsPage from './pages/admin/RolesPermissionsPage';
import SettingsPage from './pages/admin/SettingsPage';
import AuditLogPage from './pages/admin/AuditLogPage';
import ClientsPage from './pages/admin/ClientsPage';
import SuppliersPage from './pages/admin/SuppliersPage';
import SupplierApplicationsPage from './pages/admin/SupplierApplicationsPage';
import SupplierApplicationDetailPage from './pages/admin/SupplierApplicationDetailPage';
import EmployeesPage from './pages/employees/EmployeesPage';
import EmployeeFormPage from './pages/employees/EmployeeFormPage';
import EmployeeDetailPage from './pages/employees/EmployeeDetailPage';
import ProjectsPage from './pages/projects/ProjectsPage';
import ProjectFormPage from './pages/projects/ProjectFormPage';
import ProjectDetailPage from './pages/projects/ProjectDetailPage';
import MyTasksPage from './pages/tasks/MyTasksPage';
import NotificationsPage from './pages/notifications/NotificationsPage';
import MaterialsPage from './pages/site/MaterialsPage';
import SiteStockPage from './pages/site/SiteStockPage';
import EquipmentPage from './pages/site/EquipmentPage';
import DailyReportsPage from './pages/site/DailyReportsPage';
import DailyReportFormPage from './pages/site/DailyReportFormPage';
import DailyReportDetailPage from './pages/site/DailyReportDetailPage';
import MaterialRequestsPage from './pages/site/MaterialRequestsPage';
import MaterialRequestFormPage from './pages/site/MaterialRequestFormPage';
import MaterialRequestDetailPage from './pages/site/MaterialRequestDetailPage';
import ApprovalsPage from './pages/site/ApprovalsPage';
import PurchaseOrdersPage from './pages/purchasing/PurchaseOrdersPage';
import PurchaseOrderDetailPage from './pages/purchasing/PurchaseOrderDetailPage';
import SupplierInvoicesPage from './pages/purchasing/SupplierInvoicesPage';
import PaymentsPage from './pages/purchasing/PaymentsPage';
import CompanyProfilePage from './pages/supplier/CompanyProfilePage';
import MyAttendancePage from './pages/workforce/MyAttendancePage';
import MyLeavePage from './pages/workforce/MyLeavePage';
import AttendancePage from './pages/workforce/AttendancePage';
import AttendanceSheetPage from './pages/workforce/AttendanceSheetPage';
import OvertimePage from './pages/workforce/OvertimePage';
import ShiftsPage from './pages/workforce/ShiftsPage';
import LeaveManagementPage from './pages/workforce/LeaveManagementPage';
import ContractsPage from './pages/workforce/ContractsPage';
import TrainingPage from './pages/workforce/TrainingPage';
import PayrollPage from './pages/payroll/PayrollPage';
import PayrollRunPage from './pages/payroll/PayrollRunPage';
import MyPayslipsPage from './pages/payroll/MyPayslipsPage';
import ExpensesPage from './pages/finance/ExpensesPage';
import ExpenseDetailPage from './pages/finance/ExpenseDetailPage';
import PettyCashPage from './pages/finance/PettyCashPage';
import ClientInvoicesPage from './pages/finance/ClientInvoicesPage';
import ClientInvoiceFormPage from './pages/finance/ClientInvoiceFormPage';
import ClientInvoiceDetailPage from './pages/finance/ClientInvoiceDetailPage';
import ProjectFinancePage from './pages/finance/ProjectFinancePage';
import ReportsPage from './pages/reports/ReportsPage';
import ProfilePage from './pages/profile/ProfilePage';
import type { RoleName } from './types/auth';

function guarded(permission: string, page: ReactNode) {
  return <RequirePermission permission={permission}>{page}</RequirePermission>;
}

const PROJECT_ROLES: RoleName[] = ['ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'WORKER', 'ACCOUNTANT'];
const TASK_ROLES: RoleName[] = ['PROJECT_MANAGER', 'SITE_ENGINEER', 'WORKER'];
const CLIENT_ROLES: RoleName[] = ['ADMIN', 'ACCOUNTANT'];
const SITE_ROLES: RoleName[] = ['ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER'];
const REQUEST_ROLES: RoleName[] = ['ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'ACCOUNTANT'];
const ORDER_ROLES: RoleName[] = ['ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'ACCOUNTANT', 'SUPPLIER'];
const MATERIAL_ROLES: RoleName[] = ['ADMIN', 'PROJECT_MANAGER', 'ACCOUNTANT'];
const ATTENDANCE_ROLES: RoleName[] = ['ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'HR_MANAGER'];
const EMPLOYEE_ROLES: RoleName[] = ['PROJECT_MANAGER', 'SITE_ENGINEER', 'WORKER', 'HR_MANAGER', 'ACCOUNTANT'];
const PAYROLL_STAFF: RoleName[] = ['ADMIN', 'HR_MANAGER', 'ACCOUNTANT'];
const HR_RECORD_ROLES: RoleName[] = ['ADMIN', 'HR_MANAGER'];
const EXPENSE_ROLES: RoleName[] = ['ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'ACCOUNTANT'];
const PETTY_CASH_ROLES: RoleName[] = ['PROJECT_MANAGER', 'SITE_ENGINEER', 'ACCOUNTANT'];
const INVOICE_ROLES: RoleName[] = ['ADMIN', 'ACCOUNTANT'];
const FINANCE_ROLES: RoleName[] = ['ADMIN', 'PROJECT_MANAGER', 'ACCOUNTANT'];
const REPORT_ROLES: RoleName[] = ['ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'HR_MANAGER', 'ACCOUNTANT'];

/** Pages that exist only in some roles' areas; permissions are checked again inside. */
function roleSpecificRoutes(role: RoleName) {
  return (
    <>
      {PROJECT_ROLES.includes(role) && (
        <>
          <Route path="projects" element={guarded('PROJECT:VIEW', <ProjectsPage />)} />
          <Route path="projects/:id" element={guarded('PROJECT:VIEW', <ProjectDetailPage />)} />
        </>
      )}
      {role === 'ADMIN' && <Route path="projects/new" element={guarded('PROJECT:CREATE', <ProjectFormPage />)} />}
      {(role === 'ADMIN' || role === 'PROJECT_MANAGER') && (
        <Route path="projects/:id/edit" element={guarded('PROJECT:EDIT', <ProjectFormPage />)} />
      )}
      {TASK_ROLES.includes(role) && <Route path="my-tasks" element={guarded('TASK:VIEW', <MyTasksPage />)} />}
      {SITE_ROLES.includes(role) && (
        <>
          <Route path="daily-reports" element={guarded('DAILY_REPORT:VIEW', <DailyReportsPage />)} />
          <Route path="daily-reports/:id" element={guarded('DAILY_REPORT:VIEW', <DailyReportDetailPage />)} />
          <Route path="equipment" element={guarded('EQUIPMENT:VIEW', <EquipmentPage />)} />
        </>
      )}
      {role === 'SITE_ENGINEER' && (
        <>
          <Route path="daily-reports/new" element={guarded('DAILY_REPORT:CREATE', <DailyReportFormPage />)} />
          <Route path="daily-reports/:id/edit" element={guarded('DAILY_REPORT:EDIT', <DailyReportFormPage />)} />
          <Route path="material-requests/new" element={guarded('MATERIAL_REQUEST:CREATE', <MaterialRequestFormPage />)} />
        </>
      )}
      {(role === 'PROJECT_MANAGER' || role === 'SITE_ENGINEER') && (
        <Route path="site-stock" element={guarded('MATERIAL:VIEW', <SiteStockPage />)} />
      )}
      {role === 'PROJECT_MANAGER' && <Route path="approvals" element={guarded('MATERIAL_REQUEST:APPROVE', <ApprovalsPage />)} />}
      {REQUEST_ROLES.includes(role) && (
        <>
          <Route path="material-requests" element={guarded('MATERIAL_REQUEST:VIEW', <MaterialRequestsPage />)} />
          <Route path="material-requests/:id" element={guarded('MATERIAL_REQUEST:VIEW', <MaterialRequestDetailPage />)} />
        </>
      )}
      {MATERIAL_ROLES.includes(role) && <Route path="materials" element={guarded('MATERIAL:VIEW', <MaterialsPage />)} />}
      {ORDER_ROLES.includes(role) && (
        <>
          <Route path="purchase-orders" element={guarded('PURCHASE_ORDER:VIEW', <PurchaseOrdersPage />)} />
          <Route path="purchase-orders/:id" element={guarded('PURCHASE_ORDER:VIEW', <PurchaseOrderDetailPage />)} />
        </>
      )}
      {(role === 'ADMIN' || role === 'ACCOUNTANT') && (
        <>
          <Route path="supplier-invoices" element={guarded('SUPPLIER_INVOICE:VIEW', <SupplierInvoicesPage />)} />
          <Route path="payments" element={guarded('PAYMENT:VIEW', <PaymentsPage />)} />
        </>
      )}
      {(role === 'WORKER' || role === 'SITE_ENGINEER') && (
        <Route path="my-attendance" element={guarded('ATTENDANCE:CREATE', <MyAttendancePage />)} />
      )}
      {EMPLOYEE_ROLES.includes(role) && (
        <>
          <Route path="my-leave" element={guarded('LEAVE:CREATE', <MyLeavePage />)} />
          <Route path="payslips" element={guarded('PAYROLL:VIEW', <MyPayslipsPage />)} />
        </>
      )}
      {ATTENDANCE_ROLES.includes(role) && (
        <>
          <Route path="attendance" element={guarded('ATTENDANCE:VIEW', <AttendancePage />)} />
          <Route path="attendance-sheet" element={guarded('ATTENDANCE:VIEW', <AttendanceSheetPage />)} />
        </>
      )}
      {(role === 'SITE_ENGINEER' || role === 'HR_MANAGER') && (
        <Route path="overtime" element={guarded('ATTENDANCE:APPROVE', <OvertimePage />)} />
      )}
      {role === 'HR_MANAGER' && <Route path="shifts" element={guarded('ATTENDANCE:VIEW', <ShiftsPage />)} />}
      {HR_RECORD_ROLES.includes(role) && (
        <>
          <Route path="leave" element={guarded('LEAVE:VIEW', <LeaveManagementPage />)} />
          <Route path="contracts" element={guarded('EMPLOYEE:VIEW', <ContractsPage />)} />
          <Route path="training" element={guarded('EMPLOYEE:VIEW', <TrainingPage />)} />
        </>
      )}
      {PAYROLL_STAFF.includes(role) && (
        <>
          <Route path="payroll" element={guarded('PAYROLL:VIEW', <PayrollPage />)} />
          <Route path="payroll/:id" element={guarded('PAYROLL:VIEW', <PayrollRunPage />)} />
        </>
      )}
      {EXPENSE_ROLES.includes(role) && (
        <>
          <Route path="expenses" element={guarded('EXPENSE:VIEW', <ExpensesPage />)} />
          <Route path="expenses/:id" element={guarded('EXPENSE:VIEW', <ExpenseDetailPage />)} />
        </>
      )}
      {PETTY_CASH_ROLES.includes(role) && <Route path="petty-cash" element={guarded('EXPENSE:VIEW', <PettyCashPage />)} />}
      {INVOICE_ROLES.includes(role) && (
        <>
          <Route path="client-invoices" element={guarded('INVOICE:VIEW', <ClientInvoicesPage />)} />
          <Route path="client-invoices/new" element={guarded('INVOICE:CREATE', <ClientInvoiceFormPage />)} />
          <Route path="client-invoices/:id" element={guarded('INVOICE:VIEW', <ClientInvoiceDetailPage />)} />
          <Route path="client-invoices/:id/edit" element={guarded('INVOICE:EDIT', <ClientInvoiceFormPage />)} />
        </>
      )}
      {FINANCE_ROLES.includes(role) && <Route path="finance" element={guarded('PROJECT:VIEW', <ProjectFinancePage />)} />}
      {REPORT_ROLES.includes(role) && <Route path="reports" element={guarded('REPORT:VIEW', <ReportsPage />)} />}
      {role === 'SUPPLIER' && (
        <>
          <Route path="invoices" element={guarded('SUPPLIER_INVOICE:VIEW', <SupplierInvoicesPage />)} />
          <Route path="payments" element={guarded('PAYMENT:VIEW', <PaymentsPage />)} />
          <Route path="company-profile" element={guarded('SUPPLIER:VIEW', <CompanyProfilePage />)} />
        </>
      )}
      {CLIENT_ROLES.includes(role) && (
        <>
          <Route path="clients" element={guarded('CLIENT:VIEW', <ClientsPage />)} />
          <Route path="suppliers" element={guarded('SUPPLIER:VIEW', <SuppliersPage />)} />
        </>
      )}
      {role === 'ADMIN' && (
        <>
          <Route path="supplier-applications" element={guarded('SUPPLIER:VIEW', <SupplierApplicationsPage />)} />
          <Route path="supplier-applications/:id" element={guarded('SUPPLIER:VIEW', <SupplierApplicationDetailPage />)} />
          <Route path="users" element={guarded('USER:VIEW', <UsersPage />)} />
          <Route path="users/new" element={guarded('USER:CREATE', <UserFormPage />)} />
          <Route path="users/:id/edit" element={guarded('USER:EDIT', <UserFormPage />)} />
          <Route path="roles" element={guarded('ROLE:VIEW', <RolesPermissionsPage />)} />
          <Route path="settings" element={guarded('SETTINGS:VIEW', <SettingsPage />)} />
          <Route path="audit-log" element={guarded('AUDIT_LOG:VIEW', <AuditLogPage />)} />
        </>
      )}
      {(role === 'ADMIN' || role === 'HR_MANAGER') && (
        <>
          <Route path="employees" element={guarded('EMPLOYEE:VIEW', <EmployeesPage />)} />
          <Route path="employees/new" element={guarded('EMPLOYEE:CREATE', <EmployeeFormPage />)} />
          <Route path="employees/:id" element={guarded('EMPLOYEE:VIEW', <EmployeeDetailPage />)} />
          <Route path="employees/:id/edit" element={guarded('EMPLOYEE:EDIT', <EmployeeFormPage />)} />
        </>
      )}
    </>
  );
}

export default function App() {
  return (
    <Routes>
      {/* Public website */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/sites" element={<SitesMapPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/become-a-supplier" element={<SupplierApplicationPage />} />
      </Route>

      <Route element={<AuthLayout />}>
        <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
        <Route path="/forgot-password" element={<GuestOnly><ForgotPasswordPage /></GuestOnly>} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
      </Route>

      {/* One protected area per role: /admin, /pm, /site, /worker, /hr, /accounts, /supplier */}
      {ROLE_ROUTES.map(({ role, path }) => (
        <Route
          key={role}
          path={`/${path}`}
          element={
            <RequireAuth role={role}>
              <AppLayout />
            </RequireAuth>
          }
        >
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="profile" element={<ProfilePage />} />
          {roleSpecificRoutes(role)}
          <Route path="*" element={<NotFoundPage embedded />} />
        </Route>
      ))}

      <Route path="/403" element={<ForbiddenPage />} />
      <Route path="/404" element={<NotFoundPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
