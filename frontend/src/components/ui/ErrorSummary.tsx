import { AlertCircle } from 'lucide-react';

/** Shown at the top of a form after a failed submit. */
export function ErrorSummary({ messages, title = 'Fix the following to continue' }: { messages: string[]; title?: string }) {
  const unique = Array.from(new Set(messages));
  if (unique.length === 0) {
    return null;
  }
  return (
    <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
      <div className="flex items-start gap-2">
        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
        {unique.length === 1 ? (
          <p>{unique[0]}</p>
        ) : (
          <div>
            <p className="font-medium">{title}</p>
            <ul className="mt-1 list-disc space-y-0.5 pl-4">
              {unique.map((message) => (
                <li key={message}>{message}</li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
