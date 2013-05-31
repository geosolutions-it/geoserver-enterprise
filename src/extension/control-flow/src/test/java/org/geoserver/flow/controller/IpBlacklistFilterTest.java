package org.geoserver.flow.controller;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.geoserver.test.GeoServerTestSupport;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class IpBlacklistFilterTest extends GeoServerTestSupport {

    public void testFilterIp() throws IOException, ServletException {
        Properties props = new Properties();
        props.put("ip.blacklist", "192.168.1.8,192.168.1.10");
        IpBlacklistFilter filter = new IpBlacklistFilter(props);
        assertNotNull(filter);
        MockFilterChain filterChain = new MockFilterChain();
        filterChain.addFilter(filter);
        TestServlet testServlet = new TestServlet();
        filterChain.setServlet(testServlet);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filterChain.doFilter(request, response);
        assertFalse(testServlet.wasServiceCalled());
        testServlet.reset();
        request.setRemoteAddr("192.168.1.9");
        filterChain.doFilter(request, response);
        assertTrue(testServlet.wasServiceCalled());
        testServlet.reset();
        request.setRemoteAddr("192.168.1.10");
        filterChain.doFilter(request, response);
        assertFalse(testServlet.wasServiceCalled());
        testServlet.reset();
    }

    static class TestServlet extends HttpServlet {
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        private boolean serviceCalled = false;

        public void service(ServletRequest request, ServletResponse response)
                throws ServletException, IOException {
            serviceCalled = true;
        }

        public void reset() {
            serviceCalled = false;
        }

        public boolean wasServiceCalled() {
            return serviceCalled;
        }
    }

}
