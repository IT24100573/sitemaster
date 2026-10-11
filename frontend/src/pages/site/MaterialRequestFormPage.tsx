import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft } from 'lucide-react';
import { materialRequestsApi } from '../../api/materialRequestsApi';
import { materialsApi } from '../../api/materialsApi';
import { projectsApi } from '../../api/projectsApi';
import { useToast } from '../../context/ToastContext';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useEnumLookup } from '../../hooks/useLookup';
import { collectErrorMessages } from '../../lib/apiError';
import { emptyToNull } from '../../lib/params';
import { todayIso } from '../../validation/common';
import { materialRequestSchema, type MaterialRequestFormValues } from '../../validation/siteSchemas';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { Select } from '../../components/ui/Select';
import { Textarea } from '../../components/ui/Textarea';

const FIELDS = ['projectId', 'phaseId', 'materialId', 'quantity', 'urgency', 'requiredBy', 'notes'] as const;

/** Site engineer asks for material; the project's PM is notified to approve. */
export default function MaterialRequestFormPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const urgencies = useEnumLookup('urgencies');
  const materials = useQuery({ queryKey: ['material-options'], queryFn: materialsApi.options });

  const { register, handleSubmit, setError, watch, formState: { errors, isSubmitting, submitCount } } =
    useForm<MaterialRequestFormValues>({
      resolver: zodResolver(materialRequestSchema),
      mode: 'onBlur',
      defaultValues: { projectId: '', phaseId: '', materialId: '', quantity: '', urgency: 'NORMAL', requiredBy: '', notes: '' },
    });
  const feedback = useFormFeedback(setError, FIELDS);
  const projectId = watch('projectId');
  const phases = useQuery({
    queryKey: ['projects', Number(projectId), 'phases'],
    queryFn: () => projectsApi.phases(Number(projectId)),
    enabled: projectId !== '',
  });

  const onSubmit = async (v: MaterialRequestFormValues) => {
    feedback.clear();
    try {
      const saved = await materialRequestsApi.create(Number(v.projectId), {
        phaseId: Number(v.phaseId),
        materialId: Number(v.materialId),
        quantity: v.quantity,
        urgency: v.urgency,
        requiredBy: v.requiredBy,
        notes: emptyToNull(v.notes),
      });
      await queryClient.invalidateQueries({ queryKey: ['material-requests'] });
      toast.success('Request sent. Your project manager has been notified.');
      navigate(`../${saved.id}`, { relative: 'path', replace: true });
    } catch (error) {
      feedback.handleError(error);
    }
  };

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];
  const openPhases = (phases.data ?? []).filter((p) => p.status !== 'COMPLETED').map((p) => ({ value: String(p.id), label: p.name }));

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        Back to requests
      </Link>
      <PageHeader title="Request material" description="The cost is estimated from the catalogue price and checked against the phase budget." />
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="max-w-3xl space-y-6">
        <ErrorSummary messages={summary} />
        <Card>
          <div className="grid gap-5 md:grid-cols-2">
            <FormField label="Project" htmlFor="projectId" error={errors.projectId?.message} required>
              <ProjectSelect id="projectId" activeOnly placeholder="Choose a project" invalid={!!errors.projectId} {...register('projectId')} />
            </FormField>
            <FormField label="Phase" htmlFor="phaseId" error={errors.phaseId?.message} required>
              <Select id="phaseId" options={openPhases} placeholder={projectId ? 'Choose a phase' : 'Choose a project first'} invalid={!!errors.phaseId} {...register('phaseId')} />
            </FormField>
            <FormField label="Material" htmlFor="materialId" error={errors.materialId?.message} required>
              <Select id="materialId" options={materials.data ?? []} placeholder="Choose a material" invalid={!!errors.materialId} {...register('materialId')} />
            </FormField>
            <FormField label="Quantity" htmlFor="quantity" error={errors.quantity?.message} required>
              <Input id="quantity" inputMode="decimal" invalid={!!errors.quantity} {...register('quantity')} />
            </FormField>
            <FormField label="Urgency" htmlFor="urgency" error={errors.urgency?.message} required>
              <Select id="urgency" options={urgencies.data ?? []} invalid={!!errors.urgency} {...register('urgency')} />
            </FormField>
            <FormField label="Needed on site by" htmlFor="requiredBy" error={errors.requiredBy?.message} required>
              <Input id="requiredBy" type="date" min={todayIso()} invalid={!!errors.requiredBy} {...register('requiredBy')} />
            </FormField>
            <div className="md:col-span-2">
              <FormField label="Notes" htmlFor="notes" error={errors.notes?.message}>
                <Textarea id="notes" rows={3} maxLength={500} {...register('notes')} />
              </FormField>
            </div>
          </div>
        </Card>
        <div className="flex gap-2">
          <Button type="submit" loading={isSubmitting}>Send request</Button>
          <Button type="button" variant="secondary" onClick={() => navigate('..', { relative: 'path' })}>Cancel</Button>
        </div>
      </form>
    </>
  );
}
