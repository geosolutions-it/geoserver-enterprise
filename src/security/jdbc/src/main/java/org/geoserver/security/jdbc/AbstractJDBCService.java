/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.jdbc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;



/**
 * JDBC base implementation for common used methods
 * 
 * @author christian
 *
 */
public abstract class AbstractJDBCService extends AbstractGeoServerSecurityService {
    /** logger */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.security.jdbc");
    
    protected Properties ddlProps,dmlProps;
    protected DataSource datasource;
    
    /**
     * Default isolation level to use
     */
    final static int DEFAULT_ISOLATION_LEVEL=Connection.TRANSACTION_READ_COMMITTED;

    protected AbstractJDBCService() {
    }

    /**
     * initialize a {@link DataSource} form a
     * {@link JdbcSecurityServiceConfig} object
     * 
     * @param config
     * @throws IOException
     */
    public void initializeDSFromConfig(SecurityNamedServiceConfig namedConfig) throws IOException {
        JDBCSecurityServiceConfig config = (JDBCSecurityServiceConfig) namedConfig; 
        if (config.isJndi()) {
            String jndiName = config.getJndiName();            
            try {
                Context initialContext = new InitialContext();
                datasource = (DataSource)initialContext.lookup(jndiName);
            } catch (NamingException e) {
                throw new IOException(e);
            }
        } else {            
            BasicDataSource bds = new BasicDataSource();
            bds.setDriverClassName(config.getDriverClassName());
            bds.setUrl(config.getConnectURL());
            bds.setUsername(config.getUserName());
            bds.setPassword(config.getPassword());
            bds.setDefaultAutoCommit(false);
            bds.setDefaultTransactionIsolation(DEFAULT_ISOLATION_LEVEL);
            bds.setMaxActive(10);
            datasource=bds;
        } 
    }

    
    
    /**
     * simple getter
     * 
     * @return
     */
    protected DataSource getDataSource() {
        return datasource;
    }
    
    /**
     * Get a new connection from the datasource,
     * check/set autocommit == false and isolation level
     * according to {@link #DEFAULT_ISOLATION_LEVEL}
     * 
     * @return
     * @throws SQLException
     */
    protected Connection getConnection() throws SQLException{
        Connection con =  getDataSource().getConnection();
        if (con.getAutoCommit())
            con.setAutoCommit(false);
        if (con.getTransactionIsolation()!=DEFAULT_ISOLATION_LEVEL)
            con.setTransactionIsolation(DEFAULT_ISOLATION_LEVEL);

        
        return con;
    }
    
    /**
     * close a sql connection
     * @param con
     * @throws SQLException
     */
    protected void closeConnection(Connection con) throws SQLException{
        con.close();
    }
    
    /**
     * helper method, if any of the parametres
     * is not null, try to close it and throw
     * away a possible {@link SQLException}
     * 
     * @param con
     * @param ps
     * @param rs
     */
    protected void closeFinally(Connection con, PreparedStatement ps, ResultSet rs) {
        try {
            if (rs!=null) rs.close();            
        } catch (SQLException ex) {}
        try {
            if (ps!=null) ps.close();            
        } catch (SQLException ex) {}
        try {
            if (con!=null) closeConnection(con);            
        } catch (SQLException ex) {}                       
    }
    
    /**
     * get a prepared DML statement for a property key
     * 
     * @param key
     * @param con
     * @return
     * @throws IOException
     * @throws SQLException
     */
    protected PreparedStatement getDMLStatement (String key,Connection con) throws IOException,SQLException {
        return getJDBCStatement (key,dmlProps,con );    }
    
    
    /**
     * get a prepared Jdbc Statement by looking into the props
     * for the given key
     * 
     * 
     * @param key
     * @param props
     * @param con
     * @return
     * @throws IOException if key does not exist
     * @throws SQLException
     */
    protected PreparedStatement getJDBCStatement (String key,Properties props, Connection con) throws IOException,SQLException {
        String statementString = props.getProperty(key);
        if (statementString==null || statementString.trim().length()==0)
            throw new IOException("No sql statement for key : "+key );
        return con.prepareStatement(statementString.trim());
        
    }
    
    /**
     * get a prepared DDL statement for a property key
     * 
     * @param key
     * @param con
     * @return
     * @throws IOException
     * @throws SQLException
     */
    protected PreparedStatement getDDLStatement (String key,Connection con) throws IOException,SQLException {
        return getJDBCStatement (key,ddlProps,con );
    }


    /**
     * create a boolean from a String
     * 
     * "Y" or "y" results in true, all
     * other values result in false
     * 
     * @param booleanString
     * @return
     */
    protected boolean convertFromString(String booleanString) {
        if (booleanString==null)
            return false;
        return "y".equalsIgnoreCase(booleanString);
    }
    
    /**
     * convert boolean to string
     * true --> "Y"
     * false --> "N"
     * @param b
     * @return
     */
    protected String convertToString(boolean b) {
        return b ? "Y" : "N";        
    }

    
    /**
     * Get ordered property keys for creating
     * tables/indexes
     * 
     * @return
     */
    protected abstract String[] getOrderedNamesForCreate();
    
