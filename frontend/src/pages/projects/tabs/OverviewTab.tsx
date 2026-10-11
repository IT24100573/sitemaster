import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Building2 } from 'lucide-react';
import { projectsApi } from '../../../api/projectsApi';
import { useToast } from '../../../context/ToastContext';
import { useCurrency } from '../../../hooks/useCompanyProfile';
import { toApiError } from '../../../lib/apiError';
import { formatDate, formatMoney } from '../../../lib/format';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { DetailList } from '../../../components/ui/DetailList';
import { FileUpload } from '../../../components/ui/FileUpload';
import { ProgressBar } from '../../../components/ui/ProgressBar';
import { projectKeys } from '../queryKeys';
import type { ProjectPermissions } from '../useProjectPermissions';
import type { ProjectDetail } from '../../../types/project';

export function OverviewTab({ project, perms }: { project: ProjectDetail; perms: ProjectPermissions }) {
  const currency = useCurrency();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [cover, setCover] = useState<File | null>(null);
  const [coverVersion, setCoverVersion] = useState(0);

  const onSaved = (updated: ProjectDetail) => {
    queryClient.setQueryData(projectKeys.detail(project.id), updated);
    setCoverVersion((v) => v + 1);
  };

  const upload = useMutation({
    mutationFn: (file: File) => projectsApi.uploadCover(project.id, file),
    onSuccess: (updated) => {
      onSaved(updated);
      setCover(null);
      toast.success('Cover photo updated.');
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const remove = useMutation({
    mutationFn: () => projectsApi.removeCover(project.id),
    onSuccess: (updated) => {
      onSaved(updated);
      toast.success('Cover photo removed.');
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <div className="space-y-6 lg:col-span-2">
        <Card title="Progress">
          <ProgressBar value={project.progress} label="Project progress" />
          <p className="mt-2 text-sm text-muted">Average completion of the project's tasks, updated each time the PM approves a daily report.</p>
        </Card>
        <Card title="Details">
          <DetailList
            items={[
              { label: 'Client', value: project.clientName },
              { label: 'Project manager', value: project.managerName },
              { label: 'Location', value: project.location },
              { label: 'Coordinates', value: `${project.latitude}, ${project.longitude}` },
              { label: 'Start', value: formatDate(project.startDate) },
              { label: 'End', value: formatDate(project.endDate) },
              ...(project.completedDate ? [{ label: 'Completed', value: formatDate(project.completedDate) }] : []),
              ...(perms.canSeeBudget ? [{ label: 'Budget', value: formatMoney(project.budget, currency) }] : []),
            ]}
          />
          {project.description && (
            <p className="mt-6 whitespace-pre-line border-t border-line pt-4 text-sm leading-6 text-ink">{project.description}</p>
          )}
        </Card>
      </div>

      <Card title="Cover photo" description="Shown in the public gallery once the project is completed.">
        <div className="aspect-[4/3] overflow-hidden rounded-lg border border-line bg-surface">
          {project.hasCover ? (
            <img src={projectsApi.coverUrl(project.id, coverVersion)} alt={`${project.name} cover`} className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full items-center justify-center text-muted">
              <Building2 className="h-10 w-10" aria-hidden />
            </div>
          )}
        </div>
        {perms.canManage && (
          <div className="mt-4 space-y-3">
            <FileUpload kind="image" file={cover} onChange={setCover} label="Choose a photo" error={upload.error ? toApiError(upload.error).fieldErrors.file : undefined} />
            <div className="flex gap-2">
              <Button size="sm" disabled={!cover} loading={upload.isPending} onClick={() => cover && upload.mutate(cover)}>
                Upload photo
              </Button>
              {project.hasCover && (
                <Button size="sm" variant="secondary" loading={remove.isPending} onClick={() => remove.mutate()}>
                  Remove
                </Button>
              )}
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
