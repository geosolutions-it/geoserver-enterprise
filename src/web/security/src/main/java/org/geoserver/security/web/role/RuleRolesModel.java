/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.wicket.WicketRuntimeException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.web.GeoServerApplication;

public class RuleRolesModel extends RolesModel {

	private static final long serialVersionUID = 1L;

	@Override
    protected Collection<GeoServerRole> load() {
        GeoServerSecurityManager secMgr = GeoServerApplication.get().getSecurityManager();
        try {
            return new ArrayList(secMgr.getRolesForAccessControl());
        }
        catch(IOException e) {
            throw new WicketRuntimeException(e);
        }
    }

}
