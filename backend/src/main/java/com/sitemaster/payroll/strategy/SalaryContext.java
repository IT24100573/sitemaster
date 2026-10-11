package com.sitemaster.payroll.strategy;

import com.sitemaster.employee.Employee;
import com.sitemaster.payroll.AttendanceTotals;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Everything a salary strategy needs for one employee and month.
 *
 * @param employee     the employee (pay terms)
 * @param period       payroll month
 * @param totals       attendance summary from HR
 * @param workingDays  working days in the month (no Sundays or holidays)
 * @param hoursPerDay  paid hours in a working day (from System Settings)
 */
public record SalaryContext(Employee employee, YearMonth period, AttendanceTotals totals, int workingDays,
                            BigDecimal hoursPerDay) {
}
