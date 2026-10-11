import { forwardRef, type SelectHTMLAttributes } from 'react';
import { useQuery } from '@tanstack/react-query';
import { employeesApi } from '../../api/employeesApi';
import { Select } from './Select';

interface EmployeeSelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'children'> {
  placeholder?: string;
  invalid?: boolean;
}

/** Active employees (number and name), for HR forms and filters. */
export const EmployeeSelect = forwardRef<HTMLSelectElement, EmployeeSelectProps>(function EmployeeSelect(
  { placeholder = 'All employees', invalid, ...rest },
  ref,
) {
  const employees = useQuery({
    queryKey: ['employees', 'options'],
    queryFn: () => employeesApi.list({ page: 0, size: 100, sort: 'employeeNo', direction: 'asc' }, { active: 'true' }),
    staleTime: 60_000,
  });
  const options = (employees.data?.content ?? []).map((e) => ({ value: String(e.id), label: `${e.employeeNo} ${e.fullName}` }));
  return <Select ref={ref} options={options} placeholder={placeholder} invalid={invalid} {...rest} />;
});
