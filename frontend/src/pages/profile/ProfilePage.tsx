import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { profileApi } from '../../api/profileApi';
import { CURRENT_USER_KEY } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { emptyToNull } from '../../lib/params';
import { formatDate, formatDateTime, humanize } from '../../lib/format';
import { PASSWORD_RULES } from '../../validation/authSchemas';
import {
  changePasswordSchema,
  profileSchema,
  type ChangePasswordFormValues,
  type ProfileFormValues,
} from '../../validation/profileSchemas';
import { PROFILE_KEY } from '../../components/layout/TopBar';
import { Alert } from '../../components/ui/Alert';
import { Avatar } from '../../components/ui/Avatar';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { DetailList } from '../../components/ui/DetailList';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FileUpload } from '../../components/ui/FileUpload';
import { FormField } from '../../components/ui/FormField';
import { Input, PasswordInput } from '../../components/ui/Input';
import { PageHeader } from '../../components/ui/PageHeader';
import { FullPageSpinner } from '../../components/ui/Spinner';
import type { Profile } from '../../types/profile';

const DETAIL_FIELDS = ['fullName', 'phone'] as const;
const PASSWORD_FIELDS = ['currentPassword', 'newPassword', 'confirmPassword'] as const;

/** Every role: own details, photo and password. */
export default function ProfilePage() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const profile = useQuery({ queryKey: PROFILE_KEY, queryFn: profileApi.get });
  const [photo, setPhoto] = useState<File | null>(null);
  const [confirmRemove, setConfirmRemove] = useState(false);

  const details = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    mode: 'onBlur',
    defaultValues: { fullName: '', phone: '' },
  });
  const detailFeedback = useFormFeedback(details.setError, DETAIL_FIELDS);

  const password = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    mode: 'onBlur',
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  });
  const passwordFeedback = useFormFeedback(password.setError, PASSWORD_FIELDS);

  const { reset: resetDetails } = details;
  useEffect(() => {
    if (profile.data) {
      resetDetails({ fullName: profile.data.fullName, phone: profile.data.phone ?? '' });
    }
  }, [profile.data, resetDetails]);

  const applyProfile = async (data: Profile) => {
    queryClient.setQueryData(PROFILE_KEY, data);
    await queryClient.invalidateQueries({ queryKey: CURRENT_USER_KEY });
  };

  const saveDetails = async (values: ProfileFormValues) => {
    detailFeedback.clear();
    try {
      await applyProfile(await profileApi.update({ fullName: values.fullName, phone: emptyToNull(values.phone) }));
      toast.success('Profile saved.');
    } catch (error) {
      detailFeedback.handleError(error);
    }
  };

  const savePassword = async (values: ChangePasswordFormValues) => {
    passwordFeedback.clear();
    try {
      const result = await profileApi.changePassword(values);
      password.reset();
      toast.success(result.message);
    } catch (error) {
      passwordFeedback.handleError(error);
    }
  };

  const uploadPhoto = useMutation({
    mutationFn: (file: File) => profileApi.uploadPhoto(file),
    onSuccess: async (data) => {
      await applyProfile(data);
      setPhoto(null);
      toast.success('Photo updated.');
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const removePhoto = useMutation({
    mutationFn: profileApi.removePhoto,
    onSuccess: async (data) => {
      await applyProfile(data);
      setConfirmRemove(false);
      toast.success('Photo removed.');
    },
    onError: (error) => {
      setConfirmRemove(false);
      toast.error(toApiError(error).message);
    },
  });

  if (profile.isLoading) {
    return <FullPageSpinner />;
  }
  if (profile.isError || !profile.data) {
    return <Alert tone="error">{toApiError(profile.error).message}</Alert>;
  }

  const p = profile.data;
  const de = details.formState.errors;
  const pe = password.formState.errors;
  const newPassword = password.watch('newPassword');
  const detailSummary =
    details.formState.submitCount > 0 ? [...collectErrorMessages(de), ...detailFeedback.serverMessages] : [];
  const passwordSummary =
    password.formState.submitCount > 0 ? [...collectErrorMessages(pe), ...passwordFeedback.serverMessages] : [];
  const photoError = uploadPhoto.error ? toApiError(uploadPhoto.error).fieldErrors.file : undefined;

  return (
    <>
      <PageHeader title="Profile" description="Your details, photo and password." />

      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Photo">
          <div className="flex flex-col items-center gap-4 text-center">
            <Avatar key={profile.dataUpdatedAt} userId={p.id} name={p.fullName} hasPhoto={p.hasPhoto} version={profile.dataUpdatedAt} size="lg" />
            <div>
              <p className="font-semibold text-ink">{p.fullName}</p>
              <p className="text-sm text-muted">{p.roleDisplayName}</p>
            </div>
            <div className="w-full space-y-3 text-left">
              <FileUpload kind="image" file={photo} onChange={setPhoto} label="Choose a photo" error={photoError} />
              <div className="flex gap-2">
                <Button size="sm" disabled={!photo} loading={uploadPhoto.isPending} onClick={() => photo && uploadPhoto.mutate(photo)}>
                  Upload photo
                </Button>
                {p.hasPhoto && (
                  <Button size="sm" variant="secondary" onClick={() => setConfirmRemove(true)}>
                    Remove
                  </Button>
                )}
              </div>
            </div>
          </div>
        </Card>

        <div className="space-y-6 lg:col-span-2">
          <Card title="Your details" description="Your email and role are managed by the administrator.">
            <form onSubmit={details.handleSubmit(saveDetails)} noValidate className="space-y-5">
              <ErrorSummary messages={detailSummary} />
              <div className="grid gap-5 md:grid-cols-2">
                <FormField label="Full name" htmlFor="fullName" error={de.fullName?.message} required>
                  <Input id="fullName" invalid={!!de.fullName} {...details.register('fullName')} />
                </FormField>
                <FormField label="Phone" htmlFor="phone" error={de.phone?.message} hint="0XXXXXXXXX or +94XXXXXXXXX">
                  <Input id="phone" type="tel" invalid={!!de.phone} {...details.register('phone')} />
                </FormField>
              </div>
              <DetailList
                items={[
                  { label: 'Email', value: p.email },
                  { label: 'Role', value: p.roleDisplayName },
                  { label: 'Last sign-in', value: formatDateTime(p.lastLoginAt) },
                ]}
              />
              <Button type="submit" loading={details.formState.isSubmitting}>
                Save details
              </Button>
            </form>
          </Card>

          {p.employment && (
            <Card title="Employment" highlighted>
              <DetailList
                items={[
                  { label: 'Employee number', value: p.employment.employeeNo },
                  { label: 'Employment type', value: humanize(p.employment.employmentType) },
                  { label: 'Trade', value: p.employment.trade ?? '-' },
                  { label: 'Joined', value: formatDate(p.employment.joinDate) },
                ]}
              />
            </Card>
          )}

          <Card title="Change password">
            <form onSubmit={password.handleSubmit(savePassword)} noValidate className="space-y-5">
              <ErrorSummary messages={passwordSummary} />
              <FormField label="Current password" htmlFor="currentPassword" error={pe.currentPassword?.message} required>
                <PasswordInput id="currentPassword" autoComplete="current-password" invalid={!!pe.currentPassword} {...password.register('currentPassword')} />
              </FormField>
              <div className="grid gap-5 md:grid-cols-2">
                <FormField
                  label="New password"
                  htmlFor="newPassword"
                  error={pe.newPassword?.message}
                  hint={
                    <ul className="mt-1 space-y-0.5 text-xs">
                      {PASSWORD_RULES.map((rule) => (
                        <li key={rule.label} className={rule.test(newPassword) ? 'text-green-700' : 'text-muted'}>
                          {rule.label}
                        </li>
                      ))}
                    </ul>
                  }
                  required
                >
                  <PasswordInput id="newPassword" autoComplete="new-password" invalid={!!pe.newPassword} {...password.register('newPassword')} />
                </FormField>
                <FormField label="Confirm new password" htmlFor="confirmPassword" error={pe.confirmPassword?.message} required>
                  <PasswordInput id="confirmPassword" autoComplete="new-password" invalid={!!pe.confirmPassword} {...password.register('confirmPassword')} />
                </FormField>
              </div>
              <Button type="submit" loading={password.formState.isSubmitting}>
                Change password
              </Button>
            </form>
          </Card>
        </div>
      </div>

      <ConfirmDialog
        open={confirmRemove}
        title="Remove your photo?"
        message="Your initials will be shown instead."
        confirmLabel="Remove photo"
        tone="danger"
        loading={removePhoto.isPending}
        onConfirm={() => removePhoto.mutate()}
        onCancel={() => setConfirmRemove(false)}
      />
    </>
  );
}
