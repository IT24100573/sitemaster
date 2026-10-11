import type { ReactNode } from 'react';
import { AlertCircle, CheckCircle2, Info } from 'lucide-react';

type Tone = 'success' | 'error' | 'info';

const TONES: Record<Tone, { box: string; Icon: typeof Info }> = {
  success: { box: 'border-green-200 bg-green-50 text-green-800', Icon: CheckCircle2 },
  error: { box: 'border-red-200 bg-red-50 text-red-800', Icon: AlertCircle },
  info: { box: 'border-primary/30 bg-primary-light text-ink', Icon: Info },
};

export function Alert({ tone = 'info', children }: { tone?: Tone; children: ReactNode }) {
  const { box, Icon } = TONES[tone];
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={`flex items-start gap-2 rounded-lg border p-4 text-sm ${box}`}>
      <Icon className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
      <div className="flex-1">{children}</div>
    </div>
  );
}
