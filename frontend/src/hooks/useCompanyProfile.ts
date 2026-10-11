import { useQuery } from '@tanstack/react-query';
import { companyApi } from '../api/settingsApi';

export const COMPANY_PROFILE_KEY = ['company-profile'] as const;

/** Public company details (name, currency, logo) used across the app. */
export function useCompanyProfile() {
  return useQuery({ queryKey: COMPANY_PROFILE_KEY, queryFn: companyApi.profile, staleTime: 10 * 60 * 1000 });
}

/** Currency code from System Settings, e.g. LKR. */
export function useCurrency(): string {
  return useCompanyProfile().data?.currency ?? '';
}
