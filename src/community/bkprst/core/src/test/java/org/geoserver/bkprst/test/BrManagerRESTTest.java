/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst.test;

import java.util.UUID;

import org.geoserver.bkprst.BrManager;
import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.catalog.rest.StyleResource;
import org.geoserver.test.GeoServerAbstractTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Test for the BR tool core funciontalites and the locking of ReST calls
 * 
 * @author Luca Morandini lmorandini@ieee.org
 */
public class BrManagerRESTTest extends CatalogRESTTestSupport {

    private BrManager br;
    
    private String base = "/rest/styles";
    
    public void setUpInternal() {
        this.br = (BrManager) GeoServerAbstractTestSupport.applicationContext.getBean("brmanager");
    }
    
    protected String getLogConfiguration() {
        return "/GEOSERVER_DEVELOPER_LOGGING.properties";
    }
    
    String newSLDXML() {
        return 
             "<sld:StyledLayerDescriptor xmlns:sld='http://www.opengis.net/sld'>"+
                "<sld:NamedLayer>"+
                "<sld:Name>foo</sld:Name>"+
                "<sld:UserStyle>"+
                  "<sld:Name>foo</sld:Name>"+
                  "<sld:FeatureTypeStyle>"+
                     "<sld:Name>foo</sld:Name>"+
                  "</sld:FeatureTypeStyle>" + 
                "</sld:UserStyle>" + 
              "</sld:NamedLayer>" + 
            "</sld:StyledLayerDescriptor>";
    }
    
    public void testLockBackup() {
        try {
            doLogin();
            // Requests not blocked prior to adding a BR task
            MockHttpServletResponse response = getAsServletResponse(base, null);
            
            assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);
            response = postAsServletResponse(base, newSLDXML(), StyleResource.MEDIATYPE_SLD.toString());
            assertEquals(201, response.getStatusCode());

            // Blocked write requests after adding a Backup task
            UUID id = this.br.addTask(new MockBackupTask(br.generateId(), BrManagerTest.path, br.getWriteLocker()));
            Thread.sleep(2000);

            // GETs are passed
            response = getAsServletResponse(base, null);
            assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);

            // POSTs, PUTs and DELETEs are blocked
            response = postAsServletResponse(base, newSLDXML(), StyleResource.MEDIATYPE_SLD.toString());
            // FIXME: should be 423
            assertTrue(response.getStatusCode() == 500 && response.getErrorCode() == 200);

            // Waits for the task to complete
            Thread.sleep(BrManagerTest.TASKDURATION + 1000);
            
            // Requests not blocked after task completion
            response = getAsServletResponse(base, null);
            
            assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);
            response = deleteAsServletResponse(base + "/foo");
            assertEquals(200, response.getStatusCode());
        } catch (Exception e) {
            assertTrue(false);
        }
    }

    public void testLockRestore() {
        try {
            doLogin();
            // Not blocked prior to adding a BR task
            MockHttpServletResponse response = getAsServletResponse(base, null);
            assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);
            response = postAsServletResponse(base, newSLDXML(), StyleResource.MEDIATYPE_SLD.toString());
            assertEquals(201, response.getStatusCode());

            // Blocked read and write requests after adding a Restore task
            UUID id = this.br.addTask(new MockRestoreTask(br.generateId(), BrManagerTest.path, br.getWriteLocker()));
            Thread.sleep(2000);

            // Generic GETs are blocked
            response = getAsServletResponse(base, null);
            // FIXME: should be 423
            assertTrue(response.getStatusCode() == 500 && response.getErrorCode() == 200);

            // POSTs, PUTs and DELETEs are blocked
            response = postAsServletResponse(base, newSLDXML(), StyleResource.MEDIATYPE_SLD.toString());
            // FIXME: should be 423
            assertTrue(response.getStatusCode() == 500 && response.getErrorCode() == 200);

            // Waits for the task to complete
            Thread.sleep(BrManagerTest.TASKDURATION + 1000);
            
            // Requests not blocked after task completion
            response = getAsServletResponse(base, null);
            
            assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);
            response = deleteAsServletResponse(base + "/foo");
            assertEquals(200, response.getStatusCode());
        } catch (Exception e) {
            assertTrue(false);
        }
    }
}
