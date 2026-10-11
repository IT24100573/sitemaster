import { useCallback, useState } from 'react';
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';
import { applyServerFieldErrors, toApiError } from '../lib/apiError';
import { useToast } from '../context/ToastContext';

/**
 * Shared handling of a failed form submit: server field errors go under the
 * matching inputs, everything else into the error summary, plus a red toast.
 */
export function useFormFeedback<T extends FieldValues>(setError: UseFormSetError<T>, fields: readonly Path<T>[]) {
  const toast = useToast();
  const [serverMessages, setServerMessages] = useState<string[]>([]);

  const clear = useCallback(() => setServerMessages([]), []);

  const handleError = useCallback(
    (error: unknown) => {
      const apiError = toApiError(error);
      const unmatched = applyServerFieldErrors(apiError, setError, fields);
      const hasFieldErrors = Object.keys(apiError.fieldErrors).length > 0;
      setServerMessages(hasFieldErrors ? unmatched : [apiError.message]);
      toast.error(apiError.message);
    },
    [setError, fields, toast],
  );

  return { serverMessages, clear, handleError };
}
