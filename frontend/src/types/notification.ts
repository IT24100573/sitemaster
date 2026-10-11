export interface NotificationItem {
  id: number;
  eventType: string;
  title: string;
  message: string;
  /** Role-relative path such as "profile". */
  link: string | null;
  read: boolean;
  createdAt: string;
}
