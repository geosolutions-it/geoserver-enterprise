/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.context.ApplicationContext;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Various Utilities for Download Services.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 */
final class DownloadUtilities {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(DownloadUtilities.class);

    /**
     * Singleton
     */
    private DownloadUtilities() {
    }

    /**
     * This method checks whether or not the provided geometry is valid {@link Polygon}
     * or not.
     * <p>
     * In case the egometry is not a valid polygon, it throws an {@link IllegalStateException};
     * 
     * @param roi the {@link Geometry} to check.
     * @throws IllegalStateException
     */
    static void checkPolygonROI(Geometry roi) throws IllegalStateException {
        if(roi==null){
            throw new NullPointerException("The provided ROI is null!");
        }
        if (roi instanceof Point || roi instanceof MultiPoint || roi instanceof LineString
                || roi instanceof MultiLineString) {
            throw new IllegalStateException(
                    "The Region of Interest is not a Polygon or Multipolygon!");
        }
        if (roi.isEmpty() || !roi.isValid()) {
            throw new IllegalStateException("The Region of Interest is empyt or invalid!");
        }
    }

    /**
     * Looks for a valid PPIO given the provided mime type and process parameter.
     * 
     * @param p
     * @param context
     * <p>
     * The lenient approach makes this method try harder to send back a result but it is preferrable 
     * to be non-lenient since otherwise we might get s a PPIO which is not really what we need.
     * 
     * @param mime the mime-type for which we are searching for a {@link ProcessParameterIO}
     * @param lenient whether or not trying to be lenient when returning a suitable {@link ProcessParameterIO}.
     * @return either <code>null</code> or the found 
     */
    final static ProcessParameterIO find(Parameter<?> p, ApplicationContext context, String mime,
            boolean lenient) {
        //
        // lenient approach, try to give something back in any case
        //
        if (lenient) {
            return ProcessParameterIO.find(p, context, mime);
        }

        //
        // Strict match case. If we don't find a match we return null
        //
        // enum special treatment
        if (p.type.isEnum()) {
            return new LiteralPPIO(p.type);
        }

        // TODO: come up with some way to flag one as "default"
        List<ProcessParameterIO> all = ProcessParameterIO.findAll(p, context);
        if (all.isEmpty()) {
            return null;
        }

        if (mime != null) {
            for (ProcessParameterIO ppio : all) {
                if (ppio instanceof ComplexPPIO && ((ComplexPPIO) ppio).getMimeType().equals(mime)) {
                    return ppio;
                }
            }
        }

        // unable to find a match
        return null;
    }

    /**
     * This methods checks if the provided {@link FeatureCollection} is empty or not.
     * <p>
     * In case the provided feature collection is empty it throws an {@link IllegalStateException};
     * 
     * @param features the {@link SimpleFeatureCollection} to check
     * @throws IllegalStateException
     */
    final static void checkIsEmptyFeatureCollection(SimpleFeatureCollection features)
            throws IllegalStateException {
        if (features == null || features.isEmpty()) {
            throw new IllegalStateException("Got an empty feature collection.");
        }
    }

    /**
     * Retrieves the native {@link CoordinateReferenceSystem} for the provided {@link ResourceInfo}.
     * 
     * @param resourceInfo
     * @return the native {@link CoordinateReferenceSystem} for the provided {@link ResourceInfo}. 
     * @throws IOException in case something bad happems!
     */
    static CoordinateReferenceSystem getNativeCRS(ResourceInfo resourceInfo)
            throws IOException {
        // prepare native CRS
        ProjectionPolicy pp = resourceInfo.getProjectionPolicy();
        if(pp == null || pp == ProjectionPolicy.FORCE_DECLARED) {
            return resourceInfo.getCRS();
        } else {
            return resourceInfo.getNativeCRS();
        }
    }

    static Geometry transformGeometry(Geometry geometry, CoordinateReferenceSystem crs)throws IOException{
        final CoordinateReferenceSystem geometryCRS=(CoordinateReferenceSystem) geometry.getUserData();
        // find math transform between the two coordinate reference systems
        MathTransform targetTX = null;
        if (!CRS.equalsIgnoreMetadata(geometry,crs)) {
            // we MIGHT have to reproject
            try {
                targetTX = CRS.findMathTransform(geometryCRS, crs,true);
            } catch (Exception e) {
                throw new IOException(e);
            }
            // reproject
            if (!targetTX.isIdentity()) {
                try {
                    geometry = JTS.transform(geometry, targetTX);
                } catch (Exception e) {
                    throw new IOException(e);
                }
    
                // checks
                if (geometry == null) {
                    throw new IllegalStateException(
                            "The Region of Interest is null after going back to native CRS!");
                }
                geometry.setUserData(crs); // set new CRS
                DownloadUtilities.checkPolygonROI(geometry);
            }
        }
        return geometry;
    }

    /**
     * Retrieves the underlying SLD {@link File} for the provided GeoSerevr Style.
     * 
     * @param style the underlying SLD {@link File} for the provided GeoSerevr Style.
     * @return the underlying SLD {@link File} for the provided GeoSerevr Style.
     * @throws IOException 
     */
    static File findStyle(StyleInfo style) throws IOException {
    	GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        File styleFile = loader.find("styles/" + style.getFilename()); //GeoserverDataDirectory.findStyleFile(style.getFilename());
        if (styleFile != null && styleFile.exists() && styleFile.canRead()&& styleFile.isFile())  {
            // the SLD file is public and avaialble, we can attach it to the download.
            return styleFile;
        }
        else {
            // the SLD file is not public, most probably it is located under a workspace.
            // lets try to search for the file inside the same layer workspace folder ...
            File workspaces = loader.get("workspaces").dir(); // find or create
            styleFile = new File( new File( workspaces, style.getWorkspace().getName() +"/styles" ), style.getFilename() );
    
            if (!(styleFile.exists() && styleFile.canRead()&& styleFile.isFile() )) {
                LOGGER.log(Level.FINE,"The style file cannot be found anywhere. We need to skip the SLD file");
                // unfortunately the style file cannot be found anywhere. We need to skip the SLD file!
                return null;
            }
            return styleFile;
        }
    }

    /**
     * Collect all the underlying SLD {@link File}s for the provided GeoServer layer. 
     * @param layerInfo the provided GeoServer layer. 
     * @return all the underlying SLD {@link File}s for the provided GeoServer layer. 
     * @throws IOException 
     */
    static List<File> collectStyles(LayerInfo layerInfo) throws IOException {
        final List<File> styles = new ArrayList<File>();
    
        // default style
        final StyleInfo style = layerInfo.getDefaultStyle();
        File styleFile = findStyle(style);
        if (styleFile != null) {
            styles.add(styleFile);
        }
    
        // other styles
        final Set<StyleInfo> otherStyles = layerInfo.getStyles();
        if (otherStyles != null && !otherStyles.isEmpty()) {
            for (StyleInfo si : otherStyles) {
                styleFile = findStyle(si);
                if (styleFile != null) {
                    styles.add(styleFile);
                }
            }
        }
        return styles;
    }

}
