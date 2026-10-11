/** Orange progress bar with an accessible value. */
export function ProgressBar({ value, label, showValue = true }: { value: number; label: string; showValue?: boolean }) {
  const clamped = Math.max(0, Math.min(100, Math.round(value)));
  return (
    <div className="flex items-center gap-2">
      <div
        className="h-2 flex-1 overflow-hidden rounded-full bg-surface ring-1 ring-inset ring-line"
        role="progressbar"
        aria-label={label}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={clamped}
      >
        <div className="h-full rounded-full bg-primary transition-[width]" style={{ width: `${clamped}%` }} />
      </div>
      {showValue && <span className="w-10 text-right text-xs font-medium text-muted">{clamped}%</span>}
    </div>
  );
}
