package org.geoserver.flow.controller;

import org.geoserver.flow.FlowController;
import org.geoserver.ows.Request;

public class FlowControllerTestingThread extends Thread {
    enum ThreadState {STARTED, TIMED_OUT, PROCESSING, COMPLETE}; 
    
    FlowController[] controllers;
    boolean proceed;
    Request request;
    long timeout;
    long processingDelay;
    ThreadState state;
    Throwable error;
    
    
    public FlowControllerTestingThread(Request request, long timeout, long processingDelay, FlowController... controllers) {
        this.controllers = controllers;
        this.request = request;
        this.timeout = timeout;
        this.processingDelay = processingDelay;
    }
    
    @Override
    public void run() {
        state = ThreadState.STARTED;
        try {
            System.out.println(this + " calling requestIncoming");
            for (FlowController controller : controllers) {
                if(!controller.requestIncoming(request, timeout)) {
                    state = ThreadState.TIMED_OUT;
                    return;
                }
            }
        } catch(Throwable t) {
            this.error = t;
        }
        state = ThreadState.PROCESSING;
        
        try {
            System.out.println(this + " waiting");
            if(processingDelay > 0)
                sleep(processingDelay);
        } catch(InterruptedException e) {
            System.out.println(e.getLocalizedMessage());
            Thread.currentThread().interrupt();
        }
        
        try {
            System.out.println(this + " calling requestComplete");
            for (FlowController controller : controllers) {
                controller.requestComplete(request);
            }
        } catch(Throwable t) {
            this.error = t;
        }
        state = ThreadState.COMPLETE;
        System.out.println(this + " done");
    }
}
