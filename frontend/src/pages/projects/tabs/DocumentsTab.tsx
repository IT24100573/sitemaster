import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ChevronDown, ChevronRight, Download, FilePlus2, FileText, Trash2, Upload } from 'lucide-react';
import { documentsApi } from '../../../api/documentsApi';
import { useToast } from '../../../context/ToastContext';
import { useDebouncedValue } from '../../../hooks/useDebouncedValue';
import { useFormFeedback } from '../../../hooks/useFormFeedback';
import { useEnumLookup } from '../../../hooks/useLookup';
import { collectErrorMessages, toApiError } from '../../../lib/apiError';
import { formatBytes, formatDateTime, humanize } from '../../../lib/format';
import { CLIENT_MESSAGES } from '../../../validation/messages';
import { documentSchema, type ProjectDocumentFormValues } from '../../../validation/projectSchemas';
import { Alert } from '../../../components/ui/Alert';
import { Button } from '../../../components/ui/Button';
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog';
import { ErrorSummary } from '../../../components/ui/ErrorSummary';
import { FileUpload } from '../../../components/ui/FileUpload';
import { FormField } from '../../../components/ui/FormField';
import { Input } from '../../../components/ui/Input';
import { Modal } from '../../../components/ui/Modal';
import { SearchInput } from '../../../components/ui/SearchInput';
import { Select } from '../../../components/ui/Select';
import { Spinner } from '../../../components/ui/Spinner';
import { Textarea } from '../../../components/ui/Textarea';
import { projectKeys } from '../queryKeys';
import type { ProjectPermissions } from '../useProjectPermissions';
import type { ProjectDetail } from '../../../types/project';
import type { ProjectDocument } from '../../../types/document';

const FIELDS = ['title', 'category', 'description'] as const;

function NewDocumentForm({ projectId, onDone }: { projectId: number; onDone: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const categories = useEnumLookup('document-categories');
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | undefined>();
  const { register, handleSubmit, setError, formState: { errors, isSubmitting, submitCount } } =
    useForm<ProjectDocumentFormValues>({
      resolver: zodResolver(documentSchema),
      mode: 'onBlur',
      defaultValues: { title: '', category: '', description: '' },
    });
  const feedback = useFormFeedback(setError, FIELDS);

  const onSubmit = async (v: ProjectDocumentFormValues) => {
    feedback.clear();
    if (!file) {
      setFileError(CLIENT_MESSAGES.fileRequired);
      return;
    }
    try {
      await documentsApi.create(projectId, v.title, v.category, v.description.trim(), file);
      await queryClient.invalidateQueries({ queryKey: projectKeys.documents(projectId) });
      toast.success('Document uploaded.');
      onDone();
    } catch (error) {
      setFileError(toApiError(error).fieldErrors.file);
      feedback.handleError(error);
    }
  };

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <ErrorSummary messages={summary} />
      <FormField label="Title" htmlFor="doc-title" error={errors.title?.message} required>
        <Input id="doc-title" invalid={!!errors.title} {...register('title')} />
      </FormField>
      <FormField label="Category" htmlFor="doc-category" error={errors.category?.message} required>
        <Select id="doc-category" options={categories.data ?? []} placeholder="Choose a category" invalid={!!errors.category} {...register('category')} />
      </FormField>
      <FormField label="Description" htmlFor="doc-description" error={errors.description?.message}>
        <Textarea id="doc-description" rows={2} maxLength={500} {...register('description')} />
      </FormField>
      <FileUpload kind="document" file={file} onChange={(f) => { setFile(f); setFileError(undefined); }} error={fileError} />
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" loading={isSubmitting}>
          Upload document
        </Button>
      </div>
    </form>
  );
}

function NewVersionForm({ document, projectId, onDone }: { document: ProjectDocument; projectId: number; onDone: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | undefined>();

  const upload = useMutation({
    mutationFn: () => documentsApi.addVersion(document.id, file as File, notes.trim()),
    onSuccess: async (updated) => {
      await queryClient.invalidateQueries({ queryKey: projectKeys.documents(projectId) });
      toast.success(`Version ${updated.currentVersion} uploaded.`);
      onDone();
    },
    onError: (err) => {
      const apiError = toApiError(err);
      setError(apiError.fieldErrors.file ?? apiError.fieldErrors.notes ?? apiError.message);
    },
  });

  return (
    <div className="space-y-4">
      <p className="text-sm text-muted">Current version: {document.currentVersion}. Earlier versions stay available.</p>
      <FormField label="What changed" htmlFor="version-notes" hint="Optional, up to 500 characters">
        <Textarea id="version-notes" rows={2} maxLength={500} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </FormField>
      <FileUpload kind="document" file={file} onChange={(f) => { setFile(f); setError(undefined); }} error={error} />
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onDone}>
          Cancel
        </Button>
        <Button
          loading={upload.isPending}
          onClick={() => (file ? upload.mutate() : setError(CLIENT_MESSAGES.fileRequired))}
        >
          Upload version
        </Button>
      </div>
    </div>
  );
}

