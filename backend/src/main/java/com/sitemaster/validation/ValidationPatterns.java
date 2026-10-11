package com.sitemaster.validation;

/**
 * Regular expressions shared by DTO annotations. The frontend Zod schemas use
 * the same expressions (src/validation/common.ts).
 */
public final class ValidationPatterns {

    /** Letters (any script), spaces, dots and hyphens. */
    public static final String PERSON_NAME = "^[\\p{L} .\\-]+$";

    /** Something@domain.tld, stricter than @Email which accepts "a@b". */
    public static final String EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    /** 0XXXXXXXXX or +94XXXXXXXXX. */
    public static final String SRI_LANKAN_PHONE = "^(0\\d{9}|\\+94\\d{9})$";

    /** Old NIC (9 digits + V/X) or new NIC (12 digits). */
    public static final String NIC = "^(\\d{9}[VvXx]|\\d{12})$";

    /** Three-letter ISO currency code such as LKR. */
    public static final String CURRENCY = "^[A-Z]{3}$";

    /** Trade names: letters, spaces and hyphens. */
    public static final String TRADE = "^[\\p{L} \\-]+$";

    /** Business registration number: letters, digits, spaces, slashes and hyphens, 3-50 characters. */
    public static final String REGISTRATION_NO = "^[A-Za-z0-9 /\\-]{3,50}$";

    /** Unit of measure: letters, digits, spaces, dots and slashes (e.g. m3, kg, bag). */
    public static final String UNIT = "^[\\p{L}0-9 ./]{1,20}$";

    private ValidationPatterns() {
        // constants only
    }
}
