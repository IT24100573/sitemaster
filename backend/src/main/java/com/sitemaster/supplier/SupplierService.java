package com.sitemaster.supplier;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.supplier.dto.SupplierResponse;

/**
 * Approved suppliers. A SUPPLIER user only ever sees their own record.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface SupplierService {

    PageResponse<SupplierResponse> search(Boolean active, String category, PageQuery pageQuery);

    SupplierResponse get(Long id);

    SupplierResponse setActive(Long id, boolean active);
}
