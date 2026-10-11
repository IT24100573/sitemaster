export interface SystemSettings {
  companyName: string;
  companyDescription: string;
  companyAddress: string;
  companyPhone: string;
  companyEmail: string;
  hasLogo: boolean;
  currency: string;
  receiptThreshold: number;
  workStartTime: string;
  workEndTime: string;
  holidays: string[];
  leaveAnnualDays: number;
  leaveCasualDays: number;
  leaveSickDays: number;
  epfEmployeeRate: number;
  epfEmployerRate: number;
  etfRate: number;
  overtimeMultiplier: number;
  siteAllowancePerDay: number;
  trades: string[];
  materialCategories: string[];
  materialUnits: string[];
}

export type SystemSettingsPayload = Omit<SystemSettings, 'hasLogo' | 'receiptThreshold' | 'epfEmployeeRate' | 'epfEmployerRate' | 'etfRate' | 'overtimeMultiplier' | 'siteAllowancePerDay'> & {
  receiptThreshold: string;
  epfEmployeeRate: string;
  epfEmployerRate: string;
  etfRate: string;
  overtimeMultiplier: string;
  siteAllowancePerDay: string;
};

export interface CompanyProfile {
  name: string;
  description: string;
  address: string;
  phone: string;
  email: string;
  currency: string;
  hasLogo: boolean;
}
