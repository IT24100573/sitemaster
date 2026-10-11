import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ChevronLeft, ChevronRight, LogIn, LogOut } from 'lucide-react';
import { attendanceApi } from '../../api/workforceApi';
import { useToast } from '../../context/ToastContext';
import { toApiError } from '../../lib/apiError';
import { formatDate } from '../../lib/format';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { PageHeader } from '../../components/ui/PageHeader';
import { Select } from '../../components/ui/Select';
import { Spinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';

function time(value: string | null) {
  return value ? new Date(value).toLocaleTimeString('en-LK', { hour: '2-digit', minute: '2-digit' }) : '-';
}

/** Worker / site engineer: check in and out, and see this month's record. */
export default function MyAttendancePage() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const now = new Date();
  const [period, setPeriod] = useState({ year: now.getFullYear(), month: now.getMonth() + 1 });
  const [projectId, setProjectId] = useState('');

  const today = useQuery({ queryKey: ['attendance', 'today'], queryFn: attendanceApi.today });
  const month = useQuery({
    queryKey: ['attendance', 'mine', period],
    queryFn: () => attendanceApi.mine(period.year, period.month),
  });

  const refresh = async (message: string) => {
    await queryClient.invalidateQueries({ queryKey: ['attendance'] });
    toast.success(message);
  };
  const checkIn = useMutation({
    mutationFn: () => attendanceApi.checkIn(Number(projectId || today.data?.projects[0]?.value)),
    onSuccess: () => refresh('Checked in. Have a safe day.'),
    onError: (error) => toast.error(toApiError(error).message),
  });
  const checkOut = useMutation({
    mutationFn: attendanceApi.checkOut,
    onSuccess: () => refresh('Checked out.'),
    onError: (error) => toast.error(toApiError(error).message),
  });

  const move = (delta: number) => {
    const d = new Date(period.year, period.month - 1 + delta, 1);
    setPeriod({ year: d.getFullYear(), month: d.getMonth() + 1 });
  };
  const isCurrent = period.year === now.getFullYear() && period.month === now.getMonth() + 1;
  const record = today.data?.today;
  const monthLabel = new Date(period.year, period.month - 1, 1).toLocaleDateString('en-LK', { month: 'long', year: 'numeric' });

  return (
    <>
      <PageHeader title="My attendance" description="Check in when you arrive on site and check out when you leave. Your site engineer verifies each day." />
      <Card title="Today" highlighted className="mb-6">
        {today.isLoading ? <Spinner /> : today.isError ? (
          <Alert tone="error">{toApiError(today.error).message}</Alert>
        ) : today.data?.onLeave ? (
          <p className="text-sm text-ink">You are on approved leave today.</p>
        ) : !record ? (
          today.data && today.data.projects.length === 0 ? (
            <p className="text-sm text-muted">You are not on any active project. Ask your project manager to add you to the team.</p>
          ) : (
            <div className="flex flex-wrap items-end gap-3">
              {today.data && today.data.projects.length > 1 && (
                <div className="min-w-[220px]">
                  <label htmlFor="checkin-project" className="mb-1.5 block text-sm font-medium text-ink">Site</label>
                  <Select id="checkin-project" options={today.data.projects} placeholder="Choose a site" value={projectId} onChange={(e) => setProjectId(e.target.value)} />
                </div>
              )}
              <Button
                size="lg"
                loading={checkIn.isPending}
                disabled={(today.data?.projects.length ?? 0) > 1 && !projectId}
                onClick={() => checkIn.mutate()}
              >
                <LogIn className="h-5 w-5" aria-hidden />
                Check in{today.data?.projects.length === 1 ? ` at ${today.data.projects[0].label}` : ''}
              </Button>
            </div>
          )
        ) : (
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="text-sm">
              <p className="font-medium text-ink">{record.projectName}</p>
              <p className="text-muted">In {time(record.checkIn)}, out {time(record.checkOut)}{record.hoursWorked ? `, ${record.hoursWorked} h` : ''}</p>
              <p className="mt-1">{record.verified ? <StatusBadge status="APPROVED" label="Verified" /> : <StatusBadge status="PENDING" label="Waiting for verification" />}</p>
            </div>
            {record.checkIn && !record.checkOut && (
              <Button size="lg" variant="secondary" loading={checkOut.isPending} onClick={() => checkOut.mutate()}>
                <LogOut className="h-5 w-5" aria-hidden />
                Check out
              </Button>
            )}
          </div>
        )}
      </Card>

      <Card
        title={monthLabel}
        actions={
          <div className="flex gap-1">
            <button type="button" onClick={() => move(-1)} className="rounded-md p-2 text-muted hover:bg-surface" aria-label="Previous month">
              <ChevronLeft className="h-4 w-4" aria-hidden />
            </button>
            <button type="button" onClick={() => move(1)} disabled={isCurrent} className="rounded-md p-2 text-muted hover:bg-surface disabled:opacity-40" aria-label="Next month">
              <ChevronRight className="h-4 w-4" aria-hidden />
            </button>
          </div>
        }
      >
        {month.isLoading ? <Spinner /> : month.isError ? <Alert tone="error">{toApiError(month.error).message}</Alert> : (month.data ?? []).length === 0 ? (
          <p className="text-sm text-muted">No attendance recorded this month.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[560px] text-sm">
              <thead className="text-left text-muted">
                <tr className="border-b border-line">
                  <th scope="col" className="py-2 pr-3 font-medium">Date</th>
                  <th scope="col" className="py-2 pr-3 font-medium">Site</th>
                  <th scope="col" className="py-2 pr-3 font-medium">Status</th>
                  <th scope="col" className="py-2 pr-3 font-medium">In / out</th>
                  <th scope="col" className="py-2 pr-3 font-medium">Overtime</th>
                  <th scope="col" className="py-2 font-medium">Verified</th>
                </tr>
              </thead>
              <tbody>
                {month.data?.map((a) => (
                  <tr key={a.id} className="border-b border-line last:border-0">
                    <td className="py-2 pr-3 whitespace-nowrap">{formatDate(a.date)}</td>
                    <td className="py-2 pr-3">{a.projectName}</td>
                    <td className="py-2 pr-3"><StatusBadge status={a.status} /></td>
                    <td className="py-2 pr-3 whitespace-nowrap text-muted">{time(a.checkIn)} / {time(a.checkOut)}</td>
                    <td className="py-2 pr-3">{a.overtimeHours ? <span>{a.overtimeHours} h <StatusBadge status={a.overtimeStatus ?? 'PENDING'} /></span> : '-'}</td>
                    <td className="py-2">{a.verified ? 'Yes' : 'Not yet'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  );
}
