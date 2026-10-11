export interface PublicProjectCard {
  id: number;
  name: string;
  clientName: string;
  location: string;
  completedDate: string | null;
  hasCover: boolean;
}

export interface ProjectMapMarker {
  id: number;
  name: string;
  status: 'ACTIVE' | 'DELAYED' | 'COMPLETED';
  progress: number;
  location: string;
  latitude: number;
  longitude: number;
}
