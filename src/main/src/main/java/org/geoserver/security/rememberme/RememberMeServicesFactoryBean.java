/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.rememberme;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;

/**
 * Factory bean that proxies for the global remember me service.
 * <p>
 * The actual underlying rememberme service is determined by 
 *   {@link RememberMeServicesConfig#getClassName()}, obtained from 
 *   {@link GeoServerSecurityManager#getSecurityConfig()}.
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class RememberMeServicesFactoryBean implements FactoryBean<RememberMeServices> {

    GeoServerSecurityManager securityManager;

    public RememberMeServicesFactoryBean(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public RememberMeServices getObject() throws Exception {
        //we return a proxy for the rms, one that instantiates the underlying rms lazily on demand
        // we do this to avoid trigging the security manager configuration from being loaded
        // during startup, before the app context is fully loaded
        return new RememberMeServicesProxy(securityManager);
    }

    @Override
    public Class<?> getObjectType() {
        return RememberMeServices.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    static class RememberMeServicesProxy implements RememberMeServices, LogoutHandler {

        GeoServerSecurityManager securityManager;
        RememberMeServices rms;

        RememberMeServicesProxy(GeoServerSecurityManager securityManager) {
            this.securityManager = securityManager;
        }

        @Override
        public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
            return rms().autoLogin(request, response);
        }

        @Override
        public void loginFail(HttpServletRequest request, HttpServletResponse response) {
            rms().loginFail(request, response);
        }

        @Override
        public void loginSuccess(HttpServletRequest request, HttpServletResponse response,
                Authentication successfulAuthentication) {
            rms().loginSuccess(request, response, successfulAuthentication);
        }

        @Override
        public void logout(HttpServletRequest request, HttpServletResponse response,
                Authentication authentication) {
            RememberMeServices rms = rms();
            if (rms instanceof LogoutHandler) {
                ((LogoutHandler)rms).logout(request, response, authentication);
            }
        }

        RememberMeServices rms() {
            if (rms != null) {
                return rms;
            }

            RememberMeServicesConfig rmsConfig = securityManager.getSecurityConfig().getRememberMeService();
            try {
                rms = (RememberMeServices) Class.forName(rmsConfig.getClassName()).newInstance();
            }
            catch(Exception e) {
                throw new RuntimeException(e);
            }

            if (rms instanceof AbstractRememberMeServices) {
                AbstractRememberMeServices arms = (AbstractRememberMeServices) rms; 
                arms.setUserDetailsService(new RememberMeUserDetailsService(securityManager));
                arms.setKey(rmsConfig.getKey());
            }
//            if (rms instanceof GeoServerTokenBasedRememberMeServices) {
//                ((GeoServerTokenBasedRememberMeServices) rms).setUserGroupServiceName(rmsConfig.getUserGroupService());
//            }
            return rms;
        }
    }
}
