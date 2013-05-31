/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Utility class used to handle common WFS reprojection issues
 * 
 * @author Andrea Aime, TOPP
 * 
 */
class WFSReprojectionUtil {

    static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    /**
     * Returns the declared CRS given the native CRS and the request WFS version
     * 
     * @param nativeCRS
     * @param wfsVersion
     * @return
     */
    public static CoordinateReferenceSystem getDeclaredCrs(CoordinateReferenceSystem nativeCRS,
            String wfsVersion) {
        try {
            if(nativeCRS == null)
                return null;
            
            if (wfsVersion.equals("1.0.0")) {
                return nativeCRS;
            } else {
                String code = GML2EncodingUtils.epsgCode(nativeCRS);
                //it's possible that we can't do the CRS -> code -> CRS conversion...so we'll just return what we have
                if (code == null) return nativeCRS;
                return CRS.decode("urn:x-ogc:def:crs:EPSG:6.11.2:" + code);
            }
        } catch (Exception e) {
            throw new WFSException("We have had issues trying to flip axis of " + nativeCRS, e);
        }
    }

    /**
     * Returns the declared CRS given a feature type and the request WFS version
     * 
     * @param nativeCRS
     * @param wfsVersion
     * @return
     */
    public static CoordinateReferenceSystem getDeclaredCrs(FeatureType schema, String wfsVersion) {
        CoordinateReferenceSystem crs = (schema.getGeometryDescriptor() != null) ? schema
                .getGeometryDescriptor().getCoordinateReferenceSystem() : null;

        if (schema == null)
            return null;
        return getDeclaredCrs(crs, wfsVersion);
    }

    /**
     * Applies a default CRS to all geometric filter elements that do not
     * already have one
     * 
     * @param nativeCRS
     * @param wfsVersion
     * @return
     */
    public static Filter applyDefaultCRS(Filter filter, CoordinateReferenceSystem defaultCRS) {
        DefaultCRSFilterVisitor defaultVisitor = new DefaultCRSFilterVisitor(ff, defaultCRS);
        return (Filter) filter.accept(defaultVisitor, null);
    }

    /**
     * Reprojects all geometric filter elements to the native CRS of the
     * provided schema
     * 
     * @param filter
     * @param schema
     * @return
     */
    public static Filter reprojectFilter(Filter filter, FeatureType schema) {
        ReprojectingFilterVisitor visitor = new ReprojectingFilterVisitor(ff, schema);
        return (Filter) filter.accept(visitor, null);
    }

    /**
     * Convenience method, same as calling {@link #applyDefaultCRS} and then
     * {@link #reprojectFilter(Filter, SimpleFeatureType)} in a row
     * 
     * @param filter
     * @param schema
     * @param defaultCRS
     * @return
     */
    public static Filter normalizeFilterCRS(Filter filter, FeatureType schema,
            CoordinateReferenceSystem defaultCRS) {
        Filter defaulted = applyDefaultCRS(filter, defaultCRS);
        return reprojectFilter(defaulted, schema);
    }

}
