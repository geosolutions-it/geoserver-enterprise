/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayInputStream;

import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.wps.WPSTestSupport;
import org.geotools.feature.FeatureCollection;
import org.geotools.wfs.v1_0.WFSConfiguration;
import org.geotools.xml.Parser;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class ClipProcessTest extends WPSTestSupport {

    @Test
    public void testClipRectangle() throws Exception {
        String xml = "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                + "<ows:Identifier>gs:RectangularClip</ows:Identifier>"
                + "<wps:DataInputs>"
                + "<wps:Input>"
                + "<ows:Identifier>features</ows:Identifier>"
                + "<wps:Data>"
                + "<wps:ComplexData>"
                + readFileIntoString("illinois.xml")
                + "</wps:ComplexData>"
                + "</wps:Data>"
                + "</wps:Input>"
                + "<wps:Input>"
                + "<ows:Identifier>clip</ows:Identifier>"
                + "<wps:Data>"
                + "<ows:BoundingBox>"
                + "<ows:LowerCorner>-100 40</ows:LowerCorner>"
                + "<ows:UpperCorner>-90 45</ows:UpperCorner>"
                + "</ows:BoundingBox>"
                + "</wps:Data>"
                + "</wps:Input>"
                + "</wps:DataInputs>"
                + "<wps:ResponseForm>"
                + "<wps:RawDataOutput>"
                + "<ows:Identifier>result</ows:Identifier>"
                + "</wps:RawDataOutput>" + "</wps:ResponseForm>" + "</wps:Execute>";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        // System.out.println(response.getOutputStreamContent());
        
        Parser p = new Parser(new WFSConfiguration());
        FeatureCollectionType fct = (FeatureCollectionType) p.parse(new ByteArrayInputStream(
                response.getOutputStreamContent().getBytes()));
        FeatureCollection fc = (FeatureCollection) fct.getFeature().get(0);

        assertEquals(1, fc.size());
        SimpleFeature sf = (SimpleFeature) fc.features().next();
        Geometry simplified = ((Geometry) sf.getDefaultGeometry());
        // should have been clipped to this specific area
        assertTrue(new Envelope(-100, -90, 40, 45).contains(simplified.getEnvelopeInternal()));
    }
}
