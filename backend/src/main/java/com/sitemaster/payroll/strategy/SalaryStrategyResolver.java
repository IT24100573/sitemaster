package com.sitemaster.payroll.strategy;

// PATTERN: Strategy

import com.sitemaster.common.enums.EmploymentType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy pattern - selects the strategy for an employment type. Spring passes
 * in every SalaryCalculationStrategy bean; adding a new employment type means
 * adding one new class, with no change here or in the payroll service.
 */
@Component
public class SalaryStrategyResolver {

    private final Map<EmploymentType, SalaryCalculationStrategy> strategies = new EnumMap<>(EmploymentType.class);

    public SalaryStrategyResolver(List<SalaryCalculationStrategy> available) {
        available.forEach(strategy -> strategies.put(strategy.supports(), strategy));
    }

    public SalaryCalculationStrategy forType(EmploymentType type) {
        SalaryCalculationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No salary strategy for " + type);
        }
        return strategy;
    }
}
