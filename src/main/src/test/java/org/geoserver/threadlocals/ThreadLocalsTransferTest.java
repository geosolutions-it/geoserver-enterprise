package org.geoserver.threadlocals;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalLayer;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.security.AdminRequest;
import org.geoserver.test.GeoServerTestSupport;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class ThreadLocalsTransferTest extends GeoServerTestSupport {

    protected ExecutorService executor;

    
    @Override
    public void setUpInternal() throws Exception {
        super.setUpInternal();
        executor = Executors.newCachedThreadPool();
    }

    @Override
    public void tearDownInternal() throws Exception {
        executor.shutdown();
        Dispatcher.REQUEST.remove();
        AdminRequest.finish();
        LocalLayer.remove();
        LocalWorkspace.remove();
        SecurityContextHolder.getContext().setAuthentication(null);
        super.tearDownInternal();
    }
    
       public void testThreadLocalTransfer() throws InterruptedException, ExecutionException {
        final Request request = new Request();
        Dispatcher.REQUEST.set(request);
        final LayerInfo layer = new LayerInfoImpl();
        LocalLayer.set(layer);
        final WorkspaceInfo ws = new WorkspaceInfoImpl();
        LocalWorkspace.set(ws);
        final Object myState = new Object();
        AdminRequest.start(myState);
        final Authentication auth = new UsernamePasswordAuthenticationToken("user", "password");
        SecurityContextHolder.getContext().setAuthentication(auth);
        final ThreadLocalsTransfer transfer = new ThreadLocalsTransfer();
        Future<Void> future = executor.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                testApply();
                testCleanup();
                return null;
            }

            private void testApply() {
                transfer.apply();

                // check all thread locals have been applied to the current thread
                assertSame(request, Dispatcher.REQUEST.get());
                assertSame(myState, AdminRequest.get());
                assertSame(layer, LocalLayer.get());
                assertSame(ws, LocalWorkspace.get());
                assertSame(auth, SecurityContextHolder.getContext().getAuthentication());
            }

            private void testCleanup() {
                transfer.cleanup();

                // check all thread locals have been cleaned up from the current thread
                assertNull(Dispatcher.REQUEST.get());
                assertNull(AdminRequest.get());
                assertNull(LocalLayer.get());
                assertNull(LocalWorkspace.get());
                assertNull(SecurityContextHolder.getContext().getAuthentication());
            }

        });
        future.get();
    }

    protected abstract static class ThreadLocalTransferCallable implements Callable<Void> {

        Thread originalThread;

        ThreadLocalTransfer transfer;

        Map<String, Object> storage = new HashMap<String, Object>();

        public ThreadLocalTransferCallable(ThreadLocalTransfer transfer) {
            this.originalThread = Thread.currentThread();
            this.transfer = transfer;
            this.transfer.collect(storage);
        }

        @Override
        public Void call() throws Exception {
            // this is the the main thread, we are actually running inside the thread pool
            assertNotSame(originalThread, Thread.currentThread());

            // apply the thread local, check it has been applied correctly
            transfer.apply(storage);
            assertThreadLocalApplied();

            // clean up, check the therad local is now empty
            transfer.cleanup();
            assertThreadLocalCleaned();

            return null;
        }

        abstract void assertThreadLocalCleaned();

        abstract void assertThreadLocalApplied();

    };
}