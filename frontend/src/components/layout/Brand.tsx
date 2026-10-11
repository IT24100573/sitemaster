import { HardHat } from 'lucide-react';
import { companyApi } from '../../api/settingsApi';
import { useCompanyProfile } from '../../hooks/useCompanyProfile';

/**
 * Product mark plus the company name from System Settings. When the Admin has
 * uploaded a logo it replaces the hard-hat icon.
 */
export function Brand({ inverted = false, showCompany = false }: { inverted?: boolean; showCompany?: boolean }) {
  const { data: company, dataUpdatedAt } = useCompanyProfile();

  return (
    <div className="flex min-w-0 items-center gap-2.5">
      {company?.hasLogo ? (
        <img
          src={companyApi.logoUrl(dataUpdatedAt)}
          alt={company.name}
          className="h-9 w-9 shrink-0 rounded-lg bg-white object-contain"
        />
      ) : (
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary text-white">
          <HardHat className="h-5 w-5" aria-hidden />
        </span>
      )}
      <span className="min-w-0">
        <span className={`block text-lg font-bold leading-tight tracking-tight ${inverted ? 'text-white' : 'text-ink'}`}>
          SiteMaster
        </span>
        {showCompany && company?.name && (
          <span className={`block truncate text-xs ${inverted ? 'text-white/70' : 'text-muted'}`}>{company.name}</span>
        )}
      </span>
    </div>
  );
}
