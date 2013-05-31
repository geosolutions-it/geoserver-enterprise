/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.arcsde;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.CoverageStoreEditPage;
import org.geoserver.web.data.store.CoverageStoreNewPage;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.StoreExtensionPoints;
import org.geoserver.web.data.store.panel.PasswordParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geotools.arcsde.raster.gce.ArcSDERasterFormat;
import org.geotools.arcsde.session.ArcSDEConnectionConfig;
import org.geotools.arcsde.session.ISession;
import org.geotools.arcsde.session.ISessionPool;
import org.geotools.arcsde.session.ISessionPoolFactory;
import org.geotools.arcsde.session.SessionWrapper;
import org.geotools.arcsde.session.UnavailableConnectionException;

/**
 * 
 * @author Gabriel Roldan
 */
public class ArcSDECoverageStoreEditPanelTest extends GeoServerWicketTestSupport {

    private Page page;

    private CoverageStoreInfo storeInfo;

    private Form editForm;

    @Override
    protected void setUpInternal() throws Exception {
    }

    private ArcSDECoverageStoreEditPanel startPanelToEditStore() {
        final Catalog catalog = getCatalog();
        storeInfo = catalog.getFactory().createCoverageStore();
        storeInfo.setDescription("fake arcsde store");
        storeInfo.setEnabled(true);
        storeInfo.setName("fakeArcsde");
        storeInfo.setType(ArcSDERasterFormat.getInstance().getName());
        storeInfo.setWorkspace(catalog.getDefaultWorkspace());
        storeInfo.setURL("sde://user:pass@localhost:5151/#FAKE.TABLE");

        catalog.save(storeInfo);

        final String storeId = storeInfo.getId();

        login();
        page = new CoverageStoreEditPage(storeId);
        tester.startPage(page);

        editForm = (Form) tester.getComponentFromLastRenderedPage("rasterStoreForm");

        ArcSDECoverageStoreEditPanel panel = (ArcSDECoverageStoreEditPanel) tester
                .getComponentFromLastRenderedPage("rasterStoreForm:parametersPanel");

        return panel;
    }

    private ArcSDECoverageStoreEditPanel startPanelForNewStore() {
        login();
        page = new CoverageStoreNewPage(ArcSDERasterFormat.getInstance().getName());
        tester.startPage(page);

        editForm = (Form) tester.getComponentFromLastRenderedPage("rasterStoreForm");

        ArcSDECoverageStoreEditPanel panel = (ArcSDECoverageStoreEditPanel) tester
                .getComponentFromLastRenderedPage("rasterStoreForm:parametersPanel");

        return panel;
    }

    public void testExtensionPoint() {
        storeInfo = getCatalog().getFactory().createCoverageStore();
        storeInfo.setType(ArcSDERasterFormat.getInstance().getName());
        // need to set name so BaseWicketTester does not fail on storeInfo.toString returning
        // null... odd.
        storeInfo.setName("test storeInfo Name");

        editForm = new Form("formId");
        editForm.setModel(new Model(storeInfo));
        GeoServerApplication app = getGeoServerApplication();

        StoreEditPanel storeEditPanel = StoreExtensionPoints.getStoreEditPanel("id", editForm,
                storeInfo, app);
        assertNotNull(storeEditPanel);
        assertTrue(storeEditPanel instanceof ArcSDECoverageStoreEditPanel);
    }

    public void testStartupForNew() {
        startPanelForNewStore();

        final String base = "rasterStoreForm:parametersPanel:";
        tester.assertComponent(base + "connectionPrototype", DropDownChoice.class);
        tester.assertComponent(base + "server", TextParamPanel.class);
        tester.assertComponent(base + "port", TextParamPanel.class);
        tester.assertComponent(base + "instance", TextParamPanel.class);
        tester.assertComponent(base + "user", TextParamPanel.class);
        tester.assertComponent(base + "password", PasswordParamPanel.class);
        tester.assertComponent(base + "tableNamePanel", RasterTableSelectionPanel.class);
    }

