/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFinder;
import org.geotools.data.DataStoreFactorySpi;


/**
 * Base class for DBMS configuration pages
 *
 * @author Andrea Aime, GeoSolutions
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractDBMSPage extends ImporterSecuredPage
{
    protected static final String CONNECTION_DEFAULT = "Default";

    protected static final String CONNECTION_JNDI = "JNDI";

    protected String connectionType;

    protected GeneralStoreParamPanel generalParams;

    private WebMarkupContainer connParamContainer;

    protected Component otherParamsPanel;

    private RepeatingView paramPanels;

    protected LinkedHashMap<String, Component> paramPanelMap;

    public AbstractDBMSPage()
    {
        Form form = new Form("form");
        add(form);

        // general parameters panel
        form.add(generalParams = new GeneralStoreParamPanel("generalParams"));

        // connection type chooser
        paramPanelMap = buildParamPanels();
        connectionType = paramPanelMap.keySet().iterator().next();
        updatePanelVisibility(null);
        form.add(connectionTypeSelector(paramPanelMap));

        // default param panels
        connParamContainer = new WebMarkupContainer("connectionParamsContainer");
        form.add(connParamContainer);
        connParamContainer.setOutputMarkupId(true);
        paramPanels = new RepeatingView("paramPanelRepeater");
        for (Component panel : paramPanelMap.values())
        {
            paramPanels.add(panel);
        }
        connParamContainer.add(paramPanels);

        // other params
        otherParamsPanel = buildOtherParamsPanel("otherParams");
        form.add(otherParamsPanel);

        // next button (where the action really is)
        SubmitLink submitLink = submitLink();
        form.add(submitLink);
        form.setDefaultButton(submitLink);
    }

    /**
     * Builds, configures and returns the other params panel
     *
     * @return
     */
    protected Component buildOtherParamsPanel(String id)
    {
        return new OtherDbmsParamPanel(id, "public", false, true);
    }

    /**
     * Wheter the geometryless data is going to be imported, or not
     * @return
     */
    protected boolean isGeometrylessExcluded()
    {
        return ((OtherDbmsParamPanel) otherParamsPanel).excludeGeometryless;
    }


    /**
     * Switches between the types of param panels
     *
     * @return
     */
    protected Component connectionTypeSelector(final Map<String, Component> paramPanelMap)
    {
        ArrayList<String> connectionTypeList = new ArrayList<String>(paramPanelMap.keySet());
        DropDownChoice choice = new DropDownChoice("connType", new PropertyModel(this,
                    "connectionType"), new Model(connectionTypeList), new IChoiceRenderer()
                {

                    public String getIdValue(Object object, int index)
                    {
                        return String.valueOf(object);
                    }

                    public Object getDisplayValue(Object object)
                    {
                        return new ParamResourceModel("ConnectionType." + object, null).getString();
                    }
                });

        choice.add(new AjaxFormComponentUpdatingBehavior("onchange")
            {

                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    updatePanelVisibility(target);
                    target.addComponent(connParamContainer);
                }

            });

        return choice;
    }

    /**
     * Updates the panel visibility to show only the currently selected one.
     * Can also be used to perform actions when the panel visibility is updated
     * @param paramPanelMap
     * @param target Used when doing ajax updates, might be null
     */
    protected void updatePanelVisibility(AjaxRequestTarget target)
    {
        for (String type : paramPanelMap.keySet())
        {
            Component panel = paramPanelMap.get(type);
            panel.setVisible(connectionType.equals(type));
        }
    }


    /**
     * Setups the datastore and moves to the next page
     *
     * @return
     */
    SubmitLink submitLink()
    {
        // TODO: fill this up with the required parameters
        return new SubmitLink("next")
            {

                @Override
                public void onSubmit()
                {
                    try
                    {
                        // check there is not another store with the same name
                        WorkspaceInfo workspace = generalParams.getWorkpace();
                        NamespaceInfo namespace = getCatalog().getNamespaceByPrefix(workspace.getName());
                        StoreInfo oldStore = getCatalog().getStoreByName(workspace, generalParams.name,
                                StoreInfo.class);
                        if (oldStore != null)
                        {
                            error(new ParamResourceModel("ImporterError.duplicateStore",
                                    AbstractDBMSPage.this, generalParams.name, workspace.getName()).getString());

                            return;
                        }

                        // build up the store connection param map
                        Map<String, Serializable> params = new HashMap<String, Serializable>();
                        DataStoreFactorySpi factory = fillStoreParams(namespace, params);

                        // ok, check we can connect
                        DataAccess store = null;
                        try
                        {
                            store = DataAccessFinder.getDataStore(params);
                            // force the store to open a connection
                            store.getNames();
                            store.dispose();
                        }
                        catch (Throwable e)
                        {
                            LOGGER.log(Level.INFO, "Could not connect to the datastore", e);
                            error(new ParamResourceModel("ImporterError.databaseConnectionError",
                                    AbstractDBMSPage.this, e.getMessage()).getString());

                            return;
                        }
                        finally
                        {
                            if (store != null)
                            {
                                store.dispose();
                            }
                        }

                        // build the store
                        CatalogBuilder builder = new CatalogBuilder(getCatalog());
                        builder.setWorkspace(workspace);

                        StoreInfo si = builder.buildDataStore(generalParams.name);
                        si.setDescription(generalParams.description);
                        si.getConnectionParameters().putAll(params);
                        si.setEnabled(true);
                        si.setType(factory.getDisplayName());
                        getCatalog().add(si);

                        // redirect to the layer chooser
                        PageParameters pp = new PageParameters();
                        pp.put("store", si.getName());
                        pp.put("workspace", workspace.getName());
                        pp.put("storeNew", true);
                        pp.put("workspaceNew", false);
                        pp.put("skipGeometryless", isGeometrylessExcluded());
                        setResponsePage(VectorChooserPage.class, pp);
                    }
                    catch (Exception e)
                    {
                        LOGGER.log(Level.SEVERE, "Error while setting up mass import", e);
                    }

                }
            };
    }

    /**
     * Builds and returns a map with params panels. The keys are used to fill in the drop down
     * choice and to look for the i18n key using the "ConnectionType.${key}" convention. The panels
     * built should have ids made of digits only, otherwise Wicket will complain about non safe ids
     * in repeater
     *
     * @return
     */
    protected abstract LinkedHashMap<String, Component> buildParamPanels();

    /**
     * Fills the specified params map and returns the factory spi that we're expected to use to
     * create the store
     * @param namespace
     * @param params
     * @return
     * @throws URISyntaxException
     */
    protected abstract DataStoreFactorySpi fillStoreParams(NamespaceInfo namespace, Map<String, Serializable> params)
        throws URISyntaxException;
}
