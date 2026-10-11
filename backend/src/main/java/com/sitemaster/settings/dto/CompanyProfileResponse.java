package com.sitemaster.settings.dto;

/**
 * Public company details (About Us page, app header). No internal settings.
 *
 * @param name        company name
 * @param description company introduction
 * @param address     postal address
 * @param phone       contact phone
 * @param email       contact email
 * @param currency    currency used for amounts shown in the app
 * @param hasLogo     whether GET /api/public/company/logo returns an image
 */
public record CompanyProfileResponse(String name,
                                     String description,
                                     String address,
                                     String phone,
                                     String email,
                                     String currency,
                                     boolean hasLogo) {
}
