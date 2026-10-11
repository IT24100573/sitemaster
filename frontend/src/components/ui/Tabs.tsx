export interface TabItem {
  id: string;
  label: string;
  count?: number;
}

interface TabsProps {
  tabs: TabItem[];
  active: string;
  onChange: (id: string) => void;
  label: string;
}

/** Underlined tab bar; scrolls sideways on small screens. */
export function Tabs({ tabs, active, onChange, label }: TabsProps) {
  return (
    <div className="mb-6 overflow-x-auto border-b border-line">
      <div className="flex min-w-max gap-1" role="tablist" aria-label={label}>
        {tabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={tab.id === active}
            onClick={() => onChange(tab.id)}
            className={`-mb-px border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
              tab.id === active ? 'border-primary text-primary-dark' : 'border-transparent text-muted hover:text-ink'
            }`}
          >
            {tab.label}
            {tab.count !== undefined && (
              <span className="ml-2 rounded-full bg-surface px-2 py-0.5 text-xs text-muted">{tab.count}</span>
            )}
          </button>
        ))}
      </div>
    </div>
  );
}
