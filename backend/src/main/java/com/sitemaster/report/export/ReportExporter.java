package com.sitemaster.report.export;

// PATTERN: Factory Method

import com.sitemaster.report.ReportTable;

/**
 * Factory Method pattern - the PRODUCT interface. Each format renders the same
 * {@link ReportTable}; callers never know which class they got.
 * <p>
 * OOP concepts: Abstraction and Polymorphism.
 */
public interface ReportExporter {

    byte[] export(ReportTable table);

    String contentType();

    String extension();
}
