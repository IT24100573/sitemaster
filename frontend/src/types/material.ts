export interface Material {
  id: number;
  name: string;
  category: string;
  unit: string;
  unitPrice: number;
  description: string | null;
  active: boolean;
}

export interface MaterialPayload {
  name: string;
  category: string;
  unit: string;
  unitPrice: string;
  description: string | null;
}

export interface StockLine {
  materialId: number;
  materialName: string;
  category: string;
  unit: string;
  quantity: number;
  updatedAt: string;
}
