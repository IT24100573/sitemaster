import { humanize } from '../../lib/format';

type Tone = 'amber' | 'green' | 'red' | 'blue' | 'orange' | 'gray';

const TONE_CLASSES: Record<Tone, string> = {
  amber: 'bg-amber-50 text-amber-700 ring-amber-200',
  green: 'bg-green-50 text-green-700 ring-green-200',
  red: 'bg-red-50 text-red-700 ring-red-200',
  blue: 'bg-blue-50 text-blue-700 ring-blue-200',
  orange: 'bg-primary-light text-primary-dark ring-primary/30',
  gray: 'bg-surface text-muted ring-line',
};

/**
 * One colour rule for every status in the system:
 * pending = amber, approved/done = green, rejected/delayed = red,
 * in progress = blue, paid = orange.
 */
const STATUS_TONES: Record<string, Tone> = {
  PENDING: 'amber',
  SENT: 'amber',
  OPEN: 'amber',
  SUMMARY_SENT: 'amber',
  PARTIALLY_PAID: 'amber',
  HALF_DAY: 'amber',
  UNDER_MAINTENANCE: 'amber',

  APPROVED: 'green',
  DONE: 'green',
  COMPLETED: 'green',
  ACCEPTED: 'green',
  DELIVERED: 'green',
  RESOLVED: 'green',
  PRESENT: 'green',
  ACTIVE: 'green',
  AVAILABLE: 'green',

  REJECTED: 'red',
  DELAYED: 'red',
  DECLINED: 'red',
  OVERDUE: 'red',
  ABSENT: 'red',
  CRITICAL: 'red',
  INACTIVE: 'red',
  EXPIRED: 'red',

  IN_PROGRESS: 'blue',
  ORDERED: 'blue',
  DISPATCHED: 'blue',
  INVOICED: 'blue',
  ISSUED: 'blue',
  CALCULATED: 'blue',
  ON_LEAVE: 'blue',
  IN_USE: 'blue',

  PAID: 'orange',

  NOT_STARTED: 'gray',
  LOW: 'gray',
  NORMAL: 'blue',
  MEDIUM: 'amber',
  HIGH: 'orange',
  URGENT: 'red',
  ISSUE: 'red',
  RISK: 'amber',

  TODO: 'gray',
  PLANNED: 'gray',
  DRAFT: 'gray',
  CLOSED: 'gray',
  ARCHIVED: 'gray',
  RETIRED: 'gray',
  UPCOMING: 'gray',
};

export function StatusBadge({ status, label }: { status: string; label?: string }) {
  const tone = STATUS_TONES[status] ?? 'gray';
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${TONE_CLASSES[tone]}`}>
      {label ?? humanize(status)}
    </span>
  );
}
