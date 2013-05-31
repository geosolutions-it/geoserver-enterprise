package org.geoserver.wfs.v1_1;

import org.geoserver.wfs.WFSTestSupport;
import org.geotools.filter.v1_1.OGC;
import org.w3c.dom.Document;

public class LockFeatureTest extends WFSTestSupport {

    public void testLock() throws Exception {
        String xml = "<wfs:LockFeature xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" xmlns:wfs=\"http://www.opengis.net/wfs\" expiry=\"5\" handle=\"LockFeature-tc1\" "
                + " lockAction=\"ALL\" "
                + " service=\"WFS\" "
                + " version=\"1.1.0\">"
                + "<wfs:Lock handle=\"lock-1\" typeName=\"sf:PrimitiveGeoFeature\"/>"
                + "</wfs:LockFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement()
                .getNodeName());
        assertEquals(5, dom.getElementsByTagNameNS(OGC.NAMESPACE, "FeatureId")
                .getLength());
    }
}
