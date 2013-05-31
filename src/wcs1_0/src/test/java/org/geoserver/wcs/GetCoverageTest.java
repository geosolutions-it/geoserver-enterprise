package org.geoserver.wcs;

import static org.geoserver.data.test.MockData.TASMANIA_BM;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.servlet.ServletResponse;
import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.textui.TestRunner;
import net.opengis.wcs10.GetCoverageType;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.wcs.kvp.Wcs10GetCoverageRequestReader;
import org.geoserver.wcs.test.WCSTestSupport;
import org.geoserver.wcs.xml.v1_0_0.WcsXmlReader;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.metadata.iso.spatial.PixelTranslation;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.wcs.WCSConfiguration;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Tests for GetCoverage operation on WCS.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class GetCoverageTest extends WCSTestSupport {

    private Wcs10GetCoverageRequestReader kvpreader;
    private WebCoverageService100 service;

    private WCSConfiguration configuration;

    private WcsXmlReader xmlReader;

    private Catalog catalog;
    
    private static final QName MOSAIC = new QName(MockData.SF_URI, "rasterFilter", MockData.SF_PREFIX);

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetCoverageTest());
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        kvpreader = (Wcs10GetCoverageRequestReader) applicationContext.getBean("wcs100GetCoverageRequestReader");
        service = (WebCoverageService100) applicationContext.getBean("wcs100ServiceTarget");
        configuration = new WCSConfiguration();
        catalog=(Catalog)applicationContext.getBean("catalog");
        xmlReader = new WcsXmlReader("GetCoverage", "1.0.0", configuration);
        
        // enable dimensions on the water temperature layer
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        
        // this also adds the raster style
        dataDirectory.addCoverage(MOSAIC, 
                MockData.class.getResource("raster-filter-test.zip"), null, "raster");
    }



    @Override
    protected String getLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
    }
    
    private Map<String, Object> baseMap() {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "WCS");
        raw.put("version", "1.0.0");
        raw.put("request", "GetCoverage");
        return raw;
    }
    
    public void testDomainSubsetRxRy() throws Exception {
    	// get base  coverage
        final GridCoverage baseCoverage = catalog.getCoverageByName(TASMANIA_BM.getLocalPart()).getGridCoverage(null, null);
        final AffineTransform2D expectedTx = (AffineTransform2D) baseCoverage.getGridGeometry().getGridToCRS();        
        final GeneralEnvelope originalEnvelope = (GeneralEnvelope) baseCoverage.getEnvelope();
        final GeneralEnvelope newEnvelope=new GeneralEnvelope(originalEnvelope);
        newEnvelope.setEnvelope(
        		originalEnvelope.getMinimum(0),
        		originalEnvelope.getMaximum(1)-originalEnvelope.getSpan(1)/2,
        		originalEnvelope.getMinimum(0)+originalEnvelope.getSpan(0)/2,
        		originalEnvelope.getMaximum(1)
        		);
        
        final MathTransform cornerWorldToGrid = PixelTranslation.translate(expectedTx,PixelInCell.CELL_CENTER,PixelInCell.CELL_CORNER);
        final GeneralGridEnvelope expectedGridEnvelope = new GeneralGridEnvelope(CRS.transform(cornerWorldToGrid.inverse(), newEnvelope),PixelInCell.CELL_CORNER,false);
        final StringBuilder envelopeBuilder= new StringBuilder();
        envelopeBuilder.append(newEnvelope.getMinimum(0)).append(",");
        envelopeBuilder.append(newEnvelope.getMinimum(1)).append(",");
        envelopeBuilder.append(newEnvelope.getMaximum(0)).append(",");
        envelopeBuilder.append(newEnvelope.getMaximum(1));
        
        Map<String, Object> raw = baseMap();
        final String layerID = getLayerId(TASMANIA_BM);
        raw.put("sourcecoverage", layerID);
        raw.put("version", "1.0.0");
        raw.put("format", "image/geotiff"); 
        raw.put("BBox", envelopeBuilder.toString());
        raw.put("crs", "EPSG:4326");
        raw.put("resx", Double.toString(expectedTx.getScaleX()));
        raw.put("resy", Double.toString(Math.abs(expectedTx.getScaleY())));

        final GridCoverage[] coverages = executeGetCoverageKvp(raw);
        final GridCoverage2D result=(GridCoverage2D) coverages[0];
        assertTrue(coverages.length==1);
        final AffineTransform2D tx = (AffineTransform2D) result.getGridGeometry().getGridToCRS();
        assertEquals("resx",expectedTx.getScaleX(),tx.getScaleX(),1E-6);
        assertEquals("resx",Math.abs(expectedTx.getScaleY()),Math.abs(tx.getScaleY()),1E-6);
        
        final GridEnvelope gridEnvelope = result.getGridGeometry().getGridRange();
        assertEquals("w",180,gridEnvelope.getSpan(0));
        assertEquals("h",180,gridEnvelope.getSpan(1));
        assertEquals("grid envelope",expectedGridEnvelope, gridEnvelope);
        
        // dispose
        ((GridCoverage2D)coverages[0]).dispose(true);
    }
    
    /**
	 * Compare two grid to world transformations
	 * @param expectedTx
	 * @param tx
	 */
	private static void compareGrid2World(AffineTransform2D expectedTx,
			AffineTransform2D tx) {
		assertEquals("scalex",tx.getScaleX(), expectedTx.getScaleX(), 1E-6);
        assertEquals("scaley",tx.getScaleY(), expectedTx.getScaleY(), 1E-6);
        assertEquals("shearx",tx.getShearX(), expectedTx.getShearX(), 1E-6);
        assertEquals("sheary",tx.getShearY(), expectedTx.getShearY(), 1E-6);
        assertEquals("translatex",tx.getTranslateX(), expectedTx.getTranslateX(), 1E-6);
        assertEquals("translatey",tx.getTranslateY(), expectedTx.getTranslateY(), 1E-6);
	}

    public void testWorkspaceQualified() throws Exception {
        String queryString ="&request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff&bbox=146,-45,147,-42"+
            "&crs=EPSG:4326&width=150&height=150";

        ServletResponse response = getAsServletResponse( 
            "wcs?sourcecoverage="+TASMANIA_BM.getLocalPart()+queryString);
        assertTrue(response.getContentType().startsWith("image/tiff"));
        
        Document dom = getAsDOM( 
            "cdf/wcs?sourcecoverage="+TASMANIA_BM.getLocalPart()+queryString);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
    }

    public void testLayerQualified() throws Exception {
        String queryString ="&request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff&bbox=146,-45,147,-42"+
            "&crs=EPSG:4326&width=150&height=150";

        ServletResponse response = getAsServletResponse( 
            "wcs/BlueMarble/wcs?sourcecoverage=BlueMarble"+queryString);
        assertTrue(response.getContentType().startsWith("image/tiff"));
        
        Document dom = getAsDOM( 
            "wcs/DEM/wcs?sourcecoverage=BlueMarble"+queryString);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    /**
     * Runs GetCoverage on the specified parameters and returns an array of coverages
     */
    GridCoverage[] executeGetCoverageKvp(Map<String, Object> raw) throws Exception {
        final GetCoverageType getCoverage = (GetCoverageType) kvpreader.read(kvpreader.createRequest(),parseKvp(raw), raw);
        return service.getCoverage(getCoverage);
    }

    /**
     * Runs GetCoverage on the specified parameters and returns an array of coverages
     */
    GridCoverage[] executeGetCoverageXml(String request) throws Exception {
        GetCoverageType getCoverage = (GetCoverageType) xmlReader.read(null, new StringReader(
                request), null);
        return service.getCoverage(getCoverage);
    }
    
    public void testInputLimits() throws Exception {
        try {
            // ridicolous limit, just one byte
            setInputLimit(1);
            String queryString = "&request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff&bbox=146,-45,147,-42"
                    + "&crs=EPSG:4326&width=150&height=150";
            Document dom = getAsDOM("wcs/BlueMarble/wcs?sourcecoverage=" + getLayerId(TASMANIA_BM)
                    + queryString);
            // print(dom);
            // check it's an error, check we're getting it because of the input limits
            assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
            String error = xpath.evaluate(
                    "/ServiceExceptionReport/ServiceException/text()", dom).trim();
            assertTrue(error.matches(".*read too much data.*"));
        } finally {
            setInputLimit(0);
        }
    }

    public void testOutputLimits() throws Exception {
        try {
            // ridicolous limit, just one byte
            setOutputLimit(1);
            String queryString = "&request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff&bbox=146,-45,147,-42"
                    + "&crs=EPSG:4326&width=150&height=150";
            Document dom = getAsDOM("wcs/BlueMarble/wcs?sourcecoverage=" + getLayerId(TASMANIA_BM)
                    + queryString);
            // print(dom);
            // check it's an error, check we're getting it because of the output limits
            assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
            String error = xpath.evaluate(
                    "/ServiceExceptionReport/ServiceException/text()", dom).trim();
            assertTrue(error.matches(".*generate too much data.*"));
        } finally {
            setOutputLimit(0);
        }
    }
    
    public void testReproject() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        		"<GetCoverage version=\"1.0.0\" service=\"WCS\" " +
        		"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
        		"xmlns=\"http://www.opengis.net/wcs\" " +
        		"xmlns:ows=\"http://www.opengis.net/ows/1.1\" " +
        		"xmlns:gml=\"http://www.opengis.net/gml\" " +
        		"xmlns:ogc=\"http://www.opengis.net/ogc\" " +
        		"xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/getCoverage.xsd\">\n" + 
        		"  <sourceCoverage>" +  getLayerId(TASMANIA_BM) + "</sourceCoverage>\n" + 
        		"  <domainSubset>\n" + 
        		"    <spatialSubset>\n" + 
        		"      <gml:Envelope srsName=\"EPSG:4326\">\n" + 
        		"        <gml:pos>146 -45</gml:pos>\n" + 
        		"        <gml:pos>147 42</gml:pos>\n" + 
        		"      </gml:Envelope>\n" + 
        		"      <gml:Grid dimension=\"2\">\n" + 
        		"        <gml:limits>\n" + 
        		"          <gml:GridEnvelope>\n" + 
        		"            <gml:low>0 0</gml:low>\n" + 
        		"            <gml:high>150 150</gml:high>\n" + 
        		"          </gml:GridEnvelope>\n" + 
        		"        </gml:limits>\n" + 
        		"        <gml:axisName>x</gml:axisName>\n" + 
        		"        <gml:axisName>y</gml:axisName>\n" + 
        		"      </gml:Grid>\n" + 
        		"    </spatialSubset>\n" + 
        		"  </domainSubset>\n" + 
        		"  <output>\n" + 
        		"    <crs>EPSG:3857</crs>\n" + 
        		"    <format>image/geotiff</format>\n" + 
        		"  </output>\n" + 
        		"</GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", xml);
        assertEquals("image/tiff;subtype=\"geotiff\"", response.getContentType());
        
        GeoTiffFormat format = new GeoTiffFormat();
        AbstractGridCoverage2DReader reader = format.getReader(getBinaryInputStream(response));
        
        assertEquals(CRS.decode("EPSG:3857"), reader.getOriginalEnvelope().getCoordinateReferenceSystem());
    }
    
    public void testRasterFilterGreen() throws Exception {
        String queryString = "wcs?sourcecoverage=" + getLayerId(MOSAIC) + "&request=getcoverage" +
                "&service=wcs&version=1.0.0&&format=image/tiff&crs=EPSG:4326" + 
                "&bbox=0,0,1,1&CQL_FILTER=location like 'green%25'&width=150&height=150";
        
        MockHttpServletResponse response = getAsServletResponse(queryString);

        // make sure we can read the coverage back
        RenderedImage image = readTiff(response);
        
        // check the pixel
        int[] pixel = new int[3];
        image.getData().getPixel(0, 0, pixel);
        assertEquals(0, pixel[0]);
        assertEquals(255, pixel[1]);
        assertEquals(0, pixel[2]);
    }

    /**
     * Parses teh TIFF contained in the response as a {@link RenderedImage}
     * @param response
     * @return
     * @throws IOException
     */
    RenderedImage readTiff(MockHttpServletResponse response) throws IOException {
        ImageReader reader = ImageIO.getImageReadersByFormatName("tiff").next();
        reader.setInput(ImageIO.createImageInputStream(getBinaryInputStream(response)));
        return reader.read(0);
    }
    
    public void testTimeFirstPOST() throws Exception {
        String request = getWaterTempTimeRequest("2008-10-31T00:00:00.000Z");
     
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        checkTimeFirst(response);
    }
    
    public void testTimeFirstKVP() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
        
        String queryString ="request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff" +
        		"&bbox=0.237,40.562,14.593,44.558&crs=EPSG:4326&width=25&height=25&time=2008-10-31T00:00:00.000Z" +
        		"&coverage=" + getLayerId(WATTEMP);
        MockHttpServletResponse response = getAsServletResponse("wcs?" + queryString);
        
        checkTimeFirst(response);
    }

    private void checkTimeFirst(MockHttpServletResponse response) throws IOException,
            FileNotFoundException, DataSourceException {
        // save
        File tiffFile = File.createTempFile("wcs", "", new File("target"));
        IOUtils.copy(getBinaryInputStream(response), new FileOutputStream(tiffFile));

        // make sure we can read the coverage back
        GeoTiffReader reader = new GeoTiffReader(tiffFile);
        GridCoverage2D result = reader.read(null);

        /*
         gdallocationinfo NCOM_wattemp_000_20081031T0000000_12.tiff 10 10
        Report:
          Location: (10P,10L)
          Band 1:
            Value: 18.2659999176394
        */
        
        // check a pixel
        double[] pixel = new double[1];
        result.getRenderedImage().getData().getPixel(10, 10, pixel);
        assertEquals(18.2659999176394, pixel[0], 1e-6);
        
        tiffFile.delete();
    }
    
    private void checkTimeCurrent(MockHttpServletResponse response) throws IOException,
            FileNotFoundException, DataSourceException {
        // save
        File tiffFile = File.createTempFile("wcs", "", new File("target"));
        IOUtils.copy(getBinaryInputStream(response), new FileOutputStream(tiffFile));

        // make sure we can read the coverage back
        GeoTiffReader reader = new GeoTiffReader(tiffFile);
        GridCoverage2D result = reader.read(null);

        /*
         gdallocationinfo NCOM_wattemp_000_20081101T0000000_12.tiff 10 10
         Report:
             Location: (10P,10L)
             Band 1:
               Value: 18.2849999185419
         */

        // check a pixel
        double[] pixel = new double[1];
        result.getRenderedImage().getData().getPixel(10, 10, pixel);
        assertEquals(18.2849999185419, pixel[0], 1e-6);

        tiffFile.delete();
    }
    
    public void testTimeSecond() throws Exception {
        String request = getWaterTempTimeRequest("2008-11-01T00:00:00.000Z");
     
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        checkTimeCurrent(response);
    }
    
    public void testTimeKVPNow() throws Exception {
        String queryString ="request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff" +
                        "&bbox=0.237,40.562,14.593,44.558&crs=EPSG:4326&width=25&height=25&time=now" +
                        "&coverage=" + getLayerId(WATTEMP);
        MockHttpServletResponse response = getAsServletResponse("wcs?" + queryString);
        
        checkTimeCurrent(response);
    }
    
    public void testElevationFirst() throws Exception {
        String request = getWaterTempElevationRequest("0.0");
     
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        assertEquals("image/tiff;subtype=\"geotiff\"", response.getContentType());
        
        // same result as time first
        checkTimeCurrent(response);
    }
    
    public void testElevationSecond() throws Exception {
        String request = getWaterTempElevationRequest("100.0");
     
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        assertEquals("image/tiff;subtype=\"geotiff\"", response.getContentType());
        
        // save
        File tiffFile = File.createTempFile("wcs", "", new File("target"));
        IOUtils.copy(getBinaryInputStream(response), new FileOutputStream(tiffFile));

        // make sure we can read the coverage back
        GeoTiffReader reader = new GeoTiffReader(tiffFile);
        GridCoverage2D result = reader.read(null);

        /*
         gdallocationinfo NCOM_wattemp_100_20081101T0000000_12.tiff  10 10
         Report:
          Location: (10P,10L)
          Band 1:
          Value: 13.337999683572
        */
        
        // check a pixel
        double[] pixel = new double[1];
        result.getRenderedImage().getData().getPixel(10, 10, pixel);
        assertEquals(13.337999683572, pixel[0], 1e-6);
        
        tiffFile.delete();
    }

    private String getWaterTempElevationRequest(String elevation) {
        String request =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
    		"<GetCoverage version=\"1.0.0\" service=\"WCS\"\n" + 
    		"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wcs\"\n" + 
    		"  xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\"\n" + 
    		"  xmlns:ogc=\"http://www.opengis.net/ogc\"\n" + 
    		"  xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/getCoverage.xsd\">\n" + 
    		"  <sourceCoverage>" + getLayerId(WATTEMP) + "</sourceCoverage>\n" + 
    		"  <domainSubset>\n" + 
    		"    <spatialSubset>\n" + 
    		"      <gml:Envelope srsName=\"EPSG:4326\">\n" + 
    		"        <gml:pos>0.237 40.562</gml:pos>\n" + 
    		"        <gml:pos>14.593 44.558</gml:pos>\n" + 
    		"      </gml:Envelope>\n" + 
    		"      <gml:Grid dimension=\"2\">\n" + 
    		"        <gml:limits>\n" + 
    		"          <gml:GridEnvelope>\n" + 
    		"            <gml:low>0 0</gml:low>\n" + 
    		"            <gml:high>25 24</gml:high>\n" + 
    		"          </gml:GridEnvelope>\n" + 
    		"        </gml:limits>\n" + 
    		"        <gml:axisName>x</gml:axisName>\n" + 
    		"        <gml:axisName>y</gml:axisName>\n" + 
    		"      </gml:Grid>\n" + 
    		"    </spatialSubset>\n" + 
    		"  </domainSubset>\n" + 
    		"  <rangeSubset>\n" + 
    		"    <axisSubset name=\"ELEVATION\">\n" + 
    		"      <singleValue>" + elevation + "</singleValue>\n" + 
    		"    </axisSubset>\n" + 
    		"  </rangeSubset>\n" + 
    		"  <output>\n" + 
    		"    <crs>EPSG:4326</crs>\n" + 
    		"    <format>GeoTIFF</format>\n" + 
    		"  </output>\n" + 
    		"</GetCoverage>";
        return request;
    }

    private String getWaterTempTimeRequest(String date) {
        String request =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
		"<GetCoverage version=\"1.0.0\" service=\"WCS\"\n" + 
		"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wcs\"\n" + 
		"  xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\"\n" + 
		"  xmlns:ogc=\"http://www.opengis.net/ogc\"\n" + 
		"  xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/getCoverage.xsd\">\n" + 
		"  <sourceCoverage>" + getLayerId(WATTEMP) + "</sourceCoverage>\n" + 
		"  <domainSubset>\n" + 
		"    <spatialSubset>\n" + 
		"      <gml:Envelope srsName=\"EPSG:4326\">\n" + 
		"        <gml:pos>0.237 40.562</gml:pos>\n" + 
		"        <gml:pos>14.593 44.558</gml:pos>\n" + 
		"      </gml:Envelope>\n" + 
		"      <gml:Grid dimension=\"2\">\n" + 
		"        <gml:limits>\n" + 
		"          <gml:GridEnvelope>\n" + 
		"            <gml:low>0 0</gml:low>\n" + 
		"            <gml:high>25 25</gml:high>\n" + 
		"          </gml:GridEnvelope>\n" + 
		"        </gml:limits>\n" + 
		"        <gml:axisName>x</gml:axisName>\n" + 
		"        <gml:axisName>y</gml:axisName>\n" + 
		"      </gml:Grid>\n" + 
		"    </spatialSubset>\n" + 
		"    <temporalSubset>\n" + 
		"      <gml:timePosition>" + date + "</gml:timePosition>\n" + 
		"    </temporalSubset>\n" + 
		"  </domainSubset>\n" + 
		"  <output>\n" + 
		"    <crs>EPSG:4326</crs>\n" + 
		"    <format>geotiff</format>\n" + 
		"  </output>\n" + 
		"</GetCoverage>";
        return request;
    }
    
    public void testRasterFilterRed() throws Exception {
        String queryString = "wcs?sourcecoverage=" + getLayerId(MOSAIC) + "&request=getcoverage" +
                "&service=wcs&version=1.0.0&format=image/tiff&crs=EPSG:4326" + 
                "&bbox=0,0,1,1&CQL_FILTER=location like 'red%25'&width=150&height=150";
        
        MockHttpServletResponse response = getAsServletResponse(queryString);

        RenderedImage image = readTiff(response);
        
        // check the pixel
        int[] pixel = new int[3];
        image.getData().getPixel(0, 0, pixel);
        assertEquals(255, pixel[0]);
        assertEquals(0, pixel[1]);
        assertEquals(0, pixel[2]);
    }

    private void setInputLimit(int kbytes) {
        GeoServer gs = getGeoServer();
        WCSInfo info = gs.getService(WCSInfo.class);
        info.setMaxInputMemory(kbytes);
        gs.save(info);
    } 
    

    private void setOutputLimit(int kbytes) {
        GeoServer gs = getGeoServer();
        WCSInfo info = gs.getService(WCSInfo.class);
        info.setMaxOutputMemory(kbytes);
        gs.save(info);
    } 

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

}
