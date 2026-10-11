import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { expensesApi } from '../../api/financeApi';
import { projectsApi } from '../../api/projectsApi';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useEnumLookup } from '../../hooks/useLookup';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { todayIso } from '../../validation/common';
import { expenseSchema, type ExpenseFormValues } from '../../validation/financeSchemas';
import { Button } from '../../components/ui/Button';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FileUpload } from '../../components/ui/FileUpload';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { Select } from '../../components/ui/Select';
import { Textarea } from '../../components/ui/Textarea';
import type { Expense } from '../../types/finance';

const FIELDS = ['projectId', 'phaseId', 'category', 'amount', 'expenseDate', 'payee', 'description'] as const;

/** New expense with an optional receipt (required above the receipt threshold). */
export function ExpenseForm({ onDone }: { onDone: (saved: Expense | null) => void }) {
  const toast = useToast();
  const currency = useCurrency();
  const queryClient = useQueryClient();
  const categories = useEnumLookup('expense-categories');
  const [receipt, setReceipt] = useState<File | null>(null);
  const [receiptError, setReceiptError] = useState<string | undefined>();
  const { register, handleSubmit, setError, watch, formState: { errors, isSubmitting, submitCount } } = useForm<ExpenseFormValues>({
    resolver: zodResolver(expenseSchema),
    mode: 'onBlur',
    defaultValues: { projectId: '', phaseId: '', category: '', amount: '', expenseDate: todayIso(), payee: '', description: '' },
  });
  const feedback = useFormFeedback(setError, FIELDS);
  const projectId = watch('projectId');
  const phases = useQuery({
    queryKey: ['projects', Number(projectId), 'phases'],
    queryFn: () => projectsApi.phases(Number(projectId)),
    enabled: projectId !== '',
  });

  const onSubmit = async (v: ExpenseFormValues) => {
    feedback.clear();
    setReceiptError(undefined);
    try {
      const saved = await expensesApi.create(Number(v.projectId), { ...v, receipt });
      await queryClient.invalidateQueries({ queryKey: ['expenses'] });
      toast.success('Expense submitted. The project manager has been notified.');
      onDone(saved);
    } catch (error) {
      setReceiptError(toApiError(error).fieldErrors.receipt);
      feedback.handleError(error);
    }
  };
  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <ErrorSummary messages={summary} />
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Project" htmlFor="ex-project" error={errors.projectId?.message} required>
          <ProjectSelect id="ex-project" activeOnly placeholder="Choose a project" invalid={!!errors.projectId} {...register('projectId')} />
        </FormField>
        <FormField label="Phase" htmlFor="ex-phase" error={errors.phaseId?.message} required>
          <Select id="ex-phase" options={(phases.data ?? []).filter((p) => p.status !== 'COMPLETED').map((p) => ({ value: String(p.id), label: p.name }))}
            placeholder={projectId ? 'Choose a phase' : 'Choose a project first'} invalid={!!errors.phaseId} {...register('phaseId')} />
        </FormField>
        <FormField label="Category" htmlFor="ex-category" error={errors.category?.message} required>
          <Select id="ex-category" options={categories.data ?? []} placeholder="Choose" invalid={!!errors.category} {...register('category')} />
        </FormField>
        <FormField label={`Amount (${currency})`} htmlFor="ex-amount" error={errors.amount?.message} required>
          <Input id="ex-amount" inputMode="decimal" invalid={!!errors.amount} {...register('amount')} />
        </FormField>
        <FormField label="Date" htmlFor="ex-date" error={errors.expenseDate?.message} required>
          <Input id="ex-date" type="date" max={todayIso()} invalid={!!errors.expenseDate} {...register('expenseDate')} />
        </FormField>
        <FormField label="Paid to" htmlFor="ex-payee" error={errors.payee?.message} required>
          <Input id="ex-payee" invalid={!!errors.payee} {...register('payee')} />
        </FormField>
      </div>
      <FormField label="What was it for" htmlFor="ex-description" error={errors.description?.message} required>
        <Textarea id="ex-description" rows={3} maxLength={500} invalid={!!errors.description} {...register('description')} />
      </FormField>
      <FileUpload kind="document" file={receipt} onChange={(f) => { setReceipt(f); setReceiptError(undefined); }}
        label="Attach the receipt (required above the receipt threshold)" error={receiptError} />
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={() => onDone(null)}>Cancel</Button>
        <Button type="submit" loading={isSubmitting}>Submit expense</Button>
      </div>
    </form>
  );
}