    public void testStartupForEdit() {
        startPanelToEditStore();
        // print(page, true, true);

        final String base = "rasterStoreForm:parametersPanel:";
        tester.assertComponent(base + "connectionPrototype", DropDownChoice.class);
        tester.assertComponent(base + "server", TextParamPanel.class);
        tester.assertComponent(base + "port", TextParamPanel.class);
        tester.assertComponent(base + "instance", TextParamPanel.class);
        tester.assertComponent(base + "user", TextParamPanel.class);
        tester.assertComponent(base + "password", PasswordParamPanel.class);
        // this is a TextParamPanel instead of a RasterTableSelectionPanel when editing instead of
        // adding
        tester.assertComponent(base + "tableNamePanel", TextParamPanel.class);
        tester.assertModelValue(base + "tableNamePanel:border:paramValue", "FAKE.TABLE");
    }

    /**
     * Connection parameters are not properly set and the refresh raster tables button is hit
     */
    public void testRefreshRasterTableListBadConnectionParams() {
        startPanelForNewStore();
        final FormTester formTester = tester.newFormTester("rasterStoreForm");

        final String base = "rasterStoreForm:parametersPanel:";
        RasterTableSelectionPanel tableChooserPanel = (RasterTableSelectionPanel) tester
                .getComponentFromLastRenderedPage(base + "tableNamePanel");

        tableChooserPanel.setSessionFactory(new ISessionPoolFactory() {

            public ISessionPool createPool(final ArcSDEConnectionConfig config) throws IOException {
                throw new IOException("can't connect for some reason");
            }
        });

        // print(page, true, true);
        // simulate clicking on the refresh button
        String submitLink = base + "tableNamePanel:refresh";
        tester.executeAjaxEvent(submitLink, "onclick");
        FeedbackMessage feedbackMessage = formTester.getForm().getFeedbackMessage();
        assertNotNull(feedbackMessage);
        Serializable message = feedbackMessage.getMessage();
        assertNotNull(message);
        String expectedMessage = "Refreshing raster tables list: can't connect for some reason";
        assertEquals(expectedMessage, message.toString());
    }

    /**
     * Connection parameters are properly set and the refresh raster tables button is hit, producing
     * the DropDownChoice to be filled up with the table names
     */
    public void testRefreshRasterTableList() {
        startPanelForNewStore();
        final FormTester formTester = tester.newFormTester("rasterStoreForm");

        final String base = "rasterStoreForm:parametersPanel:";
        RasterTableSelectionPanel tableChooserPanel = (RasterTableSelectionPanel) tester
                .getComponentFromLastRenderedPage(base + "tableNamePanel");

        final List<String> rasterColumns = Arrays.asList("FAKE.TABLE1", "FAKE.TABLE2",
                "FAKE.TABLE3");

        tableChooserPanel.setSessionFactory(new ISessionPoolFactory() {

            public ISessionPool createPool(final ArcSDEConnectionConfig config) throws IOException {
                return new ISessionPool() {
                    public ISession getSession() throws IOException, UnavailableConnectionException {
                        return getSession(true);
                    }
                    public ISession getSession(final boolean transactional) throws IOException, UnavailableConnectionException {
                        return new SessionWrapper(null) {
                            @Override
                            public List<String> getRasterColumns() throws IOException {
                                return rasterColumns;
                            }

                            @Override
                            public void dispose() {
                                // do nothing
                            }
                        };
                    }

                    public boolean isClosed() {
                        return false;
                    }

                    public int getPoolSize() {
                        return 1;
                    }

                    public int getInUseCount() {
                        return 0;
                    }

                    public ArcSDEConnectionConfig getConfig() {
                        return config;
                    }

                    public int getAvailableCount() {
                        return 1;
                    }

                    public void close() {
                        // do nothing
                    }
                };
            }
        });

        //print(page, true, true);

        final String dropDownPath = base + "tableNamePanel:border:rasterTable";
        final DropDownChoice choice = (DropDownChoice) tester
                .getComponentFromLastRenderedPage(dropDownPath);
        assertTrue(choice.getChoices().isEmpty());

        // simulate clicking on the refresh button
        String submitLink = base + "tableNamePanel:refresh";
        tester.executeAjaxEvent(submitLink, "onclick");
        FeedbackMessage feedbackMessage = formTester.getForm().getFeedbackMessage();
        assertNull(feedbackMessage);

        assertEquals(rasterColumns, choice.getChoices());
    }
}
