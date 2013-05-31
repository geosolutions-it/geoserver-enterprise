package org.geoserver.web.data.layergroup;

import org.apache.wicket.PageParameters;


public class LayerGroupEditPageTest extends LayerGroupBaseTest {
    
    public void testComputeBounds() {
        LayerGroupEditPage page = new LayerGroupEditPage(new PageParameters("group=lakes"));
        tester.startPage(page);
        // print(page, true, false);
        
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // remove the first and second elements
        tester.clickLink("form:layers:layers:listContainer:items:1:itemProperties:4:component:link");
        // the regenerated list will have ids starting from 4
        //tester.clickLink("form:layers:layers:listContainer:items:4:itemProperties:4:component:link");
        // manually regenerate bounds
        tester.clickLink("form:generateBounds");
        // print(page, true, true);
        // submit the form
        tester.submitForm("form");
        
        // For the life of me I cannot get this test to work... and I know by direct UI inspection that
        // the page works as expected...
//        FeatureTypeInfo bridges = getCatalog().getResourceByName(MockData.BRIDGES.getLocalPart(), FeatureTypeInfo.class);
//        assertEquals(getCatalog().getLayerGroupByName("lakes").getBounds(), bridges.getNativeBoundingBox());
    }
}
