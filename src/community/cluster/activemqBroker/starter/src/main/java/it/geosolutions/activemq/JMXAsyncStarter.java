package it.geosolutions.activemq;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(objectName = "bean:name=JMXAsyncStarter", description = "JMX AsyncStarter to start/monitor/dispose ActiveMQ broker", log = true, logFile = "jmx.log", persistName = "JMXAsyncStarter")
public class JMXAsyncStarter implements AsyncServiceMonitor, ServiceMonitor, SlaveMonitor{
    
    private transient final org.apache.activemq.xbean.XBeanBrokerService broker;
    private transient final ExecutorService executorService;
    
    public JMXAsyncStarter(final ExecutorService executorService, final org.apache.activemq.xbean.XBeanBrokerService broker, final boolean asynStart) throws Exception{
        this.broker=broker;
        this.executorService=executorService;
        if (asynStart){
        	this.asyncStart();
        }
    }
    
    @Override
    @org.springframework.jmx.export.annotation.ManagedAttribute(defaultValue="false", description = "is started")
    public boolean isStarted(){
        return broker.isStarted();
    }
    
    @Override
    @org.springframework.jmx.export.annotation.ManagedAttribute(description = "is slave")
    public boolean isSlave(){
        return broker.isSlave();
    }
    
    @Override
    @org.springframework.jmx.export.annotation.ManagedOperation(description = "async start")
    public void asyncStart() throws Exception{
        executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                start();
                return null;
            } 
        });
    }
    
    @Override
    @org.springframework.jmx.export.annotation.ManagedOperation(description = "async stop")
    public void asyncStop() throws Exception{
        executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                stop();
                return null;
            } 
        });
    }
    
    @Override
    @org.springframework.jmx.export.annotation.ManagedAttribute(description = "start")
    public void start() throws Exception{
            broker.start();
    }
    
    @Override
    @org.springframework.jmx.export.annotation.ManagedAttribute(description = "stop")
    public void stop() throws Exception{
        broker.stop();
    }
    
}
