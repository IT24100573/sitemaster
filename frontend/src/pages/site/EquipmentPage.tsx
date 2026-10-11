import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MapPin, Pencil, Plus, Wrench } from 'lucide-react';
import { equipmentApi } from '../../api/equipmentApi';
import { useAuth, useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useEnumLookup } from '../../hooks/useLookup';
import { useTableState } from '../../hooks/useTableState';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { formatDate } from '../../lib/format';
import { emptyToNull } from '../../lib/params';
import {
  equipmentConditionSchema,
  equipmentSchema,
  type EquipmentConditionFormValues,
  type EquipmentFormValues,
} from '../../validation/siteSchemas';
import { Button } from '../../components/ui/Button';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Textarea } from '../../components/ui/Textarea';
import type { Equipment } from '../../types/equipment';

const ITEM_FIELDS = ['code', 'name', 'category'] as const;
const CONDITION_FIELDS = ['status', 'notes', 'lastServiceDate', 'nextServiceDate'] as const;

function ItemForm({ item, onDone }: { item: Equipment | null; onDone: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const { register, handleSubmit, setError, formState: { errors, isSubmitting, submitCount } } = useForm<EquipmentFormValues>({
    resolver: zodResolver(equipmentSchema),
    mode: 'onBlur',
    defaultValues: { code: item?.code ?? '', name: item?.name ?? '', category: item?.category ?? '' },
  });
  const feedback = useFormFeedback(setError, ITEM_FIELDS);
  const onSubmit = async (v: EquipmentFormValues) => {
    feedback.clear();
    try {
      const saved = item ? await equipmentApi.update(item.id, v) : await equipmentApi.create(v);
      await queryClient.invalidateQueries({ queryKey: ['equipment'] });
      toast.success(`${saved.code} saved.`);
      onDone();
    } catch (error) {
      feedback.handleError(error);
    }
  };
  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];
  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <ErrorSummary messages={summary} />
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Code" htmlFor="eq-code" error={errors.code?.message} required>
          <Input id="eq-code" className="uppercase" invalid={!!errors.code} {...register('code')} />
        </FormField>
        <FormField label="Category" htmlFor="eq-category" error={errors.category?.message} hint="e.g. Excavator, Mixer" required>
          <Input id="eq-category" invalid={!!errors.category} {...register('category')} />
        </FormField>
      </div>
      <FormField label="Name" htmlFor="eq-name" error={errors.name?.message} required>
        <Input id="eq-name" invalid={!!errors.name} {...register('name')} />
      </FormField>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={isSubmitting}>Save</Button>
      </div>
    </form>
  );
}

function ConditionForm({ item, onDone }: { item: Equipment; onDone: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const statuses = useEnumLookup('equipment-statuses');
  const { register, handleSubmit, setError, formState: { errors, isSubmitting, submitCount } } =
    useForm<EquipmentConditionFormValues>({
      resolver: zodResolver(equipmentConditionSchema),
      mode: 'onBlur',
      defaultValues: {
        status: item.status,
        notes: item.notes ?? '',
        lastServiceDate: item.lastServiceDate ?? '',
        nextServiceDate: item.nextServiceDate ?? '',
      },
    });
  const feedback = useFormFeedback(setError, CONDITION_FIELDS);
  const onSubmit = async (v: EquipmentConditionFormValues) => {
    feedback.clear();
    try {
      await equipmentApi.condition(item.id, {
        status: v.status,
        notes: emptyToNull(v.notes),
        lastServiceDate: emptyToNull(v.lastServiceDate),
        nextServiceDate: emptyToNull(v.nextServiceDate),
      });
      await queryClient.invalidateQueries({ queryKey: ['equipment'] });
      toast.success(`${item.code} updated.`);
      onDone();
    } catch (error) {
      feedback.handleError(error);
    }
  };
  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];
  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <ErrorSummary messages={summary} />
      <FormField label="Status" htmlFor="eq-status" error={errors.status?.message} required>
        <Select id="eq-status" options={statuses.data ?? []} invalid={!!errors.status} {...register('status')} />
      </FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Last service" htmlFor="eq-last" error={errors.lastServiceDate?.message}>
          <Input id="eq-last" type="date" invalid={!!errors.lastServiceDate} {...register('lastServiceDate')} />
        </FormField>
        <FormField label="Next service" htmlFor="eq-next" error={errors.nextServiceDate?.message}>
          <Input id="eq-next" type="date" invalid={!!errors.nextServiceDate} {...register('nextServiceDate')} />
        </FormField>
      </div>
      <FormField label="Notes" htmlFor="eq-notes" error={errors.notes?.message}>
        <Textarea id="eq-notes" rows={3} maxLength={500} {...register('notes')} />
      </FormField>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={isSubmitting}>Save</Button>
      </div>
    </form>
  );
}

function AssignForm({ item, onDone }: { item: Equipment; onDone: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [projectId, setProjectId] = useState(item.projectId ? String(item.projectId) : '');
  const assign = useMutation({
    mutationFn: () => equipmentApi.assign(item.id, projectId ? Number(projectId) : null),
    onSuccess: async (updated) => {
      await queryClient.invalidateQueries({ queryKey: ['equipment'] });
      toast.success(updated.projectName ? `${updated.code} assigned to ${updated.projectName}.` : `${updated.code} released.`);
      onDone();
    },
    onError: (error) => toast.error(toApiError(error).message),
  });
  return (
    <div className="space-y-4">
      <FormField label="Project" htmlFor="eq-project" hint="Leave empty to release the equipment.">
        <ProjectSelect id="eq-project" activeOnly placeholder="Not assigned" value={projectId} onChange={(e) => setProjectId(e.target.value)} />
      </FormField>
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onDone}>Cancel</Button>
        <Button loading={assign.isPending} onClick={() => assign.mutate()}>Save assignment</Button>
      </div>
    </div>
  );
}

