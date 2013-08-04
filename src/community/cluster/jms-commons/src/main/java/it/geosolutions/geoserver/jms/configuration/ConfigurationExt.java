package it.geosolutions.geoserver.jms.configuration;

public interface ConfigurationExt {
	
	public void initDefaults(Configuration config);
	
	public boolean checkForOverride(Configuration config);

}
