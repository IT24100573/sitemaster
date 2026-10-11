package com.sitemaster.supplier;

import com.sitemaster.common.dto.MessageResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.ApplicationStatus;
import com.sitemaster.file.FileDownload;
import com.sitemaster.supplier.dto.SupplierApplicationForm;
import com.sitemaster.supplier.dto.SupplierApplicationResponse;

/**
 * Supplier onboarding: public application, then Admin approval or rejection.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface SupplierApplicationService {

    MessageResponse submit(SupplierApplicationForm form);

    PageResponse<SupplierApplicationResponse> search(ApplicationStatus status, PageQuery pageQuery);

    SupplierApplicationResponse get(Long id);

    FileDownload openDocument(Long id);

    SupplierApplicationResponse approve(Long id, String remarks);

    SupplierApplicationResponse reject(Long id, String remarks);
}
