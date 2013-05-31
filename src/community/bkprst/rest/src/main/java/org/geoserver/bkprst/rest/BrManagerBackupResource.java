/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst.rest;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.xpath.XPathConstants;

import org.geoserver.bkprst.BackupTask;
import org.geoserver.bkprst.BrManager;
import org.geotools.util.logging.Logging;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.SaxRepresentation;

/**
 * 
 * Resource class for backup tasks manager.
 * 
 * @author Luca Morandini lmorandini@ieee.org
 * 
 */
public class BrManagerBackupResource extends BrManagerResource {

    protected int progress;

    protected boolean includeData;

    protected boolean includeGwc;

    protected boolean includeLog;

    protected String id;

    private final static Logger LOGGER = Logging.getLogger(BrManagerBackupResource.class);

    public BrManagerBackupResource(BrManager br) {
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

        UUID taskId;

        try {
            this.extractParameters(request.getEntityAsSax());
        } catch (Exception e) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            LOGGER.log(Level.INFO,e.getLocalizedMessage(),e);
        }

        try {
            taskId = this.br
                    .addBackupTask(path, this.includeData, this.includeGwc, this.includeLog);
            response.setStatus(Status.SUCCESS_CREATED);
            response.setEntity("<id>" + taskId + "</id>", MediaType.APPLICATION_XML);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
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

        taskId = (String) request.getAttributes().get(BrManager.REST_ID);
        if(taskId==null){
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return;
        }

        try {
            BackupTask task = (BackupTask) this.br.getTask(UUID.fromString(taskId));

            if (task == null) {
                response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return;
            }

            task.stop();
            response.setStatus(Status.SUCCESS_OK);

        } catch (Exception e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
        }
    }

    /**
     * Extract parameters from request data
     * 
     * @param content
     *            XML data
     * @throws Exception
     */
    protected void extractParameters(SaxRepresentation content) throws Exception {
        super.extractParameters(content);
        this.includeData = Boolean.parseBoolean((String) content.evaluate(
                BrManager.REST_INCLUDEDATAPATH, XPathConstants.STRING));
        this.includeGwc = Boolean.parseBoolean((String) content.evaluate(
                BrManager.REST_INCLUDEGWCPATH, XPathConstants.STRING));
        this.includeLog = Boolean.parseBoolean((String) content.evaluate(
                BrManager.REST_INCLUDELOGPATH, XPathConstants.STRING));
    }
}
