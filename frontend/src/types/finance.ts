export interface Expense {
  id: number;
  projectId: number;
  projectName: string;
  phaseId: number;
  phaseName: string;
  category: string;
  amount: number;
  expenseDate: string;
  payee: string;
  description: string;
  receiptName: string | null;
  receiptRequired: boolean;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'PAID';
  requestedById: number;
  requestedByName: string;
  approvedByName: string | null;
  remarks: string | null;
  decidedAt: string | null;
  paidAt: string | null;
  paymentMethod: string | null;
  paymentReference: string | null;
  paidFromPettyCash: boolean;
  canReview: boolean;
  canPay: boolean;
  createdAt: string;
}

export interface PettyCashFund {
  id: number;
  projectId: number;
  projectName: string;
  custodianId: number;
  custodianName: string;
  floatLimit: number;
  balance: number;
  active: boolean;
  canManage: boolean;
}

export interface PettyCashTransaction {
  id: number;
  type: 'TOP_UP' | 'EXPENSE' | 'ADJUSTMENT';
  amount: number;
  balanceAfter: number;
  date: string;
  description: string;
  expenseId: number | null;
  recordedByName: string;
  createdAt: string;
}

export interface ClientInvoice {
  id: number;
  invoiceNumber: string;
  projectId: number;
  projectName: string;
  clientId: number;
  clientName: string;
  clientAddress: string | null;
  issueDate: string;
  dueDate: string;
  status: 'DRAFT' | 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE';
  taxRate: number;
  subtotal: number;
  taxAmount: number;
  total: number;
  paidAmount: number;
  outstanding: number;
  notes: string | null;
  issuedAt: string | null;
  lines: { description: string; quantity: number; unitPrice: number; lineTotal: number }[];
  receipts: { id: number; amount: number; paymentDate: string; method: string; referenceNo: string | null; notes: string | null; receivedByName: string }[];
}

export interface ClientInvoicePayload {
  projectId: number;
  issueDate: string;
  dueDate: string;
  taxRate: string;
  notes: string | null;
  lines: { description: string; quantity: string; unitPrice: string }[];
}

export interface ProjectFinance {
  projectId: number;
  projectName: string;
  status: string;
  budget: number;
  spentMaterials: number;
  spentExpenses: number;
  totalSpent: number;
  committed: number;
  remaining: number;
  usedPercent: number;
  invoiced: number;
  received: number;
  receivable: number;
  pettyCashBalance: number | null;
  byCategory: { category: string; amount: number }[];
  phases: { phaseId: number; name: string; budget: number; committed: number; remaining: number }[];
}

export interface ReportInfo {
  type: string;
  title: string;
  description: string;
}

export type ColumnKind = 'TEXT' | 'NUMBER' | 'MONEY' | 'PERCENT' | 'DATE';

export interface ReportTable {
  title: string;
  subtitle: string;
  columns: { header: string; kind: ColumnKind }[];
  rows: (string | number | null)[][];
  totals: (string | number | null)[] | null;
}

export interface Dashboard {
  title: string;
  subtitle: string;
  stats: { label: string; value: number; format: 'number' | 'money' | 'percent'; tone: 'neutral' | 'good' | 'warning' | 'danger'; link: string | null; hint: string | null }[];
  charts: { title: string; type: 'bar' | 'line' | 'pie'; format: 'number' | 'money' | 'percent'; series: string[]; points: { label: string; values: number[] }[] }[];
  lists: { title: string; emptyText: string; link: string | null; items: { title: string; subtitle: string; status: string | null; link: string | null }[] }[];
}
