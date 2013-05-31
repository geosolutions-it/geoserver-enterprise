/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.rest;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.restlet.Restlet;
import org.restlet.Route;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Protects catalog access from concurrent rest configuration calls. Will lock in write mode every
 * call modifying catalog resources, in read mode all others catalog resource related calls, no
 * locks will be performed on other rest requests.
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class RestConfigurationLockCallback implements DispatcherCallback {

    GeoServerConfigurationLock locker;

    static ThreadLocal<LockType> THREAD_LOCK = new ThreadLocal<GeoServerConfigurationLock.LockType>();

    public RestConfigurationLockCallback(GeoServerConfigurationLock locker) {
        this.locker = locker;
    }

    @Override
    public void init(Request request, Response response) {
        LockType type = THREAD_LOCK.get();
        if (type != null) {
            throw new RuntimeException("The previous lock was not released: " + type);
        }
    }

    @Override
    public void dispatched(Request request, Response response, Restlet restlet) {
        Method m = request.getMethod();

        if (restlet instanceof Route) {
            restlet = ((Route) restlet).getNext();
        }

        if (restlet instanceof BeanDelegatingRestlet) {
            restlet = ((BeanDelegatingRestlet) restlet).getBean();
        }

        if (restlet != null) {
            // these are the restlets we have to lock
            if (restlet.getClass().getPackage().getName().startsWith("org.geoserver.catalog.rest")) {
                // choose a lok type based on the method
                LockType type;
                if ((m == Method.DELETE || m == Method.COPY || m == Method.MKCOL
                        || m == Method.MOVE || m == Method.PROPPATCH || m == Method.POST || m == Method.PUT)) {
                    type = LockType.WRITE;
                } else {
                    type = LockType.READ;
                }
                THREAD_LOCK.set(type);
                locker.lock(type);
            }
        }

    }

    @Override
    public void exception(Request request, Response response, Exception error) {
        // nothing to see here, move on
    }

    @Override
    public void finished(Request request, Response response) {
        LockType type = THREAD_LOCK.get();
        if (type != null) {
            THREAD_LOCK.remove();
            locker.unlock(type);
        }

    }

}
