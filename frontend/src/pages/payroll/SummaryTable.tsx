import { humanize } from '../../lib/format';
import type { SummaryRow } from '../../types/workforce';

/** HR's attendance summary for a month, one row per employee. */
export function SummaryTable({ rows }: { rows: SummaryRow[] }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[720px] text-sm">
        <thead className="text-left text-muted">
          <tr className="border-b border-line">
            <th scope="col" className="py-2 pr-3 font-medium">Employee</th>
            <th scope="col" className="py-2 pr-3 font-medium">Type</th>
            <th scope="col" className="py-2 pr-3 text-right font-medium">Present</th>
            <th scope="col" className="py-2 pr-3 text-right font-medium">Half days</th>
            <th scope="col" className="py-2 pr-3 text-right font-medium">Absent</th>
            <th scope="col" className="py-2 pr-3 text-right font-medium">Paid leave</th>
            <th scope="col" className="py-2 pr-3 text-right font-medium">Unpaid leave</th>
            <th scope="col" className="py-2 text-right font-medium">Overtime h</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.employeeId} className="border-b border-line last:border-0">
              <td className="py-2 pr-3"><span className="font-medium text-ink">{r.employeeName}</span> <span className="text-xs text-muted">{r.employeeNo}</span></td>
              <td className="py-2 pr-3 text-muted">{humanize(r.employmentType)}</td>
              <td className="py-2 pr-3 text-right">{r.presentDays}</td>
              <td className="py-2 pr-3 text-right">{r.halfDays}</td>
              <td className="py-2 pr-3 text-right">{r.absentDays}</td>
              <td className="py-2 pr-3 text-right">{r.paidLeaveDays}</td>
              <td className="py-2 pr-3 text-right">{r.unpaidLeaveDays}</td>
              <td className="py-2 text-right">{r.overtimeHours}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
