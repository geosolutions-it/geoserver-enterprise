package it.geosolutions.geoserver.jms.impl;

import java.net.MalformedURLException;

import org.apache.activemq.spring.Utils;
import org.apache.activemq.xbean.XBeanBrokerFactory;
import org.apache.xbean.spring.context.ResourceXmlApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

public class JMSXBeanBrokerFactory extends XBeanBrokerFactory implements ApplicationContextAware {
	
        private ApplicationContext context;
	

	protected ApplicationContext createApplicationContext(String uri) throws MalformedURLException {
        Resource resource = Utils.resourceFromString(uri);
        try {
            return new ResourceXmlApplicationContext(resource,context) {
                @Override
                protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
                    reader.setValidating(isValidate());
                }
            };
        } catch (FatalBeanException errorToLog) {
            throw errorToLog;
        }
    }

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context=applicationContext;
	}
}
