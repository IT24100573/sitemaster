import { forwardRef, type SelectHTMLAttributes } from 'react';
import { useQuery } from '@tanstack/react-query';
import { projectsApi } from '../../api/projectsApi';
import { Select } from './Select';

const MAX_PROJECTS = 100;

interface ProjectSelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'children'> {
  placeholder?: string;
  invalid?: boolean;
  /** Only projects that can still be worked on (not completed or archived). */
  activeOnly?: boolean;
}

/** Projects the signed-in user can see, for filters and forms. */
export const ProjectSelect = forwardRef<HTMLSelectElement, ProjectSelectProps>(function ProjectSelect(
  { placeholder = 'All projects', invalid, activeOnly = false, ...rest },
  ref,
) {
  const projects = useQuery({
    queryKey: ['projects', 'options'],
    queryFn: () => projectsApi.list({ page: 0, size: MAX_PROJECTS, sort: 'name', direction: 'asc' }),
    staleTime: 60_000,
  });
  const options = (projects.data?.content ?? [])
    .filter((p) => !activeOnly || ['PLANNED', 'ACTIVE', 'DELAYED'].includes(p.status))
    .map((p) => ({ value: String(p.id), label: p.name }));
  return <Select ref={ref} options={options} placeholder={placeholder} invalid={invalid} {...rest} />;
});
