import { useCallback, useMemo, useState } from 'react';
import { useDebouncedValue } from './useDebouncedValue';
import type { PageParams, SortDirection } from '../types/common';

const PAGE_SIZE = 20;

/**
 * Paging, sorting and search state for a list page. Changing the search or a
 * filter returns to the first page.
 */
export function useTableState(defaultSort: string, defaultDirection: SortDirection = 'asc') {
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState(defaultSort);
  const [direction, setDirection] = useState<SortDirection>(defaultDirection);
  const [searchInput, setSearchInput] = useState('');
  const search = useDebouncedValue(searchInput.trim());

  const params = useMemo<PageParams>(
    () => ({ page, size: PAGE_SIZE, sort, direction, search: search || undefined }),
    [page, sort, direction, search],
  );

  const toggleSort = useCallback(
    (field: string) => {
      if (field === sort) {
        setDirection((current) => (current === 'asc' ? 'desc' : 'asc'));
      } else {
        setSort(field);
        setDirection('asc');
      }
      setPage(0);
    },
    [sort],
  );

  const changeSearch = useCallback((value: string) => {
    setSearchInput(value);
    setPage(0);
  }, []);

  const resetPage = useCallback(() => setPage(0), []);

  return { params, page, setPage, sort, direction, toggleSort, searchInput, changeSearch, resetPage };
}
