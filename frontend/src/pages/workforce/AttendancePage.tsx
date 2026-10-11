import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Lock, Pencil, Plus } from 'lucide-react';
import { attendanceApi } from '../../api/workforceApi';
import { useAuth, useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { formatDate } from '../../lib/format';
import { Button } from '../../components/ui/Button';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { AttendanceForm } from './AttendanceForm';
import type { AttendanceRecord } from '../../types/workforce';

function time(value: string | null) {
  return value ? value.slice(11, 16) : '-';
}

/** Attendance on the user's sites (all sites for HR): verify check-ins, mark, correct. */
export default function AttendancePage() {
  const user = useCurrentUser();
  const { hasPermission } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const canMark = hasPermission('ATTENDANCE:EDIT') && (user.role === 'SITE_ENGINEER' || user.role === 'HR_MANAGER');
  const table = useTableState('date', 'desc');
  const [projectId, setProjectId] = useState('');
  const [verified, setVerified] = useState(user.role === 'SITE_ENGINEER' ? 'false' : '');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [form, setForm] = useState<{ record: AttendanceRecord | null } | null>(null);

  const records = useQuery({
    queryKey: ['attendance', 'list', table.params, projectId, verified, from, to],
    queryFn: () => attendanceApi.list(table.params, { projectId: projectId || undefined, verified: verified || undefined, from: from || undefined, to: to || undefined }),
    placeholderData: keepPreviousData,
  });

  const verify = useMutation({
    mutationFn: (id: number) => attendanceApi.verify(id),
    onSuccess: async (a) => {
      await queryClient.invalidateQueries({ queryKey: ['attendance'] });
      toast.success(`${a.employeeName}'s attendance on ${formatDate(a.date)} verified.`);
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const columns: Column<AttendanceRecord>[] = [
    { key: 'date', header: 'Date', sortKey: 'date', className: 'whitespace-nowrap', render: (a) => formatDate(a.date) },
    { key: 'employee', header: 'Employee', render: (a) => (
      <div>
        <p className="font-medium text-ink">{a.employeeName}</p>
        <p className="text-xs text-muted">{a.employeeNo}{a.trade ? `, ${a.trade}` : ''}</p>
      </div>
    ) },
    { key: 'project', header: 'Site', render: (a) => a.projectName },
    { key: 'status', header: 'Status', sortKey: 'status', render: (a) => <StatusBadge status={a.status} /> },
    { key: 'times', header: 'In / out', className: 'whitespace-nowrap', render: (a) => `${time(a.checkIn)} / ${time(a.checkOut)}${a.hoursWorked ? ` (${a.hoursWorked} h)` : ''}` },
    { key: 'ot', header: 'Overtime', render: (a) => (a.overtimeHours ? <span className="whitespace-nowrap">{a.overtimeHours} h <StatusBadge status={a.overtimeStatus ?? 'PENDING'} /></span> : '-') },
    { key: 'verified', header: 'Verified', render: (a) => (a.verified ? <span title={`By ${a.verifiedByName}`}>Yes</span> : <StatusBadge status="PENDING" label="No" />) },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (a) => a.locked ? (
        <span className="inline-flex items-center gap-1 text-xs text-muted" title="Payroll summary sent for this month"><Lock className="h-3.5 w-3.5" aria-hidden />Locked</span>
      ) : (
        <div className="flex justify-end gap-1">
          {a.canVerify && (
            <button type="button" onClick={() => verify.mutate(a.id)} className="rounded-md p-2 text-green-700 hover:bg-surface" aria-label={`Verify ${a.employeeName} on ${a.date}`} title="Verify">
              <CheckCircle2 className="h-4 w-4" aria-hidden />
            </button>
          )}
          {a.canCorrect && (
            <button type="button" onClick={() => setForm({ record: a })} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Correct ${a.employeeName} on ${a.date}`} title="Correct">
              <Pencil className="h-4 w-4" aria-hidden />
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Attendance"
        description={user.role === 'SITE_ENGINEER' ? 'Verify your workers\u2019 check-ins and mark anyone who did not check in. Only verified days are paid.' : 'Daily attendance across sites. Corrections are possible until the month\u2019s payroll summary is sent.'}
        actions={canMark && <Button onClick={() => setForm({ record: null })}><Plus className="h-4 w-4" aria-hidden />Mark attendance</Button>}
      />
      <DataTable
        columns={columns}
        page={records.data}
        rowKey={(a) => a.id}
        loading={records.isFetching}
        error={records.isError ? toApiError(records.error).message : null}
        emptyMessage="No attendance matches these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Employee name or number" />
            <div className="flex flex-wrap gap-2 sm:ml-auto">
              <ProjectSelect aria-label="Filter by project" value={projectId} onChange={(e) => { setProjectId(e.target.value); table.resetPage(); }} />
              <Select aria-label="Filter by verification" options={[{ value: 'false', label: 'Not verified' }, { value: 'true', label: 'Verified' }]} placeholder="All records" value={verified} onChange={(e) => { setVerified(e.target.value); table.resetPage(); }} />
              <input type="date" aria-label="From date" value={from} onChange={(e) => { setFrom(e.target.value); table.resetPage(); }} className="form-input w-auto" />
              <input type="date" aria-label="To date" value={to} onChange={(e) => { setTo(e.target.value); table.resetPage(); }} className="form-input w-auto" />
            </div>
          </>
        }
      />
      <Modal open={form !== null} size="lg" title={form?.record ? 'Correct attendance' : 'Mark attendance'} onClose={() => setForm(null)}>
        {form && <AttendanceForm record={form.record} onDone={() => setForm(null)} />}
      </Modal>
    </>
  );
}
