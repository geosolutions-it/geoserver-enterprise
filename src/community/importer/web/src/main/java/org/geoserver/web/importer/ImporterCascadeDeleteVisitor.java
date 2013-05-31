/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;

import static org.geoserver.catalog.StyleInfo.*;


/**
 * A cascade delete visitor that will get rid of the un-shared, non core layer styles as well
 *
 * @author Andrea Aime, GeoSolutions
 * @author Andrea Aime, GeoSolutions SAS
 *
 */
public class ImporterCascadeDeleteVisitor extends CascadeDeleteVisitor
{

    Catalog catalog;

    public ImporterCascadeDeleteVisitor(Catalog catalog)
    {
        super(catalog);
        this.catalog = catalog;
    }

    @Override
    public void visit(LayerInfo layer)
    {
        StyleInfo style = layer.getDefaultStyle();

        // remove the layer
        super.visit(layer);

        // NPE protection
        if (style == null)
        {
            return;
        }

        // is the style a core one? If so, don't remove
        Set<String> defaultStyles = new HashSet<String>();
        defaultStyles.addAll(Arrays.asList(DEFAULT_LINE, DEFAULT_POINT, DEFAULT_POLYGON, DEFAULT_RASTER));
        if (defaultStyles.contains(style.getName()))
        {
            return;
        }

        // remove the style (better than just removing directly, will take care of
        // fixing other layers that another admin might have linked to the new styles created by the importer
        // (the importer summary page can stay up for a long time with someone playign with CRS before it
        // may decide that he actually wants to undo it)
        visit(style);
    }
}
