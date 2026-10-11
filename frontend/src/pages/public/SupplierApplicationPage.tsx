import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery } from '@tanstack/react-query';
import { CheckCircle2 } from 'lucide-react';
import { publicApi } from '../../api/publicApi';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { CLIENT_MESSAGES } from '../../validation/messages';
import {
  supplierApplicationSchema,
  type SupplierApplicationFormValues,
} from '../../validation/projectSchemas';
import { Button } from '../../components/ui/Button';
import { CheckboxGroup } from '../../components/ui/CheckboxGroup';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FileUpload } from '../../components/ui/FileUpload';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';

const FIELDS = [
  'companyName', 'registrationNo', 'contactPerson', 'email', 'phone', 'address', 'materialCategories',
] as const;

/** Public form for companies that want to supply materials. */
export default function SupplierApplicationPage() {
  const categories = useQuery({ queryKey: ['public', 'categories'], queryFn: publicApi.materialCategories });
  const [document, setDocument] = useState<File | null>(null);
  const [documentError, setDocumentError] = useState<string | undefined>();
  const [submitted, setSubmitted] = useState<string | null>(null);

  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting, submitCount },
  } = useForm<SupplierApplicationFormValues>({
    resolver: zodResolver(supplierApplicationSchema),
    mode: 'onBlur',
    defaultValues: {
      companyName: '', registrationNo: '', contactPerson: '', email: '', phone: '', address: '', materialCategories: [],
    },
  });
  const feedback = useFormFeedback(setError, FIELDS);

  const onSubmit = async (values: SupplierApplicationFormValues) => {
    feedback.clear();
    setDocumentError(undefined);
    if (!document) {
      setDocumentError(CLIENT_MESSAGES.fileRequired);
      return;
    }
    try {
      const result = await publicApi.applyAsSupplier({ ...values, registrationDocument: document });
      setSubmitted(result.message);
      window.scrollTo({ top: 0 });
    } catch (error) {
      const apiError = toApiError(error);
      setDocumentError(apiError.fieldErrors.registrationDocument);
      feedback.handleError(error);
    }
  };

  if (submitted) {
    return (
      <section className="mx-auto max-w-2xl px-4 py-20 text-center sm:px-6">
        <CheckCircle2 className="mx-auto h-12 w-12 text-green-600" aria-hidden />
        <h1 className="mt-4 text-2xl font-semibold text-ink">Application received</h1>
        <p className="mt-2 text-muted">{submitted}</p>
      </section>
    );
  }

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <section className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-bold tracking-tight text-ink">Become a supplier</h1>
      <p className="mt-2 text-muted">
        Tell us about your company. Our team reviews every application; once approved you receive a sign-in to
        respond to purchase orders and track payments.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="card mt-8 space-y-5 p-6">
        <ErrorSummary messages={summary} />
        <div className="grid gap-5 md:grid-cols-2">
          <FormField label="Company name" htmlFor="companyName" error={errors.companyName?.message} required>
            <Input id="companyName" invalid={!!errors.companyName} {...register('companyName')} />
          </FormField>
          <FormField label="Business registration number" htmlFor="registrationNo" error={errors.registrationNo?.message} required>
            <Input id="registrationNo" className="uppercase" invalid={!!errors.registrationNo} {...register('registrationNo')} />
          </FormField>
          <FormField label="Contact person" htmlFor="contactPerson" error={errors.contactPerson?.message} required>
            <Input id="contactPerson" invalid={!!errors.contactPerson} {...register('contactPerson')} />
          </FormField>
          <FormField label="Email" htmlFor="email" error={errors.email?.message} required>
            <Input id="email" type="email" invalid={!!errors.email} {...register('email')} />
          </FormField>
          <FormField label="Phone" htmlFor="phone" error={errors.phone?.message} hint="0XXXXXXXXX or +94XXXXXXXXX" required>
            <Input id="phone" type="tel" invalid={!!errors.phone} {...register('phone')} />
          </FormField>
          <FormField label="Address" htmlFor="address" error={errors.address?.message} required>
            <Input id="address" invalid={!!errors.address} {...register('address')} />
          </FormField>
        </div>

        <div>
          <p id="materialCategories-label" className="mb-1.5 text-sm font-medium text-ink">
            Materials you supply<span className="ml-0.5 text-red-600" aria-hidden>*</span>
          </p>
          <Controller
            control={control}
            name="materialCategories"
            render={({ field }) => (
              <CheckboxGroup
                name="materialCategories"
                options={categories.data ?? []}
                value={field.value}
                onChange={field.onChange}
                invalid={!!errors.materialCategories}
              />
            )}
          />
          {errors.materialCategories?.message && (
            <p className="mt-1.5 text-sm text-red-600">{errors.materialCategories.message}</p>
          )}
        </div>

        <div>
          <p className="mb-1.5 text-sm font-medium text-ink">
            Business registration certificate<span className="ml-0.5 text-red-600" aria-hidden>*</span>
          </p>
          <FileUpload
            kind="document"
            file={document}
            onChange={(file) => {
              setDocument(file);
              setDocumentError(undefined);
            }}
            error={documentError}
          />
        </div>

        <Button type="submit" loading={isSubmitting}>
          Submit application
        </Button>
      </form>
    </section>
  );
}
