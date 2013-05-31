package org.geoserver.web.data.store;

import java.util.List;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;

public class CoverageStoreEditPageTest extends GeoServerWicketTestSupport {

    CoverageStoreInfo coverageStore;

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWellKnownCoverageTypes();
    }

    @Override
    protected void setUpInternal() throws Exception {
        login();
        
        coverageStore = getCatalog().getStoreByName(MockData.TASMANIA_BM.getLocalPart(),
                CoverageStoreInfo.class);
        tester.startPage(new CoverageStoreEditPage(coverageStore.getId()));

        // print(tester.getLastRenderedPage(), true, true);
    }

    public void testLoad() {
        tester.assertRenderedPage(CoverageStoreEditPage.class);
        tester.assertNoErrorMessage();

        tester.assertLabel("rasterStoreForm:storeType", "GeoTIFF");
        tester.assertModelValue("rasterStoreForm:namePanel:border:paramValue", "BlueMarble");
    }

    public void testChangeName() {
        FormTester form = tester.newFormTester("rasterStoreForm");
        form.setValue("namePanel:border:paramValue", "BlueMarbleModified");
        form.submit();
        tester.clickLink("rasterStoreForm:save");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(StorePage.class);
        assertNotNull(getCatalog().getStoreByName("BlueMarbleModified", CoverageStoreInfo.class));
    }

    public void testNameRequired() {
        FormTester form = tester.newFormTester("rasterStoreForm");
        form.setValue("namePanel:border:paramValue", null);
        form.submit();
        tester.clickLink("rasterStoreForm:save");

        tester.assertRenderedPage(CoverageStoreEditPage.class);
        tester.assertErrorMessages(new String[] { "Field 'Data Source Name' is required." });
    }

    /**
     * Test that changing a datastore's workspace updates the datastore's "namespace" parameter as
     * well as the namespace of its previously configured resources
     */
    public void testWorkspaceSyncsUpWithNamespace() {
        final Catalog catalog = getCatalog();

        final FormTester formTester = tester.newFormTester("rasterStoreForm");

        final String wsDropdownPath = "rasterStoreForm:workspacePanel:border:paramValue";

        tester.assertModelValue(wsDropdownPath, catalog.getWorkspaceByName(MockData.WCS_PREFIX));

        // select the fifth item in the drop down, which is the cdf workspace
        formTester.select("workspacePanel:border:paramValue", 2);

        // weird on this test I need to both call form.submit() and also simulate clicking on the
        // ajax "save" link for the model to be updated. On a running geoserver instance it works ok
        // though
        formTester.submit();

        final boolean isAjax = true;
        tester.clickLink("rasterStoreForm:save", isAjax);

        // did the save finish normally?
        tester.assertRenderedPage(StorePage.class);

        CoverageStoreInfo store = catalog.getCoverageStore(coverageStore.getId());
        WorkspaceInfo workspace = store.getWorkspace();
        assertFalse(MockData.WCS_PREFIX.equals(workspace.getName()));

        // was the namespace for the datastore resources updated?
        List<CoverageInfo> resourcesByStore;
        resourcesByStore = catalog.getResourcesByStore(store, CoverageInfo.class);

        assertTrue(resourcesByStore.size() > 0);

        for (CoverageInfo cv : resourcesByStore) {
            assertEquals("Namespace for " + cv.getName() + " was not updated", workspace.getName(),
                    cv.getNamespace().getPrefix());
        }
    }

    public void testEditDetached() throws Exception {
        final Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getFactory().createCoverageStore();
        new CatalogBuilder(catalog).updateCoverageStore(store, coverageStore);
        assertNull(store.getId());

        tester.startPage(new CoverageStoreEditPage(store));
        tester.assertNoErrorMessage();
        
        FormTester form = tester.newFormTester("rasterStoreForm");
        form.setValue("namePanel:border:paramValue", "foo");
        form.submit();
        tester.clickLink("rasterStoreForm:save");
        tester.assertNoErrorMessage();

        assertNull(store.getId());
        assertEquals("foo", store.getName());
        assertNotNull(catalog.getStoreByName(coverageStore.getName(), CoverageStoreInfo.class));
        assertNull(catalog.getStoreByName("foo", CoverageStoreInfo.class));

    }
}
