package org.geoserver.web.importer;

/**
 *
 * @author Andrea Aime, GeoSolutions SAS
 *
 */
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.geoserver.catalog.NamespaceInfo;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.sqlserver.SQLServerDataStoreFactory;
import org.geotools.data.sqlserver.SQLServerJNDIDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;

import static org.geotools.data.postgis.PostgisNGDataStoreFactory.*;
import static org.geotools.jdbc.JDBCJNDIDataStoreFactory.*;


public class SQLServerPage extends AbstractDBMSPage
{

    private JNDIParamPanel jndiParamsPanel;

    private BasicDbmsParamPanel basicDbmsPanel;

    @Override
    protected LinkedHashMap<String, Component> buildParamPanels()
    {
        LinkedHashMap<String, Component> result = new LinkedHashMap<String, Component>();

        // basic panel
        basicDbmsPanel = new BasicDbmsParamPanel("01", "localhost", 1433, false);
        result.put(CONNECTION_DEFAULT, basicDbmsPanel);

        // jndi param panels
        jndiParamsPanel = new JNDIParamPanel("02", "java:comp/env/jdbc/mydatabase");
        result.put(CONNECTION_JNDI, jndiParamsPanel);

        return result;
    }

    @Override
    protected OtherDbmsParamPanel buildOtherParamsPanel(String id)
    {
        return new OtherDbmsParamPanel(id, "public", false, true);
    }

    @Override
    protected DataStoreFactorySpi fillStoreParams(NamespaceInfo namespace, Map<String, Serializable> params)
        throws URISyntaxException
    {
        DataStoreFactorySpi factory;
        params.put(SQLServerDataStoreFactory.DBTYPE.key, (String) SQLServerDataStoreFactory.DBTYPE.sample);
        if (CONNECTION_JNDI.equals(connectionType))
        {
            factory = new SQLServerJNDIDataStoreFactory();

            params.put(JNDI_REFNAME.key, jndiParamsPanel.jndiReferenceName);
        }
        else
        {
            factory = new SQLServerDataStoreFactory();

            // basic params
            params.put(HOST.key, basicDbmsPanel.host);
            params.put(PostgisNGDataStoreFactory.PORT.key, basicDbmsPanel.port);
            params.put(USER.key, basicDbmsPanel.username);
            params.put(PASSWD.key, basicDbmsPanel.password);
            params.put(DATABASE.key, basicDbmsPanel.database);

            // connection pool params
            params.put(MINCONN.key, basicDbmsPanel.connPool.minConnection);
            params.put(MAXCONN.key, basicDbmsPanel.connPool.maxConnection);
            params.put(FETCHSIZE.key, basicDbmsPanel.connPool.fetchSize);
            params.put(MAXWAIT.key, basicDbmsPanel.connPool.timeout);
            params.put(VALIDATECONN.key, basicDbmsPanel.connPool.validate);
            params.put(PREPARED_STATEMENTS.key, basicDbmsPanel.connPool.preparedStatements);

        }

        OtherDbmsParamPanel otherParamsPanel = (OtherDbmsParamPanel) this.otherParamsPanel;
        params.put(JDBCDataStoreFactory.SCHEMA.key, otherParamsPanel.schema);
        params.put(NAMESPACE.key, new URI(namespace.getURI()).toString());
        params.put(PK_METADATA_TABLE.key, otherParamsPanel.pkMetadata);

        return factory;
    }

}
