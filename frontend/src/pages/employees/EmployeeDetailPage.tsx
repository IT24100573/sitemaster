import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Download, FileText, Pencil, Trash2 } from 'lucide-react';
import { employeesApi } from '../../api/employeesApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useLookup } from '../../hooks/useLookup';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { formatBytes, formatDate, formatMoney, humanize } from '../../lib/format';
import { documentSchema, type DocumentFormValues } from '../../validation/employeeSchemas';
import { CLIENT_MESSAGES } from '../../validation/messages';
import { Alert } from '../../components/ui/Alert';
import { Avatar } from '../../components/ui/Avatar';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { DetailList } from '../../components/ui/DetailList';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FileUpload } from '../../components/ui/FileUpload';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { PageHeader } from '../../components/ui/PageHeader';
import { Select } from '../../components/ui/Select';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { EmployeeDocument, EmployeeRecord } from '../../types/employee';

const DOC_FIELDS = ['documentType', 'title'] as const;

function payTerms(e: EmployeeRecord, currency: string) {
  switch (e.employmentType) {
    case 'DAILY_WAGE':
      return [{ label: 'Daily rate', value: formatMoney(e.dailyRate, currency) }];
    case 'PERMANENT':
      return [{ label: 'Monthly salary', value: formatMoney(e.monthlySalary, currency) }];
    default:
      return [
        { label: 'Contract amount', value: formatMoney(e.contractAmount, currency) },
        { label: 'Contract period', value: `${formatDate(e.contractStartDate)} to ${formatDate(e.contractEndDate)}` },
      ];
  }
}

