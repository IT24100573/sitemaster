import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { purchasingApi, supplierPortalApi } from '../../api/purchasingApi';
import { useCurrentUser } from '../../context/AuthContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { formatDate, formatMoney, humanize } from '../../lib/format';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { PageHeader } from '../../components/ui/PageHeader';
import type { PaymentRecord } from '../../types/purchase';

/** Payments made (Accountant) or received (Supplier). */
export default function PaymentsPage() {
  const user = useCurrentUser();
  const isSupplier = user.role === 'SUPPLIER';
  const currency = useCurrency();
  const table = useTableState('paymentDate', 'desc');

  const payments = useQuery({
    queryKey: ['payments', table.params, isSupplier],
    queryFn: () => (isSupplier ? supplierPortalApi.payments(table.params) : purchasingApi.payments(table.params)),
    placeholderData: keepPreviousData,
  });

  const columns: Column<PaymentRecord>[] = [
    { key: 'date', header: 'Date', sortKey: 'paymentDate', className: 'whitespace-nowrap', render: (p) => formatDate(p.paymentDate) },
    ...(isSupplier ? [] : [{ key: 'supplier', header: 'Supplier', render: (p: PaymentRecord) => p.supplierName ?? '-' }]),
    { key: 'invoice', header: 'Invoice', render: (p) => `${p.invoiceNumber ?? '-'}${p.poNumber ? ` (${p.poNumber})` : ''}` },
    { key: 'method', header: 'Method', render: (p) => `${humanize(p.method)}${p.referenceNo ? `, ${p.referenceNo}` : ''}` },
    { key: 'amount', header: 'Amount', sortKey: 'amount', className: 'whitespace-nowrap text-right', render: (p) => formatMoney(p.amount, currency) },
  ];

  return (
    <>
      <PageHeader title={isSupplier ? 'Payments received' : 'Payments'} description={isSupplier ? 'Payments made to your company.' : 'Payments recorded against supplier invoices.'} />
      <DataTable
        columns={columns}
        page={payments.data}
        rowKey={(p) => p.id}
        loading={payments.isFetching}
        error={payments.isError ? toApiError(payments.error).message : null}
        emptyMessage="No payments yet."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
      />
    </>
  );
}
