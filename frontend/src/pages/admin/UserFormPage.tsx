import { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft } from 'lucide-react';
import { usersApi } from '../../api/usersApi';
import { useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useLookup } from '../../hooks/useLookup';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { emptyToNull } from '../../lib/params';
import { userSchema, type UserFormValues } from '../../validation/userSchemas';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { PageHeader } from '../../components/ui/PageHeader';
import { Select } from '../../components/ui/Select';
import { FullPageSpinner } from '../../components/ui/Spinner';
import type { RoleName } from '../../types/auth';

const FIELDS = ['fullName', 'email', 'phone', 'role'] as const;

export default function UserFormPage() {
  const { id } = useParams();
  const userId = id ? Number(id) : null;
  const isEdit = userId !== null;
  const me = useCurrentUser();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const roles = useLookup('roles');

  const existing = useQuery({
    queryKey: ['users', 'detail', userId],
    queryFn: () => usersApi.get(userId as number),
    enabled: isEdit,
  });

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting, submitCount },
  } = useForm<UserFormValues>({
    resolver: zodResolver(userSchema),
    mode: 'onBlur',
    defaultValues: { fullName: '', email: '', phone: '', role: '' },
  });
  const feedback = useFormFeedback(setError, FIELDS);

  useEffect(() => {
    if (existing.data) {
      reset({
        fullName: existing.data.fullName,
        email: existing.data.email,
        phone: existing.data.phone ?? '',
        role: existing.data.role,
      });
    }
  }, [existing.data, reset]);

  const save = useMutation({
    mutationFn: (values: UserFormValues) => {
      const payload = {
        fullName: values.fullName,
        email: values.email,
        phone: emptyToNull(values.phone),
        role: values.role as RoleName,
      };
      return isEdit ? usersApi.update(userId as number, payload) : usersApi.create(payload);
    },
  });

  const onSubmit = async (values: UserFormValues) => {
    feedback.clear();
    try {
      const saved = await save.mutateAsync(values);
      await queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success(
        isEdit
          ? `${saved.fullName} was updated.`
          : `${saved.fullName} was created. A set-password link was issued (see the server console on localhost).`,
      );
      navigate(isEdit ? '../..' : '..', { relative: 'path', replace: true });
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

  // Suppliers are created from supplier applications, not here.
  const roleOptions = (roles.data ?? []).filter(
    (role) => role.value !== 'SUPPLIER' || existing.data?.role === 'SUPPLIER',
  );
  const editingSelf = isEdit && userId === me.id;
  const roleLocked = editingSelf || existing.data?.role === 'SUPPLIER';
  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <>
      <Link to={isEdit ? '../..' : '..'} relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        Back to users
      </Link>
      <PageHeader
        title={isEdit ? `Edit ${existing.data?.fullName ?? 'user'}` : 'New user'}
        description={
          isEdit
            ? 'Changes to the role apply to the user straight away.'
            : 'The new user receives a one-time link to set their own password.'
        }
      />

      <Card className="max-w-2xl">
        <div className="mb-5">
          <ErrorSummary messages={summary} />
        </div>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
          <FormField label="Full name" htmlFor="fullName" error={errors.fullName?.message} required>
            <Input id="fullName" invalid={!!errors.fullName} {...register('fullName')} />
          </FormField>
          <FormField label="Email" htmlFor="email" error={errors.email?.message} required>
            <Input id="email" type="email" autoComplete="off" invalid={!!errors.email} {...register('email')} />
          </FormField>
          <FormField label="Phone" htmlFor="phone" error={errors.phone?.message} hint="0XXXXXXXXX or +94XXXXXXXXX">
            <Input id="phone" type="tel" invalid={!!errors.phone} {...register('phone')} />
          </FormField>
          <FormField
            label="Role"
            htmlFor="role"
            error={errors.role?.message}
            hint={
              editingSelf
                ? 'You cannot change your own role.'
                : roleLocked
                  ? 'Supplier accounts keep the supplier role.'
                  : undefined
            }
            required
          >
            {roleLocked ? (
              <>
                {/* Disabled controls are not submitted, so the value travels in a hidden field. */}
                <input type="hidden" {...register('role')} />
                <Select id="role" options={roleOptions} value={existing.data?.role ?? ''} disabled onChange={() => undefined} />
              </>
            ) : (
              <Select
                id="role"
                options={roleOptions}
                placeholder="Choose a role"
                invalid={!!errors.role}
                {...register('role')}
              />
            )}
          </FormField>
          <div className="flex gap-2 pt-2">
            <Button type="submit" loading={isSubmitting}>
              {isEdit ? 'Save changes' : 'Create user'}
            </Button>
            <Button type="button" variant="secondary" onClick={() => navigate(isEdit ? '../..' : '..', { relative: 'path' })}>
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </>
  );
}
