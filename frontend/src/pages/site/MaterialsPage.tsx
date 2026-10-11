import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, Power } from 'lucide-react';
import { materialsApi } from '../../api/materialsApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useLookup } from '../../hooks/useLookup';
import { useTableState } from '../../hooks/useTableState';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { formatMoney } from '../../lib/format';
import { emptyToNull } from '../../lib/params';
import { materialSchema, type MaterialFormValues } from '../../validation/siteSchemas';
import { Button } from '../../components/ui/Button';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Textarea } from '../../components/ui/Textarea';
import type { Material } from '../../types/material';

const FIELDS = ['name', 'category', 'unit', 'unitPrice', 'description'] as const;

function MaterialForm({ material, units, onDone }: { material: Material | null; units: { value: string; label: string }[]; onDone: () => void }) {
  const toast = useToast();
  const currency = useCurrency();
  const queryClient = useQueryClient();
  const categories = useLookup('materialCategories');
  const { register, handleSubmit, setError, formState: { errors, isSubmitting, submitCount } } = useForm<MaterialFormValues>({
    resolver: zodResolver(materialSchema),
    mode: 'onBlur',
    defaultValues: {
      name: material?.name ?? '',
      category: material?.category ?? '',
      unit: material?.unit ?? '',
      unitPrice: material ? Number(material.unitPrice).toFixed(2) : '',
      description: material?.description ?? '',
    },
  });
  const feedback = useFormFeedback(setError, FIELDS);

  const onSubmit = async (v: MaterialFormValues) => {
    feedback.clear();
    const payload = { ...v, description: emptyToNull(v.description) };
    try {
      const saved = material ? await materialsApi.update(material.id, payload) : await materialsApi.create(payload);
      await queryClient.invalidateQueries({ queryKey: ['materials'] });
      await queryClient.invalidateQueries({ queryKey: ['material-options'] });
      toast.success(`${saved.name} saved.`);
      onDone();
    } catch (error) {
      feedback.handleError(error);
    }
  };

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <ErrorSummary messages={summary} />
      <FormField label="Name" htmlFor="material-name" error={errors.name?.message} required>
        <Input id="material-name" invalid={!!errors.name} {...register('name')} />
      </FormField>
      <div className="grid gap-4 sm:grid-cols-3">
        <FormField label="Category" htmlFor="material-category" error={errors.category?.message} required>
          <Select id="material-category" options={categories.data ?? []} placeholder="Choose" invalid={!!errors.category} {...register('category')} />
        </FormField>
        <FormField label="Unit" htmlFor="material-unit" error={errors.unit?.message} required>
          <Select id="material-unit" options={units} placeholder="Choose" invalid={!!errors.unit} {...register('unit')} />
        </FormField>
        <FormField label={`Standard price (${currency})`} htmlFor="material-price" error={errors.unitPrice?.message} required>
          <Input id="material-price" inputMode="decimal" invalid={!!errors.unitPrice} {...register('unitPrice')} />
        </FormField>
      </div>
      <FormField label="Description" htmlFor="material-description" error={errors.description?.message}>
        <Textarea id="material-description" rows={2} maxLength={500} {...register('description')} />
      </FormField>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" loading={isSubmitting}>
          Save material
        </Button>
      </div>
    </form>
  );
}

/** Material catalogue; standard prices estimate the cost of material requests. */
export default function MaterialsPage() {
  const { hasPermission } = useAuth();
  const canCreate = hasPermission('MATERIAL:CREATE');
  const canEdit = hasPermission('MATERIAL:EDIT');
  const currency = useCurrency();
  const toast = useToast();
  const queryClient = useQueryClient();
  const table = useTableState('name');
  const [category, setCategory] = useState('');
  const [form, setForm] = useState<{ material: Material | null } | null>(null);
  const categories = useLookup('materialCategories');
  const units = useLookup('materialUnits');

  const materials = useQuery({
    queryKey: ['materials', table.params, category],
    queryFn: () => materialsApi.list(table.params, category || undefined),
    placeholderData: keepPreviousData,
  });

  const unitOptions = units.data ?? [];

  const toggle = useMutation({
    mutationFn: (m: Material) => materialsApi.setActive(m.id, !m.active),
    onSuccess: async (m) => {
      await queryClient.invalidateQueries({ queryKey: ['materials'] });
      toast.success(`${m.name} is now ${m.active ? 'active' : 'inactive'}.`);
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const columns: Column<Material>[] = [
    { key: 'name', header: 'Material', sortKey: 'name', render: (m) => (
      <div>
        <p className="font-medium text-ink">{m.name}</p>
        {m.description && <p className="text-xs text-muted">{m.description}</p>}
      </div>
    ) },
    { key: 'category', header: 'Category', sortKey: 'category', render: (m) => m.category },
    { key: 'unit', header: 'Unit', render: (m) => m.unit },
    { key: 'price', header: 'Standard price', sortKey: 'unitPrice', className: 'text-right whitespace-nowrap', render: (m) => formatMoney(m.unitPrice, currency) },
    { key: 'status', header: 'Status', render: (m) => <StatusBadge status={m.active ? 'ACTIVE' : 'INACTIVE'} /> },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (m) =>
        canEdit && (
          <div className="flex justify-end gap-1">
            <button type="button" onClick={() => setForm({ material: m })} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Edit ${m.name}`}>
              <Pencil className="h-4 w-4" aria-hidden />
            </button>
            <button type="button" onClick={() => toggle.mutate(m)} className={`rounded-md p-2 hover:bg-surface ${m.active ? 'text-red-600' : 'text-green-700'}`} aria-label={m.active ? `Deactivate ${m.name}` : `Activate ${m.name}`}>
              <Power className="h-4 w-4" aria-hidden />
            </button>
          </div>
        ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Materials"
        description="The catalogue site engineers request from."
        actions={
          canCreate && (
            <Button onClick={() => setForm({ material: null })}>
              <Plus className="h-4 w-4" aria-hidden />
              New material
            </Button>
          )
        }
      />
      <DataTable
        columns={columns}
        page={materials.data}
        rowKey={(m) => m.id}
        loading={materials.isFetching}
        error={materials.isError ? toApiError(materials.error).message : null}
        emptyMessage="No materials match these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Search materials" />
            <div className="sm:ml-auto">
              <Select aria-label="Filter by category" options={categories.data ?? []} placeholder="All categories" value={category} onChange={(e) => { setCategory(e.target.value); table.resetPage(); }} />
            </div>
          </>
        }
      />
      <Modal open={form !== null} title={form?.material ? 'Edit material' : 'New material'} onClose={() => setForm(null)}>
        {form && <MaterialForm material={form.material} units={unitOptions} onDone={() => setForm(null)} />}
      </Modal>
    </>
  );
}
