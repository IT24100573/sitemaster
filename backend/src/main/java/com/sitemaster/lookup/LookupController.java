package com.sitemaster.lookup;

import com.sitemaster.common.dto.LookupOption;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dropdown data for signed-in users. Sensitive lists need a permission. */
@RestController
@RequestMapping("/api/lookups")
public class LookupController {

    private final LookupService lookupService;

    public LookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("/roles")
    public List<LookupOption> roles() {
        return lookupService.roles();
    }

    @GetMapping("/employment-types")
    public List<LookupOption> employmentTypes() {
        return lookupService.employmentTypes();
    }

    @GetMapping("/trades")
    public List<LookupOption> trades() {
        return lookupService.trades();
    }

    @GetMapping("/modules")
    public List<LookupOption> modules() {
        return lookupService.modules();
    }

    @GetMapping("/employee-document-types")
    public List<LookupOption> employeeDocumentTypes() {
        return lookupService.employeeDocumentTypes();
    }

    @GetMapping("/enum/{name}")
    public List<LookupOption> enumValues(@PathVariable String name) {
        return lookupService.enumValues(name);
    }

    @GetMapping("/material-categories")
    public List<LookupOption> materialCategories() {
        return lookupService.materialCategories();
    }

    @GetMapping("/material-units")
    public List<LookupOption> materialUnits() {
        return lookupService.materialUnits();
    }

    @GetMapping("/clients")
    @PreAuthorize("hasAnyAuthority('PROJECT:CREATE', 'PROJECT:EDIT', 'CLIENT:VIEW')")
    public List<LookupOption> clients() {
        return lookupService.activeClients();
    }

    @GetMapping("/project-managers")
    @PreAuthorize("hasAnyAuthority('PROJECT:CREATE', 'PROJECT:EDIT')")
    public List<LookupOption> projectManagers() {
        return lookupService.projectManagers();
    }

    @GetMapping("/unlinked-staff")
    @PreAuthorize("hasAuthority('EMPLOYEE:CREATE')")
    public List<LookupOption> unlinkedStaff() {
        return lookupService.unlinkedStaff();
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER:VIEW')")
    public List<LookupOption> users(@RequestParam(required = false) @Size(max = 100, message = "{search.size}") String search) {
        return lookupService.users(search == null || search.isBlank() ? null : search.trim());
    }
}
