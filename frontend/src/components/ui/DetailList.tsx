import type { ReactNode } from 'react';

/** Label/value pairs for read-only detail cards. */
export function DetailList({ items }: { items: { label: string; value: ReactNode }[] }) {
  return (
    <dl className="grid gap-x-6 gap-y-4 sm:grid-cols-2">
      {items.map(({ label, value }) => (
        <div key={label}>
          <dt className="text-sm text-muted">{label}</dt>
          <dd className="mt-0.5 text-sm font-medium text-ink">{value ?? '-'}</dd>
        </div>
      ))}
    </dl>
  );
}
