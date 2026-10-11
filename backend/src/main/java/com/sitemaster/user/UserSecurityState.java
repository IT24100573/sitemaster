package com.sitemaster.user;

/**
 * The two columns SessionRefreshFilter checks on every request.
 *
 * @param active          whether the user may still sign in
 * @param securityVersion bumped whenever the user's access changes
 */
public record UserSecurityState(boolean active, int securityVersion) {
}