    /**
     * Get ordered property keys for dropping
     * tables/indexes
     * 
     * @return
     */
    protected abstract String[] getOrderedNamesForDrop();
    
    
    public void createTablesIfRequired(JDBCSecurityServiceConfig config) throws IOException{
        
        if (this.canCreateStore()==false) return;
        if (config.isCreatingTables()==false) return;
        if (tablesAlreadyCreated()) return;
        
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = datasource.getConnection();            
            if (con.getAutoCommit()==true)
                con.setAutoCommit(false);
            con = getConnection();
            for (String stmt : getOrderedNamesForCreate()) {
                ps= getDDLStatement(stmt, con);
                ps.execute();
                ps.close();
            }
            con.commit();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }        
    }
    
  
    /**
     * create tables and indexes, statement order
     * defined by {@link #getOrderedNamesForCreate()}
     * 
     * @throws IOException
     */
    public void createTables() throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            for (String stmt : getOrderedNamesForCreate()) {
                ps= getDDLStatement(stmt, con);
                ps.execute();
                ps.close();
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }        
    }

    /**
     * drops tables, statement oder defined by
     * {@link #getOrderedNamesForDrop()}
     * 
     * @throws IOException
     */
    public void dropTables() throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            for (String stmt : getOrderedNamesForDrop()) {
                ps= getDDLStatement(stmt, con);
                ps.execute();
                ps.close();
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }        
    }
    
    /**
     * drops tables, ignore SQLExceptions 
     * 
     * @throws IOException
     */
//    public void dropExistingTables() throws IOException {
//        Connection con = null;
//        PreparedStatement ps = null;
//        try {
//            con = getConnection();
//            for (String stmt : getOrderedNamesForDrop()) {
//                try {
//                    ps= getDDLStatement(stmt, con);
//                    ps.execute();
//                    ps.close();
//                } catch (SQLException ex) {
//                    // ignore
//                }
//            }
//        } catch (SQLException ex) {
//            throw new IOException(ex);
//        } finally {
//            closeFinally(con, ps, null);
//        }        
//    }


    /**
     * Check DML statements using 
     * {@link #checkSQLStatements(Properties)}
     * 
     * @throws IOException
     */

    public Map<String,SQLException> checkDMLStatements() throws IOException {
        return checkSQLStatements(dmlProps);
    }

    /**
     * Check DDL statements using 
     * {@link #checkSQLStatements(Properties)}
     * 
     * @throws IOException
     */
    public Map<String,SQLException> checkDDLStatements() throws IOException {
        return checkSQLStatements(ddlProps);
    }

    /**
     * Checks if the tables are already created
     * 
     * @param con
     * @return
     * @throws IOException
     */
    public boolean tablesAlreadyCreated() throws IOException {
        ResultSet rs=null;
        Connection con=null;
        try  {
            con=getConnection();
            DatabaseMetaData md = con.getMetaData();            
            String schemaName=null;
            String tableName = ddlProps.getProperty("check.table");
            if (tableName.contains(".")) {
                StringTokenizer tok = new StringTokenizer(tableName,".");
                schemaName=tok.nextToken();
                tableName=tok.nextToken();
            }
            // try exact match
            rs = md.getTables(null, schemaName, tableName, null);
            if (rs.next()) return true;
            
            // try with upper case letters
            rs.close();
            schemaName = schemaName==null ? null : schemaName.toUpperCase();
            tableName = tableName.toUpperCase();
            rs = md.getTables(null, schemaName, tableName, null);
            if (rs.next()) return true;
            
            // try with lower case letters
            rs.close();
            schemaName = schemaName==null ? null : schemaName.toLowerCase();
            tableName = tableName.toLowerCase();
            rs = md.getTables(null, schemaName, tableName, null);
            if (rs.next()) return true;
            
            return false;

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            try {
                if (rs!=null)
                    rs.close();
                if (con!=null)
                    closeConnection(con);
            } catch (SQLException e) {
                // do nothing
            }
        }        
    }
    
    /**
     * Checks if the sql statements contained in props
     * can be prepared against the db
     * 
     * @param props
     * @return return error protocol containing key,statement and {@link SQLException}.
     * The key is created as follows:
     * 
     * property key + "|" + statement string
     *  
     * @throws IOException
     */
    protected Map<String,SQLException> checkSQLStatements(Properties props) throws IOException {
        
        Map<String,SQLException> reportMap = new HashMap<String, SQLException>();
        Connection con = null;
        try {
            con = getConnection();
            for (Object key : props.keySet()) {
                String stmt = props.getProperty(key.toString()).trim();
                try {
                    con.prepareStatement(stmt.trim());
                } catch (SQLException ex) {
                    reportMap.put(key.toString() + "|"+stmt, ex);                
                }
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, null,null);
        }
        return reportMap;
    }
    
    @Override
    public String toString() {
        return this.getClass()+ " : "+getName();
    }

    /**
     * Check for the existence of the file, if the
     * file exists do nothing.
     * 
     * If the file does not exist, check for a template
     * file contained in the jar with the same name, 
     * if found, use it.
     * 
     * If no template was found, use the default template
     * 
     * 
     * @param fileName, target location
     * @param namedRoot, parent dir if fileName is relative
     * @param defaultResource, the standard template
     * @throws IOException
     * @return the file to use
     */
    protected File checkORCreateJDBCPropertyFile(String fileName,
            File namedRoot, String defaultResource) throws IOException {

        fileName = fileName != null ? fileName : defaultResource;
        File file = new File(fileName);
        if (file.isAbsolute()==false) 
            file = new File(namedRoot,fileName);

        if (file.exists()) 
            return file; // we are happy
        
        // try to find a template with the same name
        URL url = this.getClass().getResource(fileName);
        if (url!=null) 
            FileUtils.copyURLToFile(url, file);
        else // use the default template
            FileUtils.copyURLToFile(getClass().getResource(defaultResource), file);
        
        return file;
                            
    }
}
