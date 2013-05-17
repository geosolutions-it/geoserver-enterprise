/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.rest.BeanDelegatingRestlet;
import org.restlet.Restlet;
import org.restlet.Route;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * When enabled blocks requests for the ReST config API or OWS services. The blocked request may be
 * read or write ones, depending on the lockState of this callback.
 * 
 * @author Luca Morandini lmorandini@ieee.org
 */
public class BRLockDispatcherCallback implements ConfigurableDispatcherCallback {

    GeoServerConfigurationLock locker;

    boolean enabled;

    LockType lockType = LockType.WRITE;

    public BRLockDispatcherCallback(GeoServerConfigurationLock locker) {
        this.locker = locker;
        this.enabled = false;
    }

    /**
     * Configurable callback implementation
     */

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LockType getLockType() {
        return lockType;
    }

    public void setLockType(LockType lockType) {
        this.lockType = lockType;
    }

    /**
     * ReST callback implementation
     */

    public void init(Request request, Response response) {
        // If the lock is not enabled, does not enforce it
        if (!this.enabled) {
            return;
        }

        Method m = request.getMethod();
        LockType type;

        if ((m == Method.DELETE || m == Method.COPY || m == Method.MKCOL || m == Method.MOVE
                || m == Method.PROPPATCH || m == Method.POST || m == Method.PUT)) {
            type = LockType.WRITE;
        } else {
            type = LockType.READ;
        }

        // If the lock is enabled but it is a read request and the lock is only for write
        // requests, does not enforce the lock
        if (type == LockType.READ && this.lockType == LockType.WRITE) {
            return;
        }

        // Otherwise enforces the lock, except when a request of the backup/restore component is called
        String path= request.getResourceRef().getPath();
        // TODO: is it ok to let write requests (deletes mostly) of the backup/restore component pass ? 
        // if ( type == LockType.READ && this.lockType == LockType.READ && path.contains(BrManager.REST_MAINPATH) ) {
        if ( path.contains(BrManager.REST_MAINPATH) ) {
            return;
        }
        
        // FIXME: it returns 500 instead (GeoServer limitation ?)
        response.setStatus(Status.CLIENT_ERROR_LOCKED); 
        throw new HttpErrorCodeException(Status.CLIENT_ERROR_LOCKED.getCode());
    }

    public Operation operationDispatched(Request request, Operation operation) {
        return operation;
    }

    public void dispatched(Request request, Response response, Restlet restlet) {
        // TODO Auto-generated method stub
    }

    public void exception(Request request, Response response, Exception error) {
        // nothing to see here, move on
    }

    public void finished(Request request, Response response) {
        // TODO Auto-generated method stub
    }

    /**
     * OWS Callback implementation
     */

    /**
     * When the read lock should be enacted, throws an exception to return the
     * expected HTTP status code
     */
    public org.geoserver.ows.Request init(org.geoserver.ows.Request request) {
        
        // If the lock is not enabled or the lock is for write operations, does not enforce it
        if ( !this.enabled || this.lockType == LockType.WRITE) {
            return request;
        }

        throw new HttpErrorCodeException(Status.CLIENT_ERROR_LOCKED.getCode(), "Request rejected due to an underway backup/restore");
    }

    public Service serviceDispatched(org.geoserver.ows.Request request, Service service)
            throws ServiceException {

        return service;
    }

    public Operation operationDispatched(org.geoserver.ows.Request request, Operation operation) {
        // TODO Auto-generated method stub
        return operation;
    }

    public Object operationExecuted(org.geoserver.ows.Request request, Operation operation,
            Object result) {
        // TODO Auto-generated method stub
        return result;
    }

    public org.geoserver.ows.Response responseDispatched(org.geoserver.ows.Request request,
            Operation operation, Object result, org.geoserver.ows.Response response) {
        
      return response;
    }

    /**
     * Wicket callback implementation
     */
    
    public void finished(org.geoserver.ows.Request request) {
        // TODO Auto-generated method stub
    }

    public void onBeginRequest() {
        
        // If the lock is not enabled, does not enforce it
        if ( !this.enabled) {
            return;
        }

        throw new HttpErrorCodeException(Status.CLIENT_ERROR_LOCKED.getCode(), "Request rejected due to an underway backup/restore");
    }

    public void onAfterTargetsDetached() {
        // TODO Auto-generated method stub
        
    }

    public void onEndRequest() {
        // TODO Auto-generated method stub
    }

    public void onRequestTargetSet(IRequestTarget requestTarget) {
        // TODO Auto-generated method stub
        
    }

    public void onRuntimeException(Page page, RuntimeException error) {
        // TODO Auto-generated method stub
    }

}
