import { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft } from 'lucide-react';
import { employeesApi } from '../../api/employeesApi';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useLookup } from '../../hooks/useLookup';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { emptyToNull } from '../../lib/params';
import { todayIso } from '../../validation/common';
import { employeeSchema, type EmployeeFormValues } from '../../validation/employeeSchemas';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { PageHeader } from '../../components/ui/PageHeader';
import { Select } from '../../components/ui/Select';
import { FullPageSpinner } from '../../components/ui/Spinner';
import type { EmployeePayload, EmployeeRecord, EmploymentType } from '../../types/employee';

const FIELDS = [
  'existingUserId', 'email', 'fullName', 'phone', 'nic', 'dateOfBirth', 'address', 'joinDate', 'employmentType',
  'trade', 'dailyRate', 'monthlySalary', 'contractAmount', 'contractStartDate', 'contractEndDate',
  'emergencyContactName', 'emergencyContactPhone',
] as const;

const EMPTY: EmployeeFormValues = {
  accountMode: 'new',
  existingUserId: '',
  email: '',
  fullName: '',
  phone: '',
  nic: '',
  dateOfBirth: '',
  address: '',
  joinDate: '',
  employmentType: '',
  trade: '',
  dailyRate: '',
  monthlySalary: '',
  contractAmount: '',
  contractStartDate: '',
  contractEndDate: '',
  emergencyContactName: '',
  emergencyContactPhone: '',
  isWorker: true,
  isEdit: false,
};

function money(value: number | null): string {
  return value === null ? '' : Number(value).toFixed(2);
}

function toForm(e: EmployeeRecord): EmployeeFormValues {
  return {
    ...EMPTY,
    accountMode: 'existing',
    existingUserId: String(e.userId),
    email: e.email,
    fullName: e.fullName,
    phone: e.phone ?? '',
    nic: e.nic,
    dateOfBirth: e.dateOfBirth,
    address: e.address,
    joinDate: e.joinDate,
    employmentType: e.employmentType,
    trade: e.trade ?? '',
    dailyRate: money(e.dailyRate),
    monthlySalary: money(e.monthlySalary),
    contractAmount: money(e.contractAmount),
    contractStartDate: e.contractStartDate ?? '',
    contractEndDate: e.contractEndDate ?? '',
    emergencyContactName: e.emergencyContactName ?? '',
    emergencyContactPhone: e.emergencyContactPhone ?? '',
    isWorker: e.role === 'WORKER',
    isEdit: true,
  };
}

