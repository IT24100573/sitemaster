import { Loader2 } from 'lucide-react';

export function Spinner({ className = 'h-5 w-5' }: { className?: string }) {
  return <Loader2 className={`animate-spin text-primary ${className}`} aria-hidden />;
}

export function FullPageSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-surface" role="status">
      <Spinner className="h-8 w-8" />
      <span className="sr-only">Loading</span>
    </div>
  );
}
