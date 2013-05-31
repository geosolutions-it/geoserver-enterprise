/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.apache.xpath.XPathAPI;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockServletOutputStream;


public class DefaultServiceExceptionHandlerTest extends TestCase {
    
    private DefaultServiceExceptionHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Request requestInfo;

    protected void setUp() throws Exception {
        super.setUp();
        
        HelloWorld helloWorld = new HelloWorld();
        Service service = new Service("hello", helloWorld, new Version("1.0.0"),Collections.singletonList("hello"));

        request = new MockHttpServletRequest() {
                public int getServerPort() {
                    return 8080;
                }
            };

        request.setScheme("http");
        request.setServerName("localhost");

        request.setContextPath("geoserver");

        MockServletOutputStream output = new MockServletOutputStream();
        response = new MockHttpServletResponse();

        handler = new DefaultServiceExceptionHandler();
        
        requestInfo = new Request();
        requestInfo.setHttpRequest(request);
        requestInfo.setHttpResponse(response);
        requestInfo.setService("hello");
        requestInfo.setVersion("1.0.0");
    }

    public void testHandleServiceException() throws Exception {
        ServiceException exception = new ServiceException("hello service exception");
        exception.setCode("helloCode");
        exception.setLocator("helloLocator");
        exception.getExceptionText().add("helloText");
        handler.handleServiceException(exception, requestInfo);

        InputStream input = new ByteArrayInputStream(response.getOutputStreamContent().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);

        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }

    public void testHandleServiceExceptionEncoding() throws Exception {
        String message = "foo & <foo> \"foo's\"";
        
        ServiceException exception = new ServiceException(message);
        exception.setLocator("test-locator");

        handler.handleServiceException(exception, requestInfo);

        InputStream input = new ByteArrayInputStream(response.getOutputStreamContent().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);
        
        Node exceptionText = XPathAPI.selectSingleNode(doc, "ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()");
        assertNotNull(exceptionText);
        assertEquals("round-tripped through character entities", message, exceptionText.getTextContent());
    }
    
    @SuppressWarnings("unchecked")
    public void testHandleServiceExceptionEncodingMore() throws Exception {
        String message1 = "foo & <foo> \"foo's\"";
        String message2 = "a \"different\" <message>";

        ServiceException exception = new ServiceException(message1);
        exception.setLocator("test-locator");
        exception.getExceptionText().add(message2);

        handler.handleServiceException(exception, requestInfo);

        InputStream input = new ByteArrayInputStream(response.getOutputStreamContent().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);
        
        Node exceptionText = XPathAPI.selectSingleNode(doc, "ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()");
        assertNotNull(exceptionText);
        String message = message1 + "\n" + message2;
        assertEquals("round-tripped through character entities", message, exceptionText.getTextContent());
    }

    public void testHandleServiceExceptionCauses() throws Exception {
        // create a stack of three exceptions
        IllegalArgumentException illegalArgument = new IllegalArgumentException("Illegal argument here");
        IOException ioException = new IOException("I/O exception here");
        ioException.initCause(illegalArgument);
        ServiceException serviceException = new ServiceException("hello service exception");
        serviceException.setCode("helloCode");
        serviceException.setLocator("helloLocator");
        serviceException.getExceptionText().add("helloText");
        serviceException.initCause(ioException);
        handler.handleServiceException(serviceException, requestInfo);

        InputStream input = new ByteArrayInputStream(response.getOutputStreamContent().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);
        Node exceptionTextNode = XPathAPI.selectSingleNode(doc, "ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()");
        assertNotNull(exceptionTextNode);
        // normalise whitespace
        String exceptionText = exceptionTextNode.getNodeValue().replaceAll("\\s+", " ");
        assertTrue(exceptionText.indexOf(illegalArgument.getMessage()) != -1);
        assertTrue(exceptionText.indexOf(ioException.getMessage()) != -1);
        assertTrue(exceptionText.indexOf(serviceException.getMessage()) != -1);
    }
}
