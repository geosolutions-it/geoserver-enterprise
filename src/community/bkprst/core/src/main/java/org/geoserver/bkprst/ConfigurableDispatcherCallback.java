/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst;

import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.rest.DispatcherCallback;
import org.geoserver.web.WicketCallback;

/**
 * Interface for callbacks that can be enabled/disabled and configured further.
 * The callbacks implementing this interface are supposed to intercept any
 * call either to ReST or to OWS services, hence the double inheritance.
 * 
 * @author Luca Morandini lmorandini@ieee.org
 *
 */
public interface ConfigurableDispatcherCallback extends DispatcherCallback, 
    org.geoserver.ows.DispatcherCallback, WicketCallback {

    public boolean isEnabled();
    
    public void setEnabled(boolean enabled);

    public LockType getLockType();

    public void setLockType(LockType lockType);
}
