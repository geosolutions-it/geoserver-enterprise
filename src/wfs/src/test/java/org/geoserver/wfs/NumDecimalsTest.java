/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class NumDecimalsTest extends WFSTestSupport {

    public void testDefaults() throws Exception {
        Document dom = getAsDOM("wfs?request=getfeature&featureid=PrimitiveGeoFeature.f008&version=1.0.0");
        runAssertions(dom,3);
    }

    public void testGlobal() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setNumDecimals(1);
        getGeoServer().save(global);
        
        Document dom = getAsDOM("wfs?request=getfeature&featureid=PrimitiveGeoFeature.f008&version=1.0.0");
        runAssertions(dom,1);
    }

    public void testPerFeatureType() throws Exception {
        Catalog cat = getCatalog();
        FeatureTypeInfo ft = cat.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        ft.setNumDecimals(1);
        cat.save(ft);
        
        Document dom = getAsDOM("wfs?request=getfeature&featureid=PrimitiveGeoFeature.f008&version=1.0.0");
        runAssertions(dom,1);
    }

    public void testMultipleFeatureTypes() throws Exception {
        Catalog cat = getCatalog();
        FeatureTypeInfo ft1 = cat.getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        FeatureTypeInfo ft2 = cat.getFeatureTypeByName("sf", "AggregateGeoFeature");
        ft1.setNumDecimals(3);
        cat.save(ft1);
            
        ft2.setNumDecimals(1);
        cat.save(ft2);
        
        Document dom = getAsDOM("wfs?request=getfeature&featureid=PrimitiveGeoFeature.f008,AggregateGeoFeature.f009&version=1.0.0");
        runAssertions(dom, 3);
    }
    
    void runAssertions(Document dom, int numdecimals) throws Exception {
        NodeList nl = dom.getElementsByTagName("gml:coordinates");
        for (int x = 0; x < nl.getLength(); x++) {
            Element e = (Element) nl.item(x);
            String[] tuples = e.getFirstChild().getNodeValue().split(" ");
            for (int i = 0; i < tuples.length; i++) {
                String[] coord = tuples[i].split(",");
                for (int j = 0; j < coord.length; j++) {
                    int dot = coord[j].indexOf('.');
                    assertEquals( numdecimals, coord[j].substring(dot+1).length());
                }
            }
        }
    }
}
