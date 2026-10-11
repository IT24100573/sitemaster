import type { LookupOption } from '../../types/common';

interface CheckboxGroupProps {
  options: LookupOption[];
  value: string[];
  onChange: (value: string[]) => void;
  name: string;
  invalid?: boolean;
}

/** Multi-select as a grid of checkboxes (e.g. material categories). */
export function CheckboxGroup({ options, value, onChange, name, invalid = false }: CheckboxGroupProps) {
  const toggle = (option: string) =>
    onChange(value.includes(option) ? value.filter((v) => v !== option) : [...value, option]);

  return (
    <div
      className={`grid gap-2 rounded-lg border p-3 sm:grid-cols-2 lg:grid-cols-3 ${invalid ? 'border-red-500' : 'border-line'}`}
      role="group"
      aria-labelledby={`${name}-label`}
    >
      {options.map((option) => (
        <label key={option.value} className="flex items-center gap-2 text-sm text-ink">
          <input
            type="checkbox"
            name={name}
            checked={value.includes(option.value)}
            onChange={() => toggle(option.value)}
            className="h-4 w-4 rounded border-line accent-primary"
          />
          {option.label}
        </label>
      ))}
    </div>
  );
}
