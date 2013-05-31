/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.Map;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Service and organizational settings object.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface SettingsInfo extends Info {

    /**
     * The workspace the settings are specific to.
     * <p>
     * Will be null for global settings: {@link GeoServerInfo#getSettings()} 
     * </p>
     * @return A workspace, or <code>null</code>.
     */
    WorkspaceInfo getWorkspace();

    /**
     * Sets the workspace the settings are specific to.
     */
    void setWorkspace(WorkspaceInfo workspace);

    /**
     * The title of the settings instance.
     * 
     */
    String getTitle();

    /**
     * Sets the title of the settings instance.
     */
    void setTitle(String title);
    
    /**
     * The contact information.
     */
    ContactInfo getContact();

    /**
     * Sets the contact information.
     */
    void setContact(ContactInfo contactInfo);

    /**
     * The default character set.
     */
    String getCharset();

    /**
     * Sets the default character set.
     * 
     */
    void setCharset(String charset);
    
    /**
     * A cap on the number of decimals to use when encoding floating point numbers.
     */
    int getNumDecimals();

    /**
     * Sets the cap on the number of decimals to use when encoding floating point numbers.
     */
    void setNumDecimals(int numDecimals);

    /**
     * TODO: not sure what this is supposed to do.
     */
    String getOnlineResource();
    void setOnlineResource(String onlineResource);

    /**
     * The url of a proxy in front of the GeoServer instance.
     * <p>
     * This value is used when a reference back to the GeoServer instance must 
     * be made in a response.
     * </p>
     */
    String getProxyBaseUrl();

    /**
     * Sets The url of a proxy in front of the GeoServer instance.
     */
    void setProxyBaseUrl(String proxyBaseUrl);

    /**
     * The base url to use when including a reference to an xml schema document in a response.
     */
    String getSchemaBaseUrl();

    /**
     * Sets the base url to use when including a reference to an xml schema document in a response.
     */
    void setSchemaBaseUrl(String schemaBaseUrl);

    /**
     * Verbosity flag.
     * <p>
     * When set GeoServer will log extra information it normally would not.
     * </p>
     */
    boolean isVerbose();

    /**
     * Sets verbosity flag.
     */
    void setVerbose(boolean verbose);

    /**
     * Verbosity flag for exceptions.
     * <p>
     * When set GeoServer will include full stack traces for exceptions.
     * </p>
     */
    boolean isVerboseExceptions();

    /**
     * Sets verbosity flag for exceptions.
     */
    void setVerboseExceptions(boolean verboseExceptions);
    
    /**
     * A map of metadata for services.
     */
    MetadataMap getMetadata();

    /**
     * Client properties for services.
     * <p>
     * These values are transient, and not persistent.
     * </p>
     */
    Map<Object, Object> getClientProperties();
}
