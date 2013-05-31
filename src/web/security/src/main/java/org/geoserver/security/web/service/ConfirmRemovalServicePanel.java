/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.service;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.web.AbstractConfirmRemovalPanel;

public class ConfirmRemovalServicePanel extends AbstractConfirmRemovalPanel<ServiceAccessRule> {

    private static final long serialVersionUID = 1L;

    
    public ConfirmRemovalServicePanel(String id, List<ServiceAccessRule> roots) {
        super(id, roots);        
    }
    
    public ConfirmRemovalServicePanel(String id, ServiceAccessRule... roots) {
        this(id, Arrays.asList(roots));
    }

    @Override
    protected String getConfirmationMessage(ServiceAccessRule object) throws Exception{
        return (String) BeanUtils.getProperty(object, "service") + "."
                + (String) BeanUtils.getProperty(object, "method") + "="
                + (String) BeanUtils.getProperty(object, "roles");
    }

}
