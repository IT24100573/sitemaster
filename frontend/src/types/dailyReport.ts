export interface DailyReport {
  id: number;
  projectId: number;
  projectName: string;
  submittedById: number;
  submittedByName: string;
  reportDate: string;
  workDone: string;
  workerCount: number;
  weather: string;
  problems: string | null;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewedByName: string | null;
  reviewedAt: string | null;
  reviewRemarks: string | null;
  materialsUsed: { materialId: number; materialName: string; unit: string; quantity: number }[];
  photos: { id: number; caption: string | null; fileName: string }[];
  canEdit: boolean;
  canReview: boolean;
  createdAt: string;
}

export interface DailyReportPayload {
  reportDate: string;
  workDone: string;
  workerCount: number;
  weather: string;
  problems: string | null;
  materialsUsed: { materialId: number; quantity: string }[];
}

export interface ReportFilters {
  projectId?: string;
  status?: string;
  from?: string;
  to?: string;
  mine?: boolean;
}
