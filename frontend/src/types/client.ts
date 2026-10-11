export interface ClientRecord {
  id: number;
  name: string;
  contactPerson: string | null;
  email: string;
  phone: string;
  address: string | null;
  active: boolean;
  projectCount: number;
  createdAt: string;
}

export interface ClientPayload {
  name: string;
  contactPerson: string | null;
  email: string;
  phone: string;
  address: string | null;
}
