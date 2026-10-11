package com.sitemaster.report.generator;

import com.sitemaster.report.ReportCriteria;
import com.sitemaster.report.ReportTable;
import com.sitemaster.report.ReportType;

/**
 * Produces the data of one report type as a table. Access to the report type
 * is checked by the report service; each generator applies project visibility.
 * <p>
 * OOP concepts: Abstraction and Polymorphism - the service picks the generator
 * for a type and never knows how it gathers its rows.
 */
public interface ReportGenerator {

    ReportType type();

    ReportTable generate(ReportCriteria criteria);
}
