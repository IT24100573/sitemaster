import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { companyApi, settingsApi } from '../../api/settingsApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { COMPANY_PROFILE_KEY } from '../../hooks/useCompanyProfile';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { formatDate } from '../../lib/format';
import { settingsSchema, type SettingsFormValues } from '../../validation/settingsSchemas';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FileUpload } from '../../components/ui/FileUpload';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { ListEditor } from '../../components/ui/ListEditor';
import { PageHeader } from '../../components/ui/PageHeader';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { Textarea } from '../../components/ui/Textarea';
import type { SystemSettings } from '../../types/settings';

const SETTINGS_KEY = ['settings'] as const;

const FIELDS = [
  'companyName', 'companyDescription', 'companyAddress', 'companyPhone', 'companyEmail', 'currency',
  'receiptThreshold', 'workStartTime', 'workEndTime', 'holidays', 'leaveAnnualDays', 'leaveCasualDays',
  'leaveSickDays', 'epfEmployeeRate', 'epfEmployerRate', 'etfRate', 'overtimeMultiplier', 'siteAllowancePerDay', 'trades',
  'materialCategories', 'materialUnits',
] as const;

function toForm(s: SystemSettings): SettingsFormValues {
  const decimal = (n: number) => Number(n).toFixed(2);
  return {
    companyName: s.companyName,
    companyDescription: s.companyDescription,
    companyAddress: s.companyAddress,
    companyPhone: s.companyPhone,
    companyEmail: s.companyEmail,
    currency: s.currency,
    receiptThreshold: decimal(s.receiptThreshold),
    workStartTime: s.workStartTime?.slice(0, 5) ?? '',
    workEndTime: s.workEndTime?.slice(0, 5) ?? '',
    holidays: s.holidays,
    leaveAnnualDays: String(s.leaveAnnualDays),
    leaveCasualDays: String(s.leaveCasualDays),
    leaveSickDays: String(s.leaveSickDays),
    epfEmployeeRate: decimal(s.epfEmployeeRate),
    epfEmployerRate: decimal(s.epfEmployerRate),
    etfRate: decimal(s.etfRate),
    overtimeMultiplier: decimal(s.overtimeMultiplier),
    siteAllowancePerDay: decimal(s.siteAllowancePerDay),
    trades: s.trades,
    materialCategories: s.materialCategories,
    materialUnits: s.materialUnits,
  };
}

