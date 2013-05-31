/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import org.custommonkey.xmlunit.XMLAssert;
import org.geotools.filter.v2_0.FES;
import org.geotools.wfs.v2_0.WFS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetFeatureWithLockTest extends WFS20TestSupport {
    
//    /**
//     * This is a READ ONLY TEST so we can use one time setup
//     */
//    public static Test suite() {
//        return new OneTimeTestSetup(new GetFeatureWithLockTest());
//    }
//    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        getServiceDescriptor20().getOperations().add( "ReleaseLock");
    }

    public void testPOST() throws Exception {
        String xml = "<wfs:GetFeatureWithLock service='WFS' version='2.0.0' " +
            "handle='GetFeatureWithLock-tc1' expiry='5' resultType='results' " + 
            "xmlns:sf='http://cite.opengeospatial.org/gmlsf' xmlns:wfs='" + WFS.NAMESPACE + "'>" + 
                "<wfs:Query handle='qry-1' typeNames='sf:PrimitiveGeoFeature' />" + 
            "</wfs:GetFeatureWithLock>";

        Document dom = postAsDOM("wfs", xml);
        assertGML32(dom);
        assertNotNull( dom.getDocumentElement().getAttribute("lockId") );
    }
    
    public void testUpdateLockedFeatureWithLockId() throws Exception {
        // get feature
        String xml = 
            "<wfs:GetFeature service='WFS' version='2.0.0' expiry='10' "
                + "xmlns:cdf='http://www.opengis.net/cite/data' "
                + "xmlns:fes='" + FES.NAMESPACE +  "' xmlns:wfs='" + WFS.NAMESPACE + "'>"
            + "<wfs:Query typeNames='cdf:Locks'/>" 
          + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertGML32(dom);
        
        // get a fid
        assertFalse(dom.getElementsByTagName("cdf:Locks").getLength() == 0);
        String fid = ((Element) dom.getElementsByTagName("cdf:Locks").item(0)).getAttribute("gml:id");

        // lock a feature
        xml = "<wfs:GetFeatureWithLock service='WFS' version='2.0.0' expiry='10' "
                + "xmlns:cdf='http://www.opengis.net/cite/data' "
                + "xmlns:fes='" + FES.NAMESPACE +  "' xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "<wfs:Query typeNames='cdf:Locks'>" 
                  + "<fes:Filter><fes:ResourceId rid='" + fid + "'/>" + "</fes:Filter>"
                + "</wfs:Query>" 
              + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertGML32(dom);
        
        String lockId = dom.getDocumentElement().getAttribute("lockId");

        // try to update it
        xml = "<wfs:Transaction " + "  service=\"WFS\" "
                + "  version=\"2.0.0\" "
                + "  xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "' " + "> "
                + "  <wfs:LockId>" + lockId + "</wfs:LockId>"
                + "  <wfs:Update typeName=\"cdf:Locks\" handle='foo'> "
                + "    <wfs:Property> " + "      <wfs:ValueReference>cdf:id</wfs:ValueReference> "
                + "      <wfs:Value>gfwlbt0001</wfs:Value> "
                + "    </wfs:Property> " + "    <fes:Filter> "
                + "      <fes:ResourceId rid=\"" + fid + "\"/> "
                + "    </fes:Filter> " + "  </wfs:Update> "
                + "</wfs:Transaction> ";

        dom = postAsDOM("wfs", xml);

        // release the lock
        get("wfs?request=ReleaseLock&version=2.0.0&lockId=" + lockId);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement() .getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        
        XMLAssert.assertXpathExists("//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid + "']", dom);
    }
    public void testUpdateLockedFeatureWithoutLockId() throws Exception {

        // get feature
        String xml = "<wfs:GetFeature " + "service=\"WFS\" "
                + "version=\"2.0.0\" " + "expiry=\"10\" "
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "xmlns:fes='" + FES.NAMESPACE + "' "
                + "xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "<wfs:Query typeNames=\"cdf:Locks\"/>" + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);

        // get a fid
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement()
                .getNodeName());
        assertFalse(dom.getElementsByTagName("cdf:Locks").getLength() == 0);

        String fid = ((Element) dom.getElementsByTagName("cdf:Locks").item(0))
                .getAttribute("gml:id");

        // lock a feature
        xml = "<wfs:GetFeatureWithLock " + "service=\"WFS\" "
                + "version=\"2.0.0\" " + "expiry=\"10\" "
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "xmlns:fes='" + FES.NAMESPACE + "' "
                + "xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "<wfs:Query typeNames=\"cdf:Locks\">" + "<fes:Filter>"
                + "<fes:ResourceId rid=\"" + fid + "\"/>" + "</fes:Filter>"
                + "</wfs:Query>" + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement()
                .getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");

        // try to update it
        xml = "<wfs:Transaction " + "  service=\"WFS\" "
                + "  version=\"2.0.0\" "
                + "  xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "' " + "> "
                + "  <wfs:Update typeName=\"cdf:Locks\"> "
                + "    <wfs:Property> " + "      <wfs:ValueReference>cdf:id</wfs:ValueReference> "
                + "      <wfs:Value>gfwlbt0001</wfs:Value> "
                + "    </wfs:Property> " + "    <fes:Filter> "
                + "      <fes:ResourceId rid=\"" + fid + "\"/> "
                + "    </fes:Filter> " + "  </wfs:Update> "
                + "</wfs:Transaction> ";

        dom = postAsDOM("wfs", xml);
        
        // release the lock
        get("wfs?request=ReleaseLock&lockId=" + lockId);

        assertEquals("ows:ExceptionReport", dom.getDocumentElement() .getNodeName());
        XMLAssert.assertXpathExists("//ows:Exception[@exceptionCode = 'MissingParameterValue']", dom);
    }

    public void testGetFeatureWithLockReleaseActionSome() throws Exception {
        String xml = "<wfs:GetFeature" + "  service=\"WFS\""
                + "  version=\"2.0.0\"" + "  expiry=\"10\""
                + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:Query typeNames=\"cdf:Locks\"/>" + "</wfs:GetFeature>";
        Document dom = postAsDOM("wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("gml:id");
        String fid2 = ((Element) locks.item(1)).getAttribute("gml:id");

        xml = "<wfs:GetFeatureWithLock" + "  service=\"WFS\""
                + "  version=\"2.0.0\"" + "  expiry=\"10\""
                + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:Query typeNames=\"cdf:Locks\">" + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid1 + "\"/>"
                + "      <fes:ResourceId rid=\"" + fid2 + "\"/>"
                + "    </fes:Filter>" + "  </wfs:Query>"
                + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement()
                .getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");

        xml = "<wfs:Transaction" + "  service=\"WFS\"" + "  version=\"2.0.0\""
                + "  releaseAction=\"SOME\""
                + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:LockId>" + lockId + "</wfs:LockId>"
                + "  <wfs:Update typeName=\"cdf:Locks\">"
                + "    <wfs:Property>" + "      <wfs:ValueReference>cdf:id</wfs:ValueReference>"
                + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                + "    </wfs:Property>" + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid1 + "\"/>"
                + "    </fes:Filter>" + "  </wfs:Update>"
                + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        XMLAssert.assertXpathExists("//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid1 + "']", dom);
        
        xml = "<wfs:Transaction" + "  service=\"WFS\"" + "  version=\"2.0.0\""
                + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:LockId>" + lockId + "</wfs:LockId>"
                + "  <wfs:Update typeName=\"cdf:Locks\">"
                + "    <wfs:Property>" + "      <wfs:ValueReference>cdf:id</wfs:ValueReference>"
                + "      <wfs:Value>gfwlrs0004</wfs:Value>"
                + "    </wfs:Property>" + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid2 + "\"/>"
                + "    </fes:Filter>" + "  </wfs:Update>"
                + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        // release locks
        get("wfs?request=ReleaseLock&lockId=" + lockId);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        XMLAssert.assertXpathExists("//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid2 + "']", dom);
    }
 
    public void testWorkspaceQualified() throws Exception {
        String xml = "<wfs:GetFeature" + "  service=\"WFS\""
        + "  version=\"2.0.0\"" + "  expiry=\"10\""
        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
        + "  xmlns:fes='" + FES.NAMESPACE + "' "
        + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
        + "  <wfs:Query typeNames=\"Locks\"/>" + "</wfs:GetFeature>";
        Document dom = postAsDOM("cdf/wfs", xml);
        
        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("gml:id");
        String fid2 = ((Element) locks.item(1)).getAttribute("gml:id");
        
        xml = "<wfs:GetFeatureWithLock" + "  service=\"WFS\""
                + "  version=\"2.0.0\"" + "  expiry=\"10\""
                + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:Query typeNames=\"Locks\">" + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid1 + "\"/>"
                + "      <fes:ResourceId rid=\"" + fid2 + "\"/>"
                + "    </fes:Filter>" + "  </wfs:Query>"
                + "</wfs:GetFeatureWithLock>";
        
        dom = postAsDOM("cdf/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        
        String lockId = dom.getDocumentElement().getAttribute("lockId");
        //System.out.println(lockId);
        xml = "<wfs:Transaction" + "  service=\"WFS\"" + "  version=\"2.0.0\""
                + "  releaseAction=\"SOME\""
                + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:LockId>" + lockId + "</wfs:LockId>"
                + "  <wfs:Update typeName=\"Locks\">"
                + "    <wfs:Property>" + "      <wfs:ValueReference>cdf:id</wfs:ValueReference>"
                + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                + "    </wfs:Property>" + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid1 + "\"/>"
                + "    </fes:Filter>" + "  </wfs:Update>"
                + "</wfs:Transaction>";
        
        dom = postAsDOM("cdf/wfs", xml);
        
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        XMLAssert.assertXpathExists("//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid1 + "']", dom);
        
        xml = "<wfs:Transaction" + "  service=\"WFS\"" + "  version=\"2.0.0\""
                + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:LockId>" + lockId + "</wfs:LockId>"
                + "  <wfs:Update typeName=\"Locks\">"
                + "    <wfs:Property>" + "      <wfs:ValueReference>cdf:id</wfs:ValueReference>"
                + "      <wfs:Value>gfwlrs0004</wfs:Value>"
                + "    </wfs:Property>" + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid2 + "\"/>"
                + "    </fes:Filter>" + "  </wfs:Update>"
                + "</wfs:Transaction>";
        
        dom = postAsDOM("cdf/wfs", xml);
        
        // release locks
        get("cdf/wfs?request=ReleaseLock&lockId=" + lockId);
        
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        XMLAssert.assertXpathExists("//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid2 + "']", dom);
    }
    
    public void testLayerQualified() throws Exception {
        String xml = "<wfs:GetFeature" + "  service=\"WFS\""
        + "  version=\"2.0.0\"" + "  expiry=\"10\""
        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
        + "  xmlns:fes='" + FES.NAMESPACE + "' "
        + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
        + "  <wfs:Query typeNames=\"Locks\"/>" + "</wfs:GetFeature>";
        Document dom = postAsDOM("cdf/Locks/wfs", xml);
        
        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("gml:id");
        String fid2 = ((Element) locks.item(1)).getAttribute("gml:id");
        
        xml = "<wfs:GetFeatureWithLock" + "  service=\"WFS\""
                + "  version=\"2.0.0\"" + "  expiry=\"10\""
                + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:Query typeNames=\"Locks\">" + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid1 + "\"/>"
                + "      <fes:ResourceId rid=\"" + fid2 + "\"/>"
                + "    </fes:Filter>" + "  </wfs:Query>"
                + "</wfs:GetFeatureWithLock>";
        
        
        dom = postAsDOM("cdf/Fifteen/wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        
        dom = postAsDOM("cdf/Locks/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        
        String lockId = dom.getDocumentElement().getAttribute("lockId");
        //System.out.println(lockId);
        xml = "<wfs:Transaction" + "  service=\"WFS\"" + "  version=\"2.0.0\""
                + "  releaseAction=\"SOME\""
                + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:LockId>" + lockId + "</wfs:LockId>"
                + "  <wfs:Update typeName=\"Locks\">"
                + "    <wfs:Property>" + "      <wfs:ValueReference>cdf:id</wfs:ValueReference>"
                + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                + "    </wfs:Property>" + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid1 + "\"/>"
                + "    </fes:Filter>" + "  </wfs:Update>"
                + "</wfs:Transaction>";
        
        dom = postAsDOM("cdf/Locks/wfs", xml);
        
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        XMLAssert.assertXpathExists("//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid1 + "']", dom);
        
        xml = "<wfs:Transaction" + "  service=\"WFS\"" + "  version=\"2.0.0\""
                + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:LockId>" + lockId + "</wfs:LockId>"
                + "  <wfs:Update typeName=\"Locks\">"
                + "    <wfs:Property>" + "      <wfs:ValueReference>cdf:id</wfs:ValueReference>"
                + "      <wfs:Value>gfwlrs0004</wfs:Value>"
                + "    </wfs:Property>" + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid2 + "\"/>"
                + "    </fes:Filter>" + "  </wfs:Update>"
                + "</wfs:Transaction>";
        
        dom = postAsDOM("cdf/Locks/wfs", xml);
        
        // release locks
        get("cdf/Locks/wfs?request=ReleaseLock&lockId=" + lockId);
        
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        XMLAssert.assertXpathExists("//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid2 + "']", dom);
        
    }
}
