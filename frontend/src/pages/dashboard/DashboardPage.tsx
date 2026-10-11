import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ChevronRight } from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { dashboardApi } from '../../api/financeApi';
import { useCurrentUser } from '../../context/AuthContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { toApiError } from '../../lib/apiError';
import { formatMoney, humanize } from '../../lib/format';
import { Alert } from '../../components/ui/Alert';
import { Card } from '../../components/ui/Card';
import { PageHeader } from '../../components/ui/PageHeader';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { Dashboard } from '../../types/finance';

/** Chart colours: brand orange first, then calm supporting tones. */
const COLORS = ['#F97316', '#1E4078', '#16A34A', '#9CA3AF', '#DC2626', '#0EA5E9'];

const TONES: Record<string, string> = {
  neutral: 'border-line',
  good: 'border-green-300',
  warning: 'border-amber-300 bg-amber-50/40',
  danger: 'border-red-300 bg-red-50/40',
};

function useFormatter(format: string) {
  const currency = useCurrency();
  return (value: number) => {
    if (format === 'money') {
      return formatMoney(value, currency);
    }
    if (format === 'percent') {
      return `${Math.round(value)}%`;
    }
    return Number(value).toLocaleString('en-LK');
  };
}

function StatCard({ stat }: { stat: Dashboard['stats'][number] }) {
  const format = useFormatter(stat.format);
  const body = (
    <>
      <p className="text-sm text-muted">{stat.label}</p>
      <p className="mt-1 text-2xl font-semibold text-ink">{format(Number(stat.value))}</p>
      {stat.hint && <p className="mt-1 text-xs text-muted">{stat.hint}</p>}
    </>
  );
  const className = `card block border-l-4 p-4 ${TONES[stat.tone] ?? TONES.neutral}`;
  return stat.link ? <Link to={`../${stat.link}`} relative="path" className={`${className} hover:bg-surface`}>{body}</Link> : <div className={className}>{body}</div>;
}

function ChartCard({ chart }: { chart: Dashboard['charts'][number] }) {
  const format = useFormatter(chart.format);
  const data = chart.points.map((p) => {
    const row: Record<string, string | number> = { label: humanizeLabel(p.label) };
    chart.series.forEach((s, i) => { row[s] = Number(p.values[i] ?? 0); });
    return row;
  });
  return (
    <Card title={chart.title}>
      {data.length === 0 ? <p className="text-sm text-muted">No data yet.</p> : (
        <div className="h-64" role="img" aria-label={chart.title}>
          <ResponsiveContainer width="100%" height="100%">
            {chart.type === 'pie' ? (
              <PieChart>
                <Pie data={data} dataKey={chart.series[0]} nameKey="label" innerRadius="55%" outerRadius="85%" paddingAngle={2}>
                  {data.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip formatter={(v: number) => format(v)} />
                <Legend />
              </PieChart>
            ) : chart.type === 'line' ? (
              <LineChart data={data} margin={{ left: 8, right: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="label" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} tickFormatter={(v: number) => (chart.format === 'money' ? `${Math.round(v / 1000)}k` : String(v))} />
                <Tooltip formatter={(v: number) => format(v)} />
                <Legend />
                {chart.series.map((s, i) => <Line key={s} type="monotone" dataKey={s} stroke={COLORS[i % COLORS.length]} strokeWidth={2} dot={false} />)}
              </LineChart>
            ) : (
              <BarChart data={data} margin={{ left: 8, right: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" vertical={false} />
                <XAxis dataKey="label" tick={{ fontSize: 12 }} interval={0} />
                <YAxis tick={{ fontSize: 12 }} tickFormatter={(v: number) => (chart.format === 'money' ? `${Math.round(v / 1000)}k` : String(v))}
                  domain={chart.format === 'percent' ? [0, 100] : undefined} />
                <Tooltip formatter={(v: number) => format(v)} />
                {chart.series.length > 1 && <Legend />}
                {chart.series.map((s, i) => <Bar key={s} dataKey={s} fill={COLORS[i % COLORS.length]} radius={[4, 4, 0, 0]} />)}
              </BarChart>
            )}
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  );
}

function humanizeLabel(label: string) {
  return /^[A-Z_]+$/.test(label) ? humanize(label) : label;
}

/** Role dashboard served by the backend DashboardFactory. */
export default function DashboardPage() {
  const user = useCurrentUser();
  const dashboard = useQuery({ queryKey: ['dashboard', user.role], queryFn: dashboardApi.get, refetchInterval: 60_000 });

  if (dashboard.isLoading) {
    return <FullPageSpinner />;
  }
  if (dashboard.isError || !dashboard.data) {
    return <Alert tone="error">{toApiError(dashboard.error).message}</Alert>;
  }
  const d = dashboard.data;

  return (
    <>
      <PageHeader title={d.title} description={`Welcome back, ${user.fullName.split(' ')[0]}. ${d.subtitle}`} />
      {d.stats.length > 0 && (
        <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {d.stats.map((s) => <StatCard key={s.label} stat={s} />)}
        </div>
      )}
      <div className="grid gap-6 lg:grid-cols-2">
        {d.charts.map((c) => <ChartCard key={c.title} chart={c} />)}
        {d.lists.map((list) => (
          <Card key={list.title} title={list.title} actions={list.link && (
            <Link to={`../${list.link}`} relative="path" className="text-sm font-medium text-primary hover:text-primary-dark">View all</Link>
          )}>
            {list.items.length === 0 ? <p className="text-sm text-muted">{list.emptyText}</p> : (
              <ul className="divide-y divide-line">
                {list.items.map((item, i) => {
                  const content = (
                    <>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-ink">{item.title}</p>
                        <p className="truncate text-xs text-muted">{item.subtitle}</p>
                      </div>
                      {item.status && <StatusBadge status={item.status} />}
                      {item.link && <ChevronRight className="h-4 w-4 text-muted" aria-hidden />}
                    </>
                  );
                  return (
                    <li key={i}>
                      {item.link ? <Link to={`../${item.link}`} relative="path" className="flex items-center gap-3 py-2.5 hover:bg-surface">{content}</Link>
                        : <div className="flex items-center gap-3 py-2.5">{content}</div>}
                    </li>
                  );
                })}
              </ul>
            )}
          </Card>
        ))}
      </div>
    </>
  );
}
