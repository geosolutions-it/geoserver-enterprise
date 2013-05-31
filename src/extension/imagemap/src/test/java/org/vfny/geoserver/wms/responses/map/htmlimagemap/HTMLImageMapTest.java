package org.vfny.geoserver.wms.responses.map.htmlimagemap;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.test.TestData;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.vfny.geoserver.global.GeoserverDataDirectory;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Test suite for HTMLImageMapMapProducer GetMapOutputFormat
 * 
 * @author Mauro Bartolomeoli
 */
public class HTMLImageMapTest extends TestCase {

    private static final StyleFactory sFac = CommonFactoryFinder.getStyleFactory(null);

    private static final Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(HTMLImageMapTest.class.getPackage().getName());

    private HTMLImageMapMapProducer mapProducer;

    private HTMLImageMapResponse response;

    private CoordinateReferenceSystem WGS84;

    private DataStore testDS = null;

    private int mapWidth = 600;

    private int mapHeight = 600;

    public void setUp() throws Exception {
        // initializes GeoServer Resource Loading (is needed by some tests to not produce
        // exceptions)

        System.setProperty("org.geotools.referencing.forceXY", "true");
        File testdata = TestData.file(this, ".");
        System.setProperty("GEOSERVER_DATA_DIR", testdata.getAbsolutePath());
        GeoServerResourceLoader loader = new GeoServerResourceLoader(testdata);
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.getBeanFactory().registerSingleton("resourceLoader", loader);
        GeoserverDataDirectory.init(context);

        // initialized WGS84 CRS (used by many tests)
        WGS84 = CRS.decode("EPSG:4326");

        testDS = getTestDataStore();

        // initializes GetMapOutputFormat factory and actual producer
        // this.mapFactory = getProducerFactory();
        this.mapProducer = new HTMLImageMapMapProducer();
        this.response = new HTMLImageMapResponse(); 
    }

    public void tearDown() throws Exception {
        this.mapProducer = null;
        this.response = null;
    }

    public DataStore getTestDataStore() throws IOException {
        File testdata = TestData.file(this, "featureTypes");

        return new MyPropertyDataStore(testdata);

    }

    public DataStore getProjectedTestDataStore() throws IOException {
        File testdata = TestData.file(this, "featureTypes");

        try {
            return new MyPropertyDataStore(testdata, CRS.decode("EPSG:3004"));
        } catch (NoSuchAuthorityCodeException e) {
            e.printStackTrace();
            return null;
        } catch (FactoryException e) {
            //
            e.printStackTrace();
            return null;
        }

    }

    protected Style getTestStyle(String styleName) throws Exception {
        SLDParser parser = new SLDParser(sFac);
        File styleRes = TestData.file(this, "styles/" + styleName);

        parser.setInput(styleRes);

        Style s = parser.readXML()[0];

        return s;
    }

    protected void assertTestResult(String testName, EncodeHTMLImageMap imageMap) throws Exception{

        ByteArrayOutputStream out = null;
        StringBuffer testText = new StringBuffer();
        try {

            out = new ByteArrayOutputStream();
            this.response.write(imageMap, out, null);
            out.flush();
            out.close();
            File testFile = TestData.file(this, "results/" + testName + ".txt");
            BufferedReader reader = new BufferedReader(new FileReader(testFile));

            String s = null;
            while ((s = reader.readLine()) != null)
                testText.append(s + "\n");

            reader.close();

        } finally {
            imageMap.dispose();
        }
        assertNotNull(out);
        assertTrue(out.size() > 0);
        String s = new String(out.toByteArray());

        assertEquals(testText.toString(), s);
    }

    public void testStates() throws Exception {
        File shapeFile = TestData.file(this, "featureTypes/states.shp");
        ShapefileDataStore ds = new ShapefileDataStore(shapeFile.toURL());

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = ds.getFeatureSource("states");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        map.setTransparent(false);

        Style basicStyle = getTestStyle("Population.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap imageMap = this.mapProducer.produceMap(map);

        assertTestResult("States", imageMap);
    }

    public void testMapProduceBasicPolygons() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = testDS
                .getFeatureSource("BasicPolygons");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        map.setTransparent(false);

        Style basicStyle = getTestStyle("default.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("BasicPolygons", result);

    }

    public void testMapProducePolygonsWithHoles() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = testDS
                .getFeatureSource("PolygonWithHoles");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        map.setTransparent(false);

        Style basicStyle = getTestStyle("default.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("PolygonWithHoles", result);
    }

    public void testMapProducePolygonsWithSkippedHoles() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = testDS
                .getFeatureSource("PolygonWithSkippedHoles");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        map.setTransparent(false);

        Style basicStyle = getTestStyle("default.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("PolygonWithSkippedHoles", result);
    }

    public void testMapProduceReproject() throws Exception {
        final DataStore ds = getProjectedTestDataStore();
        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = ds
                .getFeatureSource("ProjectedPolygon");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(),
                CRS.decode("EPSG:3004"));

        LOGGER.info("about to create map ctx for ProjectedPolygon with bounds " + env);

        final WMSMapContent map = new WMSMapContent();

        CoordinateReferenceSystem sourceCrs = CRS.decode("EPSG:3004");
        CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:3003");

        MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs, true);
        Envelope projEnv = JTS.transform(env, transform);
        ReferencedEnvelope refEnv = new ReferencedEnvelope(projEnv, targetCrs);

