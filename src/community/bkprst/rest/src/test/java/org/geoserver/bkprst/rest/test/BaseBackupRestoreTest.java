/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.bkprst.rest.test;

import java.io.File;
import java.io.StringReader;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.data.DataUtilities;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * @author User
 *
 */
public abstract class BaseBackupRestoreTest extends GeoServerTestSupport {

    protected GeoServerDataDirectory dataRoot;

    /**
     * 
     */
    public BaseBackupRestoreTest() {
        super();
    }

    public void setUpInternal() {
        this.dataRoot = this.getDataDirectory();
        try {
            this.populateDataDirectory(getTestData());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,e.getLocalizedMessage(),e);
        }
    }

    /**
     * This method waits for a task completion.
     * 
     * @param taskID the UUID for the task being followed.
     * 
     * @throws Exception
     */
    protected void waitForTaskCompletion(String taskID) throws Exception {
        waitForTaskStatus(taskID,"completed","stopped" );
    }
    
    protected void waitForTaskStatus(String taskID, String... status) throws Exception {
        final SAXBuilder builder = new SAXBuilder();
        while(true){
            MockHttpServletResponse response = getAsServletResponse("/rest/bkprst/" + taskID);
            if(response.getStatusCode() != MockHttpServletResponse.SC_OK ){
                return;
            }            
            String outputStreamContent = response.getOutputStreamContent();
            Document document = (Document) builder.build(new StringReader(outputStreamContent));
            final String taskStatus = document.getRootElement().getChildText("state");
            for(String st:status){
                if(taskStatus.equalsIgnoreCase(st)){
                    return;
                }                
            }

            Thread.sleep(2000);
        }
    }

    /**
     * Populates a mock data directory with standard data
     */
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWcs11Coverages();
        File mockDir= dataDirectory.getDataDirectoryRoot();
        
        File logs = new File(mockDir + "/logs");
        logs.mkdirs();
        new File(logs, "log.txt").createNewFile();
        File gwc = new File(mockDir + "/gwc");
        gwc.mkdirs();
        new File(gwc, "gwc.txt").createNewFile();
        File data = new File(mockDir + "/data");
        data.mkdirs();
        new File(data, "data.txt").createNewFile();
        
        FileUtils.copyFile(DataUtilities.urlToFile(this.getClass().getResource("global.xml")),  new File(mockDir, "global.xml"));
    }

}