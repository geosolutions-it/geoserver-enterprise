/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.Map;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.MetadataMap;

/**
 * Global GeoServer configuration.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public interface GeoServerInfo extends Info {

    /**
     * Identifier.
     */
    String getId();

    /**
     * The contact information.
     * 
     * @uml.property name="contactInfo"
     * @uml.associationEnd inverse="geoServer:org.geoserver.config.ContactInfo"
     * @deprecated use {@link #getSettings()}
     */
    ContactInfo getContact();

    /**
     * Sets the contact information.
     * 
     * @param contactInfo
     *                The contactInfo to set.
     * @uml.property name="contactInfo"
     * @deprecated use {@link #getSettings()}
     */
    void setContact(ContactInfo contactInfo);

    /**
     * The global settings.
     * <p>
     * Generally client code shoudl not call this method directly, and rather call 
     * {@link GeoServer#getSettings()}.
     * </p>
     */
    SettingsInfo getSettings();

    /**
     * Sets the global settings.
     */
    void setSettings(SettingsInfo settings);

    /**
     * The Java Advanced Imaging configuration.
     */
    JAIInfo getJAI();
    
    /**
     * Sets the Java Advanced Imaging configuration.
     */
    void setJAI( JAIInfo jai );
    
    /**
     * The Coverage Access configuration.
     */
    CoverageAccessInfo getCoverageAccess();
    
    /**
     * Sets the Coverage Access configuration.
     */
    void setCoverageAccess( CoverageAccessInfo coverageInfo );

    /**
     * The default character set.
     * 
     * @uml.property name="charset"
     * @deprecated use {@link #getSettings()}
     */
    String getCharset();

    /**
     * Sets the default character set.
     * 
     * @uml.property name="charset"
     * @deprecated use {@link #getSettings()}
     */
    void setCharset(String charset);

    /**
     * Sets the administrator username.
     */
    String getAdminUsername();
    
    /**
     * The administrator username.
     *
     */
    void setAdminUsername( String adminUsername );
    
    /**
     * The administrator password.
     */
    String getAdminPassword();
   
    /**
     * Sets the administrator password.
     */
    void setAdminPassword( String adminPassword );
    
    /**
     * The title of the GeoServer instance.
     * 
     * @uml.property name="title"
     * @deprecated use {@link #getSettings()}
     */
    String getTitle();

    /**
     * Sets the title of the GeoServer instance.
     * .
     * @uml.property name="title"
     * @deprecated use {@link #getSettings()}
     */
    void setTitle(String title);

    /**
     * A global cap on the number of decimals to use when encoding floating 
     * point numbers.
     * 
     * @uml.property name="numDecimals"
     * @deprecated use {@link #getSettings()}
     */
    int getNumDecimals();

    /**
     * Sets the global cap on the number of decimals to use when encoding floating 
     * point numbers.
     * @uml.property name="numDecimals"
     * @deprecated use {@link #getSettings()}
     */
    void setNumDecimals(int numDecimals);

    /**
     * TODO: not sure what this is supposed to do.
     * @deprecated use {@link #getSettings()}
     */
    String getOnlineResource();
    
    /**
     * @deprecated use {@link #getSettings()}
     */
    void setOnlineResource(String onlineResource);

    /**
     * The url of a proxy in front of the GeoServer instance.
     * <p>
     * This value is used when a reference back to the GeoServer instance must 
     * be made in a response.
     * </p>
     * @uml.property name="proxyBaseUrl"
     * @deprecated use {@link #getSettings()}
     */
    String getProxyBaseUrl();

    /**
     * Sets The url of a proxy in front of the GeoServer instance.
     * @uml.property name="proxyBaseUrl"
     * @deprecated use {@link #getSettings()}
     */
    void setProxyBaseUrl(String proxyBaseUrl);

    /**
     * The base url to use when including a reference to an xml schema document 
     * in a response.
     * @uml.property name="schemaBaseUrl"
     * @deprecated use {@link #getSettings()}
     */
    String getSchemaBaseUrl();

    /**
     * Sets the base url to use when including a reference to an xml schema document 
     * in a response.
     * @uml.property name="schemaBaseUrl"
     * @deprecated use {@link #getSettings()}
     */
    void setSchemaBaseUrl(String schemaBaseUrl);

    /**
     * Verbosity flag.
     * <p>
     * When set GeoServer will log extra information it normally would not.
     * </p>
     * @uml.property name="verbose"
     * @deprecated use {@link #getSettings()}
     */
    boolean isVerbose();

    /**
     * Sets verbosity flag.
     * @uml.property name="verbose"
     * @deprecated use {@link #getSettings()}
     */
    void setVerbose(boolean verbose);

    /**
     * Verbosity flag for exceptions.
     * <p>
     * When set GeoServer will include full stack traces for exceptions.
     * </p>
     * @uml.property name="verboseExceptions"
     * @deprecated use {@link #getSettings()}
     */
    boolean isVerboseExceptions();
    
    /**
     * Set the XML error handling mode for the server.
     * 
     * @see ResourceErrorHandling
     */
    void setResourceErrorHandling(ResourceErrorHandling mode);
    
    /**
     * Get the XML error handling mode for the server.
     */
    ResourceErrorHandling getResourceErrorHandling();

    /**
     * Sets verbosity flag for exceptions.
     * @uml.property name="verboseExceptions"
     * @deprecated use {@link #getSettings()}
     */
    void setVerboseExceptions(boolean verboseExceptions);
    
    /**
     * The update sequence.
     * <p>
     * This value is used by various ogc services to track changes to a capabilities
     * document.
     * </p>
     */
    long getUpdateSequence();
    
    /**
     * Sets the update sequence.
     */
    void setUpdateSequence( long updateSequence );
    
    /**
     * The size of the cache for feature type objects.
     */
    int getFeatureTypeCacheSize();

    /**
     * Sets the size of the cache for feature type objects.
     */
    void setFeatureTypeCacheSize(int featureTypeCacheSize);
   
    /**
     * Flag determining if access to services should occur only through "virtual services". 
     */
    Boolean isGlobalServices();
    
    /**
     * Sets the flag forcing access to services only through virtual services. 
     */
    void setGlobalServices(Boolean globalServices);

    /**
     * Sets logging buffer size of incoming XML Post Requests for WFS,WMS,...
     */
    void setXmlPostRequestLogBufferSize(Integer requestBufferSize);

    /**
     * Gets log buffer size of XML Post Request for WFS,WMS,...
     */
    Integer getXmlPostRequestLogBufferSize();
    
    /**
     * A map of metadata for services.
     *
     * @uml.property name="metadata"
     */
    MetadataMap getMetadata();

    /**
     * Client properties for services.
     * <p>
     * These values are transient, and not persistent.
     * </p>
     */
    Map<Object, Object> getClientProperties();
    
    /**
     * Disposes the global configuration object.
     */
    void dispose();
}