/** HR registers a worker (new account) or adds an employee record to existing staff. */
export default function EmployeeFormPage() {
  const { id } = useParams();
  const employeeId = id ? Number(id) : null;
  const isEdit = employeeId !== null;
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const currency = useCurrency();
  const types = useLookup('employmentTypes');
  const trades = useLookup('trades');
  const staff = useLookup('unlinkedStaff', !isEdit);

  const existing = useQuery({
    queryKey: ['employees', 'detail', employeeId],
    queryFn: () => employeesApi.get(employeeId as number),
    enabled: isEdit,
  });

  const {
    register,
    handleSubmit,
    setError,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting, submitCount },
  } = useForm<EmployeeFormValues>({ resolver: zodResolver(employeeSchema), mode: 'onBlur', defaultValues: EMPTY });
  const feedback = useFormFeedback(setError, FIELDS);

  useEffect(() => {
    if (existing.data) {
      reset(toForm(existing.data));
    }
  }, [existing.data, reset]);

  const accountMode = watch('accountMode');
  const employmentType = watch('employmentType') as EmploymentType | '';
  const existingUserId = watch('existingUserId');

  // Workers created here always get the WORKER role; for linked accounts the server checks the role.
  useEffect(() => {
    if (!isEdit) {
      setValue('isWorker', accountMode === 'new');
    }
  }, [accountMode, isEdit, setValue]);

  // Pre-fill the name from the chosen account so HR only confirms it.
  useEffect(() => {
    if (!isEdit && accountMode === 'existing' && existingUserId) {
      const option = staff.data?.find((o) => o.value === existingUserId);
      if (option) {
        setValue('fullName', option.label.split(' (')[0], { shouldValidate: false });
      }
    }
  }, [existingUserId, accountMode, isEdit, staff.data, setValue]);

  const onSubmit = async (v: EmployeeFormValues) => {
    feedback.clear();
    const payload: EmployeePayload = {
      fullName: v.fullName,
      phone: v.phone,
      nic: v.nic.toUpperCase(),
      dateOfBirth: v.dateOfBirth,
      address: v.address,
      joinDate: v.joinDate,
      employmentType: v.employmentType as EmploymentType,
      trade: emptyToNull(v.trade),
      dailyRate: v.employmentType === 'DAILY_WAGE' ? emptyToNull(v.dailyRate) : null,
      monthlySalary: v.employmentType === 'PERMANENT' ? emptyToNull(v.monthlySalary) : null,
      contractAmount: v.employmentType === 'CONTRACT' ? emptyToNull(v.contractAmount) : null,
      contractStartDate: v.employmentType === 'CONTRACT' ? emptyToNull(v.contractStartDate) : null,
      contractEndDate: v.employmentType === 'CONTRACT' ? emptyToNull(v.contractEndDate) : null,
      emergencyContactName: emptyToNull(v.emergencyContactName),
      emergencyContactPhone: emptyToNull(v.emergencyContactPhone),
    };
    if (!isEdit) {
      payload.existingUserId = v.accountMode === 'existing' ? Number(v.existingUserId) : null;
      payload.email = v.accountMode === 'new' ? v.email : null;
    }
    try {
      const saved = isEdit
        ? await employeesApi.update(employeeId as number, payload)
        : await employeesApi.create(payload);
      await queryClient.invalidateQueries({ queryKey: ['employees'] });
      await queryClient.invalidateQueries({ queryKey: ['lookup', 'unlinkedStaff'] });
      toast.success(
        isEdit
          ? `${saved.employeeNo} was updated.`
          : v.accountMode === 'new'
            ? `${saved.fullName} registered as ${saved.employeeNo}. A set-password link was issued.`
            : `${saved.fullName} registered as ${saved.employeeNo}.`,
      );
      navigate(isEdit ? '..' : `../${saved.id}`, { relative: 'path', replace: true });
    } catch (error) {
      feedback.handleError(error);
    }
  };

  if (isEdit && existing.isLoading) {
    return <FullPageSpinner />;
  }
  if (isEdit && existing.isError) {
    return <Alert tone="error">{toApiError(existing.error).message}</Alert>;
  }

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];
  const field = (name: (typeof FIELDS)[number]) => ({ invalid: !!errors[name], id: name });

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        {isEdit ? 'Back to employee' : 'Back to employees'}
      </Link>
      <PageHeader
        title={isEdit ? `Edit ${existing.data?.employeeNo ?? 'employee'}` : 'Register employee'}
        description={isEdit ? existing.data?.fullName : 'Create a worker account or add an employee record for existing staff.'}
      />

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="max-w-4xl space-y-6">
        <ErrorSummary messages={summary} />

        {!isEdit && (
          <Card title="Sign-in account">
            <div className="mb-5 flex flex-wrap gap-4 text-sm">
              <label className="flex items-center gap-2">
                <input type="radio" value="new" className="accent-primary" {...register('accountMode')} />
                New worker account
              </label>
              <label className="flex items-center gap-2">
                <input type="radio" value="existing" className="accent-primary" {...register('accountMode')} />
                Existing staff account
              </label>
            </div>
            {accountMode === 'new' ? (
              <FormField
                label="Email"
                htmlFor="email"
                error={errors.email?.message}
                hint="The worker receives a link to set their password."
                required
              >
                <Input type="email" autoComplete="off" {...field('email')} {...register('email')} />
              </FormField>
            ) : (
              <FormField
                label="Staff account"
                htmlFor="existingUserId"
                error={errors.existingUserId?.message}
                hint={staff.data && staff.data.length === 0 ? 'Every active staff account already has an employee record.' : 'Accounts are created by the Administrator.'}
                required
              >
                <Select options={staff.data ?? []} placeholder="Choose an account" {...field('existingUserId')} {...register('existingUserId')} />
              </FormField>
            )}
          </Card>
        )}

        <Card title="Personal details">
          <div className="grid gap-5 md:grid-cols-2">
            <FormField label="Full name" htmlFor="fullName" error={errors.fullName?.message} required>
              <Input {...field('fullName')} {...register('fullName')} />
            </FormField>
            <FormField label="Phone" htmlFor="phone" error={errors.phone?.message} hint="0XXXXXXXXX or +94XXXXXXXXX" required>
              <Input type="tel" {...field('phone')} {...register('phone')} />
            </FormField>
            <FormField label="NIC" htmlFor="nic" error={errors.nic?.message} hint="9 digits + V/X, or 12 digits" required>
              <Input className="uppercase" maxLength={12} {...field('nic')} {...register('nic')} />
            </FormField>
            <FormField label="Date of birth" htmlFor="dateOfBirth" error={errors.dateOfBirth?.message} hint="Age must be 18-65." required>
              <Input type="date" max={todayIso()} {...field('dateOfBirth')} {...register('dateOfBirth')} />
            </FormField>
            <div className="md:col-span-2">
              <FormField label="Address" htmlFor="address" error={errors.address?.message} required>
                <Input maxLength={255} {...field('address')} {...register('address')} />
              </FormField>
            </div>
            <FormField label="Emergency contact name" htmlFor="emergencyContactName" error={errors.emergencyContactName?.message}>
              <Input {...field('emergencyContactName')} {...register('emergencyContactName')} />
            </FormField>
            <FormField label="Emergency contact phone" htmlFor="emergencyContactPhone" error={errors.emergencyContactPhone?.message}>
              <Input type="tel" {...field('emergencyContactPhone')} {...register('emergencyContactPhone')} />
            </FormField>
          </div>
        </Card>

        <Card title="Employment">
          <div className="grid gap-5 md:grid-cols-2">
            <FormField label="Join date" htmlFor="joinDate" error={errors.joinDate?.message} required>
              <Input type="date" max={todayIso()} {...field('joinDate')} {...register('joinDate')} />
            </FormField>
            <FormField label="Trade" htmlFor="trade" error={errors.trade?.message} required={watch('isWorker')}>
              <Select options={trades.data ?? []} placeholder="No trade" {...field('trade')} {...register('trade')} />
            </FormField>
            <FormField label="Employment type" htmlFor="employmentType" error={errors.employmentType?.message} required>
              <Select options={types.data ?? []} placeholder="Choose a type" {...field('employmentType')} {...register('employmentType')} />
            </FormField>

            {employmentType === 'DAILY_WAGE' && (
              <FormField label={`Daily rate (${currency})`} htmlFor="dailyRate" error={errors.dailyRate?.message} required>
                <Input inputMode="decimal" {...field('dailyRate')} {...register('dailyRate')} />
              </FormField>
            )}
            {employmentType === 'PERMANENT' && (
              <FormField label={`Monthly salary (${currency})`} htmlFor="monthlySalary" error={errors.monthlySalary?.message} required>
                <Input inputMode="decimal" {...field('monthlySalary')} {...register('monthlySalary')} />
              </FormField>
            )}
            {employmentType === 'CONTRACT' && (
              <>
                <FormField label={`Contract amount (${currency})`} htmlFor="contractAmount" error={errors.contractAmount?.message} required>
                  <Input inputMode="decimal" {...field('contractAmount')} {...register('contractAmount')} />
                </FormField>
                <FormField label="Contract start" htmlFor="contractStartDate" error={errors.contractStartDate?.message} required>
                  <Input type="date" {...field('contractStartDate')} {...register('contractStartDate')} />
                </FormField>
                <FormField label="Contract end" htmlFor="contractEndDate" error={errors.contractEndDate?.message} required>
                  <Input type="date" {...field('contractEndDate')} {...register('contractEndDate')} />
                </FormField>
              </>
            )}
          </div>
        </Card>

        <div className="flex gap-2">
          <Button type="submit" loading={isSubmitting}>
            {isEdit ? 'Save changes' : 'Register employee'}
          </Button>
          <Button type="button" variant="secondary" onClick={() => navigate('..', { relative: 'path' })}>
            Cancel
          </Button>
        </div>
      </form>
    </>
  );
}
