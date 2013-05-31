package org.geoserver.wfs.v2_0;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.GMLInfo.SrsNameStyle;
import org.geoserver.wfs.WFSInfo;
import org.w3c.dom.Document;

public class SrsNameTest extends WFS20TestSupport {

    public void testSrsNameSyntax() throws Exception {
        doTestSrsNameSyntax(SrsNameStyle.URN2, false);
        doTestSrsNameSyntax(SrsNameStyle.URN, true);
        doTestSrsNameSyntax(SrsNameStyle.URL, true);
        doTestSrsNameSyntax(SrsNameStyle.NORMAL, true);
        doTestSrsNameSyntax(SrsNameStyle.XML, true);
    }
    
    void doTestSrsNameSyntax(SrsNameStyle srsNameStyle, boolean doSave) throws Exception {
        if (doSave) {
            WFSInfo wfs = getWFS();
            GMLInfo gml = wfs.getGML().get(WFSInfo.Version.V_20);
            gml.setSrsNameStyle(srsNameStyle);
            getGeoServer().save(wfs);
        }
    
        String q = "wfs?request=getfeature&service=wfs&version=2.0.0&typenames=cgf:Points";
        Document d = getAsDOM(q);
        assertEquals("wfs:FeatureCollection", d.getDocumentElement().getNodeName());
    
        XMLAssert.assertXpathExists("//gml:Envelope[@srsName = '"+srsNameStyle.getPrefix()+"32615']", d);
        XMLAssert.assertXpathExists("//gml:Point[@srsName = '"+srsNameStyle.getPrefix()+"32615']", d);
    }
}
