///*
// *  GeoServer-Manager - Simple Manager Library for GeoServer
// *  
// *  Copyright (C) 2007,2011 GeoSolutions S.A.S.
// *  http://www.geo-solutions.it
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// * 
// * The above copyright notice and this permission notice shall be included in
// * all copies or substantial portions of the Software.
// * 
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// * THE SOFTWARE.
// */
//
//package it.geosolutions.geoserver.jms.test.rest;
//
//
//import it.geosolutions.geoserver.rest.decoder.RESTCoverageStore;
//import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FilenameFilter;
//import java.io.IOException;
//
//import org.apache.commons.io.FilenameUtils;
//import org.apache.commons.io.filefilter.SuffixFileFilter;
//import org.apache.log4j.Logger;
//import org.junit.Test;
//import org.springframework.core.io.ClassPathResource;
//
///**
// * Testcase for publishing layers on geoserver.
// * We need a running GeoServer to properly run the tests. 
// * Login credentials are hardcoded at the moment (localhost:8080 admin/geoserver).
// * If such geoserver instance cannot be contacted, tests will be skipped.
// *
// *
// * @author etj
// */
//public class ConfigTest extends GeoserverRESTTest {
//    private final static Logger LOGGER = Logger.getLogger(ConfigTest.class);
//
//    private static final String DEFAULT_WS = "geosolutions";
//
//
//    public ConfigTest() {
//        super("ConfigTest");
//    }
//
//
//    @Test
////  @Ignore
//    public void publishDBLayer() throws FileNotFoundException, IOException {
//        if (!enabled()) return;
//        deleteAll();
//
////        assertTrue(reader.getWorkspaces().isEmpty());
//        assertTrue(publisher.createWorkspace(DEFAULT_WS));
//
//        insertStyles();
//        insertExternalGeotiff();
//        insertShape();
//
//        boolean ok = publisher.publishDBLayer(DEFAULT_WS, "pg_kids", "easia_gaul_0_aggr", "EPSG:4326", "default_polygon");
//        assertTrue(ok);
//    }
//    
//    @Test
////  @Ignore
//    public void insertStyles() throws FileNotFoundException, IOException {
//        if (!enabled()) return;
//        deleteAll();
//        
//        File sldDir = new ClassPathResource("testdata").getFile();
//        for(File sldFile : sldDir.listFiles((FilenameFilter)new SuffixFileFilter(".sld"))) {
//            String basename = FilenameUtils.getBaseName(sldFile.toString());
//            LOGGER.info("Publishing style " + sldFile + " as " + basename);
//            assertTrue("Cound not publish " + sldFile, publisher.publishStyle(sldFile, basename));
//        }
//    }
//
//    @Test
////    @Ignore
//    public void insertExternalGeotiff() throws FileNotFoundException, IOException {
//        if (!enabled()) return;
//        deleteAll();
//        
//        String storeName = "testRESTStoreGeotiff";
//
//        File geotiff = new ClassPathResource("testdata/resttestdem.tif").getFile();
//        RESTCoverageStore pc = publisher.publishExternalGeoTIFF(DEFAULT_WS, storeName, geotiff, null, null);
//        
//        assertNotNull(pc);
//    }
//
//    @Test
////  @Ignore
//    public void insertShape() {
//        if (!enabled()) return;
//        deleteAll();
//        
//        File zipFile;
//		try {
//			zipFile = new ClassPathResource("testdata/resttestshp.zip").getFile();
//			assertTrue("publish() failed", publisher.publishShp(DEFAULT_WS, "anyname", "anyname", zipFile, "EPSG:4326", "default_point"));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			fail();
//		}
//
//        //test delete
////        boolean ok = publisher.unpublishFeatureType(DEFAULT_WS, "anyname", "cities");
////        assertTrue("Unpublish() failed", ok);
//        
//        // remove also datastore
////        boolean dsRemoved = publisher.removeDatastore(DEFAULT_WS, "anyname");
////        assertTrue("removeDatastore() failed", dsRemoved);
//    }
//
//}