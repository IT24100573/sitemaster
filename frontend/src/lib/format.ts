/** "PURCHASE_ORDER" -> "Purchase order" */
export function humanize(value: string): string {
  const text = value.replace(/_/g, ' ').toLowerCase();
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** "Kasun Silva" -> "KS" */
export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}

const DATE = new Intl.DateTimeFormat('en-LK', { day: '2-digit', month: 'short', year: 'numeric' });
const DATE_TIME = new Intl.DateTimeFormat('en-LK', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

/** "2026-03-04" or ISO date-time -> "04 Mar 2026". Server dates are local (Asia/Colombo). */
export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return '-';
  }
  const date = value.length === 10 ? new Date(`${value}T00:00:00`) : new Date(value);
  return Number.isNaN(date.getTime()) ? value : DATE.format(date);
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : DATE_TIME.format(date);
}

/** 12500 -> "LKR 12,500.00" (currency code comes from System Settings). */
export function formatMoney(value: number | string | null | undefined, currency: string): string {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  const amount = typeof value === 'string' ? Number(value) : value;
  if (Number.isNaN(amount)) {
    return String(value);
  }
  return `${currency} ${amount.toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** "Just now", "5 min ago", "3 h ago", else the date. */
export function timeAgo(value: string): string {
  const date = new Date(value);
  const seconds = Math.round((Date.now() - date.getTime()) / 1000);
  if (seconds < 60) {
    return 'Just now';
  }
  if (seconds < 3600) {
    return `${Math.floor(seconds / 60)} min ago`;
  }
  if (seconds < 86400) {
    return `${Math.floor(seconds / 3600)} h ago`;
  }
  return formatDate(value);
}

/** "March 2026" from a year and 1-based month. */
export function formatMonth(year: number, month: number): string {
  return new Date(year, month - 1, 1).toLocaleDateString('en-LK', { month: 'long', year: 'numeric' });
}
