/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import junit.framework.Test;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.DecimalTextField;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;

import com.google.common.collect.ImmutableMap;

public class GridSetNewPageTest extends GeoServerWicketTestSupport {

    /**
     * This is a NOT a read only test, but we save the per test set up cost anyways
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GridSetNewPageTest());
    }

    /**
     * Just a smoke test to make sure the page loads as expected
     */
    public void testPageLoad() {
        GridSetNewPage page = new GridSetNewPage(new PageParameters());

        tester.startPage(page);
        tester.assertRenderedPage(GridSetNewPage.class);
        // print(page, true, true);
        tester.assertComponent("gridSetForm", Form.class);
        tester.assertComponent("gridSetForm:feedback", FeedbackPanel.class);
        tester.assertComponent("gridSetForm:name", TextParamPanel.class);
        tester.assertComponent("gridSetForm:description", TextArea.class);
        tester.assertComponent("gridSetForm:crs", CRSPanel.class);
        tester.assertComponent("gridSetForm:bounds", EnvelopePanel.class);
        tester.assertComponent("gridSetForm:tileMatrixSetEditor", TileMatrixSetEditor.class);
        tester.assertComponent("gridSetForm:cancel", BookmarkablePageLink.class);
        tester.assertComponent("gridSetForm:save", AjaxSubmitLink.class);
        tester.assertComponent("gridSetForm:addZoomLevel", GeoServerAjaxFormLink.class);
    }

    public void testCreateFromTemplate() {

        PageParameters params = new PageParameters(ImmutableMap.of("template", "EPSG:4326"));
        GridSetNewPage page = new GridSetNewPage(params);

        tester.startPage(page);
        // print(page, true, true);

        tester.assertModelValue("gridSetForm:name:border:paramValue", "My_EPSG:4326");

        FormTester ft = tester.newFormTester("gridSetForm");
        ft.setValue("name:border:paramValue", "customWGS84");

        // add two zoom levels
        tester.executeAjaxEvent("gridSetForm:addZoomLevel", "onclick");
        tester.executeAjaxEvent("gridSetForm:addZoomLevel", "onclick");

        // submit
        tester.executeAjaxEvent("gridSetForm:save", "onclick");

        GWC mediator = GWC.get();
        GridSetBroker gridSetBroker = mediator.getGridSetBroker();
        assertTrue(gridSetBroker.getNames().toString(),
                gridSetBroker.getNames().contains("customWGS84"));

        GridSet check = gridSetBroker.get("EPSG:4326");
        GridSet created = gridSetBroker.get("customWGS84");
        assertEquals(check.getNumLevels() + 2, created.getNumLevels());
    }

    public void testCreateFromScratch() {
        GWC mediator = GWC.get();
        GridSetBroker gridSetBroker = mediator.getGridSetBroker();

        GridSetNewPage page = new GridSetNewPage(new PageParameters());

        tester.startPage(page);
        final String gridsetName = "fromScratch";

        FormTester ft = tester.newFormTester("gridSetForm");
        ft.setValue("name:border:paramValue", gridsetName);
        ft.setValue("description", "sample description");
        ft.setValue("crs:srs", "EPSG:3857");

        // print(page, true, true);
        tester.executeAjaxEvent("gridSetForm:crs:srs", "onblur");
        // print(page, true, true);

        Component computeBounds = tester
                .getComponentFromLastRenderedPage("gridSetForm:computeBounds");
        assertTrue(computeBounds.isEnabled());

        // hard to trigger an onclick event for a GeoServerAjaxSubmitLink, to invoking directly
        page.computeBounds();
        // print(page, true, true);

        {
            BoundingBox expected = gridSetBroker.get("EPSG:900913").getOriginalExtent();
            Double minx = ((DecimalTextField) tester
                    .getComponentFromLastRenderedPage("gridSetForm:bounds:minX")).getModelObject();
            Double miny = ((DecimalTextField) tester
                    .getComponentFromLastRenderedPage("gridSetForm:bounds:minY")).getModelObject();
            Double maxx = ((DecimalTextField) tester
                    .getComponentFromLastRenderedPage("gridSetForm:bounds:maxX")).getModelObject();
            Double maxy = ((DecimalTextField) tester
                    .getComponentFromLastRenderedPage("gridSetForm:bounds:maxY")).getModelObject();

            assertEquals(expected.getMinX(), minx, 1.0E-2);// cm resolution
            assertEquals(expected.getMinY(), miny, 1.0E-2);
            assertEquals(expected.getMaxX(), maxx, 1.0E-2);
            assertEquals(expected.getMaxY(), maxy, 1.0E-2);

            EnvelopePanel envPanel = (EnvelopePanel) tester
                    .getComponentFromLastRenderedPage("gridSetForm:bounds");
            assertNotNull(envPanel.getModelObject());

            ft.setValue("bounds:minX", "-1000000");
            ft.setValue("bounds:minY", "-1000000");
            ft.setValue("bounds:maxX", "1000000");
            ft.setValue("bounds:maxY", "1000000");
        }

        ft.setValue("tileWidth:border:paramValue", "512");
        tester.executeAjaxEvent("gridSetForm:tileWidth:border:paramValue", "onblur");

        ft.setValue("tileHeight:border:paramValue", "512");
        tester.executeAjaxEvent("gridSetForm:tileHeight:border:paramValue", "onblur");

        // add zoom levels
        final int numLevels = 6;
        for (int i = 0; i < numLevels; i++) {
            // tester.executeAjaxEvent("gridSetForm:addZoomLevel", "onclick");
            // tester.clickLink("gridSetForm:addZoomLevel", true);
            // can't get this event to get triggered?
            AjaxRequestTarget target = new AjaxRequestTarget(page);
            page.addZoomLevel(target);
        }

        // print(page, true, true);

        // submit
        Session.get().getFeedbackMessages().clear();
        tester.executeAjaxEvent("gridSetForm:save", "onclick");

        tester.assertNoErrorMessage();

        assertTrue(gridSetBroker.getNames().toString(),
                gridSetBroker.getNames().contains(gridsetName));

        GridSet created = gridSetBroker.get(gridsetName);
        assertEquals(numLevels, created.getNumLevels());
    }

}
