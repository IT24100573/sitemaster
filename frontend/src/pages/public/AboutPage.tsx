import { Mail, MapPin, Phone } from 'lucide-react';
import { companyApi } from '../../api/settingsApi';
import { useCompanyProfile } from '../../hooks/useCompanyProfile';
import { Spinner } from '../../components/ui/Spinner';

/** Company details maintained by the Admin in System Settings. */
export default function AboutPage() {
  const { data: company, isLoading, dataUpdatedAt } = useCompanyProfile();

  if (isLoading || !company) {
    return (
      <div className="flex justify-center py-24">
        <Spinner className="h-6 w-6" />
      </div>
    );
  }

  return (
    <section className="mx-auto max-w-4xl px-4 py-16 sm:px-6">
      <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
        {company.hasLogo && (
          <img src={companyApi.logoUrl(dataUpdatedAt)} alt={`${company.name} logo`} className="h-24 w-24 rounded-card border border-line object-contain" />
        )}
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-ink">{company.name}</h1>
          <p className="mt-1 text-muted">About us</p>
        </div>
      </div>
      <p className="mt-8 max-w-prose whitespace-pre-line text-lg leading-8 text-ink">{company.description}</p>

      <dl className="mt-10 grid gap-4 sm:grid-cols-3">
        {[
          { icon: MapPin, label: 'Address', value: company.address },
          { icon: Phone, label: 'Phone', value: company.phone },
          { icon: Mail, label: 'Email', value: company.email },
        ].map(({ icon: Icon, label, value }) => (
          <div key={label} className="card p-5">
            <dt className="flex items-center gap-2 text-sm text-muted">
              <Icon className="h-4 w-4 text-primary" aria-hidden />
              {label}
            </dt>
            <dd className="mt-1 break-words font-medium text-ink">{value}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
