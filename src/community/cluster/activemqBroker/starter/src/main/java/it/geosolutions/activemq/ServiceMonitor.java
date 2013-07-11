package it.geosolutions.activemq;

public interface ServiceMonitor {
    public boolean isStarted();
    public void start()throws Exception;
    public void stop()throws Exception;
}
