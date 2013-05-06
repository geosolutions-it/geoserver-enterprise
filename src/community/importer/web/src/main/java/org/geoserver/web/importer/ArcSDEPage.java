/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.geoserver.catalog.NamespaceInfo;
import org.geotools.arcsde.ArcSDEDataStoreFactory;
import org.geotools.arcsde.ArcSDEJNDIDataStoreFactory;
import org.geotools.data.DataStoreFactorySpi;

import static org.geotools.arcsde.ArcSDEDataStoreFactory.*;
import static org.geotools.arcsde.ArcSDEJNDIDataStoreFactory.*;


/**
 * Configures ArcSDE stores
 *
 * @author Andrea Aime, GeoSolutions
 */
public class ArcSDEPage extends AbstractDBMSPage
{

    private BasicSDEParamPanel basicSDEPanel;

    private JNDIParamPanel jndiParamsPanel;

    @Override
    protected LinkedHashMap<String, Component> buildParamPanels()
    {
        LinkedHashMap<String, Component> result = new LinkedHashMap<String, Component>();

        // basic panel
        basicSDEPanel = new BasicSDEParamPanel("01");
        result.put(CONNECTION_DEFAULT, basicSDEPanel);

        // jndi param panels
        jndiParamsPanel = new JNDIParamPanel("02", "java:comp/env/geotools/arcsde");
        result.put(CONNECTION_JNDI, jndiParamsPanel);

        return result;
    }

    @Override
    protected Component buildOtherParamsPanel(String id)
    {
        return new OtherSDEParamPanel(id);
    }

    @Override
    protected DataStoreFactorySpi fillStoreParams(NamespaceInfo namespace, Map<String, Serializable> params)
        throws URISyntaxException
    {
        DataStoreFactorySpi factory;
        params.put(ArcSDEDataStoreFactory.DBTYPE_PARAM.key, (String) ArcSDEDataStoreFactory.DBTYPE_PARAM.sample);
        if (CONNECTION_JNDI.equals(connectionType))
        {
            factory = new ArcSDEJNDIDataStoreFactory();

            params.put(JNDI_REFNAME.key, jndiParamsPanel.jndiReferenceName);
        }
        else
        {
            factory = new ArcSDEDataStoreFactory();

            // basic params
            params.put(SERVER_PARAM.key, basicSDEPanel.host);
            params.put(PORT_PARAM.key, basicSDEPanel.port);
            params.put(USER_PARAM.key, basicSDEPanel.username);
            params.put(PASSWORD_PARAM.key, basicSDEPanel.password);
            params.put(INSTANCE_PARAM.key, basicSDEPanel.instance);

            // connection pool params
            params.put(MIN_CONNECTIONS_PARAM.key, basicSDEPanel.minConnections);
            params.put(MAX_CONNECTIONS_PARAM.key, basicSDEPanel.maxConnections);
            params.put(TIMEOUT_PARAM.key, basicSDEPanel.connTimeout);

        }

        OtherSDEParamPanel otherParamsPanel = (OtherSDEParamPanel) this.otherParamsPanel;
        params.put(VERSION_PARAM.key, otherParamsPanel.version);
        params.put(ALLOW_NON_SPATIAL_PARAM.key, !otherParamsPanel.excludeGeometryless);
        params.put(NAMESPACE_PARAM.key, new URI(namespace.getURI()).toString());

        return factory;
    }

    @Override
    protected boolean isGeometrylessExcluded()
    {
        OtherSDEParamPanel otherParamsPanel = (OtherSDEParamPanel) this.otherParamsPanel;

        return otherParamsPanel.excludeGeometryless;
    }

}
