/*
 * Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: RasterChooserProvider.java 174 2012-01-23 15:11:17Z alessio $
 */

package org.geoserver.web.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ResourceReference;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.feature.NameImpl;


/**
 * Provides a list of resources for a specific data store
 *
 * @author Luca Morandii lmorandini@ieee.org
 * @author Andrea Aime, GeoSolutions SAS
 *
 *
 */
@SuppressWarnings("serial")
public class RasterChooserProvider extends GeoServerDataProvider<Resource>
{

    static final Property<Resource> NAME = new BeanProperty<Resource>("name", "localName");

    public static final Property<Resource> TYPE = new BeanProperty<Resource>("type", "icon");

    static final List<Property<Resource>> PROPERTIES = Arrays.asList(TYPE, NAME);

    protected File inputFile;

    protected String wsName;

    public RasterChooserProvider(String inputFile, String wsName)
    {
        this.inputFile = new File(inputFile);
        this.wsName = wsName;
    }

    public File[] getInputFiles()
    {

        if (this.inputFile.isDirectory())
        {
            File[] files = this.inputFile.listFiles(GeoTIFFPage.FILE_FILTER);

            // Filters out directories from the list of files shown
            List<File> outFiles = new ArrayList<File>();
            for (File file : files)
            {
                if (!file.isDirectory())
                {
                    outFiles.add(file);
                }
            }

            return (outFiles.toArray(new File[outFiles.size()]));
        }
        else
        {
            return (new File[] { this.inputFile });
        }
    }

    @Override
    public int size()
    {
        return (this.getInputFiles().length);
    }

    @Override
    public List<Resource> getItems()
    {

        List<Resource> result = new ArrayList<Resource>();
        final CatalogIconFactory icons = CatalogIconFactory.get();

        File[] files = this.getInputFiles();
        for (int i = 0; i < files.length; i++)
        {
            NameImpl name = new NameImpl(this.wsName, files[i].getName());
            ResourceReference icon = icons.RASTER_ICON;
            Resource resource = new Resource(name);
            resource.setIcon(icon);
            result.add(resource);
        }

        Collections.sort(result);

        return result;
    }

    @Override
    public List<Resource> getFilteredItems()
    {
        return this.getItems();
    }

    @Override
    public List<Property<Resource>> getProperties()
    {
        return PROPERTIES;
    }

}
