export interface SupplierApplication {
  id: number;
  companyName: string;
  registrationNo: string;
  contactPerson: string;
  email: string;
  phone: string;
  address: string;
  materialCategories: string[];
  documentName: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewedByName: string | null;
  reviewedAt: string | null;
  reviewRemarks: string | null;
  supplierId: number | null;
  createdAt: string;
}

export interface SupplierRecord {
  id: number;
  name: string;
  registrationNo: string | null;
  contactPerson: string;
  email: string;
  phone: string;
  address: string;
  materialCategories: string[];
  active: boolean;
  userId: number | null;
  createdAt: string;
}
