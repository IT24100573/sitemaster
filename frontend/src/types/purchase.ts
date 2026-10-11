export type PurchaseOrderStatus = 'SENT' | 'ACCEPTED' | 'DECLINED' | 'DISPATCHED' | 'INVOICED' | 'PAID' | 'DELIVERED';

export interface PurchaseOrder {
  id: number;
  poNumber: string;
  materialRequestId: number;
  projectId: number;
  projectName: string;
  deliveryLocation: string;
  supplierId: number;
  supplierName: string;
  status: PurchaseOrderStatus;
  orderDate: string;
  totalAmount: number;
  notes: string | null;
  respondedAt: string | null;
  declineReason: string | null;
  dispatchedAt: string | null;
  expectedDeliveryDate: string | null;
  paidAt: string | null;
  deliveredAt: string | null;
  items: { materialName: string; unit: string; quantity: number; unitPrice: number; lineTotal: number }[];
  invoice: { id: number; invoiceNumber: string; amount: number; paidAmount: number; status: string } | null;
}

export interface PaymentRecord {
  id: number;
  invoiceId: number | null;
  invoiceNumber: string | null;
  poNumber: string | null;
  supplierName: string | null;
  amount: number;
  method: string;
  referenceNo: string | null;
  paymentDate: string;
  notes: string | null;
  paidByName: string;
  createdAt: string;
}

export interface SupplierInvoice {
  id: number;
  purchaseOrderId: number;
  poNumber: string;
  poStatus: string;
  supplierId: number;
  supplierName: string;
  projectName: string;
  invoiceNumber: string;
  invoiceDate: string;
  amount: number;
  paidAmount: number;
  outstanding: number;
  status: string;
  fileName: string;
  payable: boolean;
  payments: PaymentRecord[];
  createdAt: string;
}

export interface PaymentPayload {
  amount: string;
  method: string;
  referenceNo: string | null;
  paymentDate: string;
  notes: string | null;
}
