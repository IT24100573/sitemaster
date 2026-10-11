import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { attendanceApi } from '../../api/workforceApi';
import { useCurrentUser } from '../../context/AuthContext';
import { toApiError } from '../../lib/apiError';
import { formatMonth } from '../../lib/format';
import { Alert } from '../../components/ui/Alert';
import { Card } from '../../components/ui/Card';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { Spinner } from '../../components/ui/Spinner';

const CELL: Record<string, string> = {
  P: 'bg-green-100 text-green-800',
  H: 'bg-amber-100 text-amber-800',
  A: 'bg-red-100 text-red-700',
  L: 'bg-blue-100 text-blue-800',
};

/** Month grid: one row per employee, one column per day. Lowercase = not yet verified. */
export default function AttendanceSheetPage() {
  const user = useCurrentUser();
  const needsProject = !['ADMIN', 'HR_MANAGER', 'ACCOUNTANT'].includes(user.role);
  const now = new Date();
  const [value, setValue] = useState(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`);
  const [projectId, setProjectId] = useState('');
  const [year, month] = value.split('-').map(Number);
  const days = new Date(year, month, 0).getDate();

  const sheet = useQuery({
    queryKey: ['attendance', 'sheet', value, projectId],
    queryFn: () => attendanceApi.sheet(year, month, projectId || undefined),
    enabled: !!year && !!month && (!needsProject || projectId !== ''),
  });

  return (
    <>
      <PageHeader title="Attendance sheet" description="P present, H half day, A absent, L leave. Lowercase letters are not verified yet and will not be paid." />
      <div className="mb-4 flex flex-wrap gap-2">
        <input type="month" aria-label="Month" value={value} max={`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`} onChange={(e) => setValue(e.target.value)} className="form-input w-auto" />
        <ProjectSelect aria-label="Project" placeholder={needsProject ? 'Choose a project' : 'All projects'} value={projectId} onChange={(e) => setProjectId(e.target.value)} />
      </div>
      {needsProject && !projectId ? (
        <p className="card p-8 text-center text-sm text-muted">Choose a project to see its sheet.</p>
      ) : sheet.isLoading ? <Spinner /> : sheet.isError ? <Alert tone="error">{toApiError(sheet.error).message}</Alert> : (
        <Card title={formatMonth(year, month)}>
          {(sheet.data ?? []).length === 0 ? <p className="text-sm text-muted">No attendance recorded.</p> : (
            <div className="overflow-x-auto">
              <table className="text-xs">
                <thead>
                  <tr className="text-muted">
                    <th scope="col" className="sticky left-0 bg-white py-2 pr-3 text-left font-medium">Employee</th>
                    {Array.from({ length: days }, (_, i) => <th key={i} scope="col" className="w-7 py-2 text-center font-medium">{i + 1}</th>)}
                    <th scope="col" className="px-2 py-2 font-medium">P</th>
                    <th scope="col" className="px-2 py-2 font-medium">H</th>
                    <th scope="col" className="px-2 py-2 font-medium">A</th>
                    <th scope="col" className="px-2 py-2 font-medium">L</th>
                    <th scope="col" className="px-2 py-2 font-medium">To verify</th>
                  </tr>
                </thead>
                <tbody>
                  {sheet.data?.map((row) => (
                    <tr key={row.employeeId} className="border-t border-line">
                      <th scope="row" className="sticky left-0 whitespace-nowrap bg-white py-1.5 pr-3 text-left font-medium text-ink">{row.employeeNo} {row.employeeName}</th>
                      {Array.from({ length: days }, (_, i) => {
                        const code = row.days[String(i + 1)];
                        return (
                          <td key={i} className="p-0.5 text-center">
                            {code && <span className={`inline-flex h-6 w-6 items-center justify-center rounded ${CELL[code.toUpperCase()]} ${code === code.toLowerCase() ? 'opacity-60 ring-1 ring-dashed ring-current' : 'font-semibold'}`}>{code}</span>}
                          </td>
                        );
                      })}
                      <td className="px-2 text-center">{row.present}</td>
                      <td className="px-2 text-center">{row.halfDays}</td>
                      <td className="px-2 text-center">{row.absent}</td>
                      <td className="px-2 text-center">{row.onLeave}</td>
                      <td className={`px-2 text-center ${row.unverified > 0 ? 'font-semibold text-red-600' : ''}`}>{row.unverified}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      )}
    </>
  );
}
