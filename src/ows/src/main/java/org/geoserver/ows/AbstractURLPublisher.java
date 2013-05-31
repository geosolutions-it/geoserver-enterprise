/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.ows.util.EncodingInfo;
import org.geoserver.ows.util.XmlCharsetDetector;
import org.geotools.data.DataUtilities;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Controller which publishes files through a web interface from the classpath
 * <p>
 * To use this controller, it should be mapped to a particular url in the url mapping of the spring
 * dispatcher servlet. Example:
 * 
 * <pre>
 * <code>
 *   &lt;bean id="filePublisher" class="org.geoserver.ows.FilePublisher"/&gt;
 *   &lt;bean id="dispatcherMappings"
 *      &lt;property name="alwaysUseFullPath" value="true"/&gt;
 *      class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping"&gt;
 *      &lt;property name="mappings"&gt;
 *        &lt;prop key="/schemas/** /*.xsd"&gt;filePublisher&lt;/prop&gt;
 *        &lt;prop key="/schemas/** /*.dtd"&gt;filePublisher&lt;/prop&gt;
 *        &lt;prop key="/styles/*"&gt;filePublisher&lt;/prop&gt;
 *      &lt;/property&gt;
 *   &lt;/bean&gt;
 * </code>
 * </pre>
 * 
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractURLPublisher extends AbstractController {

    protected ModelAndView handleRequestInternal(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        URL url = getUrl(request);

        // if not found return a 404
        if (url == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        
        File file = DataUtilities.urlToFile(url);
        if(file != null && file.exists() && file.isDirectory()) {
            String uri = request.getRequestURI().toString();
            uri += uri.endsWith("/") ? "index.html" : "/index.html";
            
            response.addHeader("Location", uri);
            response.sendError(HttpServletResponse.SC_MOVED_TEMPORARILY);
            
            return null;
        }
        
        

        // set the mime if known by the servlet container, set nothing otherwise
        // (Tomcat behaves like this when it does not recognize the file format)
        String mime = getServletContext().getMimeType(new File(url.getFile()).getName());
        if (mime != null) {
            response.setContentType(mime);
        }

        // set the content length and content type
        URLConnection connection = null;
        InputStream input = null;
        try {
            connection = url.openConnection();
            long length = connection.getContentLength();
            if (length > 0 && length <= Integer.MAX_VALUE) {
                response.setContentLength((int) length);
            }

            long lastModified = connection.getLastModified();
            if (lastModified > 0) {
                SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss",
                        Locale.ENGLISH);
                format.setTimeZone(TimeZone.getTimeZone("GMT"));
                String formatted = format.format(new Date(lastModified)) + " GMT";
                response.setHeader("Last-Modified", formatted);
            }

            // Guessing the charset (and closing the stream)
            EncodingInfo encInfo = null;
            OutputStream output = null;
            final byte[] b4 = new byte[4];
            int count = 0;
            // open the output
            input = connection.getInputStream();

            // Read the first four bytes, and determine charset encoding
            count = input.read(b4);
            encInfo = XmlCharsetDetector.getEncodingName(b4, count);
            response.setCharacterEncoding(encInfo.getEncoding() != null ? encInfo.getEncoding()
                    : "UTF-8");

            //count < 1 -> empty file
            if (count > 0) {
                // send out the first four bytes read
                output = response.getOutputStream();
                output.write(b4, 0, count);
    
                // copy the content to the output
                byte[] buffer = new byte[8192];
                int n = -1;
                while ((n = input.read(buffer)) != -1) {
                    output.write(buffer, 0, n);
                }
            }
        } finally {
            if (input != null)
                input.close();
        }

        return null;
    }

    /**
     * Retrieves the resource URL from the specified request
     * @param request
     * @return
     * @throws IOException 
     */
    protected abstract URL getUrl(HttpServletRequest request) throws IOException;
}