/** Project documents; every upload keeps the earlier versions. */
export function DocumentsTab({ project, perms }: { project: ProjectDetail; perms: ProjectPermissions }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const categories = useEnumLookup('document-categories');
  const [category, setCategory] = useState('');
  const [search, setSearch] = useState('');
  const debounced = useDebouncedValue(search.trim());
  const [creating, setCreating] = useState(false);
  const [versioning, setVersioning] = useState<ProjectDocument | null>(null);
  const [deleting, setDeleting] = useState<ProjectDocument | null>(null);
  const [expanded, setExpanded] = useState<number | null>(null);

  const documents = useQuery({
    queryKey: [...projectKeys.documents(project.id), category, debounced],
    queryFn: () => documentsApi.list(project.id, category || undefined, debounced || undefined),
  });

  const remove = useMutation({
    mutationFn: (doc: ProjectDocument) => documentsApi.remove(doc.id),
    onSuccess: async () => {
      setDeleting(null);
      await queryClient.invalidateQueries({ queryKey: projectKeys.documents(project.id) });
      toast.success('Document deleted.');
    },
    onError: (error) => {
      setDeleting(null);
      toast.error(toApiError(error).message);
    },
  });

  return (
    <div className="space-y-4">
      <div className="card flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
        <SearchInput value={search} onChange={setSearch} placeholder="Search titles" />
        <div className="flex gap-2 sm:ml-auto">
          <Select aria-label="Filter by category" options={categories.data ?? []} placeholder="All categories" value={category} onChange={(e) => setCategory(e.target.value)} />
          {perms.canUploadDocuments && (
            <Button onClick={() => setCreating(true)} className="shrink-0">
              <FilePlus2 className="h-4 w-4" aria-hidden />
              Upload
            </Button>
          )}
        </div>
      </div>

      {documents.isLoading ? (
        <Spinner />
      ) : documents.isError ? (
        <Alert tone="error">{toApiError(documents.error).message}</Alert>
      ) : documents.data && documents.data.length > 0 ? (
        <ul className="card divide-y divide-line">
          {documents.data.map((doc) => {
            const latest = doc.versions[0];
            const open = expanded === doc.id;
            return (
              <li key={doc.id} className="p-4">
                <div className="flex items-center gap-3">
                  <FileText className="h-5 w-5 shrink-0 text-primary" aria-hidden />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-ink">{doc.title}</p>
                    <p className="truncate text-xs text-muted">
                      {humanize(doc.category)}, version {doc.currentVersion}
                      {latest ? `, ${latest.uploadedByName}, ${formatDateTime(latest.uploadedAt)}` : ''}
                    </p>
                  </div>
                  {latest && (
                    <a href={documentsApi.downloadUrl(latest.id)} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Download latest ${doc.title}`} title="Download latest">
                      <Download className="h-4 w-4" aria-hidden />
                    </a>
                  )}
                  {perms.canVersionDocuments && (
                    <button type="button" onClick={() => setVersioning(doc)} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`New version of ${doc.title}`} title="Upload new version">
                      <Upload className="h-4 w-4" aria-hidden />
                    </button>
                  )}
                  {perms.canDeleteDocuments && (
                    <button type="button" onClick={() => setDeleting(doc)} className="rounded-md p-2 text-red-600 hover:bg-surface" aria-label={`Delete ${doc.title}`} title="Delete">
                      <Trash2 className="h-4 w-4" aria-hidden />
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => setExpanded(open ? null : doc.id)}
                    className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink"
                    aria-expanded={open}
                    aria-label={`${open ? 'Hide' : 'Show'} versions of ${doc.title}`}
                  >
                    {open ? <ChevronDown className="h-4 w-4" aria-hidden /> : <ChevronRight className="h-4 w-4" aria-hidden />}
                  </button>
                </div>
                {doc.description && <p className="ml-8 mt-1 text-sm text-muted">{doc.description}</p>}
                {open && (
                  <ol className="ml-8 mt-3 space-y-2 border-l-2 border-line pl-4">
                    {doc.versions.map((v) => (
                      <li key={v.id} className="flex items-center justify-between gap-3 text-sm">
                        <div className="min-w-0">
                          <p className="text-ink">
                            Version {v.versionNo}: {v.fileName} <span className="text-muted">({formatBytes(v.sizeBytes)})</span>
                          </p>
                          <p className="text-xs text-muted">
                            {v.uploadedByName}, {formatDateTime(v.uploadedAt)}
                            {v.notes ? `. ${v.notes}` : ''}
                          </p>
                        </div>
                        <a href={documentsApi.downloadUrl(v.id)} className="shrink-0 text-sm font-medium text-primary hover:text-primary-dark">
                          Download
                        </a>
                      </li>
                    ))}
                  </ol>
                )}
              </li>
            );
          })}
        </ul>
      ) : (
        <p className="card p-8 text-center text-sm text-muted">No documents yet.</p>
      )}

      <Modal open={creating} title="Upload document" onClose={() => setCreating(false)}>
        {creating && <NewDocumentForm projectId={project.id} onDone={() => setCreating(false)} />}
      </Modal>
      <Modal open={versioning !== null} title={`New version of ${versioning?.title ?? ''}`} onClose={() => setVersioning(null)}>
        {versioning && <NewVersionForm document={versioning} projectId={project.id} onDone={() => setVersioning(null)} />}
      </Modal>
      <ConfirmDialog
        open={deleting !== null}
        title={`Delete ${deleting?.title ?? ''}?`}
        message="All versions of this document are deleted permanently."
        confirmLabel="Delete document"
        tone="danger"
        loading={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting)}
        onCancel={() => setDeleting(null)}
      />
    </div>
  );
}
