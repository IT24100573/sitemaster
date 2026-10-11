export interface Equipment {
  id: number;
  code: string;
  name: string;
  category: string;
  projectId: number | null;
  projectName: string | null;
  status: 'AVAILABLE' | 'IN_USE' | 'UNDER_MAINTENANCE' | 'RETIRED';
  notes: string | null;
  lastServiceDate: string | null;
  nextServiceDate: string | null;
  serviceDue: boolean;
}

export interface EquipmentPayload {
  code: string;
  name: string;
  category: string;
}

export interface EquipmentConditionPayload {
  status: string;
  notes: string | null;
  lastServiceDate: string | null;
  nextServiceDate: string | null;
}
