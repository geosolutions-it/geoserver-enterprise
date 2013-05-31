/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.rememberme.RememberMeServicesConfig;


/**
 * {@link GeoServerSecurityManager} configuration object.
 * 
 * @author christian
 */
public class SecurityManagerConfig implements SecurityConfig {

    private static final long serialVersionUID = 1L;

    private String roleServiceName;
    private List<String> authProviderNames = new ArrayList<String>();
    private Boolean anonymousAuth = Boolean.TRUE;
    private String configPasswordEncrypterName;
    private boolean encryptingUrlParams;

    private GeoServerSecurityFilterChain filterChain = new GeoServerSecurityFilterChain();
    private RememberMeServicesConfig rememberMeService = new RememberMeServicesConfig();

    public SecurityManagerConfig() {
    }

    public SecurityManagerConfig(SecurityManagerConfig config) {
        this.roleServiceName = config.getRoleServiceName();
        this.authProviderNames = config.getAuthProviderNames() != null ? 
            new ArrayList<String>(config.getAuthProviderNames()) : null;
        this.anonymousAuth = config.isAnonymousAuth();
        this.filterChain = config.getFilterChain() != null ? 
            new GeoServerSecurityFilterChain(config.getFilterChain()) : null;
        this.rememberMeService = new RememberMeServicesConfig(config.getRememberMeService());
        this.encryptingUrlParams = config.isEncryptingUrlParams();
        this.configPasswordEncrypterName = config.getConfigPasswordEncrypterName();
        //this.masterPasswordURL=config.getMasterPasswordURL();
        //this.masterPasswordStrategy=config.getMasterPasswordStrategy();
    }

    private Object readResolve() {
        authProviderNames = authProviderNames != null ? authProviderNames : new ArrayList<String>();
        anonymousAuth = anonymousAuth != null ? anonymousAuth : Boolean.TRUE;
        filterChain = filterChain != null ? filterChain : new GeoServerSecurityFilterChain();
        rememberMeService = rememberMeService != null ? rememberMeService : new RememberMeServicesConfig();
        return this;
    }

    /**
     * Name of {@link GeoServerRoleService} object.
     */
    public String getRoleServiceName() {
        return roleServiceName;
    }
    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }

    /**
     * @return list of names for {@link GeoServerAuthenticationProvider} objects
     */
    public List<String> getAuthProviderNames() {
        return authProviderNames;
    }

    /**
     * Flag determining if anonymous authentication is active.
     */
    public Boolean isAnonymousAuth() {
        return anonymousAuth;
    }

    /**
     * Sets flag determining if anonymous authentication is active.
     */
    public void setAnonymousAuth(Boolean anonymousAuth) {
        this.anonymousAuth = anonymousAuth;
    }

    /**
     * The security filter chain.
     */
    public GeoServerSecurityFilterChain getFilterChain() {
        return filterChain;
    }

    public void setFilterChain(GeoServerSecurityFilterChain filterChain) {
        this.filterChain = filterChain;
    }

    /**
     * The remember me service.
     */
    public RememberMeServicesConfig getRememberMeService() {
        return rememberMeService;
    }

    public void setRememberMeService(RememberMeServicesConfig rememberMeService) {
        this.rememberMeService = rememberMeService;
    }

    /**
     * Flag controlling if web admin should encrypt url parameters.
     */
    public boolean isEncryptingUrlParams() {
        return encryptingUrlParams;
    }
    public void setEncryptingUrlParams(boolean encryptingUrlParams) {
        this.encryptingUrlParams = encryptingUrlParams;
    }

    /**
     * The name of the password encrypter for encrypting password in configuration files. 
     */
    public String getConfigPasswordEncrypterName() {
        return configPasswordEncrypterName;
    }
    public void setConfigPasswordEncrypterName(String configPasswordEncrypterName) {
        this.configPasswordEncrypterName = configPasswordEncrypterName;
    }

}
