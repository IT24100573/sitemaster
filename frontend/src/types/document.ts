export interface DocumentVersion {
  id: number;
  versionNo: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  notes: string | null;
  uploadedByName: string;
  uploadedAt: string;
}

export interface ProjectDocument {
  id: number;
  title: string;
  category: string;
  description: string | null;
  currentVersion: number;
  versions: DocumentVersion[];
  updatedAt: string;
}
