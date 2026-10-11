package com.sitemaster.common.dto;

import com.sitemaster.common.exception.BusinessRuleException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Map;

/**
 * Paging, sorting and search parameters shared by every list endpoint.
 * Rules: page &gt;= 0, size 1-100, search at most 100 characters, and the sort
 * field must be in the endpoint's whitelist (checked in {@link #toPageable}).
 * <p>
 * OOP concept: Encapsulation - validation and conversion to a Pageable live here.
 */
public class PageQuery {

    public static final int DEFAULT_SIZE = 20;

    @Min(value = 0, message = "{page.min}")
    private int page = 0;

    @Min(value = 1, message = "{size.range}")
    @Max(value = 100, message = "{size.range}")
    private int size = DEFAULT_SIZE;

    @Size(max = 50, message = "{sort.invalid}")
    private String sort;

    @Pattern(regexp = "(?i)^(asc|desc)$", message = "{direction.invalid}")
    private String direction;

    @Size(max = 100, message = "{search.size}")
    private String search;

    /**
     * Builds a Pageable.
     *
     * @param allowedSorts     API sort name -&gt; entity property path
     * @param defaultSort      API sort name used when none is given
     * @param defaultDirection direction used when none is given
     */
    public Pageable toPageable(Map<String, String> allowedSorts, String defaultSort, Sort.Direction defaultDirection) {
        String sortKey = sort == null ? defaultSort : sort;
        String property = allowedSorts.get(sortKey);
        if (property == null) {
            throw BusinessRuleException.forField("sort", "sort.invalid");
        }
        Sort.Direction dir = direction == null
                ? (sort == null ? defaultDirection : Sort.Direction.ASC)
                : Sort.Direction.fromString(direction.toLowerCase(Locale.ROOT));
        return PageRequest.of(page, size, Sort.by(dir, property).and(Sort.by(Sort.Direction.DESC, "id")));
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = blankToNull(sort);
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = blankToNull(direction);
    }

    /** Trimmed search text, or null when blank. */
    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = blankToNull(search);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
