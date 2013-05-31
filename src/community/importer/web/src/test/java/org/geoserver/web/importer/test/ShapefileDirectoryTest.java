/*
 * Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: ShapefileDirectoryTest.java 174 2012-01-23 15:11:17Z alessio $
 */
package org.geoserver.web.importer.test;


import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.importer.RasterChooserPage;
import org.geoserver.web.importer.VectorChooserPage;


/**
 *
 * Checks the correctness of the Shapefile directory import
 *
 * @author Luca Morandini lmorandini@ieee.org
 *
 */
public class ShapefileDirectoryTest extends ImporterTestSupport
{

    private void executeWidgetUpdate()
    {
        this.executeClickEvent(WicketPath.IMPORT_COPY.path);
    }

    public void testImporterLinkInHomePage()
    {
        tester.assertBookmarkablePageLink(WicketPath.IMPORT_LINK.path,
            org.geoserver.web.importer.StoreChooserPage.class, "");
    }

    public void testLocaleEN()
    {
        Session.get().setLocale(Locale.ENGLISH);
        tester.clickLink(WicketPath.IMPORT_LINK.path);        
        this.assertPage(org.geoserver.web.importer.StoreChooserPage.class);
        tester.assertContains("Import data");
    }

    public void testLocaleIT()
    {
        Session.get().setLocale(Locale.ITALY);
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        this.assertPage(org.geoserver.web.importer.StoreChooserPage.class);
        tester.assertContains("Importazione dei dati");
    }

    public void testImporterStores()
    {
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        this.assertPage(org.geoserver.web.importer.StoreChooserPage.class);
        this.assertMessage("StoreChooserPage.directory_description");
        this.assertMessage("StoreChooserPage.postgis_description");
        this.assertMessage("StoreChooserPage.geotiff_description");
    }

    public void testImportShapefileInvalidDirectory()
    {
        // Go to the page for importing a directory containing shapefiles
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);

