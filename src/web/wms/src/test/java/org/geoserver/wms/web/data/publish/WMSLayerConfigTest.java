package org.geoserver.wms.web.data.publish;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.web.publish.StylesModel;
import org.geoserver.wms.web.publish.WMSLayerConfig;

@SuppressWarnings("serial")
public class WMSLayerConfigTest extends GeoServerWicketTestSupport {
    
    public void testExisting() {
        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        FormTestPage page = new FormTestPage(new ComponentBuilder() {

            public Component buildComponent(String id) {
                return new WMSLayerConfig(id, new Model(layer));
            }
        }
        );
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:panel:styles:defaultStyle", DropDownChoice.class);
        
        // check selecting something else works
        StyleInfo target = ((List<StyleInfo>) new StylesModel().getObject()).get(0); 
        FormTester ft = tester.newFormTester("form");
        ft.select("panel:styles:defaultStyle", 0);
        ft.submit();
        tester.assertModelValue("form:panel:styles:defaultStyle", target);
    }
    
    public void testNew() {
        final LayerInfo layer = getCatalog().getFactory().createLayer();
        layer.setResource(getCatalog().getFactory().createFeatureType());
        FormTestPage page = new FormTestPage(new ComponentBuilder() {

            public Component buildComponent(String id) {
                return new WMSLayerConfig(id, new Model(layer));
            }
        }
        );
        Component layerConfig = page.get("form:panel:styles:defaultStyle");
        
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:panel:styles:defaultStyle", DropDownChoice.class);
        
        // check submitting like this will create errors, there is no selection
        tester.submitForm("form");
        
        assertTrue(page.getSession().getFeedbackMessages().hasErrorMessageFor(layerConfig));
        
        // now set something and check there are no messages this time
        page.getSession().getFeedbackMessages().clear();
        FormTester ft = tester.newFormTester("form");
        ft.select("panel:styles:defaultStyle", 0);
        ft.submit();
        assertFalse(page.getSession().getFeedbackMessages().hasErrorMessageFor(layerConfig));
    }

    public void testLegendGraphicURL() throws Exception {
        final LayerInfo layer = getCatalog().getLayerByName(MockData.PONDS.getLocalPart());
        FormTestPage page = new FormTestPage(new ComponentBuilder() {

            public Component buildComponent(String id) {
                return new WMSLayerConfig(id, new Model(layer));
            }
        }
        );
        tester.startPage(page);
        tester.assertRenderedPage(FormTestPage.class);
        tester.debugComponentTrees();

        Image img = (Image) 
            tester.getComponentFromLastRenderedPage("form:panel:styles:defaultStyleLegendGraphic");
        assertNotNull(img);
        assertEquals(1, img.getBehaviors().size());
        assertTrue(img.getBehaviors().get(0) instanceof AttributeModifier);

        AttributeModifier mod = (AttributeModifier) img.getBehaviors().get(0);
        assertTrue(mod.toString().contains("../cite/wms?REQUEST=GetLegendGraphic"));

    }
}
