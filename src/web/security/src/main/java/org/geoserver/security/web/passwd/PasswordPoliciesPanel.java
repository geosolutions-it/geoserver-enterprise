/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geoserver.security.web.SecurityNamedServicesPanel;

/**
 * Panel for providing list of password policy configurations.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class PasswordPoliciesPanel extends SecurityNamedServicesPanel<PasswordPolicyConfig> {

    public PasswordPoliciesPanel(String id) {
        super(id, new PasswordPolicyProvider());
    }

    @Override
    protected Class getServiceClass() {
        return PasswordValidator.class;
    }

    @Override
    public void validateRemoveConfig(PasswordPolicyConfig config)
            throws SecurityConfigException {
        SecurityConfigValidator.getConfigurationValiator(PasswordValidator.class, config.getClassName())
            .validateRemovePasswordPolicy(config);
    }

    @Override
    public void removeConfig(PasswordPolicyConfig config) throws Exception {
        getSecurityManager().removePasswordValidator(config);
    }
}