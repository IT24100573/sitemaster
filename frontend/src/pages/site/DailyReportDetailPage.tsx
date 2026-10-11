import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Pencil, Trash2 } from 'lucide-react';
import { dailyReportsApi } from '../../api/dailyReportsApi';
import { useToast } from '../../context/ToastContext';
import { toApiError } from '../../lib/apiError';
import { formatDate, formatDateTime, humanize } from '../../lib/format';
import { CLIENT_MESSAGES } from '../../validation/messages';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { DetailList } from '../../components/ui/DetailList';
import { FileUpload } from '../../components/ui/FileUpload';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Textarea } from '../../components/ui/Textarea';

/** One daily report: details, photos, and PM approve/reject. */
export default function DailyReportDetailPage() {
  const { id } = useParams();
  const reportId = Number(id);
  const toast = useToast();
  const queryClient = useQueryClient();
  const key = ['daily-reports', 'detail', reportId];
  const [decision, setDecision] = useState<'approve' | 'reject' | null>(null);
  const [remarks, setRemarks] = useState('');
  const [error, setError] = useState<string | undefined>();
  const [photo, setPhoto] = useState<File | null>(null);
  const [caption, setCaption] = useState('');

  const report = useQuery({ queryKey: key, queryFn: () => dailyReportsApi.get(reportId) });

  const update = async (promise: Promise<unknown>, message: string) => {
    await promise;
    await queryClient.invalidateQueries({ queryKey: ['daily-reports'] });
    toast.success(message);
  };

  const review = useMutation({
    mutationFn: () =>
      decision === 'approve'
        ? dailyReportsApi.approve(reportId, remarks.trim() || null)
        : dailyReportsApi.reject(reportId, remarks.trim()),
    onSuccess: async (r) => {
      queryClient.setQueryData(key, r);
      await queryClient.invalidateQueries({ queryKey: ['daily-reports'] });
      await queryClient.invalidateQueries({ queryKey: ['projects'] });
      setDecision(null);
      toast.success(r.status === 'APPROVED' ? 'Report approved. Project progress has been updated.' : 'Report sent back to the engineer.');
    },
    onError: (err) => {
      const apiError = toApiError(err);
      setError(apiError.fieldErrors.remarks ?? apiError.message);
    },
  });

  const addPhoto = useMutation({
    mutationFn: () => dailyReportsApi.addPhoto(reportId, photo as File, caption.trim()),
    onSuccess: async (r) => {
      queryClient.setQueryData(key, r);
      setPhoto(null);
      setCaption('');
      toast.success('Photo added.');
    },
    onError: (err) => toast.error(toApiError(err).fieldErrors.file ?? toApiError(err).message),
  });

  if (report.isLoading) {
    return <FullPageSpinner />;
  }
  if (report.isError || !report.data) {
    return <Alert tone="error">{toApiError(report.error).message}</Alert>;
  }
  const r = report.data;

  const submitDecision = () => {
    setError(undefined);
    if (decision === 'reject' && remarks.trim().length < 5) {
      setError(CLIENT_MESSAGES.remarksRequired);
      return;
    }
    review.mutate();
  };

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        Back to reports
      </Link>
      <PageHeader
        title={`${r.projectName}, ${formatDate(r.reportDate)}`}
        description={<span className="flex flex-wrap items-center gap-2"><StatusBadge status={r.status} /> by {r.submittedByName}</span>}
        actions={
          <>
            {r.canEdit && (
              <Link to="edit" className="inline-flex h-11 items-center gap-2 rounded-lg border border-line bg-white px-4 text-sm font-medium text-ink hover:bg-surface">
                <Pencil className="h-4 w-4" aria-hidden />
                {r.status === 'REJECTED' ? 'Correct and resubmit' : 'Edit'}
              </Link>
            )}
            {r.canReview && (
              <>
                <Button variant="secondary" onClick={() => { setDecision('reject'); setRemarks(''); setError(undefined); }}>Reject</Button>
                <Button onClick={() => { setDecision('approve'); setRemarks(''); setError(undefined); }}>Approve</Button>
              </>
            )}
          </>
        }
      />

      {r.status === 'REJECTED' && r.reviewRemarks && (
        <div className="mb-6"><Alert tone="error">Rejected by {r.reviewedByName}: {r.reviewRemarks}</Alert></div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Work done" className="lg:col-span-2">
          <p className="whitespace-pre-line text-sm leading-6 text-ink">{r.workDone}</p>
          {r.problems && (
            <>
              <h3 className="mt-6 text-sm font-semibold text-ink">Problems</h3>
              <p className="mt-1 whitespace-pre-line text-sm leading-6 text-ink">{r.problems}</p>
            </>
          )}
        </Card>
        <Card title="Summary">
          <DetailList
            items={[
              { label: 'Workers on site', value: r.workerCount },
              { label: 'Weather', value: humanize(r.weather) },
              { label: 'Submitted', value: formatDateTime(r.createdAt) },
              ...(r.reviewedByName ? [{ label: 'Reviewed', value: `${r.reviewedByName}, ${formatDateTime(r.reviewedAt)}` }] : []),
            ]}
          />
        </Card>
      </div>

      <Card title="Materials used" className="mt-6">
        {r.materialsUsed.length === 0 ? (
          <p className="text-sm text-muted">No materials recorded.</p>
        ) : (
          <ul className="divide-y divide-line">
            {r.materialsUsed.map((m) => (
              <li key={m.materialId} className="flex justify-between py-2 text-sm">
                <span className="text-ink">{m.materialName}</span>
                <span className="text-muted">{Number(m.quantity).toLocaleString('en-LK', { maximumFractionDigits: 3 })} {m.unit}</span>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card title="Photos" className="mt-6">
        {r.photos.length === 0 ? (
          <p className="text-sm text-muted">No photos.</p>
        ) : (
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {r.photos.map((p) => (
              <li key={p.id} className="overflow-hidden rounded-lg border border-line">
                <a href={dailyReportsApi.photoUrl(p.id)} target="_blank" rel="noreferrer">
                  <img src={dailyReportsApi.photoUrl(p.id)} alt={p.caption ?? p.fileName} loading="lazy" className="aspect-[4/3] w-full object-cover" />
                </a>
                <div className="flex items-center justify-between gap-2 px-3 py-2 text-xs text-muted">
                  <span className="truncate">{p.caption ?? p.fileName}</span>
                  {r.canEdit && (
                    <button
                      type="button"
                      onClick={() => update(dailyReportsApi.removePhoto(reportId, p.id), 'Photo removed.').catch((err) => toast.error(toApiError(err).message))}
                      className="text-red-600"
                      aria-label={`Remove photo ${p.caption ?? p.fileName}`}
                    >
                      <Trash2 className="h-4 w-4" aria-hidden />
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
        {r.canEdit && r.photos.length < 5 && (
          <div className="mt-4 grid gap-3 border-t border-line pt-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
            <FileUpload kind="image" file={photo} onChange={setPhoto} label="Choose a photo" />
            <Input aria-label="Caption" placeholder="Caption (optional)" maxLength={150} value={caption} onChange={(e) => setCaption(e.target.value)} />
            <Button disabled={!photo} loading={addPhoto.isPending} onClick={() => addPhoto.mutate()}>Upload</Button>
          </div>
        )}
      </Card>

      <Modal
        open={decision !== null}
        title={decision === 'approve' ? 'Approve this report?' : 'Reject this report?'}
        description={decision === 'approve' ? 'Materials used are taken off site stock and project progress is recalculated.' : 'The engineer can correct and resubmit it.'}
        onClose={() => setDecision(null)}
        busy={review.isPending}
      >
        <div className="space-y-4">
          <FormField label={decision === 'approve' ? 'Comments (optional)' : 'What needs to change'} htmlFor="review-remarks" error={error} required={decision === 'reject'}>
            <Textarea id="review-remarks" rows={3} maxLength={500} invalid={!!error} value={remarks} onChange={(e) => setRemarks(e.target.value)} />
          </FormField>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setDecision(null)} disabled={review.isPending}>Cancel</Button>
            <Button variant={decision === 'reject' ? 'danger' : 'primary'} loading={review.isPending} onClick={submitDecision}>
              {decision === 'approve' ? 'Approve report' : 'Reject report'}
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}
