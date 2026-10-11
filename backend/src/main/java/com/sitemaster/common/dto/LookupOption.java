package com.sitemaster.common.dto;

/**
 * One dropdown option served by the API.
 *
 * @param value value sent back to the server
 * @param label text shown to the user
 */
public record LookupOption(String value, String label) {
}