/** Admin edits company details and every business value the app uses. */
export default function SettingsPage() {
  const { hasPermission } = useAuth();
  const canEdit = hasPermission('SETTINGS:EDIT');
  const toast = useToast();
  const queryClient = useQueryClient();
  const settings = useQuery({ queryKey: SETTINGS_KEY, queryFn: settingsApi.get });
  const [logo, setLogo] = useState<File | null>(null);
  const [confirmRemoveLogo, setConfirmRemoveLogo] = useState(false);

  const {
    register,
    control,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting, submitCount, isDirty },
  } = useForm<SettingsFormValues>({ resolver: zodResolver(settingsSchema), mode: 'onBlur' });
  const feedback = useFormFeedback(setError, FIELDS);

  useEffect(() => {
    if (settings.data) {
      reset(toForm(settings.data));
    }
  }, [settings.data, reset]);

  const afterSave = async (data: SystemSettings) => {
    queryClient.setQueryData(SETTINGS_KEY, data);
    await queryClient.invalidateQueries({ queryKey: COMPANY_PROFILE_KEY });
    await queryClient.invalidateQueries({ queryKey: ['lookup', 'trades'] });
    await queryClient.invalidateQueries({ queryKey: ['lookup', 'materialCategories'] });
    await queryClient.invalidateQueries({ queryKey: ['lookup', 'materialUnits'] });
  };

  const onSubmit = async (values: SettingsFormValues) => {
    feedback.clear();
    try {
      const data = await settingsApi.update({
        ...values,
        leaveAnnualDays: Number(values.leaveAnnualDays),
        leaveCasualDays: Number(values.leaveCasualDays),
        leaveSickDays: Number(values.leaveSickDays),
      });
      await afterSave(data);
      toast.success('Settings saved.');
    } catch (error) {
      feedback.handleError(error);
    }
  };

  const uploadLogo = useMutation({
    mutationFn: (file: File) => settingsApi.uploadLogo(file),
    onSuccess: async (data) => {
      await afterSave(data);
      setLogo(null);
      toast.success('Logo updated.');
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const removeLogo = useMutation({
    mutationFn: settingsApi.removeLogo,
    onSuccess: async (data) => {
      await afterSave(data);
      setConfirmRemoveLogo(false);
      toast.success('Logo removed.');
    },
    onError: (error) => {
      setConfirmRemoveLogo(false);
      toast.error(toApiError(error).message);
    },
  });

  if (settings.isLoading) {
    return <FullPageSpinner />;
  }
  if (settings.isError || !settings.data) {
    return <Alert tone="error">{toApiError(settings.error).message}</Alert>;
  }

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];
  const uploadError = uploadLogo.error ? toApiError(uploadLogo.error).fieldErrors.file : undefined;

  return (
    <>
      <PageHeader title="System settings" description="Company details and the business values used across SiteMaster." />

      <Card title="Company logo" description="Shown in the sidebar and on public pages." className="mb-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
          <div className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-card border border-line bg-surface">
            {settings.data.hasLogo ? (
              <img src={companyApi.logoUrl(settings.dataUpdatedAt)} alt="Current logo" className="h-full w-full object-contain" />
            ) : (
              <span className="text-xs text-muted">No logo</span>
            )}
          </div>
          {canEdit && (
            <div className="flex-1 space-y-3">
              <FileUpload kind="image" file={logo} onChange={setLogo} label="Choose a logo" error={uploadError} />
              <div className="flex gap-2">
                <Button size="sm" disabled={!logo} loading={uploadLogo.isPending} onClick={() => logo && uploadLogo.mutate(logo)}>
                  Upload logo
                </Button>
                {settings.data.hasLogo && (
                  <Button size="sm" variant="secondary" onClick={() => setConfirmRemoveLogo(true)}>
                    Remove logo
                  </Button>
                )}
              </div>
            </div>
          )}
        </div>
      </Card>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-6">
        <ErrorSummary messages={summary} />
        <fieldset disabled={!canEdit} className="space-y-6">
          <Card title="Company profile" description="Appears on the About Us page and on documents.">
            <div className="grid gap-5 md:grid-cols-2">
              <FormField label="Company name" htmlFor="companyName" error={errors.companyName?.message} required>
                <Input id="companyName" invalid={!!errors.companyName} {...register('companyName')} />
              </FormField>
              <FormField label="Email" htmlFor="companyEmail" error={errors.companyEmail?.message} required>
                <Input id="companyEmail" type="email" invalid={!!errors.companyEmail} {...register('companyEmail')} />
              </FormField>
              <FormField label="Phone" htmlFor="companyPhone" error={errors.companyPhone?.message} required>
                <Input id="companyPhone" type="tel" invalid={!!errors.companyPhone} {...register('companyPhone')} />
              </FormField>
              <FormField label="Address" htmlFor="companyAddress" error={errors.companyAddress?.message} required>
                <Input id="companyAddress" invalid={!!errors.companyAddress} {...register('companyAddress')} />
              </FormField>
              <div className="md:col-span-2">
                <FormField label="Description" htmlFor="companyDescription" error={errors.companyDescription?.message}>
                  <Textarea id="companyDescription" maxLength={2000} invalid={!!errors.companyDescription} {...register('companyDescription')} />
                </FormField>
              </div>
            </div>
          </Card>

          <Card title="Money and working time">
            <div className="grid gap-5 md:grid-cols-2">
              <FormField label="Currency" htmlFor="currency" error={errors.currency?.message} hint="3-letter code, e.g. LKR" required>
                <Input id="currency" maxLength={3} className="uppercase" invalid={!!errors.currency} {...register('currency', { setValueAs: (v: string) => v.toUpperCase() })} />
              </FormField>
              <FormField
                label="Receipt required above"
                htmlFor="receiptThreshold"
                error={errors.receiptThreshold?.message}
                hint="Expenses above this amount must have a receipt."
                required
              >
                <Input id="receiptThreshold" inputMode="decimal" invalid={!!errors.receiptThreshold} {...register('receiptThreshold')} />
              </FormField>
              <FormField label="Working day starts" htmlFor="workStartTime" error={errors.workStartTime?.message} required>
                <Input id="workStartTime" type="time" invalid={!!errors.workStartTime} {...register('workStartTime')} />
              </FormField>
              <FormField label="Working day ends" htmlFor="workEndTime" error={errors.workEndTime?.message} required>
                <Input id="workEndTime" type="time" invalid={!!errors.workEndTime} {...register('workEndTime')} />
              </FormField>
              <div className="md:col-span-2">
                <FormField label="Public holidays" htmlFor="holidays" error={errors.holidays?.message}>
                  <Controller
                    control={control}
                    name="holidays"
                    render={({ field }) => (
                      <ListEditor
                        values={field.value ?? []}
                        onChange={field.onChange}
                        inputType="date"
                        addLabel="Holiday date"
                        format={formatDate}
                      />
                    )}
                  />
                </FormField>
              </div>
            </div>
          </Card>

          <Card title="Leave entitlements" description="Days per employee per year.">
            <div className="grid gap-5 md:grid-cols-3">
              <FormField label="Annual" htmlFor="leaveAnnualDays" error={errors.leaveAnnualDays?.message} required>
                <Input id="leaveAnnualDays" inputMode="numeric" invalid={!!errors.leaveAnnualDays} {...register('leaveAnnualDays')} />
              </FormField>
              <FormField label="Casual" htmlFor="leaveCasualDays" error={errors.leaveCasualDays?.message} required>
                <Input id="leaveCasualDays" inputMode="numeric" invalid={!!errors.leaveCasualDays} {...register('leaveCasualDays')} />
              </FormField>
              <FormField label="Sick" htmlFor="leaveSickDays" error={errors.leaveSickDays?.message} required>
                <Input id="leaveSickDays" inputMode="numeric" invalid={!!errors.leaveSickDays} {...register('leaveSickDays')} />
              </FormField>
            </div>
          </Card>

          <Card title="Payroll rates" description="Percentages used to calculate payslips.">
            <div className="grid gap-5 md:grid-cols-2">
              <FormField label="EPF employee (%)" htmlFor="epfEmployeeRate" error={errors.epfEmployeeRate?.message} required>
                <Input id="epfEmployeeRate" inputMode="decimal" invalid={!!errors.epfEmployeeRate} {...register('epfEmployeeRate')} />
              </FormField>
              <FormField label="EPF employer (%)" htmlFor="epfEmployerRate" error={errors.epfEmployerRate?.message} required>
                <Input id="epfEmployerRate" inputMode="decimal" invalid={!!errors.epfEmployerRate} {...register('epfEmployerRate')} />
              </FormField>
              <FormField label="ETF (%)" htmlFor="etfRate" error={errors.etfRate?.message} required>
                <Input id="etfRate" inputMode="decimal" invalid={!!errors.etfRate} {...register('etfRate')} />
              </FormField>
              <FormField
                label="Overtime multiplier"
                htmlFor="overtimeMultiplier"
                error={errors.overtimeMultiplier?.message}
                hint="1.50 pays one and a half times the hourly rate."
                required
              >
                <Input id="overtimeMultiplier" inputMode="decimal" invalid={!!errors.overtimeMultiplier} {...register('overtimeMultiplier')} />
              </FormField>
              <FormField
                label="Site allowance per day"
                htmlFor="siteAllowancePerDay"
                error={errors.siteAllowancePerDay?.message}
                hint="Added to payslips for each day worked (half for half days)."
                required
              >
                <Input id="siteAllowancePerDay" inputMode="decimal" invalid={!!errors.siteAllowancePerDay} {...register('siteAllowancePerDay')} />
              </FormField>
            </div>
          </Card>

          <Card title="Worker trades" description="Options HR can choose when registering a worker.">
            <FormField label="Trades" htmlFor="trades" error={errors.trades?.message ?? errors.trades?.root?.message}>
              <Controller
                control={control}
                name="trades"
                render={({ field }) => (
                  <ListEditor values={field.value ?? []} onChange={field.onChange} addLabel="Trade name" placeholder="e.g. Scaffolder" invalid={!!errors.trades} />
                )}
              />
            </FormField>
          </Card>

          <Card title="Material categories" description="Used by supplier applications and the material catalogue.">
            <FormField
              label="Categories"
              htmlFor="materialCategories"
              error={errors.materialCategories?.message ?? errors.materialCategories?.root?.message}
            >
              <Controller
                control={control}
                name="materialCategories"
                render={({ field }) => (
                  <ListEditor values={field.value ?? []} onChange={field.onChange} addLabel="Category name" placeholder="e.g. Glass" invalid={!!errors.materialCategories} />
                )}
              />
            </FormField>
          </Card>
          <Card title="Units of measure" description="Units used in the material catalogue.">
            <FormField label="Units" htmlFor="materialUnits" error={errors.materialUnits?.message ?? errors.materialUnits?.root?.message}>
              <Controller
                control={control}
                name="materialUnits"
                render={({ field }) => (
                  <ListEditor values={field.value ?? []} onChange={field.onChange} addLabel="Unit" placeholder="e.g. cube" maxLength={20} invalid={!!errors.materialUnits} />
                )}
              />
            </FormField>
          </Card>
        </fieldset>

        {canEdit && (
          <div className="flex gap-2">
            <Button type="submit" loading={isSubmitting}>
              Save settings
            </Button>
            <Button type="button" variant="secondary" disabled={!isDirty} onClick={() => reset(toForm(settings.data))}>
              Discard changes
            </Button>
          </div>
        )}
      </form>

      <ConfirmDialog
        open={confirmRemoveLogo}
        title="Remove the company logo?"
        message="The sidebar and public pages will show the default icon."
        confirmLabel="Remove logo"
        tone="danger"
        loading={removeLogo.isPending}
        onConfirm={() => removeLogo.mutate()}
        onCancel={() => setConfirmRemoveLogo(false)}
      />
    </>
  );
}
