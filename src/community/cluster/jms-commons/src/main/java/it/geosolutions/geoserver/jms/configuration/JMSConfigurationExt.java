package it.geosolutions.geoserver.jms.configuration;

import java.io.IOException;

public interface JMSConfigurationExt {

	public void initDefaults(JMSConfiguration config) throws IOException;

	public boolean checkForOverride(JMSConfiguration config) throws IOException;

}
