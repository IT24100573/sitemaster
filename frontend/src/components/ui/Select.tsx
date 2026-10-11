import { forwardRef, type SelectHTMLAttributes } from 'react';
import type { LookupOption } from '../../types/common';

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  options: LookupOption[];
  placeholder?: string;
  invalid?: boolean;
}

/** Native select fed by API lookup data. */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { options, placeholder, invalid = false, className = '', ...rest },
  ref,
) {
  return (
    <select
      ref={ref}
      aria-invalid={invalid || undefined}
      className={`form-input pr-8 ${invalid ? 'form-input-invalid' : ''} ${className}`}
      {...rest}
    >
      {placeholder !== undefined && <option value="">{placeholder}</option>}
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  );
});
