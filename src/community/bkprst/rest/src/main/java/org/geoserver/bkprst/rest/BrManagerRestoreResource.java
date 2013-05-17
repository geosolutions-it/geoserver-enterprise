/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst.rest;

import java.util.UUID;
import java.util.logging.Level;

import org.geoserver.bkprst.BackupTask;
import org.geoserver.bkprst.BrManager;
import org.geoserver.bkprst.RestoreTask;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * 
 * Resource class for restore tasks manager.
 * 
 * @author Luca Morandini lmorandini@ieee.org
 * 
 */
public class BrManagerRestoreResource extends BrManagerResource {

    public BrManagerRestoreResource(BrManager br) {
        super(br);
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public void handlePost() {

        Request request = getRequest();
        Response response = getResponse();

        try {
            UUID taskId;
            this.extractParameters(request.getEntityAsSax());
            taskId = this.br.addRestoreTask(path);
            response.setStatus(Status.SUCCESS_CREATED);
            response.setEntity("<id>" + taskId + "</id>", MediaType.APPLICATION_XML);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
        }

    }

    @Override
    public boolean allowDelete() {
        return true;
    }

    @Override
    public void handleDelete() {

        Request request = getRequest();
        Response response = getResponse();
        String taskId;

        try {
            taskId = (String) request.getAttributes().get(BrManager.REST_ID);
        } catch (Exception e) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return;
        }

        try {
            RestoreTask task = (RestoreTask) this.br.getTask(UUID.fromString(taskId));

            if (task == null) {
                response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return;
            }

            task.stop();
            response.setStatus(Status.SUCCESS_OK);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
        }
    }

}
