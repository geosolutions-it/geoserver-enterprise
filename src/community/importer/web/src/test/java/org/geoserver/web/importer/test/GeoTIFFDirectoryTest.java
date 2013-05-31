/*
 * Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: GeoTIFFDirectoryTest.java 174 2012-01-23 15:11:17Z alessio $
 */
package org.geoserver.web.importer.test;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.importer.ImportUtilities;
import org.geoserver.web.importer.RasterChooserPage;


/**
 *
 * Checks the correctness of the GeoTIFF directory import
 *
 * @author Luca Morandini lmorandini@ieee.org
 *
 */
public class GeoTIFFDirectoryTest extends ImporterTestSupport
{

    private void executeWidgetUpdate()
    {
        this.executeClickEvent(WicketPath.IMPORT_COPY.path);
        this.executeClickEvent(WicketPath.GEOTIFF_TILE.path);
        this.executeClickEvent(WicketPath.GEOTIFF_OVERVIEW.path);
        this.executeChangeEvent(WicketPath.GEOTIFF_COMPRESSIONTYPE.path);
    }

    private void checkFormStateWhenCopyIsChecked(Form form)
    {
        this.assertEnabled(form, WicketPath.IMPORT_OUTDIRECTORY.path);
        this.assertEnabled(form, WicketPath.IMPORT_OUTCHOOSER.path);
    }

    private void checkFormStateWhenCopyIsUnchecked(Form form)
    {
        this.assertDisabled(form, WicketPath.IMPORT_OUTDIRECTORY.path);
        this.assertDisabled(form, WicketPath.IMPORT_OUTCHOOSER.path);

        this.assertEnabled(form, WicketPath.GEOTIFF_COMPRESSIONTYPE.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_COMPRESSIONRATIO.path);

        this.assertEnabled(form, WicketPath.GEOTIFF_TILE.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_TILEWIDTH.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_TILEHEIGHT.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_RETTILE.path);

        this.assertEnabled(form, WicketPath.GEOTIFF_OVERVIEW.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_DOWNSAMPLESTEP.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_SUBSAMPLEALGORITHM.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_NOVERVIEW.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_EXTOVERVIEW.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_RETOVERVIEW.path);

        this.assertEnabled(form, WicketPath.GEOTIFF_DEFAULTSRS.path);
    }

    private void checkFormStateWhenTileIsChecked(Form form)
    {
        this.assertEnabled(form, WicketPath.GEOTIFF_TILE.path);
        this.assertEnabled(form, WicketPath.GEOTIFF_TILEWIDTH.path);
        this.assertEnabled(form, WicketPath.GEOTIFF_TILEHEIGHT.path);
        this.assertEnabled(form, WicketPath.GEOTIFF_RETTILE.path);
    }

    private void checkFormStateWhenTileIsUnchecked(Form form)
    {
        this.assertEnabled(form, WicketPath.GEOTIFF_TILE.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_TILEWIDTH.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_TILEHEIGHT.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_RETTILE.path);
    }

    private void checkFormStateWhenOverviewIsChecked(Form form)
    {
        this.assertEnabled(form, WicketPath.GEOTIFF_OVERVIEW.path);
        this.assertEnabled(form, WicketPath.GEOTIFF_DOWNSAMPLESTEP.path);
        this.assertEnabled(form, WicketPath.GEOTIFF_SUBSAMPLEALGORITHM.path);
        this.assertEnabled(form, WicketPath.GEOTIFF_NOVERVIEW.path);
        this.assertEnabled(form, WicketPath.GEOTIFF_RETOVERVIEW.path);
        this.assertEnabled(form, WicketPath.GEOTIFF_EXTOVERVIEW.path);
    }

    private void checkFormStateWhenOverviewIsUnchecked(Form form)
    {
        this.assertEnabled(form, WicketPath.GEOTIFF_OVERVIEW.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_DOWNSAMPLESTEP.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_SUBSAMPLEALGORITHM.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_NOVERVIEW.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_RETOVERVIEW.path);
        this.assertDisabled(form, WicketPath.GEOTIFF_EXTOVERVIEW.path);
    }

    private void checkCompressionTypeIsNotNONE(Form form)
    {
        this.assertEnabled(form, WicketPath.GEOTIFF_COMPRESSIONRATIO.path);
    }

    private void checkCompressionTypeIsNONE(Form form)
    {
        this.assertDisabled(form, WicketPath.GEOTIFF_COMPRESSIONRATIO.path);
    }

