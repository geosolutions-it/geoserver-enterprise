/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.codehaus.org/
 *  Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.activemq;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class MainJMXClient {


    private final static Logger LOGGER = LoggerFactory.getLogger(MainJMXClient.class);

    /**
     * USAGE:<br>
     * java it.geosolutions.geobatch.services.jmx.MainJMXClientUtils
     * /PATH/TO/FILE.properties<br>
     * where FILE.properties is the command property file<br>
     * 
     * @param argv a String[0] containing the path of the environment used to
     *            run the action on GeoBatch
     * @throws Exception
     */
    public static void main(String[] argv) throws Exception {

        if (argv.length != 2) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Unable to run without a property file.\n" +
                		"command connection.properties <COMMAND>\n" +
                		"COMMAND:\n" +
                		"     - isStarted" +
                		"     - isStopped" +
                		"     - isSlave" +
                		"     - asyncStart" +
                		"     - asyncStop");
            }
            System.exit(9);
        }
        final String path = argv[0];
        File envFile = new File(path);
        if (!envFile.isFile()) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Unable to run without a property file, check the path: " + path);
            }
            System.exit(9);
        }

        // building the environment
        final Map<String, String> commonEnv = JMXClientUtils.loadEnv(path);
        
        try {
            
            // try using the main prop file
            initConnection(commonEnv);
            String command=argv[1];
            if (command==null || command.isEmpty())
                System.exit(3);
            boolean res=true;
            if (command.equalsIgnoreCase("asyncStart")){
                serviceMonitor.asyncStart();
            } else if (command.equalsIgnoreCase("asyncStop")){
                serviceMonitor.asyncStop();
            } else if (command.equalsIgnoreCase("isSlave")){
                res=serviceMonitor.isSlave();
            } else if (command.equalsIgnoreCase("isStarted")){
                res=serviceMonitor.isStarted();
            } else {
                System.exit(4);
            }
            if (LOGGER.isInfoEnabled())
                LOGGER.info("RESULT for command:"+command+" is "+res);
            
            if (res)
                System.exit(0);
            else
                System.exit(1);
            
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.error(e.getLocalizedMessage(), e);
            else
                LOGGER.error(e.getLocalizedMessage());
        } finally {
            dispose();
        }
        System.exit(2);
    }

    // the jmx connector
    private static JMXConnector jmxConnector = null;
    // the ActionManager's proxy
    private static AsyncStarter serviceMonitor = null;

    /**
     * 
     * @param connectionParams connection parameters
     */
    private static void initConnection(Map<String, String> connectionParams) throws Exception {
        if (connectionParams == null) {
            throw new IllegalArgumentException("Unable to run using a null environment map");
        }
        try {
            // get the connector using the configured environment
            jmxConnector = JMXClientUtils.getConnector(connectionParams);
            // create the proxy
            serviceMonitor = JMXClientUtils.getProxy(connectionParams, jmxConnector);

        } catch (Exception e) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error(e.getLocalizedMessage(), e);
            dispose();
            throw e;
        }
    }

    private static void dispose() throws IOException {
        // TODO dispose all the pending consumers?!?
        if (jmxConnector != null) {
            try {
                // close connector's connection
                jmxConnector.close();
            } catch (IOException e) {
                if (LOGGER.isErrorEnabled())
                    LOGGER.error(e.getMessage(), e);
            }
        }
    }

}
