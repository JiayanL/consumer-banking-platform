package com.bofa.cbp.auth;

import com.bofa.cbp.auth.compliance.ComplianceCategory;
import com.bofa.cbp.auth.compliance.ComplianceCritical;
import io.jsonwebtoken.Claims;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Role + scope enforcement helpers. The auth-service issues tokens
 * with a "roles" list claim; this class centralizes the checks so
 * every service enforces them the same way.
 */
public final class RoleChecker {

    private RoleChecker() {}

    public static List<String> rolesOf(Claims claims) {
        if (claims == null) return Collections.emptyList();
        Object raw = claims.get("roles");
        if (raw instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return Collections.emptyList();
    }

    @ComplianceCritical(category = ComplianceCategory.AUTHENTICATION)
    public static boolean hasAnyRole(Claims claims, Collection<String> required) {
        if (required == null || required.isEmpty()) return true;
        List<String> have = rolesOf(claims);
        for (String r : required) {
            if (have.contains(r)) return true;
        }
        return false;
    }

    @ComplianceCritical(category = ComplianceCategory.AUTHENTICATION)
    public static boolean hasAllRoles(Claims claims, Collection<String> required) {
        if (required == null || required.isEmpty()) return true;
        List<String> have = rolesOf(claims);
        return have.containsAll(Set.copyOf(required));
    }
}
