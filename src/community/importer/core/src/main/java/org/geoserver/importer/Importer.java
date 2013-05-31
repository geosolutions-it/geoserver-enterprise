/*+
 * Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: Importer.java 174 2012-01-23 15:11:17Z alessio $
 */

package org.geoserver.importer;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.springframework.security.core.Authentication;


/**
 * Beahviour common to both raster and feature importers
 *
 * @author Luca Morandini lmorandini@ieee.org
 * @author Andrea Aime, GeoSolutions SAS
 *
 */
public abstract class Importer implements Runnable
{

    static final Logger LOGGER = Logging.getLogger(FeatureTypeImporter.class);

    protected String defaultSRS;

    protected Catalog catalog;

    protected ImportSummary summary;

    protected boolean cancelled;

    protected Set<Name> resources;

    protected String id;

    protected ImporterThreadManager tm;
    
    protected Authentication authentication;

    /**
     * Constructor
     *
     * @param id Id of importer
     * @param defaultSRS The default SRS to use when data have none
     * @param resources The list of resources to import. Use {@code null} to import all available ones
     * @param catalog The GeoServer catalog
     * @param workspaceNew Marks the workspace as newly created and ready for rollback
     * @param storeNew Marks the store as newly created and ready for rollback
     */
    public Importer(String id, ImporterThreadManager tm, String defaultSRS, Set<Name> resources, Catalog catalog,
        boolean workspaceNew, boolean storeNew)
    {
        this.id = id;
        this.tm = tm;
        this.defaultSRS = defaultSRS;
        this.catalog = catalog;
        this.resources = resources;
        LOGGER.log(Level.FINE, "Importer " + this.toString() + " created");
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public abstract String getProject();

    public void clearTask()
    {
        this.tm.cleanImporter(this.id);
    }
    
    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public abstract void run();

    public final ImportSummary getSummary()
    {
        return summary;
    }

    public final void cancel()
    {
        this.cancelled = true;
        LOGGER.log(Level.FINE, "Importer " + this.toString() + " about to be canceled");
    }

}
