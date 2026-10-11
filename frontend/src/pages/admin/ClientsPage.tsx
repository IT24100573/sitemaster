import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, Power } from 'lucide-react';
import { clientsApi } from '../../api/clientsApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useTableState } from '../../hooks/useTableState';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { emptyToNull } from '../../lib/params';
import { clientSchema, type ClientFormValues } from '../../validation/projectSchemas';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { ClientRecord } from '../../types/client';

const FIELDS = ['name', 'contactPerson', 'email', 'phone', 'address'] as const;
const STATUS_OPTIONS = [
  { value: 'true', label: 'Active' },
  { value: 'false', label: 'Inactive' },
];

function ClientForm({ client, onDone }: { client: ClientRecord | null; onDone: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const { register, handleSubmit, setError, formState: { errors, isSubmitting, submitCount } } = useForm<ClientFormValues>({
    resolver: zodResolver(clientSchema),
    mode: 'onBlur',
    defaultValues: {
      name: client?.name ?? '',
      contactPerson: client?.contactPerson ?? '',
      email: client?.email ?? '',
      phone: client?.phone ?? '',
      address: client?.address ?? '',
    },
  });
  const feedback = useFormFeedback(setError, FIELDS);

  const onSubmit = async (v: ClientFormValues) => {
    feedback.clear();
    const payload = {
      name: v.name,
      contactPerson: emptyToNull(v.contactPerson),
      email: v.email,
      phone: v.phone,
      address: emptyToNull(v.address),
    };
    try {
      const saved = client ? await clientsApi.update(client.id, payload) : await clientsApi.create(payload);
      await queryClient.invalidateQueries({ queryKey: ['clients'] });
      await queryClient.invalidateQueries({ queryKey: ['lookup', 'clients'] });
      toast.success(client ? `${saved.name} updated.` : `${saved.name} added.`);
      onDone();
    } catch (error) {
      feedback.handleError(error);
    }
  };

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <ErrorSummary messages={summary} />
      <FormField label="Name" htmlFor="client-name" error={errors.name?.message} required>
        <Input id="client-name" invalid={!!errors.name} {...register('name')} />
      </FormField>
      <FormField label="Contact person" htmlFor="client-contact" error={errors.contactPerson?.message}>
        <Input id="client-contact" invalid={!!errors.contactPerson} {...register('contactPerson')} />
      </FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Email" htmlFor="client-email" error={errors.email?.message} required>
          <Input id="client-email" type="email" invalid={!!errors.email} {...register('email')} />
        </FormField>
        <FormField label="Phone" htmlFor="client-phone" error={errors.phone?.message} required>
          <Input id="client-phone" type="tel" invalid={!!errors.phone} {...register('phone')} />
        </FormField>
      </div>
      <FormField label="Address" htmlFor="client-address" error={errors.address?.message}>
        <Input id="client-address" invalid={!!errors.address} {...register('address')} />
      </FormField>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" loading={isSubmitting}>
          {client ? 'Save client' : 'Add client'}
        </Button>
      </div>
    </form>
  );
}

/** Clients that projects are built for. */
export default function ClientsPage() {
  const { hasPermission } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const table = useTableState('name');
  const [active, setActive] = useState('');
  const [form, setForm] = useState<{ client: ClientRecord | null } | null>(null);
  const [toggling, setToggling] = useState<ClientRecord | null>(null);

  const clients = useQuery({
    queryKey: ['clients', table.params, active],
    queryFn: () => clientsApi.list(table.params, active || undefined),
    placeholderData: keepPreviousData,
  });

  const toggle = useMutation({
    mutationFn: (c: ClientRecord) => clientsApi.setActive(c.id, !c.active),
    onSuccess: async (c) => {
      setToggling(null);
      await queryClient.invalidateQueries({ queryKey: ['clients'] });
      await queryClient.invalidateQueries({ queryKey: ['lookup', 'clients'] });
      toast.success(`${c.name} is now ${c.active ? 'active' : 'inactive'}.`);
    },
    onError: (error) => {
      setToggling(null);
      toast.error(toApiError(error).message);
    },
  });

  const columns: Column<ClientRecord>[] = [
    { key: 'name', header: 'Client', sortKey: 'name', render: (c) => <span className="font-medium text-ink">{c.name}</span> },
    { key: 'contact', header: 'Contact', render: (c) => (
      <div>
        <p>{c.contactPerson ?? '-'}</p>
        <p className="text-xs text-muted">{c.email}, {c.phone}</p>
      </div>
    ) },
    { key: 'projects', header: 'Projects', render: (c) => c.projectCount },
    { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.active ? 'ACTIVE' : 'INACTIVE'} /> },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (c) =>
        hasPermission('CLIENT:EDIT') && (
          <div className="flex justify-end gap-1">
            <button type="button" onClick={() => setForm({ client: c })} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Edit ${c.name}`}>
              <Pencil className="h-4 w-4" aria-hidden />
            </button>
            <button type="button" onClick={() => setToggling(c)} className={`rounded-md p-2 hover:bg-surface ${c.active ? 'text-red-600' : 'text-green-700'}`} aria-label={c.active ? `Deactivate ${c.name}` : `Activate ${c.name}`}>
              <Power className="h-4 w-4" aria-hidden />
            </button>
          </div>
        ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Clients"
        description="Customers your projects are delivered for."
        actions={
          hasPermission('CLIENT:CREATE') && (
            <Button onClick={() => setForm({ client: null })}>
              <Plus className="h-4 w-4" aria-hidden />
              New client
            </Button>
          )
        }
      />
      <DataTable
        columns={columns}
        page={clients.data}
        rowKey={(c) => c.id}
        loading={clients.isFetching}
        error={clients.isError ? toApiError(clients.error).message : null}
        emptyMessage="No clients yet."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Name, contact or email" />
            <div className="sm:ml-auto">
              <Select aria-label="Filter by status" options={STATUS_OPTIONS} placeholder="Any status" value={active} onChange={(e) => { setActive(e.target.value); table.resetPage(); }} />
            </div>
          </>
        }
      />
      <Modal open={form !== null} title={form?.client ? 'Edit client' : 'New client'} onClose={() => setForm(null)}>
        {form && <ClientForm client={form.client} onDone={() => setForm(null)} />}
      </Modal>
      <ConfirmDialog
        open={toggling !== null}
        title={toggling?.active ? `Deactivate ${toggling.name}?` : `Activate ${toggling?.name ?? ''}?`}
        message={toggling?.active ? 'Inactive clients cannot be chosen for new projects. Existing projects are not affected.' : 'The client can be chosen for new projects again.'}
        confirmLabel={toggling?.active ? 'Deactivate' : 'Activate'}
        tone={toggling?.active ? 'danger' : 'primary'}
        loading={toggle.isPending}
        onConfirm={() => toggling && toggle.mutate(toggling)}
        onCancel={() => setToggling(null)}
      />
    </>
  );
}
