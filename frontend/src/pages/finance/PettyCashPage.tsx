import { useEffect, useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Settings2, Wallet } from 'lucide-react';
import { projectsApi } from '../../api/projectsApi';
import { pettyCashApi } from '../../api/financeApi';
import { useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { formatDate, formatMoney, humanize } from '../../lib/format';
import { PATTERNS, todayIso } from '../../validation/common';
import { CLIENT_MESSAGES } from '../../validation/messages';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { Select } from '../../components/ui/Select';
import { Spinner } from '../../components/ui/Spinner';
import type { PettyCashFund, PettyCashTransaction } from '../../types/finance';

type Dialog = { kind: 'setup'; fund: PettyCashFund | null } | { kind: 'topup'; fund: PettyCashFund } | { kind: 'adjust'; fund: PettyCashFund } | null;

function DialogBody({ dialog, onDone }: { dialog: NonNullable<Dialog>; onDone: () => void }) {
  const toast = useToast();
  const currency = useCurrency();
  const queryClient = useQueryClient();
  const [projectId, setProjectId] = useState(dialog.fund ? String(dialog.fund.projectId) : '');
  const [custodianId, setCustodianId] = useState(dialog.kind === 'setup' && dialog.fund ? String(dialog.fund.custodianId) : '');
  const [amount, setAmount] = useState(dialog.kind === 'setup' && dialog.fund ? Number(dialog.fund.floatLimit).toFixed(2) : '');
  const [date, setDate] = useState(todayIso());
  const [text, setText] = useState('');
  const [active, setActive] = useState(dialog.fund?.active ?? true);
  const [error, setError] = useState<string | undefined>();

  const team = useQuery({
    queryKey: ['projects', Number(projectId), 'members'],
    queryFn: () => projectsApi.members(Number(projectId)),
    enabled: dialog.kind === 'setup' && projectId !== '',
  });
  useEffect(() => setError(undefined), [projectId, custodianId, amount, text]);

  const save = useMutation({
    mutationFn: () => {
      if (dialog.kind === 'setup') {
        return pettyCashApi.save(Number(projectId), { custodianId: Number(custodianId), floatLimit: amount.trim(), active });
      }
      if (dialog.kind === 'topup') {
        return pettyCashApi.topUp(dialog.fund.projectId, { amount: amount.trim(), date, description: text.trim() });
      }
      return pettyCashApi.adjust(dialog.fund.projectId, { countedBalance: amount.trim(), date, reason: text.trim() });
    },
    onSuccess: async (fund) => {
      await queryClient.invalidateQueries({ queryKey: ['petty-cash'] });
      toast.success(`${fund.projectName} petty cash: balance ${formatMoney(fund.balance, currency)}.`);
      onDone();
    },
    onError: (err) => {
      const apiError = toApiError(err);
      setError(Object.values(apiError.fieldErrors)[0] ?? apiError.message);
    },
  });

  const submit = () => {
    if (dialog.kind === 'setup' && (!projectId || !custodianId)) {
      setError(CLIENT_MESSAGES.required);
      return;
    }
    if (!PATTERNS.money.test(amount.trim()) || (dialog.kind !== 'adjust' && Number(amount) <= 0)) {
      setError(CLIENT_MESSAGES.moneyPositive);
      return;
    }
    if (dialog.kind !== 'setup' && (text.trim().length < (dialog.kind === 'adjust' ? 5 : 3) || !date || date > todayIso())) {
      setError(dialog.kind === 'adjust' ? CLIENT_MESSAGES.reason : CLIENT_MESSAGES.pettyDescription);
      return;
    }
    save.mutate();
  };

  return (
    <div className="space-y-4">
      {dialog.kind === 'setup' && (
        <>
          <FormField label="Project" htmlFor="pc-project" required>
            {dialog.fund ? <Input id="pc-project" value={dialog.fund.projectName} disabled readOnly />
              : <ProjectSelect id="pc-project" activeOnly placeholder="Choose a project" value={projectId} onChange={(e) => { setProjectId(e.target.value); setCustodianId(''); }} />}
          </FormField>
          <FormField label="Custodian" htmlFor="pc-custodian" hint="A member of the project team who holds the cash." required>
            <Select id="pc-custodian" options={(team.data ?? []).map((m) => ({ value: String(m.userId), label: `${m.fullName} (${humanize(m.role)})` }))}
              placeholder={projectId ? 'Choose a team member' : 'Choose a project first'} value={custodianId} onChange={(e) => setCustodianId(e.target.value)} />
          </FormField>
          <FormField label={`Float limit (${currency})`} htmlFor="pc-limit" required>
            <Input id="pc-limit" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />
          </FormField>
          {dialog.fund && (
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" className="accent-primary" checked={active} onChange={(e) => setActive(e.target.checked)} />Fund is open</label>
          )}
        </>
      )}
      {dialog.kind !== 'setup' && (
        <>
          <p className="text-sm text-muted">
            Balance {formatMoney(dialog.fund.balance, currency)} of {formatMoney(dialog.fund.floatLimit, currency)}.
          </p>
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField label={dialog.kind === 'topup' ? `Amount (${currency})` : `Cash counted (${currency})`} htmlFor="pc-amount" required>
              <Input id="pc-amount" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />
            </FormField>
            <FormField label="Date" htmlFor="pc-date" required>
              <Input id="pc-date" type="date" max={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} />
            </FormField>
          </div>
          <FormField label={dialog.kind === 'topup' ? 'Description' : 'Reason for the difference'} htmlFor="pc-text" required>
            <Input id="pc-text" maxLength={255} value={text} onChange={(e) => setText(e.target.value)} />
          </FormField>
        </>
      )}
      {error && <p className="text-sm text-red-600" role="alert">{error}</p>}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onDone}>Cancel</Button>
        <Button loading={save.isPending} onClick={submit}>{dialog.kind === 'setup' ? 'Save fund' : dialog.kind === 'topup' ? 'Top up' : 'Record count'}</Button>
      </div>
    </div>
  );
}