    public void testComputeMaxOverviews()
    {
        assertEquals(3, ImportUtilities.computeMaxOverviews(100, 100, 10, 10, 2));
        assertEquals(2, ImportUtilities.computeMaxOverviews(100, 100, 10, 10, 4));
        assertEquals(0, ImportUtilities.computeMaxOverviews(100, 100, 100, 100, 2));
        assertEquals(0, ImportUtilities.computeMaxOverviews(100, 100, 200, 200, 2));
        assertEquals(0, ImportUtilities.computeMaxOverviews(100, 100, 100, 100, 1));

        assertEquals(3, ImportUtilities.computeMaxOverviews(100, 200, 10, 20, 2));
        assertEquals(2, ImportUtilities.computeMaxOverviews(100, 200, 10, 20, 4));
        assertEquals(0, ImportUtilities.computeMaxOverviews(100, 200, 100, 200, 2));
        assertEquals(0, ImportUtilities.computeMaxOverviews(100, 200, 200, 300, 2));
        assertEquals(0, ImportUtilities.computeMaxOverviews(100, 200, 100, 200, 1));

        assertEquals(4, ImportUtilities.computeMaxOverviews(100, 400, 10, 30, 2));
        assertEquals(4, ImportUtilities.computeMaxOverviews(400, 100, 30, 10, 2));

        assertEquals(1, ImportUtilities.computeMaxOverviews(400, 100, 40, 10, 12));

        assertEquals(0, ImportUtilities.computeMaxOverviews(-1, 400, 10, 30, 2));
        assertEquals(0, ImportUtilities.computeMaxOverviews(400, 100, 40, -1, 2));
        assertEquals(0, ImportUtilities.computeMaxOverviews(300, 0, 10, 20, 2));
        assertEquals(0, ImportUtilities.computeMaxOverviews(400, 100, 40, 0, 2));

    }

