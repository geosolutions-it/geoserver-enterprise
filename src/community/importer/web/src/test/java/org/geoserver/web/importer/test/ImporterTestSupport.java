/*
 * Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: ImporterTestSupport.java 174 2012-01-23 15:11:17Z alessio $
 */
package org.geoserver.web.importer.test;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.importer.ImportProgressPage;


/**
 *
 * Adds some support functions to the testing process
 *
 * @author Luca Morandini lmorandini@ieee.org
 *
 */
public abstract class ImporterTestSupport extends GeoServerWicketTestSupport
{

    /**
     * Enumeration to store wicket paths
     */
    protected enum WicketPath
    {
        IMPORT_LINK("category:3:category.links:0:link"),
        IMPORT_SHAPEFILELINK("stores:0:storeLink"),
        IMPORT_POSTGISLINK("stores:1:storeLink"),
        IMPORT_GEOTIFFLINK("stores:2:storeLink"),
        IMPORT_FORM("form"),
        IMPORT_NEXT("next"),
        IMPORT_IMPORT("import"),
        IMPORT_WORKSPACE("generalParams:workspace"),
        IMPORT_NAME("generalParams:name"),
        IMPORT_DESCRIPTION("generalParams:description"),
        IMPORT_DIRECTORY("directory"),
        IMPORT_OUTDIRECTORY("outdirectory"),
        IMPORT_OUTCHOOSER("outchooser"),
        IMPORT_COPY("copy"),
        GEOTIFF_COMPRESSIONTYPE("compressiontype"),
        GEOTIFF_COMPRESSIONRATIO("compressionratio"),
        GEOTIFF_DEFAULTSRS("defaultsrs"),
        GEOTIFF_TILE("tile"),
        GEOTIFF_TILEWIDTH("tilewidth"),
        GEOTIFF_TILEHEIGHT("tileheight"),
        GEOTIFF_RETTILE("rettile"),
        GEOTIFF_OVERVIEW("overview"),
        GEOTIFF_NOVERVIEW("noverview"),
        GEOTIFF_DOWNSAMPLESTEP("downsamplestep"),
        GEOTIFF_SUBSAMPLEALGORITHM("subsamplealgorithm"),
        GEOTIFF_RETOVERVIEW("retoverview"),
        GEOTIFF_EXTOVERVIEW("extoverview"),
        SHAPEFILE_INDEX("index"),
        SHAPEFILE_MEMORY("memory");

        public String path;

        WicketPath(String path)
        {
            this.path = path;
        }
    }

    protected File dataRoot;

    /**
     * Print hierarchy current page's Wicket widgets
     */
    protected void printCurrentPage()
    {
        this.print(tester.getLastRenderedPage(), true, false);
    }

    /**
     * Checks whether a widget has been enabled
     *
     * @param form Wicket form
     * @param path Wicket path of widget
     */
    protected void assertEnabled(Form form, String path)
    {
        assertTrue(form.get(path).isEnabled());

    }

    /**
     * Checks whether a widget has been disabled
     *
     * @param form Wicket form
     * @param path Wicket path of widget
     */
    protected void assertDisabled(Form form, String path)
    {
        assertFalse(form.get(path).isEnabled());
    }

    /**
     * Checks that a style exists and a layer has it as default one
     *
     * @param layerName
     *            checked layer
     * @param styleName
     *            checked style
     */
    protected void assertStyle(String layerName, String styleName)
    {

        assertNotNull(this.getGeoServer().getCatalog().getLayerByName(layerName));
        assertNotNull(this.getGeoServer().getCatalog().getStyleByName(styleName));
        if (styleName.equals(this.getGeoServer().getCatalog().getLayerByName(layerName).getDefaultStyle().getName()))
        {
            assertTrue(true);
        }
        else
        {
            assertTrue(false);
        }
    }

    /**
     * Checks that a layer exists in the catalog
     *
     * @param layerName
     *            layer of checked layer
     */
    protected void assertLayer(String layerName)
    {
        assertNotNull(this.getGeoServer().getCatalog().getLayerByName(layerName));
    }

