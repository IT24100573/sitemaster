import {
  AreaChart,
  Award,
  Coins,
  FileBarChart,
  HandCoins,
  Banknote,
  CalendarCheck,
  CalendarClock,
  CalendarDays,
  Clock,
  FileBadge,
  Grid3x3,
  Plane,
  Wallet,
  Bell,
  Boxes,
  ClipboardCheck,
  FileText,
  Inbox,
  Package,
  Receipt,
  ShoppingCart,
  Store,
  Warehouse,
  Briefcase,
  Building2,
  ClipboardList,
  FileSignature,
  HardHat,
  LayoutDashboard,
  ListChecks,
  Settings,
  ShieldCheck,
  Truck,
  UserCircle,
  Users,
  Wrench,
  type LucideIcon,
} from 'lucide-react';
import type { RoleName } from '../types/auth';
import { ROLE_PATHS } from './roles';

export interface NavItem {
  label: string;
  /** Path below the role area, e.g. "users" -> /admin/users */
  path: string;
  icon: LucideIcon;
  /** Hidden unless the user holds this MODULE:ACTION permission. */
  permission?: string;
}

export interface NavSection {
  title?: string;
  items: NavItem[];
}

const DASHBOARD: NavItem = { label: 'Dashboard', path: 'dashboard', icon: LayoutDashboard };

const PROJECTS: NavItem = { label: 'Projects', path: 'projects', icon: Building2, permission: 'PROJECT:VIEW' };
const MY_PROJECTS: NavItem = { ...PROJECTS, label: 'My projects' };
const MY_TASKS: NavItem = { label: 'My tasks', path: 'my-tasks', icon: ListChecks, permission: 'TASK:VIEW' };
const CLIENTS: NavItem = { label: 'Clients', path: 'clients', icon: Briefcase, permission: 'CLIENT:VIEW' };
const SUPPLIERS: NavItem = { label: 'Suppliers', path: 'suppliers', icon: Truck, permission: 'SUPPLIER:VIEW' };

const MATERIALS: NavItem = { label: 'Materials', path: 'materials', icon: Boxes, permission: 'MATERIAL:VIEW' };
const EQUIPMENT: NavItem = { label: 'Equipment', path: 'equipment', icon: Wrench, permission: 'EQUIPMENT:VIEW' };
const DAILY_REPORTS: NavItem = { label: 'Daily reports', path: 'daily-reports', icon: FileText, permission: 'DAILY_REPORT:VIEW' };
const MATERIAL_REQUESTS: NavItem = { label: 'Material requests', path: 'material-requests', icon: Package, permission: 'MATERIAL_REQUEST:VIEW' };
const SITE_STOCK: NavItem = { label: 'Site stock', path: 'site-stock', icon: Warehouse, permission: 'MATERIAL:VIEW' };
const PURCHASE_ORDERS: NavItem = { label: 'Purchase orders', path: 'purchase-orders', icon: ShoppingCart, permission: 'PURCHASE_ORDER:VIEW' };
const SUPPLIER_INVOICES: NavItem = { label: 'Supplier invoices', path: 'supplier-invoices', icon: Receipt, permission: 'SUPPLIER_INVOICE:VIEW' };
const PAYMENTS: NavItem = { label: 'Payments', path: 'payments', icon: Banknote, permission: 'PAYMENT:VIEW' };

