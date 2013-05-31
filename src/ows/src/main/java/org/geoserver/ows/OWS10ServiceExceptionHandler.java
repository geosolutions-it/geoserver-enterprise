/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.geoserver.ows.util.ResponseUtils.baseURL;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import net.opengis.ows10.ExceptionReportType;
import net.opengis.ows10.ExceptionType;
import net.opengis.ows10.Ows10Factory;

import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.xml.v1_0.OWSConfiguration;
import org.geoserver.platform.ServiceException;
import org.geotools.xml.Encoder;

/**
 * A default implementation of {@link ServiceExceptionHandler} which outputs
 * as service exception in a <code>ows:ExceptionReport</code> document.
 * <p>
 * This service exception handler will generate an OWS exception report,
 * see {@linkplain http://schemas.opengis.net/ows/1.0.0/owsExceptionReport.xsd}.
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class OWS10ServiceExceptionHandler extends ServiceExceptionHandler {
    protected boolean verboseExceptions = false;
    
    /**
     * Constructor to be called if the exception is not for a particular service.
     *
     */
    public OWS10ServiceExceptionHandler() {
        super(Collections.EMPTY_LIST);
    }

    /**
     * Constructor to be called if the exception is for a particular service.
     *
     * @param services List of services this handler handles exceptions for.
     */
    public OWS10ServiceExceptionHandler(List services) {
        super(services);
    }

    /**
     * Writes out an OWS ExceptionReport document.
     */
    public void handleServiceException(ServiceException exception, Request request) {
        Ows10Factory factory = Ows10Factory.eINSTANCE;

        ExceptionType e = factory.createExceptionType();

        if (exception.getCode() != null) {
            e.setExceptionCode(exception.getCode());
        } else {
            //set a default
            e.setExceptionCode("NoApplicableCode");
        }

        e.setLocator(exception.getLocator());

        //add the message
        StringBuffer sb = new StringBuffer();
        OwsUtils.dumpExceptionMessages(exception, sb, false);
        e.getExceptionText().add(sb.toString());
        e.getExceptionText().addAll(exception.getExceptionText());

        if(verboseExceptions) {
            //add the entire stack trace
            //exception.
            e.getExceptionText().add("Details:");
            ByteArrayOutputStream trace = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(trace));
            e.getExceptionText().add(new String(trace.toByteArray()));
        }

        ExceptionReportType report = factory.createExceptionReportType();
        report.setVersion("1.0.0");
        report.getException().add(e);

        if (!request.isSOAP()) {
            //there will already be a SOAP mime type
            request.getHttpResponse().setContentType("application/xml");
        }
        

        //response.setCharacterEncoding( "UTF-8" );
        OWSConfiguration configuration = new OWSConfiguration();

        Encoder encoder = new Encoder(configuration, configuration.schema());
        encoder.setIndenting(true);
        encoder.setIndentSize(2);
        encoder.setLineWidth(60);
        encoder.setOmitXMLDeclaration(request.isSOAP());
        
        String schemaLocation = buildSchemaURL(baseURL(request.getHttpRequest()), "ows/1.0.0/owsExceptionReport.xsd");
        encoder.setSchemaLocation(org.geoserver.ows.xml.v1_0.OWS.NAMESPACE, schemaLocation);

        try {
            encoder.encode(report, org.geoserver.ows.xml.v1_0.OWS.EXCEPTIONREPORT,
                    request.getHttpResponse().getOutputStream());
        } catch (Exception ex) {
            //throw new RuntimeException(ex);
            // Hmm, not much we can do here.  I guess log the fact that we couldn't write out the exception and be done with it...
            LOGGER.log(Level.INFO, "Problem writing exception information back to calling client:", e);
        } finally {
            try {
                request.getHttpResponse().getOutputStream().flush();
            } catch (IOException ioe) {
            }
        }
    }
}
