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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.catalog.NamespaceInfo;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.oracle.OracleNGDataStoreFactory;
import org.geotools.data.oracle.OracleNGJNDIDataStoreFactory;
import org.geotools.data.oracle.OracleNGOCIDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;

import static org.geotools.data.oracle.OracleNGOCIDataStoreFactory.ALIAS;
import static org.geotools.jdbc.JDBCDataStoreFactory.FETCHSIZE;
import static org.geotools.jdbc.JDBCDataStoreFactory.MAXCONN;
import static org.geotools.jdbc.JDBCDataStoreFactory.MAXWAIT;
import static org.geotools.jdbc.JDBCDataStoreFactory.MINCONN;
import static org.geotools.jdbc.JDBCDataStoreFactory.NAMESPACE;
import static org.geotools.jdbc.JDBCDataStoreFactory.PASSWD;
import static org.geotools.jdbc.JDBCDataStoreFactory.PK_METADATA_TABLE;
import static org.geotools.jdbc.JDBCDataStoreFactory.USER;
import static org.geotools.jdbc.JDBCDataStoreFactory.VALIDATECONN;
import static org.geotools.jdbc.JDBCJNDIDataStoreFactory.JNDI_REFNAME;


/**
 * Connection params form for the Oracle database
 *
 * @author Andrea Aime, GeoSolutions
 */
public class OraclePage extends AbstractDBMSPage
{
    private static final String CONNECTION_OCI = "OCI";

    private JNDIParamPanel jndiParamsPanel;

    private BasicDbmsParamPanel basicDbmsPanel;

    private OracleOCIParamPanel ociParamsPanel;

    @Override
    protected LinkedHashMap<String, Component> buildParamPanels()
    {
        LinkedHashMap<String, Component> result = new LinkedHashMap<String, Component>();

        // basic panel
        basicDbmsPanel = new BasicDbmsParamPanel("01", "localhost", 1521, true);
        result.put(CONNECTION_DEFAULT, basicDbmsPanel);

        // oci one
        ociParamsPanel = new OracleOCIParamPanel("02");
        result.put(CONNECTION_OCI, ociParamsPanel);

        // jndi param panels
        jndiParamsPanel = new JNDIParamPanel("03", "java:comp/env/jdbc/mydatabase");
        result.put(CONNECTION_JNDI, jndiParamsPanel);

        return result;
    }

    @Override
    protected void updatePanelVisibility(AjaxRequestTarget target)
    {
        super.updatePanelVisibility(target);
        if (target != null)
        {
            ((OtherDbmsParamPanel) otherParamsPanel).setShowUserSchema(!CONNECTION_JNDI.equals(connectionType));
            target.addComponent(otherParamsPanel);
        }
    }

    @Override
    protected OtherDbmsParamPanel buildOtherParamsPanel(String id)
    {
        OtherDbmsParamPanel others = new OtherDbmsParamPanel(id, "", true, true);
        others.setOutputMarkupId(true);

        return others;
    }

    @Override
    protected DataStoreFactorySpi fillStoreParams(NamespaceInfo namespace, Map<String, Serializable> params)
        throws URISyntaxException
    {
        DataStoreFactorySpi factory;
        params.put(JDBCDataStoreFactory.DBTYPE.key, "oracle");
        if (CONNECTION_JNDI.equals(connectionType))
        {
            factory = new OracleNGJNDIDataStoreFactory();

            params.put(JNDI_REFNAME.key, jndiParamsPanel.jndiReferenceName);
        }
        else if (CONNECTION_OCI.equals(connectionType))
        {
            factory = new OracleNGOCIDataStoreFactory();

            params.put(ALIAS.key, ociParamsPanel.alias);
            params.put(USER.key, ociParamsPanel.username);
            params.put(PASSWD.key, ociParamsPanel.password);
        }
        else
        {
            factory = new OracleNGDataStoreFactory();

            // basic params
            params.put(OracleNGDataStoreFactory.HOST.key, basicDbmsPanel.host);
            params.put(OracleNGDataStoreFactory.PORT.key, basicDbmsPanel.port);
            params.put(OracleNGDataStoreFactory.USER.key, basicDbmsPanel.username);
            params.put(OracleNGDataStoreFactory.PASSWD.key, basicDbmsPanel.password);
            params.put(OracleNGDataStoreFactory.DATABASE.key, basicDbmsPanel.database);
        }
        if (!CONNECTION_JNDI.equals(connectionType))
        {
            // connection pool params common to OCI and default connections
            params.put(MINCONN.key, basicDbmsPanel.connPool.minConnection);
            params.put(MAXCONN.key, basicDbmsPanel.connPool.maxConnection);
            params.put(FETCHSIZE.key, basicDbmsPanel.connPool.fetchSize);
            params.put(MAXWAIT.key, basicDbmsPanel.connPool.timeout);
            params.put(VALIDATECONN.key, basicDbmsPanel.connPool.validate);

        }

        OtherDbmsParamPanel otherParamsPanel = (OtherDbmsParamPanel) this.otherParamsPanel;
        if (otherParamsPanel.userSchema)
        {
            params.put(JDBCDataStoreFactory.SCHEMA.key,
                ((String) params.get(USER.key)).toUpperCase());
        }
        else
        {
            params.put(JDBCDataStoreFactory.SCHEMA.key, otherParamsPanel.schema);
        }
        params.put(NAMESPACE.key, new URI(namespace.getURI()).toString());
        params.put(OracleNGDataStoreFactory.LOOSEBBOX.key, otherParamsPanel.looseBBox);
        params.put(PK_METADATA_TABLE.key, otherParamsPanel.pkMetadata);

        return factory;
    }

}
