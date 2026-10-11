import type { ReactNode } from 'react';
import { ArrowDown, ArrowUp, ArrowUpDown, ChevronLeft, ChevronRight, Inbox } from 'lucide-react';
import type { PageResponse, SortDirection } from '../../types/common';
import { Spinner } from './Spinner';

export interface Column<T> {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  /** API sort name; when set the header becomes a sort button. */
  sortKey?: string;
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  page: PageResponse<T> | undefined;
  rowKey: (row: T) => string | number;
  loading?: boolean;
  error?: string | null;
  emptyMessage?: string;
  sort?: string;
  direction?: SortDirection;
  onSort?: (sortKey: string) => void;
  onPageChange?: (page: number) => void;
  toolbar?: ReactNode;
}

/** Paged, sortable table used by every list page. */
export function DataTable<T>({
  columns,
  page,
  rowKey,
  loading = false,
  error = null,
  emptyMessage = 'Nothing to show yet.',
  sort,
  direction,
  onSort,
  onPageChange,
  toolbar,
}: DataTableProps<T>) {
  const rows = page?.content ?? [];
  const first = page && page.totalElements > 0 ? page.page * page.size + 1 : 0;
  const last = page ? page.page * page.size + rows.length : 0;

  return (
    <div className="card overflow-hidden">
      {toolbar && <div className="flex flex-col gap-3 border-b border-line p-4 sm:flex-row sm:items-center">{toolbar}</div>}

      <div className="overflow-x-auto">
        <table className="w-full min-w-[640px] text-left text-sm">
          <thead className="bg-surface text-muted">
            <tr>
              {columns.map((column) => (
                <th
                  key={column.key}
                  scope="col"
                  className={`px-4 py-3 font-medium ${column.className ?? ''}`}
                  aria-sort={
                    column.sortKey && column.sortKey === sort
                      ? direction === 'asc'
                        ? 'ascending'
                        : 'descending'
                      : undefined
                  }
                >
                  {column.sortKey && onSort ? (
                    <button
                      type="button"
                      onClick={() => onSort(column.sortKey as string)}
                      className="inline-flex items-center gap-1 hover:text-ink"
                    >
                      {column.header}
                      {column.sortKey === sort ? (
                        direction === 'asc' ? (
                          <ArrowUp className="h-3.5 w-3.5" aria-hidden />
                        ) : (
                          <ArrowDown className="h-3.5 w-3.5" aria-hidden />
                        )
                      ) : (
                        <ArrowUpDown className="h-3.5 w-3.5 opacity-40" aria-hidden />
                      )}
                    </button>
                  ) : (
                    column.header
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading && rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-12 text-center">
                  <Spinner className="mx-auto h-6 w-6" />
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-12 text-center text-sm text-red-700">
                  {error}
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-12 text-center text-sm text-muted">
                  <Inbox className="mx-auto mb-2 h-6 w-6" aria-hidden />
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={rowKey(row)} className={`border-t border-line ${loading ? 'opacity-60' : ''}`}>
                  {columns.map((column) => (
                    <td key={column.key} className={`px-4 py-3 align-middle ${column.className ?? ''}`}>
                      {column.render(row)}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {page && page.totalElements > 0 && (
        <div className="flex items-center justify-between gap-3 border-t border-line px-4 py-3 text-sm text-muted">
          <span>
            {first}-{last} of {page.totalElements}
          </span>
          <div className="flex items-center gap-1">
            <button
              type="button"
              disabled={page.page === 0}
              onClick={() => onPageChange?.(page.page - 1)}
              className="rounded-md p-1.5 hover:bg-surface hover:text-ink disabled:cursor-not-allowed disabled:opacity-40"
              aria-label="Previous page"
            >
              <ChevronLeft className="h-4 w-4" aria-hidden />
            </button>
            <span className="px-2">
              Page {page.page + 1} of {Math.max(page.totalPages, 1)}
            </span>
            <button
              type="button"
              disabled={page.page + 1 >= page.totalPages}
              onClick={() => onPageChange?.(page.page + 1)}
              className="rounded-md p-1.5 hover:bg-surface hover:text-ink disabled:cursor-not-allowed disabled:opacity-40"
              aria-label="Next page"
            >
              <ChevronRight className="h-4 w-4" aria-hidden />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
