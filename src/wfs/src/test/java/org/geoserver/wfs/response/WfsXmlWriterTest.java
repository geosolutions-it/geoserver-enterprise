package org.geoserver.wfs.response;

import java.io.File;
import java.io.FileOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.geoserver.wfs.WFSTestSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WfsXmlWriterTest extends WFSTestSupport {

    public void test() throws Exception {
        File tmp = File.createTempFile("wfs", "xml");
        tmp.deleteOnExit();

        WfsXmlWriter writer = new WfsXmlWriter.WFS1_0(getWFS(),
                new FileOutputStream(tmp));
        writer.openTag("wfs", "FeatureCollection");
        writer.openTag("gml", "Feature", new String[] { "id", "foo", "srs",
                "4326" });
        writer.text("some text");
        writer.closeTag("gml", "Feature");
        writer.closeTag("wfs", "FeatureCollection");
        writer.close();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        Document doc = factory.newDocumentBuilder().parse(tmp);

        assertNotNull(doc);

        assertEquals("wfs:FeatureCollection", doc.getDocumentElement()
                .getNodeName());
        NodeList features = doc.getElementsByTagName("gml:Feature");
        assertEquals(1, features.getLength());

        Element feature = (Element) features.item(0);
        assertEquals("foo", feature.getAttribute("id"));
        assertEquals("4326", feature.getAttribute("srs"));
    }
}
