package com.sitemaster.employee;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.employee.dto.EmployeeCreateRequest;
import com.sitemaster.employee.dto.EmployeeListItem;
import com.sitemaster.employee.dto.EmployeeResponse;
import com.sitemaster.employee.dto.EmployeeUpdateRequest;
import com.sitemaster.file.FileDownload;
import org.springframework.web.multipart.MultipartFile;

/**
 * HR employee records.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface EmployeeService {

    PageResponse<EmployeeListItem> search(EmploymentType type, String trade, Boolean active, PageQuery pageQuery);

    EmployeeResponse get(Long id);

    EmployeeResponse create(EmployeeCreateRequest request);

    EmployeeResponse update(Long id, EmployeeUpdateRequest request);

    EmployeeResponse addDocument(Long employeeId, EmployeeDocumentType type, String title, MultipartFile file);

    EmployeeResponse removeDocument(Long employeeId, Long documentId);

    FileDownload openDocument(Long employeeId, Long documentId);
}
