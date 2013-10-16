/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.raster.algebra;


import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.geoserver.data.test.MockData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.filter.v1_0.OGCConfiguration;
import org.geotools.image.ImageWorker;
import org.geotools.xml.Parser;
import org.opengis.filter.Filter;
import org.xml.sax.SAXException;

import com.sun.media.jai.util.ImageUtil;

/**
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class BaseRasterAlgebraTest extends WPSTestSupport{
    
    // dem coverages
    public static QName srtm_39_04_1 = new QName(WCS_URI, "srtm_39_04_1", WCS_PREFIX);
    public static QName srtm_39_04_2 = new QName(WCS_URI, "srtm_39_04_2", WCS_PREFIX);
    public static QName srtm_39_04_3 = new QName(WCS_URI, "srtm_39_04_3", WCS_PREFIX);
    public static QName srtm_39_04 = new QName(WCS_URI, "srtm_39_04", WCS_PREFIX);

    private OGCConfiguration configuration;
    
    
    private Parser parser;

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWcs11Coverages();
       
        
        dataDirectory.addCoverage(srtm_39_04_1, getClass().getResource( "srtm_39_04_1.tiff"), MockData.TIFF, "raster.sld");
        dataDirectory.addCoverage(srtm_39_04_2, getClass().getResource( "srtm_39_04_2.tiff"), MockData.TIFF, "raster.sld");
        dataDirectory.addCoverage(srtm_39_04_3, getClass().getResource( "srtm_39_04_3.tiff"), MockData.TIFF, "raster.sld");
        dataDirectory.addCoverage(srtm_39_04, getClass().getResource( "srtm_39_04.tiff"), MockData.TIFF, "raster.sld");
    }    
    
    
    
    /**
     * @param xml
     * @return 
     * @throws Exception
     */
    protected Filter parseFilter(final File xml) throws IOException, SAXException,
            ParserConfigurationException, FileNotFoundException {
        FileInputStream input =null;
        try{
            input=new FileInputStream(xml);
            return (Filter) parser.parse( input );
        }finally{
            if(input!=null){
                IOUtils.closeQuietly(input);
            }
        }
    } 

//    /**
//     * Adds the needed dem coverages for.
//     * @param testData 
//     */
//    @SuppressWarnings("rawtypes")
//    public void addDEMCoverages() throws Exception {
//        String styleName = "raster";
//        testData.addStyle(styleName, "raster.sld", MockData.class, getCatalog());
//        
//        Map<LayerProperty, Object> props = new HashMap<SystemTestData.LayerProperty, Object>();
//        props.put(LayerProperty.STYLE, styleName);
//        
//        // 
//        testData.addRasterLayer(srtm_39_04_1, "srtm_39_04_1.tiff", TIFF, props, getClass(), getCatalog());
//        testData.addRasterLayer(srtm_39_04_2, "srtm_39_04_2.tiff", TIFF, props,  getClass(), getCatalog());
//        testData.addRasterLayer(srtm_39_04_3, "srtm_39_04_3.tiff", TIFF, props,  getClass(), getCatalog());
//        testData.addRasterLayer(srtm_39_04, "srtm_39_04.tiff", TIFF, props,  getClass(), getCatalog());
//    }

    /**
     * @param path 
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws FileNotFoundException
     */
    protected Filter getFilter(String path) throws Exception{
        final File xml= new File(path);
        final Filter filter = parseFilter(xml);
        Assert.assertNotNull(filter);
        return filter;
    }

    
    /**
     * Testing the provided {@link GridCoverage2D} for being binary.
     * 
     * @param gc
     */
    protected void testBinaryGC(GridCoverage2D gc) {
        
        // check the produced image
        final RenderedImage image= gc.getRenderedImage();
        testBinaryImage(image);
    }

    /**
     * Testing the provided {@link RenderedImage} for being binary.
     * 
     * @param image
     */
    protected void testBinaryImage(final RenderedImage image) {
        final ImageWorker worker = new ImageWorker(image);
        // check values
        final double[] maximum = worker.getMaximums();
        Assert.assertNotNull(maximum);
        Assert.assertEquals(1, maximum.length);
        Assert.assertEquals(1.0, maximum[0],1E-6);
        final double[] minimum = worker.getMinimums();
        Assert.assertNotNull(minimum);
        Assert.assertEquals(1, minimum.length);
        Assert.assertEquals(0.0, minimum[0],1E-6);
        Assert.assertTrue(ImageUtil.isBinary(image.getSampleModel())); // checking that the final renderedimage is binary
    }
    
    /**
     * Testing the provided {@link RenderedImage} after max.
     * 
     * @param image
     */
    protected void testMaxImage(final RenderedImage image) {
        final ImageWorker worker = new ImageWorker(image);
        // check values
        final double[] maximum = worker.getMaximums();
        Assert.assertNotNull(maximum);
        Assert.assertEquals(1, maximum.length);
        Assert.assertEquals(1.0, maximum[0],1E-6);
        final double[] minimum = worker.getMinimums();
        Assert.assertNotNull(minimum);
        Assert.assertEquals(1, minimum.length);
        Assert.assertEquals(0, minimum[0],1E-6);
    }
    
    /**
     * Testing the provided {@link RenderedImage} after min
     * 
     * @param image
     */
    protected void testMinImage(final RenderedImage image) {
        final ImageWorker worker = new ImageWorker(image);
        // check values
        final double[] maximum = worker.getMaximums();
        Assert.assertNotNull(maximum);
        Assert.assertEquals(1, maximum.length);
        Assert.assertEquals(1909, maximum[0],1E-6);
        final double[] minimum = worker.getMinimums();
        Assert.assertNotNull(minimum);
        Assert.assertEquals(1, minimum.length);
        Assert.assertEquals(-32768, minimum[0],1E-6);
    }
    
    /**
     * Testing the provided {@link RenderedImage} after min
     * 
     * @param image
     */
    protected void testAbsImage(final RenderedImage image) {
        final ImageWorker worker = new ImageWorker(image);
        // check values
        final double[] maximum = worker.getMaximums();
        final double[] minimum = worker.getMinimums();
        Assert.assertNotNull(maximum);
        Assert.assertEquals(1, maximum.length);
        Assert.assertEquals(2049.0, maximum[0],1E-6);
        Assert.assertNotNull(minimum);
        Assert.assertEquals(1, minimum.length);
        Assert.assertEquals(-32768, minimum[0],1E-6);
    }
    
    /**
     * Testing the provided {@link RenderedImage} after min
     * 
     * @param image
     */
    protected void testExpImage(final RenderedImage image) {
        final ImageWorker worker = new ImageWorker(image);
        // check values
        final double[] maximum = worker.getMaximums();
        final double[] minimum = worker.getMinimums();
        Assert.assertNotNull(maximum);
        Assert.assertEquals(1, maximum.length);
        Assert.assertEquals(32767.0, maximum[0],1E-6);
        Assert.assertNotNull(minimum);
        Assert.assertEquals(1, minimum.length);
        Assert.assertEquals(0, minimum[0],1E-6);
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        configuration = new org.geotools.filter.v1_0.OGCConfiguration();
        parser = new Parser( configuration );
    }
}
