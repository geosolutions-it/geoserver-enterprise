package org.geoserver.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Request;

public class ControlFlowCallbackTest extends TestCase {

    public void testBasicFunctionality() {
        ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        CountingController controller = new CountingController(1, 0);
        tc.controllers.add(controller);
        callback.configurator = tc;
        
        callback.operationDispatched(null, null);
        assertEquals(1, controller.requestIncomingCalls);
        assertEquals(0, controller.requestCompleteCalls);
        
        callback.finished(null);
        assertEquals(1, controller.requestIncomingCalls);
        assertEquals(1, controller.requestCompleteCalls);
    }
    
    public void testTimeout() {
        ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        tc.timeout = 300;
        CountingController c1 = new CountingController(2, 200);
        CountingController c2 = new CountingController(1, 200);
        tc.controllers.add(c1);
        tc.controllers.add(c2);
        callback.configurator = tc;
        
        try {
            callback.operationDispatched(null, null);
            fail("A HTTP 503 should have been raised!");
        } catch(HttpErrorCodeException e) {
            assertEquals(503, e.getErrorCode());
        }
        assertEquals(1, c1.requestIncomingCalls);
        assertEquals(0, c1.requestCompleteCalls);
        assertEquals(1, c2.requestIncomingCalls);
        assertEquals(0, c1.requestCompleteCalls);
        callback.finished(null);
    }

    /**
     * A wide open configurator to be used for testing
     */
    static class TestingConfigurator implements ControlFlowConfigurator {
        List<FlowController> controllers = new ArrayList<FlowController>();
        long timeout;
        boolean stale = true;

        public Collection<FlowController> buildFlowControllers() throws Exception {
            stale = false;
            return controllers;
        }

        public long getTimeout() {
            return timeout;
        }

        public boolean isStale() {
            return stale;
        }

    }
    
    /**
     * A controller counting requests, can also be used to check for timeouts
     */
    static class CountingController implements FlowController {
        
        int priority;
        long delay;
        int requestCompleteCalls;
        int requestIncomingCalls;
        

        public CountingController(int priority, long delay) {
            this.priority = priority;
            this.delay = delay;
        }

        public int getPriority() {
            return priority;
        }

        public void requestComplete(Request request) {
            requestCompleteCalls++;            
        }

        public boolean requestIncoming(Request request, long timeout) {
            requestIncomingCalls++;
            if(delay > 0)
                if(timeout > delay) {
                    try {
                        Thread.sleep(delay);
                    } catch(InterruptedException e) {
                        throw new RuntimeException("This is unexpected"); 
                    }
                } else {
                    return false;
                }
            return true;
        }
        
    }
}
