package com.sitemaster.lookup;

import com.sitemaster.common.dto.LookupOption;

import java.util.List;

/**
 * Dropdown data for the frontend, so no option list is hard-coded in React.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface LookupService {

    List<LookupOption> roles();

    List<LookupOption> employmentTypes();

    List<LookupOption> trades();

    List<LookupOption> modules();

    List<LookupOption> employeeDocumentTypes();

    /** Active staff accounts without an employee record (for "link existing account"). */
    List<LookupOption> unlinkedStaff();

    /** Values of a named enumeration (e.g. "project-statuses"); 404 for an unknown name. */
    List<LookupOption> enumValues(String name);

    /** Material categories from System Settings (also used by the public supplier form). */
    List<LookupOption> materialCategories();

    /** Units of measure from System Settings. */
    List<LookupOption> materialUnits();

    /** Active clients, for the project form. */
    List<LookupOption> activeClients();

    /** Active Project Managers, for the project form. */
    List<LookupOption> projectManagers();

    /** Users matching a search, for filters such as the audit log's user filter. */
    List<LookupOption> users(String search);
}
