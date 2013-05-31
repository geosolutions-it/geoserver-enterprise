package org.geoserver.config;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A proxy for {@link GeoServerLoader} that loads the actual loader instance based on 
 * the spring context.
 * <p>
 * This method will first attempt to lookup an instance of {@link GeoServerLoader} in the spring
 * context and if none found will fall back on {@link DefaultGeoServerLoader}.
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class GeoServerLoaderProxy implements BeanPostProcessor, DisposableBean, ApplicationContextAware {

    /**
     * resource loader
     */
    protected GeoServerResourceLoader resourceLoader;
    
    /**
     * the actual loader
     */
    GeoServerLoader loader;
    
    public GeoServerLoaderProxy(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.loader = lookupGeoServerLoader(applicationContext);
        loader.setApplicationContext(applicationContext);
    }
    
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        if (loader != null) {
            return loader.postProcessAfterInitialization(bean, beanName);
        }
        return bean;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        if (loader != null) {
            return loader.postProcessBeforeInitialization(bean, beanName);
        }
        return bean;
    }

    public void reload() throws Exception {
        if (loader != null) {
            loader.reload();
        }
    }
    public void destroy() throws Exception {
        if (loader != null) {
            loader.destroy();
        }
    }
    
    protected GeoServerLoader lookupGeoServerLoader(ApplicationContext appContext) {
        GeoServerLoader loader = GeoServerExtensions.bean(GeoServerLoader.class, appContext);
        if (loader == null) {
            loader = new DefaultGeoServerLoader(resourceLoader);
        }
        return loader;
    }


}
