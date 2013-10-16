package org.geoserver.wps;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wps.xml.WPSConfiguration;
import org.geotools.process.Processors;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.opengis.coverage.grid.GridCoverage;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockServletOutputStream;

public abstract class WPSTestSupport extends GeoServerTestSupport {

    protected static Catalog catalog;
    protected static XpathEngine xp;

    // WCS 1.1  
    public static String WCS_PREFIX = "wcs";
    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName TASMANIA_DEM = new QName(WCS_URI, "DEM", WCS_PREFIX);
    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);
    public static QName ROTATED_CAD = new QName(WCS_URI, "RotatedCad", WCS_PREFIX);
    public static QName WORLD = new QName(WCS_URI, "World", WCS_PREFIX);
    public static String TIFF = "tiff";
    
    List<GridCoverage> coverages = new ArrayList<GridCoverage>();

    static {
        Processors.addProcessFactory(MonkeyProcess.getFactory());
    }
    
    protected void scheduleForDisposal(GridCoverage coverage) {
        this.coverages.add(coverage);
    }
    
    /**
     * Extracts the true binary stream out of the response. The usual way (going
     * thru {@link MockHttpServletResponse#getOutputStreamContent()}) mangles
     * bytes if the content is not made of chars.
     * 
     * @param response
     * @return
     */
    protected byte[] getBinary(MockHttpServletResponse response) {
        try {
            MockServletOutputStream os = (MockServletOutputStream) response.getOutputStream();
            final Field field = os.getClass().getDeclaredField("buffer");
            field.setAccessible(true);
            ByteArrayOutputStream bos = (ByteArrayOutputStream) field.get(os);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Whoops, did you change the MockRunner version? "
                    + "If so, you might want to change this method too");
        }
    }
    private void disposeCoverages() {
        for (GridCoverage coverage : coverages) {
            CoverageCleanerCallback.disposeCoverage(coverage);
        }
    }
    protected void setUpInternal() throws Exception {
        super.setUpInternal();


        catalog = getCatalog();
        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wps", "http://www.opengis.net/wps/1.0.0");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("feature", "http://geoserver.sf.net"); 
        

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();
    }


    
    /**
     * Validates a document based on the WPS schema 
     * @throws TransformerException 
     * @throws ParserConfigurationException 
     */
    protected void checkValidationErrors(Document dom) throws Exception {
        checkValidationErrors(dom, new WPSConfiguration());
    }

    /**
     * Validates a document against the 
     * @param dom
     * @param configuration
     */
    protected void checkValidationErrors(Document dom, Configuration configuration) throws Exception {
        Parser p = new Parser(configuration);
        p.setValidating( true );
        p.parse( new DOMSource( dom ) );
    
        if ( !p.getValidationErrors().isEmpty() ) {
            for ( Iterator e = p.getValidationErrors().iterator(); e.hasNext(); ) {
                SAXParseException ex = (SAXParseException) e.next();
                System.out.println( ex.getLineNumber() + "," + ex.getColumnNumber() + " -" + ex.toString()  );
            }
            fail( "Document did not validate.");
        }
    }

    protected String readFileIntoString(String filename) throws IOException {
        BufferedReader in = 
            new BufferedReader( new InputStreamReader(getClass().getResourceAsStream( filename ) ) );
        StringBuffer sb = new StringBuffer();
        String line = null;
        while( (line = in.readLine() ) != null ) {
            sb.append( line );
        }
        in.close();
        return sb.toString();
    }


    protected String root() {
        return "wps?";
    }


    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();
        disposeCoverages();
    }

}
