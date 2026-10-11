import { useCurrency } from '../../hooks/useCompanyProfile';
import { formatMoney } from '../../lib/format';
import type { Payslip } from '../../types/workforce';

/** Every payslip calculation step with its running total, then the totals. */
export function PayslipLines({ payslip }: { payslip: Payslip }) {
  const currency = useCurrency();
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[480px] text-sm">
        <thead className="text-left text-muted">
          <tr className="border-b border-line">
            <th scope="col" className="py-2 pr-3 font-medium">Item</th>
            <th scope="col" className="py-2 pr-3 text-right font-medium">Amount</th>
            <th scope="col" className="py-2 text-right font-medium">Running total</th>
          </tr>
        </thead>
        <tbody>
          {payslip.lines.map((line, i) => (
            <tr key={i} className="border-b border-line">
              <td className="py-2 pr-3 text-ink">{line.label}</td>
              <td className={`py-2 pr-3 text-right ${Number(line.amount) < 0 ? 'text-red-600' : ''}`}>{formatMoney(line.amount, currency)}</td>
              <td className="py-2 text-right text-muted">{formatMoney(line.runningTotal, currency)}</td>
            </tr>
          ))}
          <tr><td className="py-2 pr-3 text-right text-muted" colSpan={2}>Gross pay</td><td className="py-2 text-right">{formatMoney(payslip.grossPay, currency)}</td></tr>
          <tr><td className="py-2 pr-3 text-right text-muted" colSpan={2}>Deductions</td><td className="py-2 text-right">{formatMoney(payslip.totalDeductions, currency)}</td></tr>
          <tr><td className="py-2 pr-3 text-right font-semibold text-ink" colSpan={2}>Net pay</td><td className="py-2 text-right font-semibold text-ink">{formatMoney(payslip.netPay, currency)}</td></tr>
        </tbody>
      </table>
    </div>
  );
}
