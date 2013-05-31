/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.web.SecurityNamedServiceProvider;

/**
 * Data provider for password policy configurations.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class PasswordPolicyProvider extends SecurityNamedServiceProvider<PasswordPolicyConfig> {

    @Override
    protected List<PasswordPolicyConfig> getItems() {
        List <PasswordPolicyConfig> result = new ArrayList<PasswordPolicyConfig>();
        try {
            for (String name : getSecurityManager().listPasswordValidators()) {
                result.add((PasswordPolicyConfig) getSecurityManager().loadPasswordPolicyConfig(name));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }

}
