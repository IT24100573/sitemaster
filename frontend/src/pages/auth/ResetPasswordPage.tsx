import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Check, Circle } from 'lucide-react';
import { resetPassword } from '../../api/authApi';
import { useToast } from '../../context/ToastContext';
import { applyServerFieldErrors, collectErrorMessages, toApiError } from '../../lib/apiError';
import {
  PASSWORD_RULES,
  resetPasswordSchema,
  type ResetPasswordFormValues,
} from '../../validation/authSchemas';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { PasswordInput } from '../../components/ui/Input';

const FIELDS = ['newPassword', 'confirmPassword'] as const;

function PasswordChecklist({ value }: { value: string }) {
  return (
    <ul className="mt-2 grid grid-cols-1 gap-1 sm:grid-cols-2">
      {PASSWORD_RULES.map((rule) => {
        const met = rule.test(value);
        return (
          <li key={rule.label} className={`flex items-center gap-1.5 text-xs ${met ? 'text-green-700' : 'text-muted'}`}>
            {met ? <Check className="h-3.5 w-3.5" aria-hidden /> : <Circle className="h-3 w-3" aria-hidden />}
            {rule.label}
          </li>
        );
      })}
    </ul>
  );
}

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const navigate = useNavigate();
  const toast = useToast();
  const [serverMessages, setServerMessages] = useState<string[]>([]);

  const {
    register,
    handleSubmit,
    setError,
    watch,
    formState: { errors, isSubmitting, submitCount },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    mode: 'onBlur',
    defaultValues: { newPassword: '', confirmPassword: '' },
  });

  const newPassword = watch('newPassword');

  const onSubmit = async (values: ResetPasswordFormValues) => {
    setServerMessages([]);
    try {
      const { message } = await resetPassword({ token, ...values });
      toast.success(message);
      navigate('/login', { replace: true, state: { resetSuccess: true } });
    } catch (error) {
      const apiError = toApiError(error);
      const unmatched = applyServerFieldErrors(apiError, setError, FIELDS);
      setServerMessages(Object.keys(apiError.fieldErrors).length > 0 ? unmatched : [apiError.message]);
      toast.error(apiError.message);
    }
  };

  if (!token) {
    return (
      <div className="card space-y-5 p-6 sm:p-8">
        <h2 className="text-2xl font-semibold tracking-tight text-ink">Link not valid</h2>
        <Alert tone="error">This page needs the link from your password reset message. Request a new link to continue.</Alert>
        <Link to="/forgot-password" className="text-sm font-medium text-primary hover:text-primary-dark">
          Request a new link
        </Link>
      </div>
    );
  }

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...serverMessages] : [];

  return (
    <div className="card p-6 sm:p-8">
      <h2 className="text-2xl font-semibold tracking-tight text-ink">Set a new password</h2>
      <p className="mt-1 text-sm text-muted">Choose a strong password that meets every rule below.</p>

      <div className="mt-6">
        <ErrorSummary messages={summary} />
      </div>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-6 space-y-5">
        <FormField
          label="New password"
          htmlFor="newPassword"
          error={errors.newPassword?.message}
          hint={<PasswordChecklist value={newPassword} />}
          required
        >
          <PasswordInput
            id="newPassword"
            autoComplete="new-password"
            invalid={Boolean(errors.newPassword)}
            aria-describedby="newPassword-message"
            {...register('newPassword')}
          />
        </FormField>

        <FormField label="Confirm new password" htmlFor="confirmPassword" error={errors.confirmPassword?.message} required>
          <PasswordInput
            id="confirmPassword"
            autoComplete="new-password"
            invalid={Boolean(errors.confirmPassword)}
            aria-describedby="confirmPassword-message"
            {...register('confirmPassword')}
          />
        </FormField>

        <Button type="submit" fullWidth loading={isSubmitting}>
          Change password
        </Button>
      </form>
    </div>
  );
}
