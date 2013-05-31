/*
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.bkprst.rest;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.bkprst.BrManager;
import org.geoserver.bkprst.BrTask;
import org.geotools.util.logging.Logging;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * Resource class for single backup/restore tasks
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 */
public class BrTaskResource extends Resource {

    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog.rest");

    protected BrManager br;

    protected XStream xstream;

    public BrTaskResource(BrManager br) {
        this.br = br;
        this.xstream = new XStream();

    }

    @Override
    public void handleGet() {
        final Request request = getRequest();
        final Map<String, Object> attributes = request.getAttributes();
        final Response response = getResponse();
        String taskId = (String) attributes.get(BrManager.REST_ID);
        if(taskId==null){

            LOGGER.log(Level.SEVERE,"Unable to locate the TaskID in the parsed request");
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return;
        }

        try {
            BrTask task = this.br.getTask(UUID.fromString(taskId));
            if (task != null) {
                response.setStatus(Status.SUCCESS_OK);
                response.setEntity(this.br.toXML(task), MediaType.APPLICATION_XML);
            } else {
                response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
        }

    }
}