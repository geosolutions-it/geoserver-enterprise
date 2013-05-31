/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.usergroup;

import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.web.SecurityNamedServicePanelInfo;

/**
 * Extension point for user group service configuration panels.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class UserGroupServicePanelInfo 
    <C extends SecurityUserGroupServiceConfig, T extends UserGroupServicePanel<C>>
    extends SecurityNamedServicePanelInfo<C,T>{

}