type Dialog = { kind: 'item'; item: Equipment | null } | { kind: 'condition'; item: Equipment } | { kind: 'assign'; item: Equipment } | null;

/** Equipment register: Admin registers, PMs assign to their projects, site teams record condition. */
export default function EquipmentPage() {
  const user = useCurrentUser();
  const { hasPermission } = useAuth();
  const table = useTableState('code');
  const [projectId, setProjectId] = useState('');
  const [status, setStatus] = useState('');
  const [dialog, setDialog] = useState<Dialog>(null);
  const statuses = useEnumLookup('equipment-statuses');
  const canRegister = hasPermission('EQUIPMENT:CREATE');
  const canAssign = hasPermission('EQUIPMENT:EDIT') && (user.role === 'ADMIN' || user.role === 'PROJECT_MANAGER');
  const canUpdate = hasPermission('EQUIPMENT:EDIT');

  const equipment = useQuery({
    queryKey: ['equipment', table.params, projectId, status],
    queryFn: () => equipmentApi.list(table.params, projectId || undefined, status || undefined),
    placeholderData: keepPreviousData,
  });

  const columns: Column<Equipment>[] = [
    { key: 'code', header: 'Code', sortKey: 'code', render: (e) => <span className="font-medium text-ink">{e.code}</span> },
    { key: 'name', header: 'Equipment', sortKey: 'name', render: (e) => (
      <div>
        <p>{e.name}</p>
        <p className="text-xs text-muted">{e.category}</p>
      </div>
    ) },
    { key: 'project', header: 'Site', render: (e) => e.projectName ?? <span className="text-muted">Not assigned</span> },
    { key: 'status', header: 'Status', sortKey: 'status', render: (e) => <StatusBadge status={e.status} /> },
    {
      key: 'service',
      header: 'Next service',
      sortKey: 'nextServiceDate',
      render: (e) => <span className={e.serviceDue ? 'font-medium text-red-600' : ''}>{formatDate(e.nextServiceDate)}{e.serviceDue ? ', due' : ''}</span>,
    },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (e) => (
        <div className="flex justify-end gap-1">
          {canRegister && (
            <button type="button" onClick={() => setDialog({ kind: 'item', item: e })} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Edit ${e.code}`} title="Edit">
              <Pencil className="h-4 w-4" aria-hidden />
            </button>
          )}
          {canAssign && e.status !== 'RETIRED' && (
            <button type="button" onClick={() => setDialog({ kind: 'assign', item: e })} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Assign ${e.code}`} title="Assign to project">
              <MapPin className="h-4 w-4" aria-hidden />
            </button>
          )}
          {canUpdate && (user.role === 'ADMIN' || e.projectId !== null) && (
            <button type="button" onClick={() => setDialog({ kind: 'condition', item: e })} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Update condition of ${e.code}`} title="Status and service">
              <Wrench className="h-4 w-4" aria-hidden />
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Equipment"
        description={user.role === 'ADMIN' ? 'Every machine and tool the company owns.' : 'Equipment on your sites and equipment available to request.'}
        actions={
          canRegister && (
            <Button onClick={() => setDialog({ kind: 'item', item: null })}>
              <Plus className="h-4 w-4" aria-hidden />
              Register equipment
            </Button>
          )
        }
      />
      <DataTable
        columns={columns}
        page={equipment.data}
        rowKey={(e) => e.id}
        loading={equipment.isFetching}
        error={equipment.isError ? toApiError(equipment.error).message : null}
        emptyMessage="No equipment matches these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Code, name or category" />
            <div className="flex flex-wrap gap-2 sm:ml-auto">
              <ProjectSelect aria-label="Filter by project" value={projectId} onChange={(e) => { setProjectId(e.target.value); table.resetPage(); }} />
              <Select aria-label="Filter by status" options={statuses.data ?? []} placeholder="Any status" value={status} onChange={(e) => { setStatus(e.target.value); table.resetPage(); }} />
            </div>
          </>
        }
      />
      <Modal open={dialog?.kind === 'item'} title={dialog?.kind === 'item' && dialog.item ? `Edit ${dialog.item.code}` : 'Register equipment'} onClose={() => setDialog(null)}>
        {dialog?.kind === 'item' && <ItemForm item={dialog.item} onDone={() => setDialog(null)} />}
      </Modal>
      <Modal open={dialog?.kind === 'assign'} title={`Assign ${dialog?.item?.code ?? ''}`} onClose={() => setDialog(null)}>
        {dialog?.kind === 'assign' && <AssignForm item={dialog.item} onDone={() => setDialog(null)} />}
      </Modal>
      <Modal open={dialog?.kind === 'condition'} title={`Status of ${dialog?.item?.code ?? ''}`} onClose={() => setDialog(null)}>
        {dialog?.kind === 'condition' && <ConditionForm item={dialog.item} onDone={() => setDialog(null)} />}
      </Modal>
    </>
  );
}