const ATTENDANCE: NavItem = { label: 'Attendance', path: 'attendance', icon: CalendarCheck, permission: 'ATTENDANCE:VIEW' };
const ATTENDANCE_SHEET: NavItem = { label: 'Attendance sheet', path: 'attendance-sheet', icon: Grid3x3, permission: 'ATTENDANCE:VIEW' };
const OVERTIME: NavItem = { label: 'Overtime', path: 'overtime', icon: Clock, permission: 'ATTENDANCE:APPROVE' };
const MY_ATTENDANCE: NavItem = { label: 'My attendance', path: 'my-attendance', icon: CalendarClock, permission: 'ATTENDANCE:CREATE' };
const MY_LEAVE: NavItem = { label: 'My leave', path: 'my-leave', icon: Plane, permission: 'LEAVE:CREATE' };
const MY_PAYSLIPS: NavItem = { label: 'My payslips', path: 'payslips', icon: Wallet, permission: 'PAYROLL:VIEW' };
const PAYROLL: NavItem = { label: 'Payroll', path: 'payroll', icon: Banknote, permission: 'PAYROLL:VIEW' };
const CONTRACTS: NavItem = { label: 'Contracts', path: 'contracts', icon: FileBadge, permission: 'EMPLOYEE:VIEW' };
const TRAINING: NavItem = { label: 'Training & safety', path: 'training', icon: Award, permission: 'EMPLOYEE:VIEW' };
const LEAVE_ADMIN: NavItem = { label: 'Leave', path: 'leave', icon: CalendarDays, permission: 'LEAVE:VIEW' };

const EXPENSES: NavItem = { label: 'Expenses', path: 'expenses', icon: Coins, permission: 'EXPENSE:VIEW' };
const PETTY_CASH: NavItem = { label: 'Petty cash', path: 'petty-cash', icon: HandCoins, permission: 'EXPENSE:VIEW' };
const CLIENT_INVOICES: NavItem = { label: 'Client invoices', path: 'client-invoices', icon: Receipt, permission: 'INVOICE:VIEW' };
const PROJECT_FINANCE: NavItem = { label: 'Project finance', path: 'finance', icon: AreaChart, permission: 'PROJECT:VIEW' };
const REPORTS: NavItem = { label: 'Reports', path: 'reports', icon: FileBarChart, permission: 'REPORT:VIEW' };

const ACCOUNT: NavSection = {
  title: 'My account',
  items: [
    { label: 'Notifications', path: 'notifications', icon: Bell },
    { label: 'Profile', path: 'profile', icon: UserCircle },
  ],
};

/**
 * Sidebar per role. Each phase adds the pages it delivers; only pages that exist
 * are listed. Items with a permission are also hidden when an Admin removes it.
 */
