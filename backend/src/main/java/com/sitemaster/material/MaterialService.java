package com.sitemaster.material;

import com.sitemaster.common.dto.LookupOption;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.material.dto.MaterialRequestBody;
import com.sitemaster.material.dto.MaterialResponse;
import com.sitemaster.material.dto.StockResponse;

import java.util.List;

/**
 * Material catalogue and site stock.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface MaterialService {

    PageResponse<MaterialResponse> search(String category, Boolean active, PageQuery pageQuery);

    List<LookupOption> activeOptions();

    MaterialResponse create(MaterialRequestBody request);

    MaterialResponse update(Long id, MaterialRequestBody request);

    MaterialResponse setActive(Long id, boolean active);

    List<StockResponse> stock(Long projectId);
}