        map.getViewport().setBounds(refEnv);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        map.setBgColor(Color.red);
        map.setTransparent(false);

        map.getViewport().setCoordinateReferenceSystem(targetCrs);
        Style basicStyle = getTestStyle("BasicPolygons.sld");

        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("ProjectedPolygon", result);
    }

    public void testMapProduceLines() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = testDS
                .getFeatureSource("RoadSegments");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for RoadSegments with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("RoadSegments.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("RoadSegments", result);

    }

    public void testMapRuleWithFilters() throws Exception {
        /*
         * Filter
         * f=filterFactory.equals(filterFactory.property("NAME"),filterFactory.literal("Route 5"));
         * Query q=new Query("RoadSegments",f);
         */
        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = testDS
                .getFeatureSource("RoadSegments");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for RoadSegments with filter on name and bounds "
                + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("RoadSegmentsFiltered.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("RoadSegmentsFiltered", result);

    }

    public void testMapProducePoints() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = testDS
                .getFeatureSource("BuildingCenters");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BuildingCenters with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("BuildingCenters.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("BuildingCenters", result);

    }
	
	public void testMapProducePointsWithSize() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = testDS
                .getFeatureSource("BuildingCenters");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BuildingCenters with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("BuildingCenters2.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("BuildingCenters2", result);

    }
	public void testMapProducePointsWithDifferenSizeInScale1() throws Exception {
		
        final FeatureSource<SimpleFeatureType,SimpleFeature> fs = testDS.getFeatureSource("BuildingCenters");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(),WGS84);

        LOGGER.info("about to create map ctx for BuildingCenters with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        
        map.setTransparent(false);
                
        Style basicStyle = getTestStyle("BuildingCenters3.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));
        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        
        assertTestResult("BuildingCenters3", result);

	}
	public void testMapProducePointsWithDifferenSizeInScale2() throws Exception {
		
        final FeatureSource<SimpleFeatureType,SimpleFeature> fs = testDS.getFeatureSource("BuildingCenters");
        ReferencedEnvelope tmp=fs.getBounds();
        tmp.expandBy(5, 5);
        final ReferencedEnvelope env = new ReferencedEnvelope(tmp,WGS84);
        
        LOGGER.info("about to create map ctx for BuildingCenters with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        
        map.setTransparent(false);
                
        Style basicStyle = getTestStyle("BuildingCenters3.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));
        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        
        assertTestResult("BuildingCenters4", result);

	}

    public void testMapProduceMultiPoints() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = testDS
                .getFeatureSource("BuildingCentersMultiPoint");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BuildingCentersMultiPoint with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("BuildingCenters.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("BuildingCentersMultiPoint", result);

    }

    public void testMapProduceCollection() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = testDS
                .getFeatureSource("CollectionSample");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for RoadSegments with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("CollectionSample.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("CollectionSample", result);

    }

    public void testMapProduceNoCoords() throws Exception {
        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = testDS
                .getFeatureSource("NoCoords");
        final ReferencedEnvelope env = new ReferencedEnvelope(2.0, 6.0, 2.0, 6.0, WGS84);

        LOGGER.info("about to create map ctx for NamedPlaces with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("NamedPlaces.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("NoCoords", result);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(HTMLImageMapTest.class);
    }

    static class MyPropertyDataStore extends PropertyDataStore {

        CoordinateReferenceSystem myCRS = DefaultGeographicCRS.WGS84;

        /**
         * Creates a new MyPropertyDataStore object.
         */
        public MyPropertyDataStore(File dir) {
            super(dir);
        }

        /**
         * Creates a new MyPropertyDataStore object.
         * 
         * @param dir
         *            DOCUMENT ME!
         */
        public MyPropertyDataStore(File dir, CoordinateReferenceSystem coordinateSystem) {
            super(dir);
            this.myCRS = coordinateSystem;
        }

        public SimpleFeatureType getSchema(String typeName) throws IOException {
            SimpleFeatureType schema = super.getSchema(typeName);

            try {
                return DataUtilities.createSubType(schema, null, myCRS);
            } catch (SchemaException e) {
                throw new DataSourceException(e.getMessage(), e);
            }
        }

    }

}
