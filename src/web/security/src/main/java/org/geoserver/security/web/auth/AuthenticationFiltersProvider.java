package org.geoserver.security.web.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;

import org.geoserver.security.web.SecurityNamedServiceProvider;

public class AuthenticationFiltersProvider extends SecurityNamedServiceProvider<SecurityAuthFilterConfig> {

    @Override
    protected List<SecurityAuthFilterConfig> getItems() {
        List <SecurityAuthFilterConfig> result = new ArrayList<SecurityAuthFilterConfig>();
        try {
            for (String name : getSecurityManager().listFilters(GeoServerAuthenticationFilter.class)) {
                result.add((SecurityAuthFilterConfig) getSecurityManager().loadFilterConfig(name));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }

}