const SECTIONS: Record<RoleName, NavSection[]> = {
  ADMIN: [
    {
      items: [
        DASHBOARD,
        PROJECTS,
        CLIENTS,
        SUPPLIERS,
        { label: 'Supplier applications', path: 'supplier-applications', icon: FileSignature, permission: 'SUPPLIER:VIEW' },
      ],
    },
    {
      title: 'Operations',
      items: [DAILY_REPORTS, MATERIAL_REQUESTS, PURCHASE_ORDERS, SUPPLIER_INVOICES, PAYMENTS, MATERIALS, EQUIPMENT],
    },
    { title: 'Finance', items: [PROJECT_FINANCE, EXPENSES, CLIENT_INVOICES, REPORTS] },
    { title: 'Workforce', items: [ATTENDANCE, ATTENDANCE_SHEET, LEAVE_ADMIN, CONTRACTS, TRAINING, PAYROLL] },
    {
      title: 'Administration',
      items: [
        { label: 'Users', path: 'users', icon: Users, permission: 'USER:VIEW' },
        { label: 'Roles & permissions', path: 'roles', icon: ShieldCheck, permission: 'ROLE:VIEW' },
        { label: 'Employees', path: 'employees', icon: HardHat, permission: 'EMPLOYEE:VIEW' },
        { label: 'Audit log', path: 'audit-log', icon: ClipboardList, permission: 'AUDIT_LOG:VIEW' },
        { label: 'System settings', path: 'settings', icon: Settings, permission: 'SETTINGS:VIEW' },
      ],
    },
    ACCOUNT,
  ],
  PROJECT_MANAGER: [
    {
      items: [
        DASHBOARD,
        MY_PROJECTS,
        { label: 'Approvals', path: 'approvals', icon: ClipboardCheck, permission: 'MATERIAL_REQUEST:APPROVE' },
        MY_TASKS,
      ],
    },
    { title: 'Site', items: [DAILY_REPORTS, MATERIAL_REQUESTS, PURCHASE_ORDERS, SITE_STOCK, MATERIALS, EQUIPMENT] },
    { title: 'Money', items: [EXPENSES, PETTY_CASH, PROJECT_FINANCE, REPORTS] },
    { title: 'Team', items: [ATTENDANCE, ATTENDANCE_SHEET] },
    { title: 'Me', items: [MY_LEAVE, MY_PAYSLIPS] },
    ACCOUNT,
  ],
  SITE_ENGINEER: [
    { items: [DASHBOARD, MY_PROJECTS, MY_TASKS] },
    { title: 'Site', items: [DAILY_REPORTS, MATERIAL_REQUESTS, EXPENSES, PETTY_CASH, SITE_STOCK, EQUIPMENT, PURCHASE_ORDERS, REPORTS] },
    { title: 'Team', items: [ATTENDANCE, ATTENDANCE_SHEET, OVERTIME] },
    { title: 'Me', items: [MY_ATTENDANCE, MY_LEAVE, MY_PAYSLIPS] },
    ACCOUNT,
  ],
  WORKER: [{ items: [DASHBOARD, MY_ATTENDANCE, MY_TASKS, MY_PROJECTS, MY_LEAVE, MY_PAYSLIPS] }, ACCOUNT],
  HR_MANAGER: [
    { items: [DASHBOARD, { label: 'Employees', path: 'employees', icon: HardHat, permission: 'EMPLOYEE:VIEW' }, CONTRACTS, TRAINING] },
    {
      title: 'Time & pay',
      items: [
        ATTENDANCE,
        ATTENDANCE_SHEET,
        OVERTIME,
        { label: 'Shifts', path: 'shifts', icon: CalendarClock, permission: 'ATTENDANCE:VIEW' },
        LEAVE_ADMIN,
        PAYROLL,
        REPORTS,
      ],
    },
    { title: 'Me', items: [MY_LEAVE, MY_PAYSLIPS] },
    ACCOUNT,
  ],
  ACCOUNTANT: [
    { items: [DASHBOARD, PROJECTS, CLIENTS, SUPPLIERS] },
    { title: 'Purchasing', items: [MATERIAL_REQUESTS, PURCHASE_ORDERS, SUPPLIER_INVOICES, PAYMENTS, MATERIALS] },
    { title: 'Finance', items: [EXPENSES, PETTY_CASH, CLIENT_INVOICES, PROJECT_FINANCE, REPORTS] },
    { title: 'Payroll', items: [PAYROLL] },
    { title: 'Me', items: [MY_LEAVE, MY_PAYSLIPS] },
    ACCOUNT,
  ],
  SUPPLIER: [
    {
      items: [
        DASHBOARD,
        { label: 'Purchase orders', path: 'purchase-orders', icon: Inbox, permission: 'PURCHASE_ORDER:VIEW' },
        { label: 'Invoices', path: 'invoices', icon: Receipt, permission: 'SUPPLIER_INVOICE:VIEW' },
        { label: 'Payments received', path: 'payments', icon: Banknote, permission: 'PAYMENT:VIEW' },
        { label: 'Company profile', path: 'company-profile', icon: Store, permission: 'SUPPLIER:VIEW' },
      ],
    },
    ACCOUNT,
  ],
};

export function navigationFor(role: RoleName, permissions: string[]): NavSection[] {
  const base = `/${ROLE_PATHS[role]}`;
  return SECTIONS[role]
    .map((section) => ({
      ...section,
      items: section.items
        .filter((item) => !item.permission || permissions.includes(item.permission))
        .map((item) => ({ ...item, path: `${base}/${item.path}` })),
    }))
    .filter((section) => section.items.length > 0);
}
