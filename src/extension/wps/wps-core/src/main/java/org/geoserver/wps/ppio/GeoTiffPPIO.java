/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;

import org.apache.commons.io.IOUtils;
import org.geoserver.wps.WPSException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridCoverageWriter;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.image.ImageWorker;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.EngineeringCRS;

import com.sun.media.jai.operator.ImageReadDescriptor;

/**
 * Decodes/encodes a GeoTIFF file
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class GeoTiffPPIO extends BinaryPPIO {

    private final static GeoTiffWriteParams DEFAULT_WRITE_PARAMS;

    static {
        // setting the write parameters (write out using tiling)
        DEFAULT_WRITE_PARAMS = new GeoTiffWriteParams();
        DEFAULT_WRITE_PARAMS.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
        final Dimension defaultTileSize = JAI.getDefaultTileSize();
        DEFAULT_WRITE_PARAMS.setTiling(defaultTileSize.width, defaultTileSize.height);
    }

    protected GeoTiffPPIO() {
        super(GridCoverage2D.class, GridCoverage2D.class, "image/tiff");
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        // in order to read a grid coverage we need to first store it on disk
        File root = new File(System.getProperty("java.io.tmpdir", "."));
        File f = File.createTempFile("wps", "tiff", root);
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(f);
            IOUtils.copy(input, os);
        } finally {
            IOUtils.closeQuietly(os);
        }

        // and then we try to read it as a geotiff
        AbstractGridFormat format = GridFormatFinder.findFormat(f);
        if (format instanceof UnknownFormat) {
            throw new WPSException(
                    "Could not find the GeoTIFF GT2 format, please check it's in the classpath");
        }
        return format.getReader(f).read(null);
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        GridCoverage2D coverage = (GridCoverage2D) value;
        
        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
        boolean unreferenced = crs == null || crs instanceof EngineeringCRS;
        
        // did we get lucky and all we need to do is to copy a file over?
        final Object fileSource = coverage.getProperty(GridCoverage2DReader.FILE_SOURCE_PROPERTY);
        if (fileSource != null && fileSource instanceof String) {
            File file = new File((String) fileSource);
            if(file.exists()) {
                GeoTiffReader reader = null;
                FileInputStream fis = null;
                try {
                    // geotiff reader won't read unreferenced tiffs unless we tell it to
                    if(unreferenced) {
                        // just check if it has the proper extension for the moment, until
                        // we get a more reliable way to check if it's a tiff
                        String name = file.getName().toLowerCase();
                        if(!name.endsWith(".tiff") && !name.endsWith(".tif")) {
                            throw new IOException("Not a tiff");
                        }
                    } else {
                        reader = new GeoTiffReader(file);
                        reader.read(null);
                    }
                    // ooh, a geotiff already!
                    fis = new FileInputStream(file);
                    IOUtils.copyLarge(fis, os);
                    return;
                } catch(Exception e) {
                    // ok, not a geotiff!
                } finally {
                    if(reader != null) {
                        reader.dispose();
                    } 
                    if(fis != null) {
                        fis.close();
                    }
                }
            }
        }

        // ok, encode in geotiff
        if(unreferenced) {
            new ImageWorker(coverage.getRenderedImage()).writeTIFF(os, "LZW", 0.75f, 256, 256);
        } else {
            GeoTiffFormat format = new GeoTiffFormat();
            final GeoTiffFormat wformat = new GeoTiffFormat();
            final GeoTiffWriteParams wp = new GeoTiffWriteParams();
            wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
            wp.setCompressionType("LZW");
            wp.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
            wp.setTiling(256, 256);
            final ParameterValueGroup wparams = wformat.getWriteParameters();
            wparams.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                    .setValue(wp);
            
            final GeneralParameterValue[] wps = (GeneralParameterValue[]) wparams.values().toArray(
                    new GeneralParameterValue[1]);
            // write out the coverage
            AbstractGridCoverageWriter writer = (AbstractGridCoverageWriter) format.getWriter(os);
            if (writer == null)
                throw new WPSException(
                        "Could not find the GeoTIFF writer, please check it's in the classpath");
            try {
                writer.write(coverage, wps);
            } finally {
                try {
                    writer.dispose();
                } catch (Exception e) {
                    // swallow
                }
            }
        }
    }
    
    @Override
    public String getFileExtension() {
        return "tiff";
    }

}