        // Fills the form with an invalid directory
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_NAME.path, "testshp");
        form.setValue(WicketPath.IMPORT_DESCRIPTION.path, "Shapefile test");
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, "/xxx");
        form.submit(WicketPath.IMPORT_NEXT.path);
        this.assertNoException();
        this.assertMessage("ImporterSecuredPage.invalidPath");
    }


    public void testImportShapefileNoDataDirectory()
    {
        // Go to the page for importing a directory containing shapefiles
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);

        // Fills the form with a directory containing no Shapefiles
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_NAME.path, "testshp");
        form.setValue(WicketPath.IMPORT_DESCRIPTION.path, "Shapefile test");
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() + "/../../src/test/data/geotiff");
        form.submit(WicketPath.IMPORT_NEXT.path);
        this.assertNoException();
        this.assertMessage("ImporterSecuredPage.noData");
    }

    public void testImportShapefileCorruptedDirectory()
    {
        // Go to the page for importing a directory containing corrupted shapefiles
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);

        // Fills the form with a directory containing no Shapefiles
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_NAME.path, "testshp");
        form.setValue(WicketPath.IMPORT_DESCRIPTION.path, "Shapefile test");
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() + "/../../src/test/data/corruptedshp");
        form.submit(WicketPath.IMPORT_NEXT.path);
        this.assertNoException();
    }

    public void testImportShapefileDirectory()
    {
        // Go to the page for importing a directory containing shapefiles
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);

        // Fills the form with a directory containing test data
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_NAME.path, "testshp");
        form.setValue(WicketPath.IMPORT_DESCRIPTION.path, "Shapefile test");
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() + "/../../src/test/data/shp");
        form.submit(WicketPath.IMPORT_NEXT.path);

        // Imports all the shapefiles in the list
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(VectorChooserPage.class);

        tester.assertContains("BridgesTest");
        tester.assertContains("PondsTest");
        tester.assertContains("StreamsTest");
        tester.assertContains("BridgesTestNoPRJ");

        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();
        this.assertNoException();
        tester.assertNoErrorMessage();

        // Checks layers were actually imported
        this.assertLayer("BridgesTest");
        this.assertLayer("PondsTest");
        this.assertLayer("StreamsTest");
        this.assertNoLayer("BridgesTestNoPRJ");

        // Checks layers' styles
        this.assertStyle("BridgesTest", "gs_BridgesTest");
        this.assertStyle("PondsTest", "gs_PondsTest");
        this.assertStyle("StreamsTest", "gs_StreamsTest");
    }

    public void testImportShapefileFile()
    {
        // Go to the page for importing a directory containing shapefiles
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);
        

        // Fills the form with a directory containing test data
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_NAME.path, "testshp");
        form.setValue(WicketPath.IMPORT_DESCRIPTION.path, "Shapefile test");
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() + "/../../src/test/data/shp/PondsTest.shp");
        form.submit(WicketPath.IMPORT_NEXT.path);

        // Imports all the shapefiles in the list
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(VectorChooserPage.class);
        
        print(tester.getLastRenderedPage(), true, true);

        tester.assertContains("PondsTest");

        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();
        this.assertNoException();
        tester.assertNoErrorMessage();

        // Checks layers were actually imported
        this.assertLayer("PondsTest");

        // Checks layers' styles
        this.assertStyle("PondsTest", "gs_PondsTest");
    }

    public void testImportShapefileDirectoryOptions()
    {
        // Go to the page for importing a directory containing shapefiles
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);

        // Fills the form with a directory containing test data
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_NAME.path, "testshp");
        form.setValue(WicketPath.IMPORT_DESCRIPTION.path, "Shapefile test");
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() + "/../../src/test/data/shp");

        // Sets options
        form.setValue(WicketPath.SHAPEFILE_INDEX.path, true);
        form.setValue(WicketPath.SHAPEFILE_MEMORY.path, true);
        form.submit(WicketPath.IMPORT_NEXT.path);

        // Imports all the shapefiles in the list
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(VectorChooserPage.class);

        tester.assertContains("BridgesTest");
        tester.assertContains("PondsTest");
        tester.assertContains("StreamsTest");
        tester.assertContains("BridgesTestNoPRJ");

        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();
        this.assertNoException();
        tester.assertNoErrorMessage();

        // Checks layers were actually imported
        this.assertLayer("BridgesTest");
        this.assertLayer("PondsTest");
        this.assertLayer("StreamsTest");
        this.assertNoLayer("BridgesTestNoPRJ");
    }

    public void testPartialImportShapefileDirectory()
    {
        // Go to the page for importing a directory containing shapefiles
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);

        // Fills the form with a directory containing test data
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_NAME.path, "testshp");
        form.setValue(WicketPath.IMPORT_DESCRIPTION.path, "Shapefile test");
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() + "/../../src/test/data/shp");
        form.submit(WicketPath.IMPORT_NEXT.path);
        waitOnImportProgressPage();

        // Imports only some shapefiles in the list
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(VectorChooserPage.class);

        tester.assertContains("BridgesTest");
        tester.assertContains("BridgesTestNoPRJ");
        tester.assertContains("PondsTest");
        tester.assertContains("StreamsTest");

        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", true);
        form.setValue("layerChooser:listContainer:items:2:selectItemContainer:selectItem", false);
        form.setValue("layerChooser:listContainer:items:3:selectItemContainer:selectItem", true);
        form.setValue("layerChooser:listContainer:items:4:selectItemContainer:selectItem", false);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();
        this.assertNoException();
        tester.assertNoErrorMessage();

        // Checks layers were actually imported
        this.assertLayer("BridgesTest");
        this.assertNoLayer("BridgesTestNoPRJ");
        this.assertLayer("PondsTest");
        this.assertNoLayer("StreamsTest");

        // Checks layers' styles
        this.assertStyle("BridgesTest", "gs_BridgesTest");
        this.assertStyle("PondsTest", "gs_PondsTest");
    }

    public void testExistingVectorStore()
    {

        // Imports data
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);

        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_NAME.path, "testshp");
        form.setValue(WicketPath.IMPORT_DESCRIPTION.path, "Shapefile test");
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() + "/../../src/test/data/shp");
        form.submit(WicketPath.IMPORT_NEXT.path);
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(VectorChooserPage.class);
        tester.assertContains("BridgesTest");
        tester.assertContains("PondsTest");
        tester.assertContains("StreamsTest");
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();

        // Tries to re-import the same data

        // Go to the page for importing
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);

        // Fills the form with a directory alredy used as datastore
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_NAME.path, "testshp");
        form.setValue(WicketPath.IMPORT_DESCRIPTION.path, "Shapefile test");
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() + "/../../src/test/data/shp");
        form.submit(WicketPath.IMPORT_NEXT.path);
        waitOnImportProgressPage();

        // Checks the error message
        // NOTE: this must change when property DirectoryPage.duplicateStore changes
        tester.assertContains("A store named");
    }

    public void testExistingRasterDatastore()
    {
        // Creates some GeoTIFF stores
        // Imports a directory
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);

        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() + "/../../src/test/data/geotiff");
        form.submit(WicketPath.IMPORT_NEXT.path);
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(RasterChooserPage.class);
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();

        // Tries to import a Shapefile directory with the same "rotated.tiff" name
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_NAME.path, "rotated.tiff");
        form.setValue(WicketPath.IMPORT_DESCRIPTION.path, "Shapefile test");
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() + "/../../src/test/data/shp");
        form.submit(WicketPath.IMPORT_NEXT.path);
        waitOnImportProgressPage();

        // Checks the GeoTIFF has not been imported
        this.assertNoException();
        tester.assertContains("A store named rotated.tiff");
    }

    public void testImportShapefileCopyGUI()
    {
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);

        // Checks that, by default, the output directory cannot be set
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        this.assertDisabled(form.getForm(), WicketPath.IMPORT_OUTDIRECTORY.path);

        // Checks that, the output directory is enabled when "copy" is set
        form.setValue(WicketPath.IMPORT_COPY.path, true);
        this.executeWidgetUpdate();
        this.assertEnabled(form.getForm(), WicketPath.IMPORT_OUTDIRECTORY.path);

    }

    /* FIXME: the test case does not proceed past the ImportProgress page (see #50)
    public void testImportShapefileCopy() {
        // Go to the page for importing a directory containing shapefiles
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);

        // Fills the form with a directory containing test data and specifies output directory
        FormTester form= tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_NAME.path, "testshp");
        form.setValue(WicketPath.IMPORT_DESCRIPTION.path, "Shapefile test");
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() + "/../../src/test/data/shp");
        form.setValue(WicketPath.IMPORT_COPY.path, true);
        this.executeWidgetUpdate();
        form.setValue(WicketPath.IMPORT_OUTDIRECTORY.path, "/tmp");
        this.executeClickEvent(WicketPath.IMPORT_NEXT.path);
        form.submit(WicketPath.IMPORT_NEXT.path);

        // Imports all the shapefiles in the list
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(VectorChooserPage.class);

        tester.assertContains("BridgesTest");
        tester.assertContains("PondsTest");
        tester.assertContains("StreamsTest");
        tester.assertContains("BridgesTestNoPRJ");

        form= tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        this.pause();
        this.assertNoException();
        tester.assertNoErrorMessage();

        // Checks layers were actually imported
        this.assertLayer("BridgesTest");
        this.assertLayer("PondsTest");
        this.assertLayer("StreamsTest");
        this.assertNoLayer("BridgesTestNoPRJ");
    }
     */
}