/** Petty cash floats per project: the Accountant manages them, site teams follow their balance. */
export default function PettyCashPage() {
  const user = useCurrentUser();
  const isAccountant = user.role === 'ACCOUNTANT';
  const currency = useCurrency();
  const table = useTableState('createdAt', 'desc');
  const [selected, setSelected] = useState<number | null>(null);
  const [dialog, setDialog] = useState<Dialog>(null);
  const funds = useQuery({ queryKey: ['petty-cash', 'funds'], queryFn: pettyCashApi.funds });
  const fund = funds.data?.find((f) => f.projectId === selected) ?? funds.data?.[0] ?? null;

  const transactions = useQuery({
    queryKey: ['petty-cash', 'tx', fund?.projectId, table.params],
    queryFn: () => pettyCashApi.transactions(fund!.projectId, table.params),
    enabled: !!fund,
    placeholderData: keepPreviousData,
  });

  const columns: Column<PettyCashTransaction>[] = [
    { key: 'date', header: 'Date', className: 'whitespace-nowrap', render: (t) => formatDate(t.date) },
    { key: 'type', header: 'Type', render: (t) => humanize(t.type) },
    { key: 'description', header: 'Description', render: (t) => t.description },
    { key: 'by', header: 'Recorded by', render: (t) => t.recordedByName },
    { key: 'amount', header: 'Amount', className: 'whitespace-nowrap text-right', render: (t) => <span className={Number(t.amount) < 0 ? 'text-red-600' : 'text-green-700'}>{formatMoney(t.amount, currency)}</span> },
    { key: 'balance', header: 'Balance', className: 'whitespace-nowrap text-right', render: (t) => formatMoney(t.balanceAfter, currency) },
  ];

  return (
    <>
      <PageHeader title="Petty cash" description="Small site payments are paid from a project float the accountant tops up."
        actions={isAccountant && <Button onClick={() => setDialog({ kind: 'setup', fund: null })}><Plus className="h-4 w-4" aria-hidden />Set up a fund</Button>} />
      {funds.isLoading ? <Spinner /> : funds.isError ? <Alert tone="error">{toApiError(funds.error).message}</Alert> : (funds.data ?? []).length === 0 ? (
        <p className="card p-8 text-center text-sm text-muted">No petty cash funds yet.</p>
      ) : (
        <>
          <ul className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {funds.data?.map((f) => {
              const share = Number(f.floatLimit) > 0 ? (Number(f.balance) / Number(f.floatLimit)) * 100 : 0;
              const active = fund?.projectId === f.projectId;
              return (
                <li key={f.id}>
                  <button type="button" onClick={() => { setSelected(f.projectId); table.resetPage(); }}
                    className={`card w-full p-4 text-left ${active ? 'ring-2 ring-primary' : 'hover:bg-surface'}`} aria-pressed={active}>
                    <div className="flex items-center gap-2">
                      <Wallet className="h-4 w-4 text-primary" aria-hidden />
                      <span className="font-medium text-ink">{f.projectName}</span>
                      {!f.active && <span className="text-xs text-muted">(closed)</span>}
                    </div>
                    <p className="mt-2 text-xl font-semibold text-ink">{formatMoney(f.balance, currency)}</p>
                    <p className="mb-2 text-xs text-muted">of {formatMoney(f.floatLimit, currency)}, held by {f.custodianName}</p>
                    <ProgressBar value={share} label={`${f.projectName} petty cash level`} />
                  </button>
                </li>
              );
            })}
          </ul>
          {fund && (
            <DataTable
              columns={columns}
              page={transactions.data}
              rowKey={(t) => t.id}
              loading={transactions.isFetching}
              error={transactions.isError ? toApiError(transactions.error).message : null}
              emptyMessage="No movements yet."
              sort={table.sort}
              direction={table.direction}
              onSort={table.toggleSort}
              onPageChange={table.setPage}
              toolbar={
                <>
                  <h2 className="text-base font-semibold text-ink">{fund.projectName}</h2>
                  {fund.canManage && (
                    <div className="flex flex-wrap gap-2 sm:ml-auto">
                      <Button size="sm" variant="secondary" onClick={() => setDialog({ kind: 'setup', fund })}><Settings2 className="h-4 w-4" aria-hidden />Settings</Button>
                      <Button size="sm" variant="secondary" onClick={() => setDialog({ kind: 'adjust', fund })}>Record cash count</Button>
                      <Button size="sm" onClick={() => setDialog({ kind: 'topup', fund })}>Top up</Button>
                    </div>
                  )}
                </>
              }
            />
          )}
        </>
      )}
      <Modal open={dialog !== null} title={dialog?.kind === 'setup' ? (dialog.fund ? 'Fund settings' : 'Set up petty cash') : dialog?.kind === 'topup' ? 'Top up petty cash' : 'Record cash count'} onClose={() => setDialog(null)}>
        {dialog && <DialogBody dialog={dialog} onDone={() => setDialog(null)} />}
      </Modal>
    </>
  );
}