    /**
     * Checks that a layer DOES NOT exist in the catalog
     *
     * @param layerName
     *            layer of checked layer
     */
    protected void assertNoLayer(String layerName)
    {
        if (this.getGeoServer().getCatalog().getLayerByName(layerName) == null)
        {
            assertTrue(true);
        }
        else
        {
            assertTrue(false);
        }
    }

    /**
     * Checks that a given page has been rendered
     *
     * @param clazz
     *            Class of page that is supposed to be rendered
     */
    protected void assertPage(Class<? extends GeoServerBasePage> clazz)
    {
        if (tester.getLastRenderedPage().getClass() == clazz)
        {
            assertTrue(true);
        }
        else
        {
            assertTrue(false);
        }
    }

    /**
     * Checks that no exception page has been shown
     */
    protected void assertNoException()
    {
        if (tester.getLastRenderedPage().getClass() == org.geoserver.web.GeoServerErrorPage.class)
        {
            fail("We got to the error page, why??");
        }
    }

    /**
     * Checks that am exception page has been shown
     */
    protected void assertException()
    {
        if (tester.getLastRenderedPage().getClass() != org.geoserver.web.GeoServerErrorPage.class)
        {
            fail("We should have got to the errore page, but we did not");
        }
    }

    /**
     * Checks that a given message has been shown
     *
     * @param key
     *            Key of the message in the properties file
     */
    protected void assertMessage(String key)
    {
        Component page = tester.getLastRenderedPage();
        tester.assertContains(page.getLocalizer().getString(key, page));
        // tester.assertErrorMessages(new String[] {page.getLocalizer().getString(key, page)});
    }

    /**
     * Populates a mock data directory with standard data
     */
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception
    {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWellKnownCoverageTypes();
        this.dataRoot = dataDirectory.getDataDirectoryRoot();
    }

    /**
     * Executes login and goes to the home page
     *
     * @see org.geoserver.test.OneTimeSetupTest#setUpInternal()
     */
    @Override
    protected void setUpInternal() throws Exception
    {
        login();
        tester.startPage(new org.geoserver.web.GeoServerHomePage());
    }

//    /**
//     * Pauses to allow import operations in the backgorund to complete
//     */
//    protected void pause()
//    {
//        try
//        {
//            Thread.sleep(10000);
//        }
//        catch (InterruptedException e)
//        {
//            LOGGER.log(Level.FINER, e.getMessage(), e);
//        }
//    }
    
    protected void waitOnImportProgressPage() {
        do {
            // manually run the ajax timer behavior attached to the "info" markup container
            try {
                List<IBehavior> behaviors = tester.getComponentFromLastRenderedPage("info").getBehaviors();
                for (IBehavior behavior: behaviors) {
                    if(behavior instanceof AbstractAjaxTimerBehavior) {
                        AbstractAjaxTimerBehavior tb = (AbstractAjaxTimerBehavior) behavior;
                        CharSequence url = tb.getCallbackUrl(false);
                        WebRequestCycle cycle = tester.setupRequestAndResponse(true);
                        tester.getServletRequest().setRequestToRedirectString(url.toString());
                        tester.processRequestCycle(cycle);
                    }
                }
            } catch(WicketRuntimeException e) {
                // this happens a lot if the page switched while we where looping
            }
        } while(tester.getLastRenderedPage().getClass().equals(ImportProgressPage.class));

    }

    /**
     * Checks whether text is NOT present in the rendered page
     * @param text
     */
    protected void assertNotContains(String text)
    {
        if (tester.ifContains("^" + text).wasFailed() == true)
        {
            assertTrue(true);
        }
        else
        {
            assertTrue(false);
        }
    }

    protected void executeClickEvent(String id)
    {
        this.tester.executeAjaxEvent(WicketPath.IMPORT_FORM.path + ":" + id, "onclick");
    }

    protected void executeChangeEvent(String id)
    {
        this.tester.executeAjaxEvent(WicketPath.IMPORT_FORM.path + ":" + id, "onchange");
    }
}
