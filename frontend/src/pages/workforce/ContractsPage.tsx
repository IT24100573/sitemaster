import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, Pencil, Plus } from 'lucide-react';
import { hrRecordsApi } from '../../api/workforceApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useTableState } from '../../hooks/useTableState';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { formatDate, formatMoney } from '../../lib/format';
import { emptyToNull } from '../../lib/params';
import { contractSchema, type ContractFormValues } from '../../validation/workforceSchemas';
import { Button } from '../../components/ui/Button';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { EmployeeSelect } from '../../components/ui/EmployeeSelect';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FileUpload } from '../../components/ui/FileUpload';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Textarea } from '../../components/ui/Textarea';
import type { Contract } from '../../types/workforce';

const FIELDS = ['employeeId', 'contractNo', 'title', 'startDate', 'endDate', 'contractValue', 'notes'] as const;

function ContractForm({ contract, onDone }: { contract: Contract | null; onDone: () => void }) {
  const toast = useToast();
  const currency = useCurrency();
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const { register, handleSubmit, setError, formState: { errors, isSubmitting, submitCount } } = useForm<ContractFormValues>({
    resolver: zodResolver(contractSchema),
    mode: 'onBlur',
    defaultValues: {
      employeeId: contract ? String(contract.employeeId) : '', contractNo: contract?.contractNo ?? '', title: contract?.title ?? '',
      startDate: contract?.startDate ?? '', endDate: contract?.endDate ?? '',
      contractValue: contract?.contractValue != null ? Number(contract.contractValue).toFixed(2) : '', notes: contract?.notes ?? '',
    },
  });
  const feedback = useFormFeedback(setError, FIELDS);
  const onSubmit = async (v: ContractFormValues) => {
    feedback.clear();
    try {
      const saved = await hrRecordsApi.saveContract(contract?.id ?? null, {
        employeeId: Number(v.employeeId), contractNo: v.contractNo, title: v.title, startDate: v.startDate, endDate: v.endDate,
        contractValue: emptyToNull(v.contractValue), notes: emptyToNull(v.notes),
      });
      if (file) {
        try {
          await hrRecordsApi.uploadContractFile(saved.id, file);
        } catch (error) {
          toast.error(`Contract saved, but the file was not uploaded: ${toApiError(error).message}`);
        }
      }
      await queryClient.invalidateQueries({ queryKey: ['contracts'] });
      toast.success(`Contract ${saved.contractNo} saved.`);
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
        <FormField label="Employee" htmlFor="c-employee" error={errors.employeeId?.message} required>
          <EmployeeSelect id="c-employee" placeholder="Choose an employee" invalid={!!errors.employeeId} {...register('employeeId')} />
        </FormField>
        <FormField label="Contract number" htmlFor="c-no" error={errors.contractNo?.message} required>
          <Input id="c-no" className="uppercase" invalid={!!errors.contractNo} {...register('contractNo')} />
        </FormField>
        <div className="sm:col-span-2">
          <FormField label="Title" htmlFor="c-title" error={errors.title?.message} required>
            <Input id="c-title" invalid={!!errors.title} {...register('title')} />
          </FormField>
        </div>
        <FormField label="Start date" htmlFor="c-start" error={errors.startDate?.message} required>
          <Input id="c-start" type="date" invalid={!!errors.startDate} {...register('startDate')} />
        </FormField>
        <FormField label="End date" htmlFor="c-end" error={errors.endDate?.message} required>
          <Input id="c-end" type="date" invalid={!!errors.endDate} {...register('endDate')} />
        </FormField>
        <FormField label={`Contract value (${currency})`} htmlFor="c-value" error={errors.contractValue?.message}>
          <Input id="c-value" inputMode="decimal" invalid={!!errors.contractValue} {...register('contractValue')} />
        </FormField>
      </div>
      <FormField label="Notes" htmlFor="c-notes" error={errors.notes?.message}>
        <Textarea id="c-notes" rows={2} maxLength={500} {...register('notes')} />
      </FormField>
      <FileUpload kind="document" file={file} onChange={setFile} label={contract?.fileName ? `Replace ${contract.fileName}` : 'Attach the signed contract (optional)'} />
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={isSubmitting}>Save contract</Button>
      </div>
    </form>
  );
}

/** HR: employment contracts with renewal warnings 30 days ahead. */
export default function ContractsPage() {
  const { hasPermission } = useAuth();
  const canEdit = hasPermission('EMPLOYEE:EDIT');
  const currency = useCurrency();
  const table = useTableState('endDate', 'asc');
  const [employeeId, setEmployeeId] = useState('');
  const [expiring, setExpiring] = useState('');
  const [form, setForm] = useState<{ contract: Contract | null } | null>(null);

  const contracts = useQuery({
    queryKey: ['contracts', table.params, employeeId, expiring],
    queryFn: () => hrRecordsApi.contracts(table.params, { employeeId: employeeId || undefined, expiringWithinDays: expiring || undefined }),
    placeholderData: keepPreviousData,
  });

  const columns: Column<Contract>[] = [
    { key: 'no', header: 'Contract', sortKey: 'contractNo', render: (c) => <div><p className="font-medium text-ink">{c.contractNo}</p><p className="text-xs text-muted">{c.title}</p></div> },
    { key: 'employee', header: 'Employee', render: (c) => `${c.employeeName} (${c.employeeNo})` },
    { key: 'period', header: 'Period', sortKey: 'endDate', className: 'whitespace-nowrap', render: (c) => `${formatDate(c.startDate)} to ${formatDate(c.endDate)}` },
    { key: 'value', header: 'Value', className: 'text-right whitespace-nowrap', render: (c) => (c.contractValue != null ? formatMoney(c.contractValue, currency) : '-') },
    {
      key: 'status',
      header: 'Status',
      render: (c) => (
        <span className="flex flex-wrap items-center gap-1">
          <StatusBadge status={c.status} />
          {c.expiringSoon && <StatusBadge status="PENDING" label={`Ends in ${c.daysToExpiry} day(s)`} />}
        </span>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (c) => (
        <div className="flex justify-end gap-1">
          {c.fileName && (
            <a href={hrRecordsApi.contractFileUrl(c.id)} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Download ${c.contractNo}`} title="Download">
              <Download className="h-4 w-4" aria-hidden />
            </a>
          )}
          {canEdit && (
            <button type="button" onClick={() => setForm({ contract: c })} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Edit ${c.contractNo}`}>
              <Pencil className="h-4 w-4" aria-hidden />
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader title="Contracts" description="HR is notified 30 days before a contract ends."
        actions={canEdit && <Button onClick={() => setForm({ contract: null })}><Plus className="h-4 w-4" aria-hidden />New contract</Button>} />
      <DataTable
        columns={columns}
        page={contracts.data}
        rowKey={(c) => c.id}
        loading={contracts.isFetching}
        error={contracts.isError ? toApiError(contracts.error).message : null}
        emptyMessage="No contracts match these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Contract or employee" />
            <div className="flex flex-wrap gap-2 sm:ml-auto">
              <EmployeeSelect aria-label="Filter by employee" value={employeeId} onChange={(e) => { setEmployeeId(e.target.value); table.resetPage(); }} />
              <Select aria-label="Filter by expiry" options={[{ value: '30', label: 'Ending in 30 days' }, { value: '90', label: 'Ending in 90 days' }]}
                placeholder="Any end date" value={expiring} onChange={(e) => { setExpiring(e.target.value); table.resetPage(); }} />
            </div>
          </>
        }
      />
      <Modal open={form !== null} size="lg" title={form?.contract ? `Edit ${form.contract.contractNo}` : 'New contract'} onClose={() => setForm(null)}>
        {form && <ContractForm contract={form.contract} onDone={() => setForm(null)} />}
      </Modal>
    </>
  );
}
