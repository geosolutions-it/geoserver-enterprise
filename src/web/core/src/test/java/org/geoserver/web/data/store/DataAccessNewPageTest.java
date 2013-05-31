/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.util.Arrays;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.data.property.PropertyDataStoreFactory;

/**
 * Test suite for {@link DataAccessNewPage}
 * 
 * @author Gabriel Roldan
 */
public class DataAccessNewPageTest extends GeoServerWicketTestSupport {

    /**
     * print page structure?
     */
    private static final boolean debugMode = false;

    private AbstractDataAccessPage startPage() {
        final String dataStoreFactoryDisplayName = new PropertyDataStoreFactory().getDisplayName();

        final AbstractDataAccessPage page = new DataAccessNewPage(dataStoreFactoryDisplayName);
        login();
        tester.startPage(page);

        if (debugMode) {
            print(page, true, true);
        }

        return page;
    }

    public void testInitCreateNewDataStoreInvalidDataStoreFactoryName() {

        final String dataStoreFactoryDisplayName = "_invalid_";
        try {
            new DataAccessNewPage(dataStoreFactoryDisplayName);
            fail("Expected IAE on invalid datastore factory name");
        } catch (IllegalArgumentException e) {
            // hum.. change the assertion if the text changes in GeoserverApplication.properties...
            // but I still want to assert the reason for failure is the expected one..
            assertTrue(e.getMessage().startsWith("Can't find the factory"));
        }
    }

    /**
     * A kind of smoke test that only asserts the page is rendered when first loaded
     */
    public void testPageRendersOnLoad() {

        final PropertyDataStoreFactory dataStoreFactory = new PropertyDataStoreFactory();
        final String dataStoreFactoryDisplayName = dataStoreFactory.getDisplayName();

        startPage();

        tester.assertLabel("dataStoreForm:storeType", dataStoreFactoryDisplayName);
        tester.assertLabel("dataStoreForm:storeTypeDescription", dataStoreFactory.getDescription());

        tester.assertComponent("dataStoreForm:workspacePanel", WorkspacePanel.class);
    }

    public void testDefaultWorkspace() {

        startPage();

        WorkspaceInfo defaultWs = getCatalog().getDefaultWorkspace();

        tester.assertModelValue("dataStoreForm:workspacePanel:border:paramValue", defaultWs);

        WorkspaceInfo anotherWs = getCatalog().getFactory().createWorkspace();
        anotherWs.setName("anotherWs");

        getCatalog().add(anotherWs);
        getCatalog().setDefaultWorkspace(anotherWs);
        anotherWs = getCatalog().getDefaultWorkspace();

        startPage();
        tester.assertModelValue("dataStoreForm:workspacePanel:border:paramValue", anotherWs);

    }

    public void testDefaultNamespace() {

        // final String namespacePath =
        // "dataStoreForm:parameters:1:parameterPanel:border:paramValue";
        final String namespacePath = "dataStoreForm:parametersPanel:parameters:1:parameterPanel:paramValue";

        startPage();

        NamespaceInfo defaultNs = getCatalog().getDefaultNamespace();

        tester.assertModelValue(namespacePath, defaultNs.getURI());

    }

    public void testDataStoreParametersAreCreated() {
        startPage();
        List parametersListViewValues = Arrays.asList(new Object[] { "directory", "namespace" });
        tester.assertListView("dataStoreForm:parametersPanel:parameters", parametersListViewValues);
    }

    /**
     * Make sure in case the DataStore has a "namespace" parameter, its value is initialized to the
     * NameSpaceInfo one that matches the workspace
     */
    public void testInitCreateNewDataStoreSetsNamespaceParam() {
        final Catalog catalog = getGeoServerApplication().getCatalog();

        final AbstractDataAccessPage page = startPage();

        page.get(null);
        // final NamespaceInfo assignedNamespace = (NamespaceInfo) page.parametersMap
        // .get(AbstractDataAccessPage.NAMESPACE_PROPERTY);
        // final NamespaceInfo expectedNamespace = catalog.getDefaultNamespace();
        //
        // assertEquals(expectedNamespace, assignedNamespace);

    }

}
