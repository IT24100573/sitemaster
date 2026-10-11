import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useFieldArray, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Plus, Trash2 } from 'lucide-react';
import { dailyReportsApi } from '../../api/dailyReportsApi';
import { materialsApi } from '../../api/materialsApi';
import { useToast } from '../../context/ToastContext';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useEnumLookup } from '../../hooks/useLookup';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { emptyToNull } from '../../lib/params';
import { todayIso } from '../../validation/common';
import { dailyReportSchema, REPORT_MIN_DATE, type DailyReportFormValues } from '../../validation/siteSchemas';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FileUpload } from '../../components/ui/FileUpload';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { Select } from '../../components/ui/Select';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { Textarea } from '../../components/ui/Textarea';
import type { DailyReport } from '../../types/dailyReport';

const FIELDS = ['projectId', 'reportDate', 'workDone', 'workerCount', 'weather', 'problems', 'materialsUsed'] as const;
const MAX_PHOTOS = 5;

/** Site engineer writes (or corrects) a daily report, then attaches up to 5 photos. */
export default function DailyReportFormPage() {
  const { id } = useParams();
  const reportId = id ? Number(id) : null;
  const isEdit = reportId !== null;
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const weather = useEnumLookup('weather-conditions');
  const materials = useQuery({ queryKey: ['material-options'], queryFn: materialsApi.options });
  const [photos, setPhotos] = useState<{ file: File; caption: string }[]>([]);
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [photoCaption, setPhotoCaption] = useState('');

  const existing = useQuery({
    queryKey: ['daily-reports', 'detail', reportId],
    queryFn: () => dailyReportsApi.get(reportId as number),
    enabled: isEdit,
  });

  const { register, control, handleSubmit, setError, reset, formState: { errors, isSubmitting, submitCount } } =
    useForm<DailyReportFormValues>({
      resolver: zodResolver(dailyReportSchema),
      mode: 'onBlur',
      defaultValues: {
        projectId: '', reportDate: todayIso(), workDone: '', workerCount: '0', weather: 'SUNNY', problems: '', materialsUsed: [],
      },
    });
  const lines = useFieldArray({ control, name: 'materialsUsed' });
  const feedback = useFormFeedback(setError, FIELDS);

  useEffect(() => {
    if (existing.data) {
      const r = existing.data;
      reset({
        projectId: String(r.projectId),
        reportDate: r.reportDate,
        workDone: r.workDone,
        workerCount: String(r.workerCount),
        weather: r.weather,
        problems: r.problems ?? '',
        materialsUsed: r.materialsUsed.map((m) => ({ materialId: String(m.materialId), quantity: String(m.quantity) })),
      });
    }
  }, [existing.data, reset]);

  const existingPhotos = existing.data?.photos.length ?? 0;
  const room = MAX_PHOTOS - existingPhotos - photos.length;

  const onSubmit = async (v: DailyReportFormValues) => {
    feedback.clear();
    const payload = {
      reportDate: v.reportDate,
      workDone: v.workDone,
      workerCount: Number(v.workerCount),
      weather: v.weather,
      problems: emptyToNull(v.problems),
      materialsUsed: v.materialsUsed.map((m) => ({ materialId: Number(m.materialId), quantity: m.quantity })),
    };
    let saved: DailyReport;
    try {
      saved = isEdit
        ? await dailyReportsApi.update(reportId as number, payload)
        : await dailyReportsApi.submit(Number(v.projectId), payload);
    } catch (error) {
      feedback.handleError(error);
      return;
    }
    // Photos are uploaded one by one after the report exists.
    // A photo that fails is reported; it can be added again from the report page.
    for (const photo of photos) {
      try {
        await dailyReportsApi.addPhoto(saved.id, photo.file, photo.caption.trim());
      } catch (error) {
        const apiError = toApiError(error);
        toast.error(`${photo.file.name}: ${apiError.fieldErrors.file ?? apiError.message}`);
      }
    }
    await queryClient.invalidateQueries({ queryKey: ['daily-reports'] });
    toast.success(isEdit ? 'Report updated and sent for review.' : 'Report submitted. Your project manager has been notified.');
    navigate(isEdit ? '..' : `../${saved.id}`, { relative: 'path', replace: true });
  };

  if (isEdit && existing.isLoading) {
    return <FullPageSpinner />;
  }
  if (isEdit && existing.isError) {
    return <Alert tone="error">{toApiError(existing.error).message}</Alert>;
  }

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        {isEdit ? 'Back to report' : 'Back to reports'}
      </Link>
      <PageHeader
        title={isEdit ? 'Correct daily report' : 'Submit daily report'}
        description={isEdit && existing.data?.reviewRemarks ? `Project manager's remarks: ${existing.data.reviewRemarks}` : 'One report per project per day, for today or the last 7 days.'}
      />
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="max-w-4xl space-y-6">
        <ErrorSummary messages={summary} />
        <Card title="The day">
          <div className="grid gap-5 md:grid-cols-2">
            <FormField label="Project" htmlFor="projectId" error={errors.projectId?.message} required>
              {isEdit ? (
                <>
                  <input type="hidden" {...register('projectId')} />
                  <Input id="projectId" value={existing.data?.projectName ?? ''} disabled readOnly />
                </>
              ) : (
                <ProjectSelect id="projectId" activeOnly placeholder="Choose a project" invalid={!!errors.projectId} {...register('projectId')} />
              )}
            </FormField>
            <FormField label="Date" htmlFor="reportDate" error={errors.reportDate?.message} required>
              <Input id="reportDate" type="date" min={REPORT_MIN_DATE()} max={todayIso()} readOnly={isEdit} invalid={!!errors.reportDate} {...register('reportDate')} />
            </FormField>
            <FormField label="Workers on site" htmlFor="workerCount" error={errors.workerCount?.message} required>
              <Input id="workerCount" inputMode="numeric" invalid={!!errors.workerCount} {...register('workerCount')} />
            </FormField>
            <FormField label="Weather" htmlFor="weather" error={errors.weather?.message} required>
              <Select id="weather" options={weather.data ?? []} invalid={!!errors.weather} {...register('weather')} />
            </FormField>
            <div className="md:col-span-2">
              <FormField label="Work done" htmlFor="workDone" error={errors.workDone?.message} required>
                <Textarea id="workDone" rows={5} maxLength={2000} invalid={!!errors.workDone} {...register('workDone')} />
              </FormField>
            </div>
            <div className="md:col-span-2">
              <FormField label="Problems" htmlFor="problems" error={errors.problems?.message} hint="Delays, shortages, safety concerns">
                <Textarea id="problems" rows={3} maxLength={2000} {...register('problems')} />
              </FormField>
            </div>
          </div>
        </Card>

        <Card title="Materials used" description="Taken off site stock when the report is approved.">
          <div className="space-y-3">
            {lines.fields.map((line, index) => (
              <div key={line.id} className="grid gap-3 sm:grid-cols-[2fr_1fr_auto] sm:items-start">
                <FormField label="Material" htmlFor={`mat-${index}`} error={errors.materialsUsed?.[index]?.materialId?.message}>
                  <Select id={`mat-${index}`} options={materials.data ?? []} placeholder="Choose a material" {...register(`materialsUsed.${index}.materialId`)} />
                </FormField>
                <FormField label="Quantity" htmlFor={`qty-${index}`} error={errors.materialsUsed?.[index]?.quantity?.message}>
                  <Input id={`qty-${index}`} inputMode="decimal" {...register(`materialsUsed.${index}.quantity`)} />
                </FormField>
                <button type="button" onClick={() => lines.remove(index)} className="mt-7 rounded-md p-2 text-red-600 hover:bg-surface" aria-label="Remove material line">
                  <Trash2 className="h-4 w-4" aria-hidden />
                </button>
              </div>
            ))}
            <Button type="button" variant="secondary" size="sm" onClick={() => lines.append({ materialId: '', quantity: '' })}>
              <Plus className="h-4 w-4" aria-hidden />
              Add material
            </Button>
          </div>
        </Card>

        <Card title="Photos" description={`Up to ${MAX_PHOTOS} site photos (jpg, png or webp, 2 MB each).`}>
          {existingPhotos > 0 && <p className="mb-3 text-sm text-muted">{existingPhotos} photo(s) already attached. Manage them from the report page.</p>}
          {photos.length > 0 && (
            <ul className="mb-3 space-y-2">
              {photos.map((p, i) => (
                <li key={`${p.file.name}-${i}`} className="flex items-center justify-between rounded-lg bg-surface px-3 py-2 text-sm">
                  <span className="truncate">{p.file.name}{p.caption ? `: ${p.caption}` : ''}</span>
                  <button type="button" onClick={() => setPhotos(photos.filter((_, j) => j !== i))} className="text-red-600" aria-label={`Remove ${p.file.name}`}>
                    <Trash2 className="h-4 w-4" aria-hidden />
                  </button>
                </li>
              ))}
            </ul>
          )}
          {room > 0 ? (
            <div className="grid gap-3 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
              <FileUpload kind="image" file={photoFile} onChange={setPhotoFile} label="Choose a photo" />
              <Input aria-label="Caption" placeholder="Caption (optional)" maxLength={150} value={photoCaption} onChange={(e) => setPhotoCaption(e.target.value)} />
              <Button
                type="button"
                variant="secondary"
                disabled={!photoFile}
                onClick={() => {
                  if (photoFile) {
                    setPhotos([...photos, { file: photoFile, caption: photoCaption }]);
                    setPhotoFile(null);
                    setPhotoCaption('');
                  }
                }}
              >
                Add photo
              </Button>
            </div>
          ) : (
            <p className="text-sm text-muted">The photo limit has been reached.</p>
          )}
        </Card>

        <div className="flex gap-2">
          <Button type="submit" loading={isSubmitting}>
            {isEdit ? 'Resubmit report' : 'Submit report'}
          </Button>
          <Button type="button" variant="secondary" onClick={() => navigate('..', { relative: 'path' })}>
            Cancel
          </Button>
        </div>
      </form>
    </>
  );
}
