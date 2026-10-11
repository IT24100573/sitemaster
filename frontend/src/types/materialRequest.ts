export interface MaterialRequest {
  id: number;
  projectId: number;
  projectName: string;
  phaseId: number;
  phaseName: string;
  materialId: number;
  materialName: string;
  unit: string;
  quantity: number;
  urgency: string;
  requiredBy: string;
  estimatedCost: number;
  notes: string | null;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'ORDERED' | 'PAID' | 'DELIVERED';
  requestedById: number;
  requestedByName: string;
  approvedByName: string | null;
  remarks: string | null;
  decidedAt: string | null;
  deliveredQuantity: number | null;
  deliveredAt: string | null;
  deliveredByName: string | null;
  deliveryNotes: string | null;
  purchaseOrderId: number | null;
  purchaseOrderNumber: string | null;
  purchaseOrderStatus: string | null;
  canReview: boolean;
  canConfirmDelivery: boolean;
  createdAt: string;
}

export interface MaterialRequestPayload {
  phaseId: number;
  materialId: number;
  quantity: string;
  urgency: string;
  requiredBy: string;
  notes: string | null;
}

export interface BudgetCheck {
  phaseId: number;
  phaseName: string;
  phaseBudget: number;
  committed: number;
  remaining: number;
  cost: number;
  overrun: boolean;
}
