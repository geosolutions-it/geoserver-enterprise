/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst.test;

import java.util.UUID;
import org.geoserver.test.GeoServerAbstractTestSupport;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.bkprst.BrManager;
import org.geoserver.catalog.rest.StyleResource;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Test the BR tool locks for OWS calls
 * 
 * @author Luca Morandini lmorandini@ieee.org
 */
public class BrManagerOWSTest extends WMSTestSupport {

    private BrManager br;

    private String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
            + "&info_format=application/vnd.ogc.gml&request=GetFeatureInfo&layers=topp:states"
            + "&query_layers=topp:states&width=20&height=20&x=20&y=20";

    public void setUpInternal() throws Exception {
        super.setUpInternal();
        this.br = (BrManager) GeoServerAbstractTestSupport.applicationContext.getBean("brmanager");
    }

    public void testLockBackup() throws Exception {

        // Not blocked prior to adding a BR task
        // response= getAsServletResponse(buildRequest("WFS", "GetFeature", "GML").);
        MockHttpServletResponse response = getAsServletResponse(request);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);

        // Blocked write requests after adding a Backup task
        UUID id = this.br.addTask(new MockBackupTask(br.generateId(), "", br.getWriteLocker()));
        Thread.sleep(2000);

        // OWS are passed
        response = getAsServletResponse(request);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);
    }

    public void testLockRestore() throws Exception {
        // Not blocked prior to adding a BR task
        MockHttpServletResponse response = getAsServletResponse(request);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);

        // Blocked read and write requests after adding a Restore task
        UUID id = this.br.addTask(new MockRestoreTask(br.generateId(), "", br.getWriteLocker()));
        Thread.sleep(2000);

        // OWS requests are blocked
        // FIXME: HttpErrorCodeException sets error, not status
        response = getAsServletResponse(request);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 423);
        
        // Waits for the task to complete
        Thread.sleep(BrManagerTest.TASKDURATION + 1000);
        
        // Requests not blocked after task completion
        response = getAsServletResponse(request);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);
    }

}