/** Employee record with NIC copies, certificates and other documents. */
export default function EmployeeDetailPage() {
  const { id } = useParams();
  const employeeId = Number(id);
  const { hasPermission } = useAuth();
  const canEdit = hasPermission('EMPLOYEE:EDIT');
  const currency = useCurrency();
  const toast = useToast();
  const queryClient = useQueryClient();
  const docTypes = useLookup('employeeDocumentTypes');
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | undefined>();
  const [removing, setRemoving] = useState<EmployeeDocument | null>(null);
  const detailKey = ['employees', 'detail', employeeId];

  const employee = useQuery({ queryKey: detailKey, queryFn: () => employeesApi.get(employeeId) });

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting, submitCount },
  } = useForm<DocumentFormValues>({
    resolver: zodResolver(documentSchema),
    mode: 'onBlur',
    defaultValues: { documentType: '', title: '' },
  });
  const feedback = useFormFeedback(setError, DOC_FIELDS);

  const onUpload = async (values: DocumentFormValues) => {
    feedback.clear();
    setFileError(undefined);
    if (!file) {
      setFileError(CLIENT_MESSAGES.fileRequired);
      return;
    }
    try {
      const updated = await employeesApi.addDocument(employeeId, values.documentType, values.title, file);
      queryClient.setQueryData(detailKey, updated);
      reset();
      setFile(null);
      toast.success('Document uploaded.');
    } catch (error) {
      setFileError(toApiError(error).fieldErrors.file);
      feedback.handleError(error);
    }
  };

  const remove = useMutation({
    mutationFn: (doc: EmployeeDocument) => employeesApi.removeDocument(employeeId, doc.id),
    onSuccess: (updated) => {
      queryClient.setQueryData(detailKey, updated);
      setRemoving(null);
      toast.success('Document removed.');
    },
    onError: (error) => {
      setRemoving(null);
      toast.error(toApiError(error).message);
    },
  });

  if (employee.isLoading) {
    return <FullPageSpinner />;
  }
  if (employee.isError || !employee.data) {
    return <Alert tone="error">{toApiError(employee.error).message}</Alert>;
  }

  const e = employee.data;
  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        Back to employees
      </Link>
      <PageHeader
        title={e.fullName}
        description={`${e.employeeNo} · ${e.roleDisplayName}`}
        actions={
          canEdit && (
            <Link
              to="edit"
              className="inline-flex h-11 items-center gap-2 rounded-lg border border-line bg-white px-4 text-sm font-medium text-ink hover:bg-surface"
            >
              <Pencil className="h-4 w-4" aria-hidden />
              Edit
            </Link>
          )
        }
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2" title="Details">
          <div className="mb-5 flex items-center gap-4">
            <Avatar userId={e.userId} name={e.fullName} hasPhoto={e.hasPhoto} size="md" />
            <StatusBadge status={e.active ? 'ACTIVE' : 'INACTIVE'} />
          </div>
          <DetailList
            items={[
              { label: 'Email', value: e.email },
              { label: 'Phone', value: e.phone ?? '-' },
              { label: 'NIC', value: e.nic },
              { label: 'Date of birth', value: `${formatDate(e.dateOfBirth)} (age ${e.age})` },
              { label: 'Address', value: e.address },
              { label: 'Emergency contact', value: e.emergencyContactName ? `${e.emergencyContactName} ${e.emergencyContactPhone ?? ''}` : '-' },
            ]}
          />
        </Card>

        <Card title="Employment" highlighted>
          <DetailList
            items={[
              { label: 'Employment type', value: humanize(e.employmentType) },
              { label: 'Trade', value: e.trade ?? '-' },
              { label: 'Joined', value: formatDate(e.joinDate) },
              ...payTerms(e, currency),
            ]}
          />
        </Card>
      </div>

      <Card className="mt-6" title="Documents" description="NIC copies, certificates and other records. PDF, Word, Excel, JPG or PNG up to 10 MB.">
        {e.documents.length === 0 ? (
          <p className="rounded-lg bg-surface p-4 text-sm text-muted">No documents yet.</p>
        ) : (
          <ul className="divide-y divide-line">
            {e.documents.map((doc) => (
              <li key={doc.id} className="flex items-center gap-3 py-3">
                <FileText className="h-5 w-5 shrink-0 text-primary" aria-hidden />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-ink">{doc.title}</p>
                  <p className="truncate text-xs text-muted">
                    {humanize(doc.documentType)} · {doc.fileName} · {formatBytes(doc.sizeBytes)} · {formatDate(doc.uploadedAt)}
                  </p>
                </div>
                <a
                  href={employeesApi.documentUrl(e.id, doc.id)}
                  className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink"
                  aria-label={`Download ${doc.title}`}
                  title="Download"
                >
                  <Download className="h-4 w-4" aria-hidden />
                </a>
                {canEdit && (
                  <button
                    type="button"
                    onClick={() => setRemoving(doc)}
                    className="rounded-md p-2 text-red-600 hover:bg-surface"
                    aria-label={`Remove ${doc.title}`}
                    title="Remove"
                  >
                    <Trash2 className="h-4 w-4" aria-hidden />
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}

        {canEdit && (
          <form onSubmit={handleSubmit(onUpload)} noValidate className="mt-6 space-y-4 border-t border-line pt-6">
            <h3 className="text-sm font-semibold text-ink">Upload a document</h3>
            <ErrorSummary messages={summary} />
            <div className="grid gap-4 md:grid-cols-2">
              <FormField label="Type" htmlFor="documentType" error={errors.documentType?.message} required>
                <Select id="documentType" options={docTypes.data ?? []} placeholder="Choose a type" invalid={!!errors.documentType} {...register('documentType')} />
              </FormField>
              <FormField label="Title" htmlFor="title" error={errors.title?.message} required>
                <Input id="title" maxLength={100} placeholder="e.g. NVQ Level 3 certificate" invalid={!!errors.title} {...register('title')} />
              </FormField>
            </div>
            <FileUpload kind="document" file={file} onChange={(f) => { setFile(f); setFileError(undefined); }} error={fileError} />
            <Button type="submit" loading={isSubmitting}>
              Upload document
            </Button>
          </form>
        )}
      </Card>

      <ConfirmDialog
        open={removing !== null}
        title={`Remove "${removing?.title ?? ''}"?`}
        message="The file is deleted permanently."
        confirmLabel="Remove document"
        tone="danger"
        loading={remove.isPending}
        onConfirm={() => removing && remove.mutate(removing)}
        onCancel={() => setRemoving(null)}
      />
    </>
  );
}
