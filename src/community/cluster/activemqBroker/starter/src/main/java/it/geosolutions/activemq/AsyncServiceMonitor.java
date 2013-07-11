package it.geosolutions.activemq;

public interface AsyncServiceMonitor {
    public void asyncStart() throws Exception;
    public void asyncStop() throws Exception;
}
