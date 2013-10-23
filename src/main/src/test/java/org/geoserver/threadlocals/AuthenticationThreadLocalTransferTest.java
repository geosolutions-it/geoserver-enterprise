package org.geoserver.threadlocals;


import java.util.concurrent.ExecutionException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthenticationThreadLocalTransferTest extends AbstractThreadLocalTransferTest {

    @Override
    public void tearDown() throws Exception{
        SecurityContextHolder.getContext().setAuthentication(null);
        super.tearDown();
    }

        
    public void testRequest() throws InterruptedException, ExecutionException {
        // setup the state
        final Authentication auth = new UsernamePasswordAuthenticationToken("user", "password");
        SecurityContextHolder.getContext().setAuthentication(auth);
        // test it's transferred properly using the base class machinery
        testThreadLocalTransfer(new ThreadLocalTransferCallable(new AuthenticationThreadLocalTransfer()) {
            
            @Override
            void assertThreadLocalCleaned() {
                assertNull(SecurityContextHolder.getContext().getAuthentication());
            }
            
            @Override
            void assertThreadLocalApplied() {
                assertSame(auth, SecurityContextHolder.getContext().getAuthentication());
            }
        });
    }
}