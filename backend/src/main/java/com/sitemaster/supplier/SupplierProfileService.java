package com.sitemaster.supplier;

import com.sitemaster.supplier.dto.SupplierProfileRequest;
import com.sitemaster.supplier.dto.SupplierResponse;

/**
 * The signed-in supplier's company profile.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface SupplierProfileService {

    SupplierResponse mine();

    SupplierResponse updateMine(SupplierProfileRequest request);
}
