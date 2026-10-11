import { useState, type KeyboardEvent } from 'react';
import { Plus, X } from 'lucide-react';

interface ListEditorProps {
  values: string[];
  onChange: (values: string[]) => void;
  inputType?: 'text' | 'date';
  placeholder?: string;
  addLabel: string;
  format?: (value: string) => string;
  invalid?: boolean;
  maxLength?: number;
}

/** Edits a list of short values as removable chips (holidays, trades). */
export function ListEditor({
  values,
  onChange,
  inputType = 'text',
  placeholder,
  addLabel,
  format = (v) => v,
  invalid = false,
  maxLength = 50,
}: ListEditorProps) {
  const [draft, setDraft] = useState('');

  const add = () => {
    const value = draft.trim();
    if (value && !values.includes(value)) {
      const next = [...values, value];
      onChange(inputType === 'date' ? next.sort() : next);
    }
    setDraft('');
  };

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      add();
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex gap-2">
        <input
          type={inputType}
          value={draft}
          maxLength={maxLength}
          placeholder={placeholder}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={onKeyDown}
          className={`form-input ${invalid ? 'form-input-invalid' : ''}`}
          aria-label={addLabel}
        />
        <button
          type="button"
          onClick={add}
          className="inline-flex h-11 shrink-0 items-center gap-1.5 rounded-lg border border-line px-3 text-sm font-medium text-ink hover:bg-surface"
        >
          <Plus className="h-4 w-4" aria-hidden />
          Add
        </button>
      </div>
      {values.length > 0 && (
        <ul className="flex flex-wrap gap-2">
          {values.map((value) => (
            <li
              key={value}
              className="inline-flex items-center gap-1 rounded-full bg-surface py-1 pl-3 pr-1 text-sm text-ink ring-1 ring-inset ring-line"
            >
              {format(value)}
              <button
                type="button"
                onClick={() => onChange(values.filter((v) => v !== value))}
                className="rounded-full p-0.5 text-muted hover:bg-white hover:text-ink"
                aria-label={`Remove ${format(value)}`}
              >
                <X className="h-3.5 w-3.5" aria-hidden />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
