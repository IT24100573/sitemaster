import { useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { supplierPortalApi } from '../../api/purchasingApi';
import { useToast } from '../../context/ToastContext';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useLookup } from '../../hooks/useLookup';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { personName, requiredPhone } from '../../validation/common';
import { CLIENT_MESSAGES as MSG } from '../../validation/messages';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { CheckboxGroup } from '../../components/ui/CheckboxGroup';
import { DetailList } from '../../components/ui/DetailList';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { PageHeader } from '../../components/ui/PageHeader';
import { FullPageSpinner } from '../../components/ui/Spinner';

const schema = z.object({
  contactPerson: personName,
  phone: requiredPhone,
  address: z.string().trim().min(1, MSG.required).max(255, MSG.addressSize),
  materialCategories: z.array(z.string()).min(1, MSG.categoriesRequired),
});
type Values = z.infer<typeof schema>;
const FIELDS = ['contactPerson', 'phone', 'address', 'materialCategories'] as const;

/** Supplier keeps its contact details and material categories up to date. */
export default function CompanyProfilePage() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const categories = useLookup('materialCategories');
  const profile = useQuery({ queryKey: ['supplier-profile'], queryFn: supplierPortalApi.profile });
  const { register, control, handleSubmit, setError, reset, formState: { errors, isSubmitting, submitCount } } = useForm<Values>({
    resolver: zodResolver(schema),
    mode: 'onBlur',
    defaultValues: { contactPerson: '', phone: '', address: '', materialCategories: [] },
  });
  const feedback = useFormFeedback(setError, FIELDS);

  useEffect(() => {
    if (profile.data) {
      reset({
        contactPerson: profile.data.contactPerson,
        phone: profile.data.phone,
        address: profile.data.address,
        materialCategories: profile.data.materialCategories,
      });
    }
  }, [profile.data, reset]);

  const onSubmit = async (v: Values) => {
    feedback.clear();
    try {
      queryClient.setQueryData(['supplier-profile'], await supplierPortalApi.updateProfile(v));
      toast.success('Company profile saved.');
    } catch (error) {
      feedback.handleError(error);
    }
  };

  if (profile.isLoading) {
    return <FullPageSpinner />;
  }
  if (profile.isError || !profile.data) {
    return <Alert tone="error">{toApiError(profile.error).message}</Alert>;
  }
  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <>
      <PageHeader title="Company profile" description="Your company name, registration number and sign-in email are maintained by our administrator." />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card title={profile.data.name} highlighted>
          <DetailList
            items={[
              { label: 'Registration number', value: profile.data.registrationNo ?? '-' },
              { label: 'Sign-in email', value: profile.data.email },
            ]}
          />
        </Card>
        <Card title="Contact details" className="lg:col-span-2">
          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
            <ErrorSummary messages={summary} />
            <div className="grid gap-5 md:grid-cols-2">
              <FormField label="Contact person" htmlFor="contactPerson" error={errors.contactPerson?.message} required>
                <Input id="contactPerson" invalid={!!errors.contactPerson} {...register('contactPerson')} />
              </FormField>
              <FormField label="Phone" htmlFor="phone" error={errors.phone?.message} required>
                <Input id="phone" type="tel" invalid={!!errors.phone} {...register('phone')} />
              </FormField>
              <div className="md:col-span-2">
                <FormField label="Address" htmlFor="address" error={errors.address?.message} required>
                  <Input id="address" invalid={!!errors.address} {...register('address')} />
                </FormField>
              </div>
            </div>
            <div>
              <p id="materialCategories-label" className="mb-1.5 text-sm font-medium text-ink">Materials you supply</p>
              <Controller
                control={control}
                name="materialCategories"
                render={({ field }) => (
                  <CheckboxGroup name="materialCategories" options={categories.data ?? []} value={field.value} onChange={field.onChange} invalid={!!errors.materialCategories} />
                )}
              />
              {errors.materialCategories?.message && <p className="mt-1.5 text-sm text-red-600">{errors.materialCategories.message}</p>}
            </div>
            <Button type="submit" loading={isSubmitting}>Save profile</Button>
          </form>
        </Card>
      </div>
    </>
  );
}
