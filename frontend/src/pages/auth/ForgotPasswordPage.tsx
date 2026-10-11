import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft } from 'lucide-react';
import { requestPasswordReset } from '../../api/authApi';
import { useToast } from '../../context/ToastContext';
import { applyServerFieldErrors, collectErrorMessages, toApiError } from '../../lib/apiError';
import { forgotPasswordSchema, type ForgotPasswordFormValues } from '../../validation/authSchemas';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';

const FIELDS = ['email'] as const;

export default function ForgotPasswordPage() {
  const toast = useToast();
  const [sentMessage, setSentMessage] = useState<string | null>(null);
  const [serverMessages, setServerMessages] = useState<string[]>([]);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting, submitCount },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    mode: 'onBlur',
    defaultValues: { email: '' },
  });

  const onSubmit = async (values: ForgotPasswordFormValues) => {
    setServerMessages([]);
    try {
      const { message } = await requestPasswordReset(values);
      setSentMessage(message);
      toast.success('Reset link requested.');
    } catch (error) {
      const apiError = toApiError(error);
      const unmatched = applyServerFieldErrors(apiError, setError, FIELDS);
      setServerMessages(Object.keys(apiError.fieldErrors).length > 0 ? unmatched : [apiError.message]);
      toast.error(apiError.message);
    }
  };

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...serverMessages] : [];

  return (
    <div className="card p-6 sm:p-8">
      <h2 className="text-2xl font-semibold tracking-tight text-ink">Reset your password</h2>
      <p className="mt-1 text-sm text-muted">
        Enter your account email. If it matches an active account, we will send a link to set a new password.
      </p>

      {sentMessage ? (
        <div className="mt-6 space-y-6">
          <Alert tone="success">
            <p>{sentMessage}</p>
            <p className="mt-1 text-green-700">The link works once and expires after 30 minutes.</p>
          </Alert>
          <Link to="/login" className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:text-primary-dark">
            <ArrowLeft className="h-4 w-4" aria-hidden />
            Back to sign in
          </Link>
        </div>
      ) : (
        <>
          <div className="mt-6">
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

            <Button type="submit" fullWidth loading={isSubmitting}>
              Send reset link
            </Button>

            <Link to="/login" className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:text-primary-dark">
              <ArrowLeft className="h-4 w-4" aria-hidden />
              Back to sign in
            </Link>
          </form>
        </>
      )}
    </div>
  );
}
