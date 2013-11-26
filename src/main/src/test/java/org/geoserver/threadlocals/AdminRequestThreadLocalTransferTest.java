package org.geoserver.threadlocals;



import java.util.concurrent.ExecutionException;

import org.geoserver.security.AdminRequest;


public class AdminRequestThreadLocalTransferTest extends AbstractThreadLocalTransferTest {
    
    @Override
    public void tearDown() throws Exception {
        AdminRequest.finish();
        super.tearDown();
    }


    public void testAdminRequest() throws InterruptedException, ExecutionException {
        // setup the state
        final Object myState = new Object();
        AdminRequest.start(myState);
        // test it's transferred properly using the base class machinery
        testThreadLocalTransfer(new ThreadLocalTransferCallable(new AdminRequestThreadLocalTransfer()) {
            
            @Override
            void assertThreadLocalCleaned() {
                assertNull(AdminRequest.get());
                
            }
            
            @Override
            void assertThreadLocalApplied() {
                assertSame(myState, AdminRequest.get());
            }
        });
    }
}