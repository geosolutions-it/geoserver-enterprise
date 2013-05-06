/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import java.net.URLEncoder;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspaceDetachableModel;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.ParamResourceModel;


@SuppressWarnings("serial")
class GeneralStoreParamPanel extends Panel
{
    WorkspaceDetachableModel workspace;
    String name;
    String description;
    GeoServerDialog dialog;
    private DropDownChoice wsChoice;

    public GeneralStoreParamPanel(String id)
    {
        super(id);

        // workspace chooser
        workspace = new WorkspaceDetachableModel(GeoServerApplication.get().getCatalog().getDefaultWorkspace());
        wsChoice = new DropDownChoice("workspace", workspace, new WorkspacesModel(), new WorkspaceChoiceRenderer());
        wsChoice.setOutputMarkupId(true);
        add(wsChoice);

        // add workspace link and the popup dialog
        add(createWorkspaceLink());
        dialog = new GeoServerDialog("dialog");
        add(dialog);

        // name and description
        add(new TextField("name", new PropertyModel(this, "name")).setRequired(true));
        add(new TextField("description", new PropertyModel(this, "description")));
    }

    private AjaxLink createWorkspaceLink()
    {
        return new AjaxLink("createWorkspace")
            {

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    dialog.setTitle(new ParamResourceModel("dialogTitle", GeneralStoreParamPanel.this));
                    dialog.setInitialWidth(400);
                    dialog.setInitialHeight(150);
                    dialog.setMinimalHeight(150);

                    dialog.showOkCancel(target, new DialogDelegate()
                        {
                            String wsName;

                            @Override
                            protected boolean onSubmit(AjaxRequestTarget target, Component contents)
                            {
                                try
                                {
                                    Catalog catalog = GeoServerApplication.get().getCatalog();

                                    NewWorkspacePanel panel = (NewWorkspacePanel) contents;
                                    wsName = panel.workspace;

                                    WorkspaceInfo ws = catalog.getFactory().createWorkspace();
                                    ws.setName(wsName);

                                    NamespaceInfo ns = catalog.getFactory().createNamespace();
                                    ns.setPrefix(wsName);
                                    ns.setURI("http://opengeo.org/#" + URLEncoder.encode(wsName, "ASCII"));

                                    catalog.add(ws);
                                    catalog.add(ns);

                                    return true;
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();

                                    return false;
                                }
                            }

                            @Override
                            public void onClose(AjaxRequestTarget target)
                            {
                                Catalog catalog = GeoServerApplication.get().getCatalog();
                                workspace = new WorkspaceDetachableModel(catalog.getWorkspaceByName(wsName));
                                wsChoice.setModel(workspace);
                                target.addComponent(wsChoice);
                            }

                            @Override
                            protected Component getContents(String id)
                            {
                                return new NewWorkspacePanel(id);
                            }
                        });
                }
            };
    }

    public WorkspaceInfo getWorkpace()
    {
        return (WorkspaceInfo) workspace.getObject();
    }


}
