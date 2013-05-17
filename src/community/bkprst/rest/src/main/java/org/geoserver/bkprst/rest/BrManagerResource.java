/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.bkprst.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.xpath.XPathConstants;

import org.geoserver.bkprst.BackupTask;
import org.geoserver.bkprst.BrManager;
import org.geoserver.bkprst.BrTask;
import org.geoserver.bkprst.RestoreTask;
import org.geotools.util.logging.Logging;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;
import org.restlet.resource.SaxRepresentation;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * Resource class for backup/restore tasks manager.
 * 
 * @author Luca Morandini lmorandini@ieee.org
 * 
 */
public class BrManagerResource extends Resource {

    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog.rest");

    protected BrManager br;

    protected String path;

    public BrManagerResource(BrManager br) {
        super();
        this.br = br;
    }

    @Override
    public void handleGet() {

        Request request = getRequest();
        Response response = getResponse();

        String xml = this.br.toXML(this.br);

        response.setEntity(xml, MediaType.APPLICATION_XML);
        response.setStatus(Status.SUCCESS_OK);
    }

    /**
     * Extract parameters from request data
     * 
     * @param content XML data 
     * @throws Exception
     */
    protected void extractParameters(SaxRepresentation content) throws Exception {
        this.path = (String) (content.evaluate(BrManager.REST_TASKPATH,
            XPathConstants.STRING));
    }
}