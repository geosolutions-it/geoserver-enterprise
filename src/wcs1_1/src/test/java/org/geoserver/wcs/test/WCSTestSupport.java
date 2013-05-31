/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs.test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.wcs.WCSInfo;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Base support class for wcs tests.
 * 
 * @author Andrea Aime, TOPP
 * 
 */
public abstract class WCSTestSupport extends CoverageTestSupport {
    protected static XpathEngine xpath;
    
    protected static final boolean IS_WINDOWS;
    
    protected static final Schema WCS11_SCHEMA;
    
    static {
        try {
            final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            WCS11_SCHEMA = factory.newSchema(new File("./schemas/wcs/1.1.1/wcsAll.xsd"));
        } catch(Exception e) {
            throw new RuntimeException("Could not parse the WCS 1.1.1 schemas", e);
        }
        boolean windows = false;
        try {
            windows = System.getProperty("os.name").matches(".*Windows.*");
        } catch(Exception e) {
            // no os.name? oh well, never mind
        }
        IS_WINDOWS = windows;
    }

    /**
     * @return The global wfs instance from the application context.
     */
    protected WCSInfo getWCS() {
        return getGeoServer().getService(WCSInfo.class);
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        
        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }
    
    @Override
    protected boolean isMemoryCleanRequired() {
        return IS_WINDOWS;
    }
    
    /**
     * Parses a multipart message from the response
     * @param response
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    protected Multipart getMultipart(MockHttpServletResponse response) throws MessagingException,
            IOException {
        MimeMessage body = new MimeMessage((Session) null, getBinaryInputStream(response));
        Multipart multipart = (Multipart) body.getContent();
        return multipart;
    }
}
