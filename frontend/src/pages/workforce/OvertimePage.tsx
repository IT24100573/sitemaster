import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { attendanceApi } from '../../api/workforceApi';
import { useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { formatDate } from '../../lib/format';
import { CLIENT_MESSAGES } from '../../validation/messages';
import { Button } from '../../components/ui/Button';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { FormField } from '../../components/ui/FormField';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Textarea } from '../../components/ui/Textarea';
import type { OvertimeRecord } from '../../types/workforce';

const STATUS_OPTIONS = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
];

/** HR approves overtime; site engineers follow what they recorded. */
export default function OvertimePage() {
  const user = useCurrentUser();
  const isHr = user.role === 'HR_MANAGER';
  const toast = useToast();
  const queryClient = useQueryClient();
  const table = useTableState('createdAt', 'desc');
  const [status, setStatus] = useState('PENDING');
  const [decision, setDecision] = useState<{ item: OvertimeRecord; approve: boolean } | null>(null);
  const [note, setNote] = useState('');
  const [error, setError] = useState<string | undefined>();

  const items = useQuery({
    queryKey: ['attendance', 'overtime', table.params, status],
    queryFn: () => attendanceApi.overtime(table.params, status || undefined),
    placeholderData: keepPreviousData,
  });

  const decide = useMutation({
    mutationFn: () => decision!.approve
      ? attendanceApi.approveOvertime(decision!.item.id, note.trim() || null)
      : attendanceApi.rejectOvertime(decision!.item.id, note.trim()),
    onSuccess: async (o) => {
      setDecision(null);
      await queryClient.invalidateQueries({ queryKey: ['attendance'] });
      toast.success(`Overtime for ${o.employeeName} ${o.status.toLowerCase()}.`);
    },
    onError: (err) => setError(toApiError(err).fieldErrors.remarks ?? toApiError(err).message),
  });

  const columns: Column<OvertimeRecord>[] = [
    { key: 'date', header: 'Date', className: 'whitespace-nowrap', render: (o) => formatDate(o.date) },
    { key: 'employee', header: 'Employee', render: (o) => `${o.employeeName} (${o.employeeNo})` },
    { key: 'project', header: 'Site', render: (o) => o.projectName },
    { key: 'hours', header: 'Hours', sortKey: 'hours', render: (o) => o.hours },
    { key: 'reason', header: 'Reason', render: (o) => <span className="text-muted">{o.reason ?? '-'}</span> },
    { key: 'by', header: 'Recorded by', render: (o) => o.recordedByName },
    { key: 'status', header: 'Status', render: (o) => <StatusBadge status={o.status} /> },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (o) => isHr && o.status === 'PENDING' && (
        <div className="flex justify-end gap-2">
          <Button size="sm" variant="secondary" onClick={() => { setDecision({ item: o, approve: false }); setNote(''); setError(undefined); }}>Reject</Button>
          <Button size="sm" onClick={() => { setDecision({ item: o, approve: true }); setNote(''); setError(undefined); }}>Approve</Button>
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader title="Overtime" description={isHr ? 'Approved overtime on verified days is paid in payroll.' : 'Overtime you recorded and HR\u2019s decisions.'} />
      <DataTable
        columns={columns}
        page={items.data}
        rowKey={(o) => o.id}
        loading={items.isFetching}
        error={items.isError ? toApiError(items.error).message : null}
        emptyMessage="No overtime to show."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={<div className="sm:ml-auto"><Select aria-label="Filter by status" options={STATUS_OPTIONS} placeholder="Any status" value={status} onChange={(e) => { setStatus(e.target.value); table.resetPage(); }} /></div>}
      />
      <Modal open={decision !== null} title={decision?.approve ? 'Approve overtime' : 'Reject overtime'}
        description={decision ? `${decision.item.employeeName}, ${decision.item.hours} h on ${formatDate(decision.item.date)}${decision.item.attendanceVerified ? '' : '. The attendance is not verified yet, so it will not be paid until it is.'}` : undefined}
        onClose={() => setDecision(null)} busy={decide.isPending}>
        <div className="space-y-4">
          <FormField label={decision?.approve ? 'Note (optional)' : 'Reason'} htmlFor="ot-note" error={error} required={!decision?.approve}>
            <Textarea id="ot-note" rows={3} maxLength={500} invalid={!!error} value={note} onChange={(e) => setNote(e.target.value)} />
          </FormField>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setDecision(null)}>Cancel</Button>
            <Button variant={decision?.approve ? 'primary' : 'danger'} loading={decide.isPending}
              onClick={() => (!decision?.approve && note.trim().length < 5 ? setError(CLIENT_MESSAGES.remarksRequired) : decide.mutate())}>
              {decision?.approve ? 'Approve' : 'Reject'}
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}
