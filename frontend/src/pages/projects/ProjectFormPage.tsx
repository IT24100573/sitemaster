import { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft } from 'lucide-react';
import { projectsApi } from '../../api/projectsApi';
import { useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useLookup } from '../../hooks/useLookup';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { emptyToNull } from '../../lib/params';
import { projectSchema, type ProjectFormValues } from '../../validation/projectSchemas';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { LocationPicker } from '../../components/ui/LocationPicker';
import { PageHeader } from '../../components/ui/PageHeader';
import { Select } from '../../components/ui/Select';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { Textarea } from '../../components/ui/Textarea';
import type { ProjectPayload } from '../../types/project';

const FIELDS = [
  'name', 'description', 'clientId', 'managerId', 'location', 'latitude', 'longitude', 'startDate', 'endDate', 'budget',
] as const;

const EMPTY: ProjectFormValues = {
  name: '', description: '', clientId: '', managerId: '', location: '', latitude: '', longitude: '',
  startDate: '', endDate: '', budget: '',
};

/** Admin creates projects; Admin or the project's PM edits them. */
export default function ProjectFormPage() {
  const { id } = useParams();
  const projectId = id ? Number(id) : null;
  const isEdit = projectId !== null;
  const user = useCurrentUser();
  const isAdmin = user.role === 'ADMIN';
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const currency = useCurrency();
  const clients = useLookup('clients');
  const managers = useLookup('projectManagers');

  const existing = useQuery({
    queryKey: ['projects', 'detail', projectId],
    queryFn: () => projectsApi.get(projectId as number),
    enabled: isEdit,
  });

  const {
    register,
    handleSubmit,
    setError,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting, submitCount },
  } = useForm<ProjectFormValues>({ resolver: zodResolver(projectSchema), mode: 'onBlur', defaultValues: EMPTY });
  const feedback = useFormFeedback(setError, FIELDS);

  useEffect(() => {
    if (existing.data) {
      const p = existing.data;
      reset({
        name: p.name,
        description: p.description ?? '',
        clientId: String(p.clientId),
        managerId: String(p.managerId),
        location: p.location,
        latitude: String(p.latitude),
        longitude: String(p.longitude),
        startDate: p.startDate,
        endDate: p.endDate,
        budget: Number(p.budget).toFixed(2),
      });
    }
  }, [existing.data, reset]);

  const latitude = watch('latitude');
  const longitude = watch('longitude');

  const onSubmit = async (v: ProjectFormValues) => {
    feedback.clear();
    const payload: ProjectPayload = {
      name: v.name,
      description: emptyToNull(v.description),
      clientId: Number(v.clientId),
      managerId: Number(v.managerId),
      location: v.location,
      latitude: v.latitude,
      longitude: v.longitude,
      startDate: v.startDate,
      endDate: v.endDate,
      budget: v.budget,
    };
    try {
      const saved = isEdit ? await projectsApi.update(projectId as number, payload) : await projectsApi.create(payload);
      await queryClient.invalidateQueries({ queryKey: ['projects'] });
      toast.success(isEdit ? `${saved.name} was updated.` : `${saved.name} was created and its manager notified.`);
      navigate(isEdit ? '..' : `../${saved.id}`, { relative: 'path', replace: true });
    } catch (error) {
      feedback.handleError(error);
    }
  };

  if (isEdit && existing.isLoading) {
    return <FullPageSpinner />;
  }
  if (isEdit && existing.isError) {
    return <Alert tone="error">{toApiError(existing.error).message}</Alert>;
  }

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];
  // A PM editing their project cannot change its client or manager; the values travel in hidden fields.
  const adminFieldsLocked = isEdit && !isAdmin;

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        {isEdit ? 'Back to project' : 'Back to projects'}
      </Link>
      <PageHeader
        title={isEdit ? `Edit ${existing.data?.name ?? 'project'}` : 'New project'}
        description={isEdit ? undefined : 'The project manager you choose is notified and added to the team.'}
      />

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="max-w-4xl space-y-6">
        <ErrorSummary messages={summary} />
        <Card title="Project">
          <div className="grid gap-5 md:grid-cols-2">
            <div className="md:col-span-2">
              <FormField label="Name" htmlFor="name" error={errors.name?.message} required>
                <Input id="name" invalid={!!errors.name} {...register('name')} />
              </FormField>
            </div>
            <FormField label="Client" htmlFor="clientId" error={errors.clientId?.message} required>
              {adminFieldsLocked ? (
                <>
                  <input type="hidden" {...register('clientId')} />
                  <Input id="clientId" value={existing.data?.clientName ?? ''} disabled readOnly />
                </>
              ) : (
                <Select id="clientId" options={clients.data ?? []} placeholder="Choose a client" invalid={!!errors.clientId} {...register('clientId')} />
              )}
            </FormField>
            <FormField label="Project manager" htmlFor="managerId" error={errors.managerId?.message} required>
              {adminFieldsLocked ? (
                <>
                  <input type="hidden" {...register('managerId')} />
                  <Input id="managerId" value={existing.data?.managerName ?? ''} disabled readOnly />
                </>
              ) : (
                <Select id="managerId" options={managers.data ?? []} placeholder="Choose a project manager" invalid={!!errors.managerId} {...register('managerId')} />
              )}
            </FormField>
            <FormField label="Start date" htmlFor="startDate" error={errors.startDate?.message} required>
              <Input id="startDate" type="date" invalid={!!errors.startDate} {...register('startDate')} />
            </FormField>
            <FormField label="End date" htmlFor="endDate" error={errors.endDate?.message} required>
              <Input id="endDate" type="date" invalid={!!errors.endDate} {...register('endDate')} />
            </FormField>
            <FormField label={`Budget (${currency})`} htmlFor="budget" error={errors.budget?.message} required>
              <Input id="budget" inputMode="decimal" invalid={!!errors.budget} {...register('budget')} />
            </FormField>
            <div className="md:col-span-2">
              <FormField label="Description" htmlFor="description" error={errors.description?.message}>
                <Textarea id="description" maxLength={2000} invalid={!!errors.description} {...register('description')} />
              </FormField>
            </div>
          </div>
        </Card>

        <Card title="Site location" description="Click the map to drop the pin, or type the coordinates.">
          <div className="space-y-5">
            <FormField label="Location" htmlFor="location" error={errors.location?.message} hint="Town or address shown to the public" required>
              <Input id="location" invalid={!!errors.location} {...register('location')} />
            </FormField>
            <LocationPicker
              latitude={latitude === '' ? null : Number(latitude)}
              longitude={longitude === '' ? null : Number(longitude)}
              onPick={(lat, lng) => {
                setValue('latitude', String(lat), { shouldValidate: true, shouldDirty: true });
                setValue('longitude', String(lng), { shouldValidate: true, shouldDirty: true });
              }}
            />
            <div className="grid gap-5 md:grid-cols-2">
              <FormField label="Latitude" htmlFor="latitude" error={errors.latitude?.message} required>
                <Input id="latitude" inputMode="decimal" invalid={!!errors.latitude} {...register('latitude')} />
              </FormField>
              <FormField label="Longitude" htmlFor="longitude" error={errors.longitude?.message} required>
                <Input id="longitude" inputMode="decimal" invalid={!!errors.longitude} {...register('longitude')} />
              </FormField>
            </div>
          </div>
        </Card>

        <div className="flex gap-2">
          <Button type="submit" loading={isSubmitting}>
            {isEdit ? 'Save changes' : 'Create project'}
          </Button>
          <Button type="button" variant="secondary" onClick={() => navigate('..', { relative: 'path' })}>
            Cancel
          </Button>
        </div>
      </form>
    </>
  );
}
