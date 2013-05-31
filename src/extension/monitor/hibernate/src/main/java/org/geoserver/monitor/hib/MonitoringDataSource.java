/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.hib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorConfig;
import org.geotools.util.logging.Logging;

public class MonitoringDataSource extends BasicDataSource {

    static Logger LOGGER = Logging.getLogger(Monitor.class);

    MonitorConfig config;
    GeoServerDataDirectory dataDirectory;

    public void setConfig(MonitorConfig config) {
        this.config = config;
    }
    
    public void setDataDirectory(GeoServerDataDirectory dataDir) {
        this.dataDirectory = dataDir;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            if(getDriverClassName() == null) {
                synchronized(this) {
                    if (getDriverClassName() == null) {
                        initializeDataSource();
                    }
                }
            }
            return super.getConnection();
        }
        catch(Exception e) {
            //LOGGER.log(Level.WARNING, "Database connection error", e);
            config.setError(e);
            config.setEnabled(false);
            
            if (e instanceof SQLException) {
                throw (SQLException) e;
            }
            
            throw (SQLException) new SQLException().initCause(e);
        }
    }

    void initializeDataSource() throws Exception {
        File monitoringDir = dataDirectory.findOrCreateDir("monitoring");
        File dbprops = new File(monitoringDir, "db.properties");
        if (dbprops.exists()) {
            LOGGER.info("Configuring monitoring database from: " + dbprops.getAbsolutePath());

            //attempt to configure
            try {
                configureDataSource(dbprops, monitoringDir);
            }
            catch(SQLException e) {
                //configure failed, try db1.properties
                dbprops = new File(monitoringDir, "db1.properties");
                if (dbprops.exists()) {
                    try {
                        configureDataSource(dbprops, monitoringDir);
                        
                        //secondary file worked, return
                        return;
                    }
                    catch(SQLException e1) {
                        //secondary file failed as well, try for third
                        dbprops = new File(monitoringDir, "db2.properties");
                        if (dbprops.exists()) {
                            try {
                                configureDataSource(dbprops, monitoringDir);
                                
                                //third file worked, return
                                return;
                            }
                            catch(SQLException e2) {}
                        }
                    }
                }
                
                throw e;
            }
        }
        else {
            //no db.properties file, use internal default
            configureDataSource(null, monitoringDir);
        }
    }
    
    void configureDataSource(File dbprops, File monitoringDir) throws Exception {
        Properties db = new Properties();

        if (dbprops == null) {
            dbprops = new File(monitoringDir, "db.properties");
            
            //use a default, and copy the template over
            InputStream in = getClass().getResourceAsStream("db.properties");
            IOUtils.copy(in, new FileOutputStream(dbprops));
            
            db.load(getClass().getResourceAsStream("db.properties"));
        }
        else {
            FileInputStream in = new FileInputStream(dbprops);
            db.load(in);
            in.close();
        }

        logDbProperties(db);

        //TODO: check for nulls
        setDriverClassName(db.getProperty("driver"));
        setUrl(getURL(db));
        
        if (db.containsKey("username")) {
            setUsername(db.getProperty("username"));
        }
        if (db.containsKey("password")) {
            setPassword(db.getProperty("password"));
        }
        
        setDefaultAutoCommit(Boolean.valueOf(db.getProperty("defaultAutoCommit", "true")));
        
        //TODO: make other parameters configurable
        setMinIdle(1);
        setMaxActive(4);
        
        //test the connection
        super.getConnection();
    }
    
    void logDbProperties(Properties db) {
        if (LOGGER.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer("Monitoring database connection info:\n");
            for (Map.Entry e : db.entrySet()) {
                sb.append(e.getKey()).append(" = ").append(e.getValue()).append("\n"); 
            }
            LOGGER.fine(sb.toString());
        }
    }

    String getURL(Properties db) {
        return db.getProperty("url").replace("${GEOSERVER_DATA_DIR}", dataDirectory.root().getAbsolutePath());
    }
}
