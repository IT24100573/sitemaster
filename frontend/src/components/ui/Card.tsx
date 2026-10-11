import type { ReactNode } from 'react';

interface CardProps {
  title?: string;
  description?: string;
  actions?: ReactNode;
  highlighted?: boolean;
  className?: string;
  children: ReactNode;
}

/** 12px rounded, soft-shadow card. `highlighted` uses the light orange tint. */
export function Card({ title, description, actions, highlighted = false, className = '', children }: CardProps) {
  return (
    <section className={`card p-5 ${highlighted ? 'border-primary/30 bg-primary-light' : ''} ${className}`}>
      {(title || actions) && (
        <header className="mb-4 flex flex-wrap items-start justify-between gap-3">
          <div>
            {title && <h2 className="text-base font-semibold text-ink">{title}</h2>}
            {description && <p className="mt-0.5 text-sm text-muted">{description}</p>}
          </div>
          {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
        </header>
      )}
      {children}
    </section>
  );
}
