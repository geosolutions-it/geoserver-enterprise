package org.geoserver.kml;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wms.WMSMapContent;
import org.geotools.map.Layer;
import org.opengis.filter.Filter;

/**
 * Common interface for classes defining a mechanism for regionating KML placemarks.  
 * @author David Winslow
 * @author Andrea Aime
 */
public interface RegionatingStrategy {
    /**
     * Given the KML request context, asks the strategy to return a filter matching only
     * the features that have to be included in the output. 
     * An SLD based strategy will use the current scale, a tiling based one the area occupied
     * by the requested tile and some criteria to fit in features, and so on. 
     * @param context
     * @param layer
     */
    public Filter getFilter(WMSMapContent context, Layer layer);

    /**
     * Clear any cached work (indexing, etc.) for a particular feature type's default regionating 
     * options.
     */
    public void clearCache(FeatureTypeInfo cfg);
}
