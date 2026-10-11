import axios from 'axios';
import type { FieldErrors, FieldValues, Path, UseFormSetError } from 'react-hook-form';
import type { ApiErrorBody } from '../types/api';
import { CLIENT_MESSAGES } from '../validation/messages';

function body(status: number, message: string): ApiErrorBody {
  return { status, message, fieldErrors: {}, globalErrors: [] };
}

/** Normalises any thrown value into the server's error shape. */
export function toApiError(error: unknown): ApiErrorBody {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as Partial<ApiErrorBody> | undefined;
    if (data && typeof data.message === 'string') {
      return {
        status: data.status ?? error.response?.status ?? 0,
        message: data.message,
        fieldErrors: data.fieldErrors ?? {},
        globalErrors: data.globalErrors ?? [],
      };
    }
    if (!error.response) {
      return body(0, CLIENT_MESSAGES.network);
    }
    return body(error.response.status, CLIENT_MESSAGES.unexpected);
  }
  return body(0, CLIENT_MESSAGES.unexpected);
}

/**
 * Puts server field errors onto the matching React Hook Form fields.
 * Returns messages for fields the form does not have, so they can be shown
 * in the error summary instead of being lost.
 */
export function applyServerFieldErrors<T extends FieldValues>(
  error: ApiErrorBody,
  setError: UseFormSetError<T>,
  knownFields: readonly Path<T>[],
): string[] {
  const unmatched: string[] = [];
  Object.entries(error.fieldErrors).forEach(([field, message]) => {
    if ((knownFields as readonly string[]).includes(field)) {
      setError(field as Path<T>, { type: 'server', message });
    } else {
      unmatched.push(message);
    }
  });
  return [...unmatched, ...error.globalErrors];
}

/** Flat list of the current form error messages, for the error summary. */
export function collectErrorMessages<T extends FieldValues>(errors: FieldErrors<T>): string[] {
  return Object.values(errors)
    .map((entry) => {
      const message = (entry as { message?: unknown } | undefined)?.message;
      return typeof message === 'string' ? message : null;
    })
    .filter((message): message is string => Boolean(message));
}
