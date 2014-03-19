/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.geoserver.wps.WPSStorageCleaner;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Stores a coverage and the file system and returns a link to retrieve it back
 * 
 * @author Andrea Aime - GeoSolutions
 * @author ETj <etj at geo-solutions.it>
 */
@DescribeProcess(title = "Store Coverage", description = "Stores a raster on the server.")
public class StoreCoverage implements GSProcess {

    private final static GeoTiffWriteParams DEFAULT_WRITE_PARAMS;

    static {
        // setting the write parameters (we my want to make these configurable in the future
        DEFAULT_WRITE_PARAMS = new GeoTiffWriteParams();
        DEFAULT_WRITE_PARAMS.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
        DEFAULT_WRITE_PARAMS.setCompressionType("LZW");
        DEFAULT_WRITE_PARAMS.setCompressionQuality(0.75F);
        DEFAULT_WRITE_PARAMS.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
        DEFAULT_WRITE_PARAMS.setTiling(512, 512);
    }

    WPSStorageCleaner storage;

    public StoreCoverage(WPSStorageCleaner storage) {
        this.storage = storage;
    }

    @DescribeResult(name = "coverageLocation", description = "URL at which raster can be accessed")
    public URL execute(
            @DescribeParameter(name = "coverage", description = "Input raster") GridCoverage2D coverage)
            throws IOException {
        final File file = File.createTempFile(coverage.getName().toString(), ".tif", storage.getStorage());

        // TODO check file prior to writing
        GeoTiffWriter writer = new GeoTiffWriter(file);

        // setting the write parameters for this geotiff
        final ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
        params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(
                DEFAULT_WRITE_PARAMS);
        final GeneralParameterValue[] wps = (GeneralParameterValue[]) params.values().toArray(
                new GeneralParameterValue[1]);
        try {
            writer.write(coverage, wps);
        } finally {
            try {
                writer.dispose();
            } catch (Exception e) {
                // we tried, no need to fuss around this one
            }
        }

        return storage.getURL(file);
    }

}
