/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.security.iride;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;

/**
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 *
 */
public class IrideSecurityServiceConfig  extends BaseSecurityNamedServiceConfig 
implements SecurityAuthProviderConfig, SecurityRoleServiceConfig{

    private String serverURL;
    private String applicationName;
    private String adminRole;

    public IrideSecurityServiceConfig() {
    }
    
    public IrideSecurityServiceConfig(IrideSecurityServiceConfig other) {
        super(other);
        serverURL = other.getServerURL();
        adminRole = other.getAdminRole();
    }

    @Override
    public String getUserGroupServiceName() {
        return null;
    }

    @Override
    public void setUserGroupServiceName(String userGroupServiceName) {
        
    }
    
    public String getServerURL() {
        return serverURL;
    }
    
    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    
    
    /**
     * @return the applicationName
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * @param applicationName the applicationName to set
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * @return the adminRole
     */
    public String getAdminRole() {
        return adminRole;
    }

    /**
     * @param adminRole the adminRole to set
     */
    public void setAdminRole(String adminRole) {
        this.adminRole = adminRole;
    }

    @Override
    public String getAdminRoleName() {
        return this.getAdminRole();
    }

    @Override
    public void setAdminRoleName(String adminRoleName) {
        this.setAdminRole(adminRoleName);
    }

    @Override
    public String getGroupAdminRoleName() {
        return this.getAdminRole();
    }

    @Override
    public void setGroupAdminRoleName(String adminRoleName) {
        this.setAdminRole(adminRoleName);
    }

    
}
