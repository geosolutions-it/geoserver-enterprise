package org.geoserver.threadlocals;


import java.util.concurrent.ExecutionException;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.ows.LocalWorkspace;

public class LocalWorkspaceThreadLocalTransferTest extends AbstractThreadLocalTransferTest {

    @Override
    public void tearDown() throws Exception {
        LocalWorkspace.remove();
        super.tearDown();
    }
       
    public void testRequest() throws InterruptedException, ExecutionException {
        // setup the state
        final WorkspaceInfo ws = new WorkspaceInfoImpl();
        LocalWorkspace.set(ws);
        // test it's transferred properly using the base class machinery
        testThreadLocalTransfer(new ThreadLocalTransferCallable(new LocalWorkspaceThreadLocalTransfer()) {
            
            @Override
            void assertThreadLocalCleaned() {
                assertNull(LocalWorkspace.get());
            }
            
            @Override
            void assertThreadLocalApplied() {
                assertSame(ws, LocalWorkspace.get());
            }
        });
    }
}