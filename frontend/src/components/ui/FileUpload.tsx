import { useId, useRef, useState, type ChangeEvent } from 'react';
import { FileUp, X } from 'lucide-react';
import { formatBytes } from '../../lib/format';

export type FileKind = 'image' | 'document';

/** Client-side mirror of FileCategory on the server (the server re-checks the real content). */
const RULES: Record<FileKind, { extensions: string[]; maxBytes: number; accept: string }> = {
  image: {
    extensions: ['jpg', 'jpeg', 'png', 'webp'],
    maxBytes: 2 * 1024 * 1024,
    accept: '.jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp',
  },
  document: {
    extensions: ['pdf', 'docx', 'xlsx', 'jpg', 'png'],
    maxBytes: 10 * 1024 * 1024,
    accept: '.pdf,.docx,.xlsx,.jpg,.png',
  },
};

interface FileUploadProps {
  kind: FileKind;
  file: File | null;
  onChange: (file: File | null) => void;
  label?: string;
  error?: string;
  disabled?: boolean;
}

/** Choose one file, with type/size checks before upload. */
export function FileUpload({ kind, file, onChange, label = 'Choose file', error, disabled = false }: FileUploadProps) {
  const inputId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const [localError, setLocalError] = useState<string | null>(null);
  const rule = RULES[kind];

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    const chosen = event.target.files?.[0] ?? null;
    event.target.value = '';
    if (!chosen) {
      return;
    }
    const extension = chosen.name.split('.').pop()?.toLowerCase() ?? '';
    if (chosen.size === 0) {
      setLocalError('The file is empty.');
      return;
    }
    if (!rule.extensions.includes(extension)) {
      setLocalError(`Upload a file of type ${rule.extensions.join(', ')}.`);
      return;
    }
    if (chosen.size > rule.maxBytes) {
      setLocalError(`The file must be ${formatBytes(rule.maxBytes)} or smaller.`);
      return;
    }
    setLocalError(null);
    onChange(chosen);
  };

  const message = localError ?? error;

  return (
    <div>
      <input
        ref={inputRef}
        id={inputId}
        type="file"
        accept={rule.accept}
        onChange={handleChange}
        disabled={disabled}
        className="sr-only"
      />
      {file ? (
        <div className="flex items-center gap-3 rounded-lg border border-line bg-surface px-3 py-2.5 text-sm">
          <FileUp className="h-4 w-4 shrink-0 text-primary" aria-hidden />
          <span className="min-w-0 flex-1 truncate text-ink">{file.name}</span>
          <span className="shrink-0 text-muted">{formatBytes(file.size)}</span>
          <button
            type="button"
            onClick={() => onChange(null)}
            disabled={disabled}
            className="rounded p-1 text-muted hover:text-ink"
            aria-label="Remove selected file"
          >
            <X className="h-4 w-4" aria-hidden />
          </button>
        </div>
      ) : (
        <label
          htmlFor={inputId}
          className={`flex cursor-pointer items-center gap-3 rounded-lg border border-dashed px-3 py-3 text-sm transition-colors hover:border-primary hover:bg-primary-light ${
            message ? 'border-red-500' : 'border-line'
          } ${disabled ? 'pointer-events-none opacity-60' : ''}`}
        >
          <FileUp className="h-4 w-4 text-primary" aria-hidden />
          <span className="font-medium text-ink">{label}</span>
          <span className="text-muted">
            {rule.extensions.join(', ')} up to {formatBytes(rule.maxBytes)}
          </span>
        </label>
      )}
      {message && <p className="mt-1.5 text-sm text-red-600">{message}</p>}
    </div>
  );
}
