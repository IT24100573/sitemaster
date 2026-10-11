import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { FileSpreadsheet, FileText, Sheet } from 'lucide-react';
import { reportsApi, type ReportParams } from '../../api/financeApi';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { toApiError } from '../../lib/apiError';
import { formatDate, formatMoney } from '../../lib/format';
import { todayIso } from '../../validation/common';
import { CLIENT_MESSAGES } from '../../validation/messages';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { Spinner } from '../../components/ui/Spinner';
import type { ColumnKind, ReportTable } from '../../types/finance';

const MAX_DAYS = 366;

function firstOfMonth() {
  return `${todayIso().slice(0, 7)}-01`;
}

function cell(value: string | number | null, kind: ColumnKind, currency: string) {
  if (value === null || value === undefined || value === '') {
    return '';
  }
  switch (kind) {
    case 'MONEY':
      return formatMoney(Number(value), currency);
    case 'PERCENT':
      return `${value}%`;
    case 'DATE':
      return formatDate(String(value));
    default:
      return String(value);
  }
}

/** Pick a report, preview it, then download it as PDF, Excel or CSV. */
export default function ReportsPage() {
  const currency = useCurrency();
  const available = useQuery({ queryKey: ['reports', 'available'], queryFn: reportsApi.available });
  const [type, setType] = useState('');
  const [form, setForm] = useState<ReportParams>({ from: firstOfMonth(), to: todayIso(), projectId: '' });
  const [params, setParams] = useState<ReportParams | null>(null);
  const [error, setError] = useState<string | undefined>();

  useEffect(() => {
    if (!type && available.data?.length) {
      setType(available.data[0].type);
    }
  }, [available.data, type]);

  const preview = useQuery<ReportTable>({
    queryKey: ['reports', type, params],
    queryFn: () => reportsApi.preview(type, params as ReportParams),
    enabled: !!type && params !== null,
    retry: false,
  });

  const run = () => {
    setError(undefined);
    if (!form.from || !form.to) {
      setError(CLIENT_MESSAGES.required);
      return;
    }
    if (form.to < form.from) {
      setError(CLIENT_MESSAGES.dateRange);
      return;
    }
    const days = (new Date(form.to).getTime() - new Date(form.from).getTime()) / 86_400_000;
    if (days >= MAX_DAYS) {
      setError(CLIENT_MESSAGES.reportRange);
      return;
    }
    setParams({ ...form, projectId: form.projectId || undefined });
  };

  const selected = available.data?.find((r) => r.type === type);
  const table = preview.data;

  return (
    <>
      <PageHeader title="Reports" description="Preview a report, then download it for printing or for Excel." />
      {available.isLoading ? <Spinner /> : available.isError ? <Alert tone="error">{toApiError(available.error).message}</Alert> : (
        <>
          <div className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
            {available.data?.map((r) => (
              <button key={r.type} type="button" onClick={() => { setType(r.type); setParams(null); }} aria-pressed={r.type === type}
                className={`card p-4 text-left ${r.type === type ? 'ring-2 ring-primary' : 'hover:bg-surface'}`}>
                <p className="font-medium text-ink">{r.title}</p>
                <p className="mt-1 text-xs text-muted">{r.description}</p>
              </button>
            ))}
          </div>
          <Card title={selected?.title ?? 'Report'}>
            <div className="grid gap-4 sm:grid-cols-[1fr_1fr_1.5fr_auto] sm:items-end">
              <FormField label="From" htmlFor="rp-from" required>
                <Input id="rp-from" type="date" max={todayIso()} value={form.from} onChange={(e) => setForm({ ...form, from: e.target.value })} />
              </FormField>
              <FormField label="To" htmlFor="rp-to" required>
                <Input id="rp-to" type="date" value={form.to} onChange={(e) => setForm({ ...form, to: e.target.value })} />
              </FormField>
              <FormField label="Project" htmlFor="rp-project">
                <ProjectSelect id="rp-project" value={form.projectId ?? ''} onChange={(e) => setForm({ ...form, projectId: e.target.value })} />
              </FormField>
              <Button onClick={run}>Preview</Button>
            </div>
            {error && <p className="mt-3 text-sm text-red-600" role="alert">{error}</p>}
          </Card>

          {params && (
            <Card className="mt-6" title={table?.title} description={table?.subtitle}
              actions={table && (
                <>
                  <a href={reportsApi.exportUrl(type, params, 'PDF')} className="inline-flex h-9 items-center gap-1 rounded-lg border border-line px-3 text-sm font-medium hover:bg-surface"><FileText className="h-4 w-4" aria-hidden />PDF</a>
                  <a href={reportsApi.exportUrl(type, params, 'XLSX')} className="inline-flex h-9 items-center gap-1 rounded-lg border border-line px-3 text-sm font-medium hover:bg-surface"><FileSpreadsheet className="h-4 w-4" aria-hidden />Excel</a>
                  <a href={reportsApi.exportUrl(type, params, 'CSV')} className="inline-flex h-9 items-center gap-1 rounded-lg border border-line px-3 text-sm font-medium hover:bg-surface"><Sheet className="h-4 w-4" aria-hidden />CSV</a>
                </>
              )}>
              {preview.isLoading ? <Spinner /> : preview.isError ? <Alert tone="error">{toApiError(preview.error).message}</Alert> : table && (
                table.rows.length === 0 ? <p className="text-sm text-muted">No records for this period.</p> : (
                  <div className="max-h-[60vh] overflow-auto">
                    <table className="w-full min-w-[640px] text-sm">
                      <thead className="sticky top-0 bg-white text-left text-muted">
                        <tr className="border-b border-line">
                          {table.columns.map((c) => (
                            <th key={c.header} scope="col" className={`py-2 pr-3 font-medium ${['MONEY', 'NUMBER', 'PERCENT'].includes(c.kind) ? 'text-right' : ''}`}>{c.header}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {table.rows.map((row, r) => (
                          <tr key={r} className="border-b border-line">
                            {row.map((value, c) => (
                              <td key={c} className={`py-2 pr-3 ${['MONEY', 'NUMBER', 'PERCENT'].includes(table.columns[c].kind) ? 'text-right whitespace-nowrap' : ''}`}>
                                {cell(value, table.columns[c].kind, currency)}
                              </td>
                            ))}
                          </tr>
                        ))}
                        {table.totals && (
                          <tr className="font-semibold">
                            {table.totals.map((value, c) => (
                              <td key={c} className={`py-2 pr-3 ${['MONEY', 'NUMBER', 'PERCENT'].includes(table.columns[c].kind) ? 'text-right whitespace-nowrap' : ''}`}>
                                {cell(value, table.columns[c].kind, currency)}
                              </td>
                            ))}
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                )
              )}
            </Card>
          )}
        </>
      )}
    </>
  );
}
