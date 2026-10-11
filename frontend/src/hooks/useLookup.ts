import { useQuery } from '@tanstack/react-query';
import { lookupsApi } from '../api/lookupsApi';

type LookupName =
  | 'roles'
  | 'employmentTypes'
  | 'trades'
  | 'modules'
  | 'employeeDocumentTypes'
  | 'unlinkedStaff'
  | 'clients'
  | 'projectManagers'
  | 'materialCategories'
  | 'materialUnits';

const ALWAYS_FRESH: LookupName[] = ['unlinkedStaff', 'clients', 'projectManagers'];

/** Cached dropdown data from /api/lookups. */
export function useLookup(name: LookupName, enabled = true) {
  return useQuery({
    queryKey: ['lookup', name],
    queryFn: lookupsApi[name],
    staleTime: ALWAYS_FRESH.includes(name) ? 0 : 5 * 60 * 1000,
    enabled,
  });
}

export type EnumLookupName =
  | 'project-statuses'
  | 'phase-statuses'
  | 'task-statuses'
  | 'urgencies'
  | 'issue-types'
  | 'issue-severities'
  | 'issue-statuses'
  | 'document-categories'
  | 'application-statuses'
  | 'request-statuses'
  | 'equipment-statuses'
  | 'weather-conditions'
  | 'purchase-order-statuses'
  | 'payment-methods'
  | 'invoice-statuses'
  | 'attendance-statuses'
  | 'payroll-statuses'
  | 'expense-categories';

/** Values of a server-side enumeration, e.g. useEnumLookup('issue-severities'). */
export function useEnumLookup(name: EnumLookupName) {
  return useQuery({
    queryKey: ['lookup', 'enum', name],
    queryFn: () => lookupsApi.enumValues(name),
    staleTime: Infinity,
  });
}
