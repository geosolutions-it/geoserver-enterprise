/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.PageMap;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.GeoServerBasePage;
import org.vfny.geoserver.wfs.servlets.TestWfsPost;

/**
 * An intermediate page used to submit a demo request to the {@link TestWfsPost /TestWfsPost}
 * servlet.
 * <p>
 * This page does not extend {@link GeoServerBasePage} since its just an intermediate form to submit
 * to the servlet.
 * </p>
 * 
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 * @since 2.0.x
 */
public class DemoRequestResponse extends WebPage {

    /**
     * Fills out the form to be submitted to {@code TestWfsPost} with the properties from the
     * {@code DemoRequestModel} provided, and auto-submit the form on page load so the results get
     * loaded in the page body.
     * 
     * @param model
     *            the demo request parameters holder, as a model for {@link DemoRequest}
     */
    public DemoRequestResponse(final IModel model) {
        // this page being in an IFrame needs to grap its own PageMap
        // in order not to share it with the parent page and thus be
        // marked as expired
        super(PageMap.forName("demoRequestResponse"));

        Form form = new Form("form");
        add(form);
        form.add(new HiddenField("url", new PropertyModel(model, "requestUrl")));
        form.add(new TextArea("body", new PropertyModel(model, "requestBody")));
        form.add(new HiddenField("username", new PropertyModel(model, "userName")));
        form.add(new HiddenField("password", new PropertyModel(model, "password")));

        // override the action property of the form to submit to the TestWfsPost
        // servlet
        form.add(new SimpleAttributeModifier("action", "../TestWfsPost"));

        // Set the same markup is as in the html page so wicket does not
        // generates
        // its own and the javascript code in the onLoad event for the <body>
        // element
        // finds out the form by id
        form.setMarkupId("form");
    }

}