import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Building2, MapPin } from 'lucide-react';
import { publicApi } from '../../api/publicApi';
import { useCompanyProfile } from '../../hooks/useCompanyProfile';
import { formatDate } from '../../lib/format';
import { Spinner } from '../../components/ui/Spinner';

/** Company introduction and a gallery of completed projects. */
export default function HomePage() {
  const { data: company } = useCompanyProfile();
  const projects = useQuery({ queryKey: ['public', 'completed'], queryFn: publicApi.completedProjects });

  return (
    <>
      <section className="bg-ink text-white">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 sm:py-24">
          <h1 className="max-w-3xl text-4xl font-bold leading-tight tracking-tight sm:text-5xl">
            {company?.name ?? 'Building across Sri Lanka'}
          </h1>
          <p className="mt-5 max-w-2xl text-lg leading-8 text-white/80">{company?.description}</p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/sites" className="inline-flex h-11 items-center gap-2 rounded-lg bg-primary px-5 text-sm font-medium text-white hover:bg-primary-dark">
              <MapPin className="h-4 w-4" aria-hidden />
              See our sites
            </Link>
            <Link to="/become-a-supplier" className="inline-flex h-11 items-center rounded-lg border border-white/30 px-5 text-sm font-medium text-white hover:bg-white/10">
              Become a supplier
            </Link>
          </div>
        </div>
        <div className="hazard-band h-2" aria-hidden />
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
        <h2 className="text-2xl font-semibold tracking-tight text-ink">Completed projects</h2>
        <p className="mt-1 text-muted">Work we have handed over to our clients.</p>

        {projects.isLoading ? (
          <div className="flex justify-center py-16">
            <Spinner className="h-6 w-6" />
          </div>
        ) : projects.data && projects.data.length > 0 ? (
          <ul className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {projects.data.map((project) => (
              <li key={project.id} className="card overflow-hidden">
                <div className="aspect-[4/3] bg-surface">
                  {project.hasCover ? (
                    <img src={publicApi.coverUrl(project.id)} alt={project.name} loading="lazy" className="h-full w-full object-cover" />
                  ) : (
                    <div className="flex h-full items-center justify-center text-muted">
                      <Building2 className="h-10 w-10" aria-hidden />
                    </div>
                  )}
                </div>
                <div className="p-5">
                  <h3 className="font-semibold text-ink">{project.name}</h3>
                  <p className="mt-1 text-sm text-muted">For {project.clientName}</p>
                  <p className="mt-3 flex items-center gap-1.5 text-sm text-muted">
                    <MapPin className="h-4 w-4 text-primary" aria-hidden />
                    {project.location}
                  </p>
                  <p className="mt-1 text-sm text-muted">Completed {formatDate(project.completedDate)}</p>
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-8 rounded-card bg-surface p-8 text-center text-muted">Completed projects will appear here.</p>
        )}
      </section>
    </>
  );
}
