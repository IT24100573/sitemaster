import type { ReactNode } from 'react';

interface FormFieldProps {
  label: string;
  htmlFor: string;
  error?: string;
  hint?: ReactNode;
  required?: boolean;
  children: ReactNode;
}

/** Label + control + hint or red error message under the field. */
export function FormField({ label, htmlFor, error, hint, required = false, children }: FormFieldProps) {
  const messageId = `${htmlFor}-message`;
  return (
    <div className="space-y-1.5">
      <label htmlFor={htmlFor} className="block text-sm font-medium text-ink">
        {label}
        {required && (
          <span className="ml-0.5 text-red-600" aria-hidden>
            *
          </span>
        )}
      </label>
      {children}
      {error ? (
        <p id={messageId} className="text-sm text-red-600">
          {error}
        </p>
      ) : (
        hint && (
          <div id={messageId} className="text-sm text-muted">
            {hint}
          </div>
        )
      )}
    </div>
  );
}
