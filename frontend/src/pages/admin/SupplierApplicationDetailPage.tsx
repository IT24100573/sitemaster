import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Download } from 'lucide-react';
import { supplierApplicationsApi } from '../../api/suppliersApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { toApiError } from '../../lib/apiError';
import { formatDateTime } from '../../lib/format';
import { CLIENT_MESSAGES } from '../../validation/messages';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { DetailList } from '../../components/ui/DetailList';
import { FormField } from '../../components/ui/FormField';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Textarea } from '../../components/ui/Textarea';

/** One supplier application with approve / reject. */
export default function SupplierApplicationDetailPage() {
  const { id } = useParams();
  const applicationId = Number(id);
  const { hasPermission } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [decision, setDecision] = useState<'approve' | 'reject' | null>(null);
  const [remarks, setRemarks] = useState('');
  const [error, setError] = useState<string | undefined>();
  const key = ['supplier-applications', 'detail', applicationId];

  const application = useQuery({ queryKey: key, queryFn: () => supplierApplicationsApi.get(applicationId) });

  const review = useMutation({
    mutationFn: () =>
      decision === 'approve'
        ? supplierApplicationsApi.approve(applicationId, remarks.trim() || null)
        : supplierApplicationsApi.reject(applicationId, remarks.trim()),
    onSuccess: async (updated) => {
      queryClient.setQueryData(key, updated);
      await queryClient.invalidateQueries({ queryKey: ['supplier-applications'] });
      await queryClient.invalidateQueries({ queryKey: ['suppliers'] });
      setDecision(null);
      toast.success(
        updated.status === 'APPROVED'
          ? `${updated.companyName} approved. A set-password link was issued for ${updated.email}.`
          : `${updated.companyName} rejected.`,
      );
    },
    onError: (err) => {
      const apiError = toApiError(err);
      setError(apiError.fieldErrors.remarks ?? apiError.message);
    },
  });

  const submit = () => {
    setError(undefined);
    if (decision === 'reject' && remarks.trim().length < 5) {
      setError(CLIENT_MESSAGES.remarksRequired);
      return;
    }
    review.mutate();
  };

  if (application.isLoading) {
    return <FullPageSpinner />;
  }
  if (application.isError || !application.data) {
    return <Alert tone="error">{toApiError(application.error).message}</Alert>;
  }
  const a = application.data;
  const canReview = a.status === 'PENDING' && hasPermission('SUPPLIER:APPROVE');

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        Back to applications
      </Link>
      <PageHeader
        title={a.companyName}
        description={<StatusBadge status={a.status} />}
        actions={
          canReview && (
            <>
              <Button variant="secondary" onClick={() => { setDecision('reject'); setRemarks(''); setError(undefined); }}>
                Reject
              </Button>
              <Button onClick={() => { setDecision('approve'); setRemarks(''); setError(undefined); }}>Approve</Button>
            </>
          )
        }
      />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Company" className="lg:col-span-2">
          <DetailList
            items={[
              { label: 'Registration number', value: a.registrationNo },
              { label: 'Contact person', value: a.contactPerson },
              { label: 'Email', value: a.email },
              { label: 'Phone', value: a.phone },
              { label: 'Address', value: a.address },
              { label: 'Received', value: formatDateTime(a.createdAt) },
              { label: 'Supplies', value: a.materialCategories.join(', ') },
            ]}
          />
        </Card>
        <Card title="Registration document">
          <a
            href={supplierApplicationsApi.documentUrl(a.id)}
            className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:text-primary-dark"
          >
            <Download className="h-4 w-4" aria-hidden />
            {a.documentName}
          </a>
          {a.status !== 'PENDING' && (
            <div className="mt-6 border-t border-line pt-4">
              <DetailList
                items={[
                  { label: 'Reviewed by', value: a.reviewedByName ?? '-' },
                  { label: 'Reviewed', value: formatDateTime(a.reviewedAt) },
                  { label: 'Remarks', value: a.reviewRemarks ?? '-' },
                ]}
              />
            </div>
          )}
        </Card>
      </div>

      <Modal
        open={decision !== null}
        title={decision === 'approve' ? `Approve ${a.companyName}?` : `Reject ${a.companyName}?`}
        description={
          decision === 'approve'
            ? 'A supplier record and sign-in account are created, and a set-password link is issued.'
            : 'The applicant is not given an account. Give a reason for the record.'
        }
        onClose={() => setDecision(null)}
        busy={review.isPending}
      >
        <div className="space-y-4">
          <FormField label={decision === 'approve' ? 'Remarks (optional)' : 'Reason'} htmlFor="review-remarks" error={error} required={decision === 'reject'}>
            <Textarea id="review-remarks" rows={3} maxLength={500} invalid={!!error} value={remarks} onChange={(e) => setRemarks(e.target.value)} />
          </FormField>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setDecision(null)} disabled={review.isPending}>
              Cancel
            </Button>
            <Button variant={decision === 'reject' ? 'danger' : 'primary'} loading={review.isPending} onClick={submit}>
              {decision === 'approve' ? 'Approve supplier' : 'Reject application'}
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}
