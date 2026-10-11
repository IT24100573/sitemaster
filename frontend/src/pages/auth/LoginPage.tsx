import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { applyServerFieldErrors, collectErrorMessages, toApiError } from '../../lib/apiError';
import { loginSchema, type LoginFormValues } from '../../validation/authSchemas';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input, PasswordInput } from '../../components/ui/Input';

const FIELDS = ['email', 'password'] as const;

export default function LoginPage() {
  const { login } = useAuth();
  const toast = useToast();
  const location = useLocation();
  const resetSuccess = Boolean((location.state as { resetSuccess?: boolean } | null)?.resetSuccess);
  const [serverMessages, setServerMessages] = useState<string[]>([]);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting, submitCount },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    mode: 'onBlur',
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = async (values: LoginFormValues) => {
    setServerMessages([]);
    try {
      const user = await login(values);
      toast.success(`Welcome back, ${user.fullName}.`);
      // GuestOnly redirects to the dashboard as soon as the user is set.
    } catch (error) {
      const apiError = toApiError(error);
      const unmatched = applyServerFieldErrors(apiError, setError, FIELDS);
      const hasFieldErrors = Object.keys(apiError.fieldErrors).length > 0;
      setServerMessages(hasFieldErrors ? unmatched : [apiError.message]);
    }
  };

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...serverMessages] : [];

  return (
    <div className="card p-6 sm:p-8">
      <h2 className="text-2xl font-semibold tracking-tight text-ink">Sign in</h2>
      <p className="mt-1 text-sm text-muted">Use the email your administrator registered for you.</p>

      <div className="mt-6 space-y-4">
        {resetSuccess && <Alert tone="success">Your password has been changed. Sign in with the new password.</Alert>}
        <ErrorSummary messages={summary} />
      </div>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-6 space-y-5">
        <FormField label="Email" htmlFor="email" error={errors.email?.message} required>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="name@company.lk"
            invalid={Boolean(errors.email)}
            aria-describedby="email-message"
            {...register('email')}
          />
        </FormField>

        <FormField label="Password" htmlFor="password" error={errors.password?.message} required>
          <PasswordInput
            id="password"
            autoComplete="current-password"
            invalid={Boolean(errors.password)}
            aria-describedby="password-message"
            {...register('password')}
          />
        </FormField>

        <div className="flex justify-end">
          <Link to="/forgot-password" className="text-sm font-medium text-primary hover:text-primary-dark">
            Forgot password?
          </Link>
        </div>

        <Button type="submit" fullWidth loading={isSubmitting}>
          {isSubmitting ? 'Signing in' : 'Sign in'}
        </Button>
      </form>
    </div>
  );
}
