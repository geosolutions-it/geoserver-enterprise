/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.test;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.geoserver.data.util.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.ui.context.Theme;
import org.springframework.web.context.WebApplicationContext;

/**
 * A spring application context used for GeoServer testing.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class GeoServerTestApplicationContext extends ClassPathXmlApplicationContext
    implements WebApplicationContext {
    ServletContext servletContext;

    boolean useLegacyGeoServerLoader = true;
    
    public GeoServerTestApplicationContext(String configLocation, ServletContext servletContext)
        throws BeansException {
        this(new String[] { configLocation }, servletContext);
    }

    public GeoServerTestApplicationContext(String[] configLocation, ServletContext servletContext)
        throws BeansException {
        super(configLocation, false);
        try {
            servletContext.setAttribute(
                "javax.servlet.context.tempdir", 
                IOUtils.createRandomDirectory("./target", "mock", "tmp")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.servletContext = servletContext;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public Theme getTheme(String themeName) {
        return null;
    }
    
    public void setUseLegacyGeoServerLoader(boolean useLegacyGeoServerLoader) {
        this.useLegacyGeoServerLoader = useLegacyGeoServerLoader;
    }
    
    /*
     * JD: Overriding manually and playing with bean definitions. We do this
     * because we have not ported all the mock test data to a 2.x style configuration
     * directory, so we need to force the legacy data directory loader to engage.
     */
    protected void loadBeanDefinitions(XmlBeanDefinitionReader reader)
            throws BeansException, IOException {
        super.loadBeanDefinitions(reader);
        
        if (useLegacyGeoServerLoader) {
            BeanDefinition def = reader.getBeanFactory().getBeanDefinition("geoServerLoader");
            def.setBeanClassName( "org.geoserver.test.TestGeoServerLoaderProxy");
        }
    }
}
