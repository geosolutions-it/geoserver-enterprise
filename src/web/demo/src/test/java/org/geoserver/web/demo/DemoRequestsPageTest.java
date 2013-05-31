/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.test.TestData;

/**
 * 
 * @author Gabriel Roldan
 * @verion $Id$
 */
public class DemoRequestsPageTest extends GeoServerWicketTestSupport {

    private File demoDir;

    @Override
    protected void setUpInternal() throws Exception {
        demoDir = TestData.file(this, "demo-requests");
        tester.startPage(new DemoRequestsPage(demoDir));
    }

    /**
     * Kind of smoke test to make sure the page structure was correctly set up once loaded
     */
    public void testStructure() {
        // print(tester.getLastRenderedPage(), true, true);

        assertTrue(tester.getLastRenderedPage() instanceof DemoRequestsPage);

        tester.assertComponent("demoRequestsForm", Form.class);
        tester.assertComponent("demoRequestsForm:demoRequestsList", DropDownChoice.class);
        tester.assertComponent("demoRequestsForm:url", TextField.class);
        tester.assertComponent("demoRequestsForm:body:editorContainer:editor", TextArea.class);
        tester.assertComponent("demoRequestsForm:username", TextField.class);
        tester.assertComponent("demoRequestsForm:password", PasswordTextField.class);
        tester.assertComponent("demoRequestsForm:submit", AjaxSubmitLink.class);

        tester.assertComponent("responseWindow", ModalWindow.class);
    }

    @SuppressWarnings("unchecked")
    public void testDemoListLoaded() {
        // print(tester.getLastRenderedPage(), true, true);

        /*
         * Expected choices are the file names in the demo requests dir
         * (/src/test/resources/test-data/demo-requests in this case)
         */
        final List<String> expectedList = Arrays.asList(new String[] { "WFS_getFeature-1.1.xml",
                "WMS_describeLayer.url" });

        DropDownChoice dropDown = (DropDownChoice) tester
                .getComponentFromLastRenderedPage("demoRequestsForm:demoRequestsList");
        List choices = dropDown.getChoices();
        assertEquals(expectedList, choices);
    }

    public void testUrlLinkUnmodified() {
        // print(tester.getLastRenderedPage(), true, true);

        final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");

        final String requestName = "WMS_describeLayer.url";
        requestFormTester.select("demoRequestsList", 1);

        /*
         * There's an AjaxFormSubmitBehavior attached to onchange so force it
         */
        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "onchange");
        
        tester.assertModelValue("demoRequestsForm:demoRequestsList", requestName);

        final boolean isAjax = true;
        tester.clickLink("demoRequestsForm:submit", isAjax);

        tester.assertVisible("responseWindow");

        IModel model = tester.getLastRenderedPage().getDefaultModel();
        assertTrue(model.getObject() instanceof DemoRequest);
        DemoRequest req = (DemoRequest) model.getObject();

        assertEquals(demoDir, req.getDemoDir());
        String requestFileName = req.getRequestFileName();
        String requestUrl = req.getRequestUrl();
        String requestBody = req.getRequestBody();

        assertEquals(requestName, requestFileName);
        assertNotNull(requestUrl);
        assertNull(requestBody);
    }

    public void testUrlLinkSelected() {
        // print(tester.getLastRenderedPage(), true, true);

        final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");

        final String requestName = "WMS_describeLayer.url";

        requestFormTester.select("demoRequestsList", 1);
        
        /*
         * There's an AjaxFormSubmitBehavior attached to onchange so force it
         */
        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "onchange");
        
        
        tester.assertModelValue("demoRequestsForm:demoRequestsList", requestName);

        final boolean isAjax = true;
        tester.clickLink("demoRequestsForm:submit", isAjax);

        tester.assertVisible("responseWindow");

        IModel model = tester.getLastRenderedPage().getDefaultModel();
        assertTrue(model.getObject() instanceof DemoRequest);
        DemoRequest req = (DemoRequest) model.getObject();

        assertEquals(demoDir, req.getDemoDir());
        String requestFileName = req.getRequestFileName();
        String requestUrl = req.getRequestUrl();
        String requestBody = req.getRequestBody();

        assertEquals(requestName, requestFileName);
        assertNotNull(requestUrl);
        assertNull(requestBody);
    }

    public void testUrlLinkModified() {
        // print(tester.getLastRenderedPage(), true, true);

        final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");

        final String requestName = "WMS_describeLayer.url";

        requestFormTester.select("demoRequestsList", 1);
        
        /*
         * There's an AjaxFormSubmitBehavior attached to onchange so force it
         */
        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "onchange");

        tester.assertModelValue("demoRequestsForm:demoRequestsList", requestName);

        final String modifiedUrl = "http://modified/url";

        TextField url = (TextField) tester.getComponentFromLastRenderedPage("demoRequestsForm:url");
        url.setModelValue(new String[] { modifiedUrl });

        assertEquals(modifiedUrl, url.getValue());

        final boolean isAjax = true;
        tester.clickLink("demoRequestsForm:submit", isAjax);

        tester.assertVisible("responseWindow");

        IModel model = tester.getLastRenderedPage().getDefaultModel();
        assertTrue(model.getObject() instanceof DemoRequest);
        DemoRequest req = (DemoRequest) model.getObject();

        String requestUrl = req.getRequestUrl();
        assertEquals(modifiedUrl, requestUrl);
    }

}