    public void testGeoTIFFGUIWidgetsEnabling()
    {
        // Tries to import a GeoTIFF copying data in the GEOSERVER_DATA_DIR
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);

        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() +
            "/../../src/test/data/geotiff/rotated.tiff");
        form.setValue(WicketPath.IMPORT_OUTDIRECTORY.path, "/tmp");

        // Checks initial state of form
        this.checkFormStateWhenCopyIsUnchecked(form.getForm());

        // Checks enabling of options when "copy" is checked
        form.setValue(WicketPath.IMPORT_COPY.path, true);
        this.executeWidgetUpdate();
        this.checkFormStateWhenCopyIsChecked(form.getForm());
        this.checkFormStateWhenTileIsUnchecked(form.getForm());
        this.checkFormStateWhenOverviewIsUnchecked(form.getForm());

        // Checks enabling of options when "copy" and "tile" are checked
        form.setValue(WicketPath.GEOTIFF_TILE.path, true);
        this.executeWidgetUpdate();
        this.checkFormStateWhenCopyIsChecked(form.getForm());
        this.checkFormStateWhenTileIsChecked(form.getForm());
        this.checkFormStateWhenOverviewIsUnchecked(form.getForm());

        // Checks enabling of options when "copy", "tile" and "overview" are checked
        form.setValue(WicketPath.GEOTIFF_OVERVIEW.path, true);
        this.executeWidgetUpdate();
        this.checkFormStateWhenCopyIsChecked(form.getForm());
        this.checkFormStateWhenTileIsChecked(form.getForm());
        this.checkFormStateWhenOverviewIsChecked(form.getForm());

        // Checks enabling of options when "overview" is un-checked
        form.setValue(WicketPath.GEOTIFF_OVERVIEW.path, false);
        this.executeWidgetUpdate();
        this.checkFormStateWhenCopyIsChecked(form.getForm());
        this.checkFormStateWhenTileIsChecked(form.getForm());
        this.checkFormStateWhenOverviewIsUnchecked(form.getForm());

        // Checks enabling of options when "tile" is un-checked
        form.setValue(WicketPath.GEOTIFF_TILE.path, false);
        this.executeWidgetUpdate();
        this.checkFormStateWhenCopyIsChecked(form.getForm());
        this.checkFormStateWhenTileIsUnchecked(form.getForm());
        this.checkFormStateWhenOverviewIsUnchecked(form.getForm());

        // Checks enabling of options when "copy" is un-checked
        form.setValue(WicketPath.IMPORT_COPY.path, false);
        this.executeWidgetUpdate();
        this.checkFormStateWhenCopyIsUnchecked(form.getForm());

        // Checks enabling of compression ratio
        this.checkCompressionTypeIsNONE(form.getForm());
        form.select(WicketPath.GEOTIFF_COMPRESSIONTYPE.path, 1);
        this.executeWidgetUpdate();
        this.checkCompressionTypeIsNotNONE(form.getForm());
        form.select(WicketPath.GEOTIFF_COMPRESSIONTYPE.path, 0);
        this.executeWidgetUpdate();
        this.checkCompressionTypeIsNONE(form.getForm());
        form.select(WicketPath.GEOTIFF_COMPRESSIONTYPE.path, 1);
        this.executeWidgetUpdate();
        this.checkCompressionTypeIsNotNONE(form.getForm());
    }

    public void testSingleGeoTIFFImport() {
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/geotiff/rotated.tiff");
        form.submit(WicketPath.IMPORT_NEXT.path);

        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(RasterChooserPage.class);
        this.assertNotContains("corrupted");
        tester.assertContains("rotated");
        this.assertNotContains("tazbm");

        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();

        this.assertNoException();
        tester.assertNoErrorMessage();

        tester.assertContains("Successfully imported 1");
        this.assertLayer("rotated");
        this.assertNoLayer("tazbm");
        this.assertNoLayer("corrupted");
    }

    public void testImportGeoTIFFInvalidDirectory()
    {
        // Goes to the page for importing a directory containing GeoTIFFs
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);

        // Fills the form with an invalid directory
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, "/xxx");
        form.submit(WicketPath.IMPORT_NEXT.path);
        this.assertNoException();
        this.assertMessage("ImporterSecuredPage.invalidPath");
    }

    /*
    public void testImportGeoTIFFInvalidOutputDirectory() {
        // Goes to the page for importing a directory containing GeoTIFFs
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);

        // Fills the form with an invalid directory
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/geotiff");
        form.setValue(WicketPath.IMPORT_COPY.path, true);
        // this.executeWidgetUpdate();
        form.setValue(WicketPath.GEOTIFF_TILE.path, false);
        form.setValue(WicketPath.GEOTIFF_OVERVIEW.path, false);
        // this.executeWidgetUpdate();
        form.setValue(WicketPath.IMPORT_OUTDIRECTORY.path, "/xxx");
        print(tester.getLastRenderedPage(), true, true, true);
        form.submitLink(WicketPath.IMPORT_NEXT.path, false);
        System.out.println(tester.getLastRenderedPage());
        this.assertNoException();
        this.assertMessage("ImporterSecuredPage.invalidPath");
    }
     
    public void testImportGeoTIFFNoDataDirectory()
    {
        // Go to the page for importing a directory containing GeoTIFFs
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);

        // Fills the form with a directory containing no Shapefiles
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath() +
            "/../../src/test/data/shp");
        form.submit(WicketPath.IMPORT_NEXT.path);
        this.assertNoException();
        this.assertMessage("ImporterSecuredPage.noData");
    }

    
    public void testImportCorruptedGeoTIFFDirectory() {
        // Go to the page for importing a directory containing corrupted GeoTIFFs
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);

        // Fills the form with a directory containing corrupted test data
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/corruptedgeotiff");
        form.submit(WicketPath.IMPORT_NEXT.path);
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(RasterChooserPage.class);
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();

        this.assertNoException();
        tester.assertNoErrorMessage();
        tester.assertContains("Successfully imported 0");
    }
    
    public void testImportGeoTIFFDirectory() {
        // Goes to the page for importing a directory containing GeoTIFFs
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);

        // Fills the form with a directory containing test data
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/geotiff");
        form.submit(WicketPath.IMPORT_NEXT.path);
        tester.assertNoErrorMessage();

        tester.assertContains("corrupted");
        tester.assertContains("MOD021KM.01.20060425083931000.000");
        tester.assertContains("MOD021KM.01.20060426091905000.000");
        tester.assertContains("MOD021KM.01.20070129084157000.000");
        tester.assertContains("rotated");
        tester.assertContains("tazbm");

        // Imports all the layers in the list
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(RasterChooserPage.class);
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();
        this.assertNoException();
        tester.assertNoErrorMessage();
        tester.assertContains("Successfully imported 5");

        // Checks correct layers were actually imported
        this.assertLayer("MOD021KM.01.20060425083931000.000");
        this.assertLayer("MOD021KM.01.20060426091905000.000");
        this.assertLayer("MOD021KM.01.20070129084157000.000");
        this.assertLayer("rotated");
        this.assertLayer("tazbm");

        // Checks invlaid layers were not imported
        this.assertNoLayer("corrupted");
    }
    
    public void testPartialImportGeoTIFFDirectory() {
        // Goes to the page for importing a directory containing GeoTIFFs
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);

        // Fills the form with a directory containing test data
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/geotiff");
        form.submit(WicketPath.IMPORT_NEXT.path);

        // Imports some of the layers in the list
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(RasterChooserPage.class);

        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", false);
        form.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", false);
        form.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", false);
        form.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", false);
        form.setValue("layerChooser:listContainer:items:2:selectItemContainer:selectItem", true);
        form.setValue("layerChooser:listContainer:items:3:selectItemContainer:selectItem", false);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();
        this.assertNoException();
        tester.assertNoErrorMessage();
        tester.assertContains("Successfully imported 1 layers");

        // Checks some layers were imported
        this.assertNoLayer("corrupted");
        this.assertNoLayer("MOD021KM.01.20060425083931000.000");
        this.assertNoLayer("MOD021KM.01.20060426091905000.000");
        this.assertNoLayer("MOD021KM.01.20070129084157000.000");
        this.assertLayer("rotated");
        this.assertNoLayer("tazbm");
    }

    public void testParallelImportGeoTIFFDirectory() {
        // Imports second image
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/geotiff");
        form.submit(WicketPath.IMPORT_NEXT.path);
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(RasterChooserPage.class);
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", false);
        form.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", false);
        form.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", false);
        form.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", false);
        form.setValue("layerChooser:listContainer:items:2:selectItemContainer:selectItem", true);
        form.setValue("layerChooser:listContainer:items:3:selectItemContainer:selectItem", false);
        form.submit(WicketPath.IMPORT_IMPORT.path);

        // Imports third image
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);
        FormTester form2 = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form2.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/geotiff");
        form2.submit(WicketPath.IMPORT_NEXT.path);
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(RasterChooserPage.class);
        form2 = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form2.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", false);
        form2.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", false);
        form2.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", false);
        form2.setValue("layerChooser:listContainer:items:1:selectItemContainer:selectItem", false);
        form2.setValue("layerChooser:listContainer:items:2:selectItemContainer:selectItem", false);
        form2.setValue("layerChooser:listContainer:items:3:selectItemContainer:selectItem",true);
        form2.submit(WicketPath.IMPORT_IMPORT.path);
        this.assertNoException();
        tester.assertNoErrorMessage();
        tester.assertContains("Successfully imported 2");

        waitOnImportProgressPage();
        this.assertNoException();
        tester.assertNoErrorMessage();
        tester.assertContains("Successfully imported 2 layers");

        // Checks some layers were imported
        this.assertNoLayer("corrupted");
        this.assertNoLayer("MOD021KM.01.20060425083931000.000");
        this.assertNoLayer("MOD021KM.01.20060426091905000.000");
        this.assertNoLayer("MOD021KM.01.20070129084157000.000");
        this.assertLayer("rotated");
        this.assertLayer("tazbm");
    }


    public void testExistingRasterStore() throws InterruptedException {
        // Imports a directory
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/geotiff");
        form.submit(WicketPath.IMPORT_NEXT.path);
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(RasterChooserPage.class);
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();

        this.assertLayer("rotated");
        this.assertLayer("tazbm");
        this.assertLayer("MOD021KM.01.20060425083931000.000");
        this.assertLayer("MOD021KM.01.20060426091905000.000");
        this.assertLayer("MOD021KM.01.20070129084157000.000");
        this.assertNoLayer("corrupted");
        int nStores = this.getCatalog().getStores(CoverageStoreInfo.class).size();

        // Tries to re-import the same directory (it should re-uses the stores withour duplicating
        // them)
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/geotiff");
        form.submit(WicketPath.IMPORT_NEXT.path);
        this.assertNoException();
        tester.assertNoErrorMessage();
        assertPage(RasterChooserPage.class);
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();
        this.assertNoException();
        tester.assertNoErrorMessage();
        tester.assertContains("Successfully imported 5");

        this.assertLayer("rotated");
        this.assertLayer("tazbm");
        this.assertLayer("MOD021KM.01.20060425083931000.000");
        this.assertLayer("MOD021KM.01.20060426091905000.000");
        this.assertLayer("MOD021KM.01.20070129084157000.000");
        this.assertNoLayer("corrupted");
        assertEquals(nStores, this.getCatalog().getStores(CoverageStoreInfo.class).size());
    }

    // FIXME: does not work
    public void testExistingVectorStore() {
        // Creates a Shapefile datastore with the name "rotated.tiff"
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_SHAPEFILELINK.path);
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_NAME.path, "rotated.tiff");
        form.setValue(WicketPath.IMPORT_DESCRIPTION.path, "Shapefile test");
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/shp");
        form.submit(WicketPath.IMPORT_NEXT.path);
        waitOnImportProgressPage();
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();

        // Tries to import a GeoTIFF with the same "rotated.tiff" name
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/geotiff/rotated.tiff");
        form.submit(WicketPath.IMPORT_NEXT.path);
        waitOnImportProgressPage();
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();
        this.assertNoException();
        tester.assertNoErrorMessage();

        // Checks the GeoTIFF has not been imported
        tester.assertContains("Successfully imported 0");
    }

    public void testCopyCoverageData_1() {
        // Tries to import a GeoTIFF copying data in the GEOSERVER_DATA_DIR
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/geotiff/rotated.tiff");
        form.setValue(WicketPath.IMPORT_COPY.path, true);
        this.executeWidgetUpdate();
        form.setValue(WicketPath.GEOTIFF_TILE.path, false);
        form.setValue(WicketPath.GEOTIFF_OVERVIEW.path, false);
        this.executeWidgetUpdate();
        form.setValue(WicketPath.IMPORT_OUTDIRECTORY.path, "/tmp");
        form.submit(WicketPath.IMPORT_NEXT.path);
        waitOnImportProgressPage();
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();

        // Checks the GeoTIFF has been imported into the data directory
        this.assertNoException();
        tester.assertNoErrorMessage();
        tester.assertContains("Successfully imported 0");
        this.assertLayer("rotated");
    }

    public void testCopyCoverageData_2() {
        // Tries to import a GeoTIFF copying data in the GEOSERVER_DATA_DIR
        tester.clickLink(WicketPath.IMPORT_LINK.path);
        tester.clickLink(WicketPath.IMPORT_GEOTIFFLINK.path);
        FormTester form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.setValue(WicketPath.IMPORT_DIRECTORY.path, this.dataRoot.getAbsolutePath()
                + "/../../src/test/data/geotiff/rotated.tiff");
        form.setValue(WicketPath.IMPORT_COPY.path, true);
        this.executeWidgetUpdate();
        form.setValue(WicketPath.IMPORT_OUTDIRECTORY.path, "/tmp");
        form.setValue(WicketPath.GEOTIFF_COMPRESSIONTYPE.path, "CCITT T.4");
        form.setValue(WicketPath.GEOTIFF_COMPRESSIONRATIO.path, "80");
        form.setValue(WicketPath.GEOTIFF_TILEWIDTH.path, "64");
        form.setValue(WicketPath.GEOTIFF_TILEHEIGHT.path, "64");
        form.setValue(WicketPath.GEOTIFF_DOWNSAMPLESTEP.path, "2");
        form.setValue(WicketPath.GEOTIFF_SUBSAMPLEALGORITHM.path, "Bicubic");
        form.setValue(WicketPath.GEOTIFF_NOVERVIEW.path, "3");
        form.submit(WicketPath.IMPORT_NEXT.path);
        waitOnImportProgressPage();
        form = tester.newFormTester(WicketPath.IMPORT_FORM.path);
        form.submit(WicketPath.IMPORT_IMPORT.path);
        waitOnImportProgressPage();
        this.assertNoException();
        tester.assertNoErrorMessage();

        // Checks the GeoTIFF has been imported into the data directory
        tester.assertContains("Successfully imported 5");
        this.assertNoLayer("corrupted");
        this.assertLayer("MOD021KM.01.20060425083931000.000");
        this.assertLayer("MOD021KM.01.20060426091905000.000");
        this.assertLayer("MOD021KM.01.20070129084157000.000");
        this.assertLayer("rotated");
        this.assertLayer("tazbm");
    }
     */
}
