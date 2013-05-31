/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSMockData.DummyRasterMapProducer;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.EnvFunction;
import org.opengis.filter.FilterFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

/**
 * Unit test for {@link GetMap}
 * 
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 * @since 2.5.x
 * @source $URL:
 *         https://svn.codehaus.org/geoserver/branches/1.7.x/geoserver/wms/src/test/java/org/vfny
 *         /geoserver/wms/responses/GetMapResponseTest.java $
 */
public class GetMapTest extends TestCase {

    private WMSMockData mockData;

    private GetMapRequest request;

    private GetMap getMapOp;

    @Override
    protected void setUp() throws Exception {
        mockData = new WMSMockData();
        mockData.setUp();

        request = mockData.createRequest();
        // add a layer so its a valid request
        MapLayerInfo layer = mockData.addFeatureTypeLayer("testType", Point.class);
        request.setLayers(Arrays.asList(layer));

        getMapOp = new GetMap(mockData.getWMS());
    }

    @Override
    protected void tearDown() throws Exception {

    }

    /**
     * Test method for {@link GetMapResponse#execute(org.vfny.geoserver.Request)}.
     */
    public void testExecuteNoExtent() {
        request.setBbox(null);
        assertInvalidMandatoryParam("MissingBBox");
    }

    public void testExecuteEmptyExtent() {
        request.setBbox(new Envelope());
        assertInvalidMandatoryParam("InvalidBBox");
    }

    public void testSingleVectorLayer() throws IOException {
        request.setFormat(DummyRasterMapProducer.MIME_TYPE);

        MapLayerInfo layer = mockData.addFeatureTypeLayer("testSingleVectorLayer", Point.class);
        request.setLayers(Arrays.asList(layer));

        final DummyRasterMapProducer producer = new DummyRasterMapProducer();
        final WMS wms = new WMS(mockData.getGeoServer()) {
            @Override
            public GetMapOutputFormat getMapOutputFormat(final String mimeType) {
                if (DummyRasterMapProducer.MIME_TYPE.equals(mimeType)) {
                    return producer;
                }
                return null;
            }
        };
        getMapOp = new GetMap(wms);
        getMapOp.run(request);
        assertTrue(producer.produceMapCalled);
    }

    public void testExecuteNoLayers() throws Exception {
        request.setLayers(null);
        assertInvalidMandatoryParam("LayerNotDefined");
    }

    public void testExecuteNoWidth() {
        request.setWidth(0);
        assertInvalidMandatoryParam("MissingOrInvalidParameter");

        request.setWidth(-1);
        assertInvalidMandatoryParam("MissingOrInvalidParameter");
    }

    public void testExecuteNoHeight() {
        request.setHeight(0);
        assertInvalidMandatoryParam("MissingOrInvalidParameter");

        request.setHeight(-1);
        assertInvalidMandatoryParam("MissingOrInvalidParameter");
    }

    public void testExecuteInvalidFormat() {
        request.setFormat("non-existent-output-format");
        assertInvalidMandatoryParam("InvalidFormat");
    }

    public void testExecuteNoFormat() {
        request.setFormat(null);
        assertInvalidMandatoryParam("InvalidFormat");
    }

    public void testExecuteNoStyles() {
        request.setStyles(null);
        assertInvalidMandatoryParam("StyleNotDefined");
    }

    public void testEnviroment() {
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        EnvFunction.setLocalValues(Collections.singletonMap("myParam", (Object) 23));

        final DummyRasterMapProducer producer = new DummyRasterMapProducer() {
            @Override
            public WebMap produceMap(WMSMapContent ctx) throws ServiceException, IOException {
                assertEquals(23, ff.function("env", ff.literal("myParam")).evaluate(null));
                assertEquals(10, ff.function("env", ff.literal("otherParam"), ff.literal(10))
                        .evaluate(null));
                super.produceMapCalled = true;
                return null;
            }
        };
        final WMS wms = new WMS(mockData.getGeoServer()) {
            @Override
            public GetMapOutputFormat getMapOutputFormat(final String mimeType) {
                if (DummyRasterMapProducer.MIME_TYPE.equals(mimeType)) {
                    return producer;
                }
                return null;
            }
        };
        
        getMapOp = new GetMap(wms);
        WebMap map = getMapOp.run(request);
        assertTrue(producer.produceMapCalled);
        // there used to be a test that the values are reset right after 
        // GetMap, but this is wrong, the producer can be streaming and thus
        // the env variable must stay until the full request lifecycle is done,
        // we now use a DispatcherCallback to clean up the env variables:
        // EnvVariableCleaner
    }

    private void assertInvalidMandatoryParam(String expectedExceptionCode) {
        try {
            getMapOp.run(request);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals(expectedExceptionCode, e.getCode());
        }
    }

}
