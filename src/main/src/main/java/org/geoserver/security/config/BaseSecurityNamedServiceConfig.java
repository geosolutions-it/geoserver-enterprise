/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

/**
 * Base class for named security service configuration objects.
 * 
 * @author christian
 */
public class BaseSecurityNamedServiceConfig implements SecurityNamedServiceConfig {

    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String className;

    public BaseSecurityNamedServiceConfig() {
    }

    public BaseSecurityNamedServiceConfig(BaseSecurityNamedServiceConfig other) {
        name = other.getName();
        className = other.getClassName();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name of the service
     */
    public String getName() {
        return name;
    }

    /**
     * sets the name for a service
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the service class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * The class name of the service to be constructed
     * The class must have a constructor with a string
     * argument, specifying the name of the service
     * 
     * @param className
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Does nothing, subclasses may override.
     */
    @Override
    public void initBeforeSave() {
    }
}
