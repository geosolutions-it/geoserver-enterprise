/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.feature.NameImpl;
import org.geotools.styling.Style;

import com.mockrunner.mock.web.MockHttpServletRequest;

public class GetLegendGraphicKvpReaderTest extends WMSTestSupport {
    /**
     * request reader to test against, initialized by default with all parameters from
     * <code>requiredParameters</code> and <code>optionalParameters</code>
     */
    GetLegendGraphicKvpReader requestReader;

    /** test values for required parameters */
    Map<String, String> requiredParameters;

    /** test values for optional parameters */
    Map<String, String> optionalParameters;

    /** both required and optional parameters joint up */
    Map<String, String> allParameters;

    /** mock request */
    MockHttpServletRequest httpRequest;

    /** mock config object */
    WMS wms;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetLegendGraphicKvpReaderTest());
    }

    /**
     * Remainder:
     * <ul>
     * <li>VERSION/Required
     * <li>REQUEST/Required
     * <li>LAYER/Required
     * <li>FORMAT/Required
     * <li>STYLE/Optional
     * <li>FEATURETYPE/Optional
     * <li>RULE/Optional
     * <li>SCALE/Optional
     * <li>SLD/Optional
     * <li>SLD_BODY/Optional
     * <li>WIDTH/Optional
     * <li>HEIGHT/Optional
     * <li>EXCEPTIONS/Optional
     * </ul>
     */
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        requiredParameters = new HashMap<String, String>();
        requiredParameters.put("VERSION", "1.0.0");
        requiredParameters.put("REQUEST", "GetLegendGraphic");
        requiredParameters.put("LAYER", "cite:Ponds");
        requiredParameters.put("FORMAT", "image/png");

        optionalParameters = new HashMap<String, String>();
        optionalParameters.put("STYLE", "Ponds");
        optionalParameters.put("FEATURETYPE", "fake_not_used");
        // optionalParameters.put("RULE", "testRule");
        optionalParameters.put("SCALE", "1000");
        optionalParameters.put("WIDTH", "120");
        optionalParameters.put("HEIGHT", "90");
        // ??optionalParameters.put("EXCEPTIONS", "");
        allParameters = new HashMap<String, String>(requiredParameters);
        allParameters.putAll(optionalParameters);

        wms = getWMS();

        this.requestReader = new GetLegendGraphicKvpReader(wms);
        this.httpRequest = createRequest("wms", allParameters);
    }

    /**
     * This test ensures that when a SLD parameter has been passed that refers to a SLD document
     * with multiple styles, the required one is choosed based on the LAYER parameter.
     * <p>
     * This is the case where a remote SLD document is used in "library" mode.
     * </p>
     * 
     * @throws Exception
     */
    public void testRemoteSLDMultipleStyles() throws Exception {
        final URL remoteSldUrl = getClass().getResource("MultipleStyles.sld");
        this.allParameters.put("SLD", remoteSldUrl.toExternalForm());

        this.allParameters.put("LAYER", "cite:Ponds");
        this.allParameters.put("STYLE", "Ponds");

        GetLegendGraphicRequest request = requestReader.read(new GetLegendGraphicRequest(),
                allParameters, allParameters);

        // the style names Ponds is declared in third position on the sld doc
        Style selectedStyle = request.getStyles().get(0);
        assertNotNull(selectedStyle);
        assertEquals("Ponds", selectedStyle.getName());

        this.allParameters.put("LAYER", "cite:Lakes");
        this.allParameters.put("STYLE", "Lakes");

        request = requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);

        // the style names Ponds is declared in third position on the sld doc
        selectedStyle = request.getStyles().get(0);
        assertNotNull(selectedStyle);
        assertEquals("Lakes", selectedStyle.getName());
    }

    public void testMissingLayerParameter() throws Exception {
        requiredParameters.remove("LAYER");
        try {
            requestReader.read(new GetLegendGraphicRequest(), requiredParameters,
                    requiredParameters);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("LayerNotDefined", e.getCode());
        }
    }

    public void testMissingFormatParameter() throws Exception {
        requiredParameters.remove("FORMAT");
        try {
            requestReader.read(new GetLegendGraphicRequest(), requiredParameters,
                    requiredParameters);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("MissingFormat", e.getCode());
        }
    }

    public void testStrictParameter() throws Exception {
        GetLegendGraphicRequest request;

        // default value
        request = requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);
        assertTrue(request.isStrict());

        allParameters.put("STRICT", "false");
        allParameters.remove("LAYER");
        request = requestReader.read(new GetLegendGraphicRequest(), allParameters, allParameters);
        assertFalse(request.isStrict());
    }
    
    public void testLayerGroup() throws Exception {
        GetLegendGraphicRequest request;
        
        request = requestReader.read(new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertTrue(request.getLayers().size() == 1);
        
        requiredParameters.put("LAYER", "nature");
        request = requestReader.read(new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertTrue(request.getLayers().size() > 1);
    }
    
    public void testStylesForLayerGroup() throws Exception {
        GetLegendGraphicRequest request;
               
        requiredParameters.put("LAYER", "nature");
        requiredParameters.put("STYLE", "style1,style2");
        request = requestReader.read(new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertTrue(request.getStyles().size() == 2);
    }
    
    public void testRulesForLayerGroup() throws Exception {
        GetLegendGraphicRequest request;
               
        requiredParameters.put("LAYER", "nature");
        requiredParameters.put("RULE", "rule1,rule2");
        request = requestReader.read(new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertTrue(request.getRules().size() == 2);
    }
    
    public void testLabelsForLayerGroup() throws Exception {
        GetLegendGraphicRequest request;
               
        requiredParameters.put("LAYER", "nature");
        request = requestReader.read(new GetLegendGraphicRequest(), requiredParameters, requiredParameters);
        assertNotNull(request.getTitle(new NameImpl("http://www.opengis.net/cite","Lakes")));
    }
}
