package com.hieupn.book_review.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts JWT to Authentication containing all GrantedAuthorities
 * from roles and permissions claims in the JWT token
 */
@Component
public class JwtRolePermissionConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("sub"));
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> claims = jwt.getClaims();
        Collection<GrantedAuthority> authorities = new HashSet<>();

        // Process roles claim - add ROLE_ prefix
        if (claims.containsKey("roles")) {
            Collection<String> roles = getClaimAsCollection(claims, "roles");
            Collection<GrantedAuthority> roleAuthorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet());
            authorities.addAll(roleAuthorities);
        }

        // Process permissions claim
        if (claims.containsKey("permissions")) {
            Collection<String> permissions = getClaimAsCollection(claims, "permissions");
            Collection<GrantedAuthority> permissionAuthorities = permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
            authorities.addAll(permissionAuthorities);
        }

        // Combine with default authorities (scope)
        authorities.addAll(defaultConverter.convert(jwt));

        return authorities;
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getClaimAsCollection(Map<String, Object> claims, String claimName) {
        Object claim = claims.get(claimName);

        if (claim instanceof String) {
            return Collections.singleton((String) claim);
        } else if (claim instanceof Collection) {
            return ((Collection<?>) claim).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
