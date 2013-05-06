/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.Arrays;
import java.util.List;

import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;


/**
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 *
 *
 */
public final class ImportUtilities
{

    static final List<String> SHAPEFILE_EXTENSION = Arrays.asList("shp", "shx", "dbf", "prj");

    static final ReferencedEnvelope WORLD = new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);

    /**
     * Computes the maximum number of overviews for a given image size and downsample ratio (returns
     * 0 when no overviews are possible, including in case of incorrect parameters)
     *
     * @param width
     *            Width of image
     * @param heig      ht
     *            Height of image
     * @param minWidth
     *            minimum width of overview
     * @param minHeight
     *            minimum height of overview
     * @param downscale
     *            Ratio of downscale
     */
    public static int computeMaxOverviews(int width, int height, int minWidth, int minHeight,
        int downScale)
    {

        if ((minWidth < 1) || (minHeight < 1) || (width <= minWidth) || (height <= minHeight))
        {
            return 0;
        }

        final double downSampleCorrection = Math.log(downScale);
        int minOverviewsW = (int) ((Math.log(width / minWidth) / downSampleCorrection) + 0.5);
        int minOverviewsH = (int) ((Math.log(height / minHeight) / downSampleCorrection) + 0.5);

        return Math.max(minOverviewsW, minOverviewsH);
    }

    static final AbstractGridFormat GEOTIFF_FORMAT = new GeoTiffFormat();

}
