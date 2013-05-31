/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import java.io.ByteArrayInputStream;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.filter.v2_0.FES;
import org.geotools.gml3.v3_2.GML;
import org.geotools.wfs.v2_0.WFS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class TransactionTest extends WFS20TestSupport {

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        
        dataDirectory.addPropertiesType( 
            new QName( MockData.SF_URI, "WithGMLProperties", MockData.SF_PREFIX ), 
            org.geoserver.wfs.v1_1.TransactionTest.class.getResource("WithGMLProperties.properties"),
            Collections.EMPTY_MAP
         );
    }
    
    public void testInsert1() throws Exception {
        String xml = "<wfs:Transaction service='WFS' version='2.0.0' "
            + " xmlns:wfs='" + WFS.NAMESPACE + "' xmlns:gml='" + GML.NAMESPACE + "' "
            + " xmlns:sf='http://cite.opengeospatial.org/gmlsf'>"
            + "<wfs:Insert handle='insert-1'>"
            + " <sf:PrimitiveGeoFeature gml:id='cite.gmlsf0-f01'>"
            + "  <gml:description>"
            + "Fusce tellus ante, tempus nonummy, ornare sed, accumsan nec, leo."
            + "Vivamus pulvinar molestie nisl."
            + "</gml:description>"
            + "<gml:name>Aliquam condimentum felis sit amet est.</gml:name>"
            //+ "<gml:name codeSpace='http://cite.opengeospatial.org/gmlsf'>cite.gmlsf0-f01</gml:name>"
            + "<sf:curveProperty>"
            + "  <gml:LineString gml:id='cite.gmlsf0-g01' srsName='urn:x-fes:def:crs:EPSG:6.11.2:4326'>"
            + "   <gml:posList>47.608284 19.034142 51.286873 16.7836 49.849854 15.764992</gml:posList>"
            + " </gml:LineString>"
            + "</sf:curveProperty>"
            + "<sf:intProperty>1025</sf:intProperty>"
            + "<sf:measurand>7.405E2</sf:measurand>"
            + "<sf:dateTimeProperty>2006-06-23T12:43:12+01:00</sf:dateTimeProperty>"
            + "<sf:decimalProperty>90.62</sf:decimalProperty>"
            + "</sf:PrimitiveGeoFeature>"
            + "</wfs:Insert>"
            + "<wfs:Insert handle='insert-2'>"
            + "<sf:AggregateGeoFeature gml:id='cite.gmlsf0-f02'>"
            + " <gml:description>"
            + "Duis nulla nisi, molestie vel, rhoncus a, ullamcorper eu, justo. Sed bibendum."
            + " Ut sem. Mauris nec nunc a eros aliquet pharetra. Mauris nonummy, pede et"
            + " tincidunt ultrices, mauris lectus fermentum massa, in ullamcorper lectus"
            + "felis vitae metus. Sed imperdiet sollicitudin dolor."
            + " </gml:description>"
            + " <gml:name codeSpace='http://cite.opengeospatial.org/gmlsf'>cite.gmlsf0-f02</gml:name>"
            + " <gml:name>Quisqué viverra</gml:name>"
            + " <gml:boundedBy>"
            + "   <gml:Envelope srsName='urn:x-fes:def:crs:EPSG:6.11.2:4326'>"
            + "     <gml:lowerCorner>36.1 8.0</gml:lowerCorner>"
            + "    <gml:upperCorner>52.0 21.1</gml:upperCorner>"
            + "   </gml:Envelope>"
            + "  </gml:boundedBy>"
            + "   <sf:multiPointProperty>"
            + "<gml:MultiPoint srsName='urn:x-fes:def:crs:EPSG:6.11.2:4326'>"
            + "<gml:pointMember>"
            + " <gml:Point><gml:pos>49.325176 21.036873</gml:pos></gml:Point>"
            + "</gml:pointMember>"
            + "<gml:pointMember>"
            + "  <gml:Point><gml:pos>36.142586 13.56189</gml:pos></gml:Point>"
            + "</gml:pointMember>"
            + "<gml:pointMember>"
            + "  <gml:Point><gml:pos>51.920937 8.014193</gml:pos></gml:Point>"
            + "</gml:pointMember>"
            + "</gml:MultiPoint>"
            + "</sf:multiPointProperty>"
            +

            "<sf:doubleProperty>2012.78</sf:doubleProperty>"
            + "  <sf:intRangeProperty>43</sf:intRangeProperty>"
            + " <sf:strProperty>"
            + "Donec ligulä pede, sodales iń, vehicula eu, sodales et, lêo."
            + "</sf:strProperty>"
            + "<sf:featureCode>AK121</sf:featureCode>"
            + "</sf:AggregateGeoFeature>"
            + "</wfs:Insert>"
            + "</wfs:Transaction>";


        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertTrue(dom.getElementsByTagName("fes:ResourceId").getLength() > 0);
    }

    	
     public void testInsertWithNoSRS() throws Exception {
        // 1. do a getFeature
        String getFeature = 
            "<wfs:GetFeature service='WFS' version='2.0.0' "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:fes='" + FES.NAMESPACE + "' xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "<wfs:Query typeNames=\"cgf:Points\"> "
                + "<wfs:ValueReference>cite:id</wfs:ValueReference> " + "</wfs:Query> "
            + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        int n = dom.getElementsByTagName("cgf:Points").getLength();

        // perform an insert
        String insert = 
            "<wfs:Transaction service='WFS' version='2.0.0' "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:fes='" + FES.NAMESPACE + "' "
                + "xmlns:wfs='" + WFS.NAMESPACE + "' "
                + "xmlns:gml='" + GML.NAMESPACE + "'> " 
                + "<wfs:Insert > " 
                  + "<cgf:Points>"
                    + "<cgf:pointProperty>" 
                      + "<gml:Point>" 
                      + "<gml:pos>20 40</gml:pos>"
                      + "</gml:Point>" 
                    + "</cgf:pointProperty>"
                  + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Points>" 
              + "</wfs:Insert>" 
           + "</wfs:Transaction>";

        dom = postAsDOM("wfs", insert);

        NodeList numberInserteds = dom.getElementsByTagName("wfs:totalInserted");
        Element numberInserted = (Element) numberInserteds.item(0);
        assertNotNull(numberInserted);
        assertEquals("1", numberInserted.getFirstChild().getNodeValue());
        String fid = getFirstElementByTagName(dom, "fes:ResourceId").getAttribute("rid");
        
        // check insertion occurred
        dom = postAsDOM("wfs", getFeature);
        assertEquals(n + 1, dom.getElementsByTagName("cgf:Points").getLength());

        // check coordinate order is preserved
        getFeature = "<wfs:GetFeature service='WFS' version='2.0.0' "
            + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
            + "xmlns:fes='" + FES.NAMESPACE + "' "
            + "xmlns:wfs='" + WFS.NAMESPACE + "'>"
            + "<wfs:Query typeNames=\"cgf:Points\"> "
            + "<fes:Filter>"
              + "<fes:PropertyIsEqualTo>"
                + "<fes:ValueReference>cgf:id</fes:ValueReference>"
                + "<fes:Literal>t0002</fes:Literal>"
              + "</fes:PropertyIsEqualTo>"
            + "</fes:Filter>"
            + "</wfs:Query> "
            + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", getFeature);
        assertEquals("20.0 40.0", getFirstElementByTagName(dom, "gml:pos").getFirstChild().getNodeValue());
    }

    public void testInsertWithSRS() throws Exception {

        // 1. do a getFeature
        String getFeature = 
        "<wfs:GetFeature service='WFS' version='2.0.0' xmlns:cgf='http://www.opengis.net/cite/geometry' " +
        "  xmlns:fes='" + FES.NAMESPACE + "' " +
        "  xmlns:wfs='" + WFS.NAMESPACE + "'> " + 
        " <wfs:Query typeNames='cgf:Points'/> " + 
        "</wfs:GetFeature> "; 

        Document dom = postAsDOM("wfs", getFeature);
//        print(dom);
        int n = dom.getElementsByTagName("cgf:Points").getLength();

        // perform an insert
        String insert = 
        "<wfs:Transaction service='WFS' version='2.0.0' xmlns:cgf='http://www.opengis.net/cite/geometry' " + 
        "  xmlns:fes='" + FES.NAMESPACE + "' " +
        "  xmlns:wfs='" + WFS.NAMESPACE + "' " +
        "  xmlns:gml='" + GML.NAMESPACE + "'> " +  
        " <wfs:Insert srsName='EPSG:32615'> " + 
        "  <cgf:Points> " + 
        "   <cgf:pointProperty> " + 
        "    <gml:Point> " + 
        "     <gml:pos>1 1</gml:pos> " + 
        "    </gml:Point> " + 
        "   </cgf:pointProperty> " + 
        "   <cgf:id>t0003</cgf:id> " + 
        "  </cgf:Points> " + 
        " </wfs:Insert> " + 
        "</wfs:Transaction>"; 

        dom = postAsDOM("wfs", insert);

        NodeList numberInserteds = dom.getElementsByTagName("wfs:totalInserted");
        Element numberInserted = (Element) numberInserteds.item(0);

        assertNotNull(numberInserted);
        assertEquals("1", numberInserted.getFirstChild().getNodeValue());

        // do another get feature
        getFeature = "<wfs:GetFeature " + "service=\"WFS\" version=\"2.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" " + 
                "  xmlns:fes='" + FES.NAMESPACE + "' " +
                "  xmlns:wfs='" + WFS.NAMESPACE + "' " +
                "  xmlns:gml='" + GML.NAMESPACE + "'> " +  
                "<wfs:Query typeNames=\"cgf:Points\"> "
                + "</wfs:Query> "
                + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", getFeature);

        NodeList pointsList = dom.getElementsByTagName("cgf:Points");
        assertEquals(n + 1, pointsList.getLength());
    }

    public void testInsertWithGMLProperties() throws Exception {
    
         String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
             "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
             "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" " + 
             "xmlns:gml='" + GML.NAMESPACE + "'>" +  
             "<wfs:Insert>" +    
               "<sf:WithGMLProperties>" + 
                  "<gml:location>" + 
                       "<gml:Point>" + 
                          "<gml:coordinates>2,2</gml:coordinates>" + 
                       "</gml:Point>" + 
                   "</gml:location>" + 
                   "<gml:name>two</gml:name>" + 
                   "<sf:foo>2</sf:foo>" +
                 "</sf:WithGMLProperties>" + 
               "</wfs:Insert>" + 
             "</wfs:Transaction>";
         
         Document dom = postAsDOM("wfs", xml);
         assertEquals( "wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
         
         Element inserted = getFirstElementByTagName(dom, "wfs:totalInserted");
         assertEquals( "1", inserted.getFirstChild().getNodeValue());
         
         dom = getAsDOM("wfs?request=getfeature&service=wfs&version=1.1.0&typename=sf:WithGMLProperties");
         NodeList features = dom.getElementsByTagName("sf:WithGMLProperties");
         assertEquals( 2, features.getLength() );
         
         Element feature = (Element) features.item( 1 );
         assertEquals( "two", getFirstElementByTagName(feature, "gml:name" ).getFirstChild().getNodeValue());
         assertEquals( "2", getFirstElementByTagName(feature, "sf:foo" ).getFirstChild().getNodeValue());
         
         Element location = getFirstElementByTagName( feature, "gml:location" );
         Element pos = getFirstElementByTagName(location, "gml:pos");
         
         assertEquals( "2.0 2.0", pos.getFirstChild().getNodeValue() );
         
         xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
         "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
         "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" " + 
         "xmlns:gml='" + GML.NAMESPACE + "'>" +  
         "<wfs:Insert>" +    
           "<sf:WithGMLProperties>" + 
              "<sf:location>" + 
                   "<gml:Point>" + 
                      "<gml:coordinates>3,3</gml:coordinates>" + 
                   "</gml:Point>" + 
               "</sf:location>" + 
               "<sf:name>three</sf:name>" +
               "<sf:foo>3</sf:foo>" +
             "</sf:WithGMLProperties>" + 
           "</wfs:Insert>" + 
         "</wfs:Transaction>";
         
         dom = postAsDOM("wfs", xml);
         
         assertEquals( "wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
         
         dom = getAsDOM("wfs?request=getfeature&service=wfs&version=2.0.0&typename=sf:WithGMLProperties");
         
         features = dom.getElementsByTagName("sf:WithGMLProperties");
         assertEquals( 3, features.getLength() );
         
         feature = (Element) features.item( 2 );
         assertEquals( "three", getFirstElementByTagName(feature, "gml:name" ).getFirstChild().getNodeValue());
         assertEquals( "3", getFirstElementByTagName(feature, "sf:foo" ).getFirstChild().getNodeValue());
         
         location = getFirstElementByTagName( feature, "gml:location" );
         pos = getFirstElementByTagName(location, "gml:pos");
         
         assertEquals( "3.0 3.0", pos.getFirstChild().getNodeValue() );
    }
    
    public void testUpdateWithGMLProperties() throws Exception {
        String xml = 
            "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
               "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" " +
               "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
               "xmlns:fes='" + FES.NAMESPACE + "' " + 
               "xmlns:gml='" + GML.NAMESPACE + "'>" +
               " <wfs:Update typeName=\"sf:WithGMLProperties\">" +
               "   <wfs:Property>" +
               "     <wfs:ValueReference>gml:name</wfs:ValueReference>" +
               "     <wfs:Value>two</wfs:Value>" +
               "   </wfs:Property>" + 
               "   <wfs:Property>" +
               "     <wfs:ValueReference>gml:location</wfs:ValueReference>" +
               "     <wfs:Value>" +
               "        <gml:Point>" + 
               "          <gml:coordinates>2,2</gml:coordinates>" + 
               "        </gml:Point>" + 
               "     </wfs:Value>" +
               "   </wfs:Property>" +
               "   <wfs:Property>" +
               "     <wfs:ValueReference>sf:foo</wfs:ValueReference>" +
               "     <wfs:Value>2</wfs:Value>" +
               "   </wfs:Property>" +
               "   <fes:Filter>" +
               "     <fes:PropertyIsEqualTo>" +
               "       <fes:ValueReference>foo</fes:ValueReference>" + 
               "       <fes:Literal>1</fes:Literal>" + 
               "     </fes:PropertyIsEqualTo>" + 
               "   </fes:Filter>" +
               " </wfs:Update>" +
              "</wfs:Transaction>"; 

        Document dom = postAsDOM( "wfs", xml );
        assertEquals( "wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        
        Element updated = getFirstElementByTagName(dom, "wfs:totalUpdated");
        assertEquals( "1", updated.getFirstChild().getNodeValue());
        
        dom = getAsDOM("wfs?request=getfeature&service=wfs&version=2.0.0&typename=sf:WithGMLProperties");
        NodeList features = dom.getElementsByTagName("sf:WithGMLProperties");
        assertEquals( 1, features.getLength() );
     
        Element feature = (Element) features.item( 0 );
        assertEquals( "two", getFirstElementByTagName(feature, "gml:name" ).getFirstChild().getNodeValue());
        assertEquals( "2", getFirstElementByTagName(feature, "sf:foo" ).getFirstChild().getNodeValue());
        
        Element location = getFirstElementByTagName( feature, "gml:location" );
        Element pos = getFirstElementByTagName(location, "gml:pos");
        
        assertEquals( "2.0 2.0", pos.getFirstChild().getNodeValue() );
        
        xml = 
            "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
                "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" " +
                "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
                "xmlns:fes='" + FES.NAMESPACE + "' " + 
                "xmlns:gml='" + GML.NAMESPACE + "'>" +
             " <wfs:Update typeName=\"sf:WithGMLProperties\">" +
               "   <wfs:Property>" +
               "     <wfs:ValueReference>sf:name</wfs:ValueReference>" +
               "     <wfs:Value>trhee</wfs:Value>" +
               "   </wfs:Property>" + 
               "   <wfs:Property>" +
               "     <wfs:ValueReference>sf:location</wfs:ValueReference>" +
               "     <wfs:Value>" +
               "        <gml:Point>" + 
               "          <gml:coordinates>3,3</gml:coordinates>" + 
               "        </gml:Point>" + 
               "     </wfs:Value>" +
               "   </wfs:Property>" +
               "   <wfs:Property>" +
               "     <wfs:ValueReference>sf:foo</wfs:ValueReference>" +
               "     <wfs:Value>3</wfs:Value>" +
               "   </wfs:Property>" +
               "   <fes:Filter>" +
               "     <fes:PropertyIsEqualTo>" +
               "       <fes:ValueReference>foo</fes:ValueReference>" + 
               "       <fes:Literal>2</fes:Literal>" + 
               "     </fes:PropertyIsEqualTo>" + 
               "   </fes:Filter>" +
               " </wfs:Update>" +
              "</wfs:Transaction>"; 

        dom = postAsDOM( "wfs", xml );
        assertEquals( "wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        
        updated = getFirstElementByTagName(dom, "wfs:totalUpdated");
        assertEquals( "1", updated.getFirstChild().getNodeValue());
        
        dom = getAsDOM("wfs?request=getfeature&service=wfs&version=2.0.0&typename=sf:WithGMLProperties");
        
        features = dom.getElementsByTagName("sf:WithGMLProperties");
        assertEquals( 1, features.getLength() );
     
        feature = (Element) features.item( 0 );
        assertEquals( "trhee", getFirstElementByTagName(feature, "gml:name" ).getFirstChild().getNodeValue());
        assertEquals( "3", getFirstElementByTagName(feature, "sf:foo" ).getFirstChild().getNodeValue());
        
        location = getFirstElementByTagName( feature, "gml:location" );
        pos = getFirstElementByTagName(location, "gml:pos");
        
        assertEquals( "3.0 3.0", pos.getFirstChild().getNodeValue() );
    }
    
    public void testInsertWithBoundedBy() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" "
            + " xmlns:wfs='" + WFS.NAMESPACE + "' "
            + " xmlns:gml='" + GML.NAMESPACE + "' "
            + " xmlns:cite=\"http://www.opengis.net/cite\">"
            + "<wfs:Insert>"
            + " <cite:BasicPolygons>"
            + " <gml:boundedBy>"
            + "  <gml:Envelope>"
            + "<gml:lowerCorner>-1.0 2.0</gml:lowerCorner>"
            + "<gml:upperCorner>2.0 5.0</gml:upperCorner>"
            + "  </gml:Envelope>"
            + " </gml:boundedBy>"
            + "  <cite:the_geom>"
            + "    <gml:MultiPolygon>"
            + "      <gml:polygonMember>" 
            + "         <gml:Polygon>" 
            + "<gml:exterior>" 
            + "<gml:LinearRing>" 
            + "<gml:posList>-1.0 5.0 2.0 5.0 2.0 2.0 -1.0 2.0 -1.0 5.0</gml:posList>" 
            + "</gml:LinearRing>" 
            + "</gml:exterior>" 
            + "         </gml:Polygon>" 
            + "      </gml:polygonMember>"
            + "    </gml:MultiPolygon>"
            + "  </cite:the_geom>"
            + "  <cite:ID>foo</cite:ID>"
            + " </cite:BasicPolygons>"
            + "</wfs:Insert>"
            + "</wfs:Transaction>";
    
        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());
        assertTrue(dom.getElementsByTagName("fes:ResourceId").getLength() > 0);
    }
    
    public void testInsert2() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" "
            + " xmlns:wfs='" + WFS.NAMESPACE + "' "
            + " xmlns:gml='" + GML.NAMESPACE + "' "
            + " xmlns:cite=\"http://www.opengis.net/cite\">"
            + "<wfs:Insert>"
            + " <cite:RoadSegments>"
            + "  <cite:the_geom>"
            + "<gml:MultiCurve srsName=\"EPSG:4326\">"
            + " <gml:curveMember>"
            + "   <gml:LineString>"
            + "        <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>"
            + "   </gml:LineString>"
            + " </gml:curveMember>"
            + "</gml:MultiCurve>"
            + "  </cite:the_geom>"
            + "  <cite:FID>foo</cite:FID>"
            + "  <cite:NAME>bar</cite:NAME>" 
            + " </cite:RoadSegments>"
            + "</wfs:Insert>"
            + "</wfs:Transaction>";
    
        Document dom = postAsDOM( "wfs", xml );
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());
        
        dom = getAsDOM( "wfs?version=2.0.0&request=getfeature&typename=cite:RoadSegments&srsName=EPSG:4326&" +
    		"cql_filter=FID%3D'foo'");
        print(dom);
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );
        
        assertEquals( 1, dom.getElementsByTagName("cite:RoadSegments").getLength() );
        
        Element roadSegment = getFirstElementByTagName(dom, "cite:RoadSegments" );
        Element posList = getFirstElementByTagName( roadSegment, "gml:posList" );
        String[] pos = posList.getFirstChild().getTextContent().split( " " );
        assertEquals( 4, pos.length );
        assertEquals( 4.2582, Double.parseDouble( pos[0] ), 1E-4 );
        assertEquals( 52.0643, Double.parseDouble( pos[1] ), 1E-4 );
        assertEquals( 4.2584, Double.parseDouble( pos[2] ), 1E-4 );
        assertEquals( 52.0648, Double.parseDouble( pos[3] ), 1E-4 );
    }

    public void testInsert3() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" "
                + " xmlns:wfs='" + WFS.NAMESPACE + "' "
                + " xmlns:gml='" + GML.NAMESPACE + "' "
                + " xmlns:cite=\"http://www.opengis.net/cite\">"
                + "<wfs:Insert>"
                + " <cite:Buildings>"
                + "  <cite:the_geom>" + 
                "<gml:MultiSurface> " + 
                " <gml:surfaceMember> " + 
                "  <gml:Polygon> " + 
                "   <gml:exterior> " + 
                "    <gml:LinearRing> " + 
                "     <gml:posList>-123.9 40.0 -124.0 39.9 -124.1 40.0 -124.0 40.1 -123.9 40.0</gml:posList>" + 
                "    </gml:LinearRing> " + 
                "   </gml:exterior> " + 
                "  </gml:Polygon> " + 
                " </gml:surfaceMember> " + 
                "</gml:MultiSurface> " 
                + "  </cite:the_geom>"
                + "  <cite:FID>115</cite:FID>"
                + "  <cite:ADDRESS>987 Foo St</cite:ADDRESS>" 
                + " </cite:Buildings>"
                + "</wfs:Insert>"
                + "</wfs:Transaction>";
        
        Document dom = postAsDOM( "wfs", xml );
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());

        dom = getAsDOM( "wfs?version=2.0.0&request=getfeature&typename=cite:Buildings&srsName=EPSG:4326&" +
            "cql_filter=FID%3D'115'");
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );

        assertEquals( 1, dom.getElementsByTagName("cite:Buildings").getLength() );
        XMLAssert.assertXpathExists("//gml:Polygon",dom);
        
        Element posList = getFirstElementByTagName( dom.getDocumentElement(), "gml:posList" );
        String[] pos = posList.getFirstChild().getTextContent().split( " " );

        assertEquals( 10, pos.length );
        assertEquals( -123.9, Double.parseDouble( pos[0] ), 1E-1 );
        assertEquals( 40.0, Double.parseDouble( pos[1] ), 1E-1 );
        assertEquals( -124.0, Double.parseDouble( pos[2] ), 1E-1 );
        assertEquals( 39.9, Double.parseDouble( pos[3] ), 1E-1 );
        
        assertEquals( -124.1, Double.parseDouble( pos[4] ), 1E-1 );
        assertEquals( 40.0, Double.parseDouble( pos[5] ), 1E-1 );
        assertEquals( -124.0, Double.parseDouble( pos[6] ), 1E-1 );
        assertEquals( 40.1, Double.parseDouble( pos[7] ), 1E-1 );
        
        assertEquals( -123.9, Double.parseDouble( pos[8] ), 1E-1 );
        assertEquals( 40.0, Double.parseDouble( pos[9] ), 1E-1 );
    }

    public void testUpdateForcedSRS() throws Exception {
        testUpdate("srsName=\"EPSG:4326\"");
    }
    
    public void testUpdateNoSRS() throws Exception {
        testUpdate("");
    }
    
    private void testUpdate(String srs) throws Exception {
        String xml =
        "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
        "xmlns:cite=\"http://www.opengis.net/cite\" " +
        "xmlns:fes='" + FES.NAMESPACE + "' " + 
        "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
        "xmlns:gml='" + GML.NAMESPACE + "'>" + 
        " <wfs:Update typeName=\"cite:RoadSegments\">" +
        "   <wfs:Property>" +
        "     <wfs:ValueReference>cite:the_geom</wfs:ValueReference>" +
        "     <wfs:Value>" +
        "      <gml:MultiCurve " + srs + ">" + 
        "       <gml:curveMember>" + 
        "         <gml:LineString>" +
        "            <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>" +
        "         </gml:LineString>" +
        "       </gml:curveMember>" +
        "      </gml:MultiCurve>" +
        "     </wfs:Value>" +
        "   </wfs:Property>" + 
        "   <fes:Filter>" +
        "     <fes:PropertyIsEqualTo>" +
        "       <fes:ValueReference>FID</fes:ValueReference>" + 
        "       <fes:Literal>102</fes:Literal>" + 
        "     </fes:PropertyIsEqualTo>" + 
        "   </fes:Filter>" +
        " </wfs:Update>" +
       "</wfs:Transaction>"; 
        
        Document dom = postAsDOM( "wfs", xml );
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild().getNodeValue());
        
        String srsBlock = "".equals(srs) ? "" : "&" + srs.replaceAll("\"", "");
        dom = getAsDOM( "wfs?version=2.0.0&request=getfeature&typename=cite:RoadSegments" + srsBlock + "&" +
            "cql_filter=FID%3D'102'");
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );
        
        assertEquals( 1, dom.getElementsByTagName("cite:RoadSegments").getLength() );
        
        Element roadSegment = getFirstElementByTagName(dom, "cite:RoadSegments" );
        Element posList = getFirstElementByTagName( roadSegment, "gml:posList" );
        String[] pos = posList.getFirstChild().getTextContent().split( " " );
        assertEquals( 4, pos.length );
        assertEquals( 4.2582, Double.parseDouble( pos[0] ), 1E-4 );
        assertEquals( 52.0643, Double.parseDouble( pos[1] ), 1E-4 );
        assertEquals( 4.2584, Double.parseDouble( pos[2] ), 1E-4 );
        assertEquals( 52.0648, Double.parseDouble( pos[3] ), 1E-4 );
    }
    
    public void testUpdateWithInvalidProperty() throws Exception {
        String xml =
            "<wfs:Transaction service=\"WFS\" version=\"2.0.0\"" + 
            " xmlns:cite=\"http://www.opengis.net/cite\"" +
            " xmlns:fes='" + FES.NAMESPACE + "' " + 
            " xmlns:wfs='" + WFS.NAMESPACE + "' " + 
            " xmlns:gml='" + GML.NAMESPACE + "'>" +
            " <wfs:Update typeName=\"cite:RoadSegments\">" +
            "   <wfs:Property>" +
            "     <wfs:ValueReference>INVALID</wfs:ValueReference>" +
            "     <wfs:Value>INVALID</wfs:Value>" +
            "   </wfs:Property>" + 
            "   <fes:Filter>" +
            "     <fes:PropertyIsEqualTo>" +
            "       <fes:ValueReference>FID</fes:ValueReference>" + 
            "       <fes:Literal>102</fes:Literal>" + 
            "     </fes:PropertyIsEqualTo>" + 
            "   </fes:Filter>" +
            " </wfs:Update>" +
           "</wfs:Transaction>"; 
            
            Document dom = postAsDOM( "wfs", xml );
            assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    public void testInsertLayerQualified() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" "
            + " xmlns:fes='" + FES.NAMESPACE + "' " 
            + " xmlns:wfs='" + WFS.NAMESPACE + "' " 
            + " xmlns:gml='" + GML.NAMESPACE + "' "
            + " xmlns:cite=\"http://www.opengis.net/cite\">"
            + "<wfs:Insert>"
            + " <cite:RoadSegments>"
            + "  <cite:the_geom>"
            + "<gml:MultiCurve xmlns:gml=\"http://www.opengis.net/gml\""
            + "    srsName=\"EPSG:4326\">"
            + " <gml:curveMember>"
            + "                  <gml:LineString>"
            + "                   <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>"
            + "                 </gml:LineString>"
            + "               </gml:curveMember>"
            + "             </gml:MultiCurve>"
            + "  </cite:the_geom>"
            + "  <cite:FID>foo</cite:FID>"
            + "  <cite:NAME>bar</cite:NAME>" 
            + " </cite:RoadSegments>"
            + "</wfs:Insert>"
            + "</wfs:Transaction>";
    
        Document dom = postAsDOM( "cite/Forests/wfs", xml );
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        
        dom = postAsDOM( "cite/RoadSegments/wfs", xml );
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());

    }
    
    public void testUpdateLayerQualified() throws Exception {
        String xml =
            "<wfs:Transaction service=\"WFS\" version=\"2.0.0\"" 
            + " xmlns:fes='" + FES.NAMESPACE + "' " 
            + " xmlns:wfs='" + WFS.NAMESPACE + "' " 
            + " xmlns:gml='" + GML.NAMESPACE + "' "
            + " xmlns:cite=\"http://www.opengis.net/cite\">" + 
            " <wfs:Update typeName=\"RoadSegments\">" +
            "   <wfs:Property>" +
            "     <wfs:ValueReference>cite:the_geom</wfs:ValueReference>" +
            "     <wfs:Value>" +
            "      <gml:MultiCurve xmlns:gml=\"http://www.opengis.net/gml\">" + 
            "       <gml:curveMember>" + 
            "         <gml:LineString>" +
            "            <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>" +
            "         </gml:LineString>" +
            "       </gml:curveMember>" +
            "      </gml:MultiCurve>" +
            "     </wfs:Value>" +
            "   </wfs:Property>" + 
            "   <fes:Filter>" +
            "     <fes:PropertyIsEqualTo>" +
            "       <fes:ValueReference>FID</fes:ValueReference>" + 
            "       <fes:Literal>102</fes:Literal>" + 
            "     </fes:PropertyIsEqualTo>" + 
            "   </fes:Filter>" +
            " </wfs:Update>" +
           "</wfs:Transaction>"; 
            
            Document dom = postAsDOM( "cite/Forests/wfs", xml );
            XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
            
            dom = postAsDOM("cite/RoadSegments/wfs", xml);
            assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
            assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild().getNodeValue());
         
    }

   public void testUpdateWithDifferentPrefix() throws Exception {
       String xml =
           "<wfs:Transaction service=\"WFS\" version=\"2.0.0\"" + 
            " xmlns:fes='" + FES.NAMESPACE + "' " +  
            " xmlns:wfs='" + WFS.NAMESPACE + "' " + 
            " xmlns:gml='" + GML.NAMESPACE + "'>" + 
           " <wfs:Update xmlns:foo=\"http://www.opengis.net/cite\" typeName=\"foo:RoadSegments\">" +
           "   <wfs:Property>" +
           "     <wfs:ValueReference>foo:the_geom</wfs:ValueReference>" +
           "     <wfs:Value>" +
           "     </wfs:Value>" +
           "   </wfs:Property>" + 
           "   <fes:Filter>" +
           "     <fes:PropertyIsEqualTo>" +
           "       <fes:ValueReference>FID</fes:ValueReference>" + 
           "       <fes:Literal>102</fes:Literal>" + 
           "     </fes:PropertyIsEqualTo>" + 
           "   </fes:Filter>" +
           " </wfs:Update>" +
          "</wfs:Transaction>";
       
       Document dom = postAsDOM( "wfs", xml );
       assertEquals( "wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
       
       Element updated = getFirstElementByTagName(dom, "wfs:totalUpdated");
       assertEquals( "1", updated.getFirstChild().getNodeValue());
   }

   public void testReplace() throws Exception {
       Document dom = getAsDOM("wfs?service=wfs&version=2.0.0&request=getfeature&typename=cite:RoadSegments" +
           "&cql_filter=FID+EQ+'102'");
       XMLAssert.assertXpathExists("//cite:RoadSegments/cite:FID[text() = '102']", dom);
       
       String xml =
           "<wfs:Transaction service=\"WFS\" version=\"2.0.0\"" + 
            " xmlns:fes='" + FES.NAMESPACE + "' " +  
            " xmlns:wfs='" + WFS.NAMESPACE + "' " + 
            " xmlns:gml='" + GML.NAMESPACE + "' " + 
            " xmlns:cite='http://www.opengis.net/cite'>" + 
           " <wfs:Replace>" + 
           "  <cite:RoadSegments gml:id='RoadSegments.1107532045088'> " + 
           "      <cite:the_geom> " + 
           "         <gml:MultiCurve srsDimension='2' srsName='urn:x-ogc:def:crs:EPSG:4326'> " + 
           "          <gml:curveMember> " + 
           "            <gml:LineString> " + 
           "              <gml:posList>1 2 3 5 6 7</gml:posList> " + 
           "            </gml:LineString> " + 
           "          </gml:curveMember> " + 
           "        </gml:MultiCurve> " + 
           "      </cite:the_geom> " + 
           "      <cite:FID>1234</cite:FID> " + 
           "      <cite:NAME>Route 1234</cite:NAME> " + 
           "   </cite:RoadSegments> " + 

           "   <fes:Filter>" + 
           "     <fes:PropertyIsEqualTo>" +
           "       <fes:ValueReference>FID</fes:ValueReference>" + 
           "       <fes:Literal>102</fes:Literal>" + 
           "     </fes:PropertyIsEqualTo>" + 
           "   </fes:Filter>" +
           " </wfs:Replace>" +
          "</wfs:Transaction>";
       
       dom = postAsDOM("wfs", xml);
       assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
       XMLAssert.assertXpathExists("//wfs:totalReplaced[text() = 1]", dom);
       XMLAssert.assertXpathExists("//wfs:ReplaceResults/wfs:Feature/fes:ResourceId", dom);

       dom = getAsDOM("wfs?service=wfs&version=2.0.0&request=getfeature&typename=cite:RoadSegments" +
               "&cql_filter=FID+EQ+'102'");
       XMLAssert.assertXpathNotExists("//cite:RoadSegments/cite:FID[text() = '102']", dom);
       
       dom = getAsDOM("wfs?service=wfs&version=2.0.0&request=getfeature&typename=cite:RoadSegments" +
               "&cql_filter=FID+EQ+'1234'");
       XMLAssert.assertXpathExists("//cite:RoadSegments/cite:FID[text() = '1234']", dom);
   }

   public void testSOAP() throws Exception {
       String xml = 
          "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> " + 
               " <soap:Header/> " + 
               " <soap:Body>"
                 +  "<wfs:Transaction service='WFS' version='2.0.0' "
                       + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                       + "xmlns:fes='" + FES.NAMESPACE + "' "
                       + "xmlns:wfs='" + WFS.NAMESPACE + "' "
                       + "xmlns:gml='" + GML.NAMESPACE + "'> " 
                       + "<wfs:Insert > " 
                         + "<cgf:Points>"
                           + "<cgf:pointProperty>" 
                             + "<gml:Point>" 
                             + "<gml:pos>20 40</gml:pos>"
                             + "</gml:Point>" 
                           + "</cgf:pointProperty>"
                         + "<cgf:id>t0002</cgf:id>"
                       + "</cgf:Points>" 
                     + "</wfs:Insert>" 
                  + "</wfs:Transaction>" + 
               " </soap:Body> " + 
           "</soap:Envelope> "; 
             
       MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/soap+xml");
       assertEquals("application/soap+xml", resp.getContentType());
       
       Document dom = dom(new ByteArrayInputStream(resp.getOutputStreamContent().getBytes()));
       assertEquals("soap:Envelope", dom.getDocumentElement().getNodeName());
       assertEquals(1, dom.getElementsByTagName("wfs:TransactionResponse").getLength());
   }
}
