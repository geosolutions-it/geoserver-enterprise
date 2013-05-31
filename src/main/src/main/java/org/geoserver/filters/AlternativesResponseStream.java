/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.filters;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;


/**
 * A response stream that figures out whether or not to compress the output
 * just before the first write.  The decision is based on the mimetype set
 * for the output request.
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
public class AlternativesResponseStream extends ServletOutputStream {
    HttpServletResponse myResponse;
    ServletOutputStream myStream;
    Set myCompressibleTypes;
    Logger logger = org.geotools.util.logging.Logging.getLogger("org.geoserver.filters");

    public AlternativesResponseStream(HttpServletResponse response, Set compressible) throws IOException {
        super();
        myResponse = response;
        myCompressibleTypes = compressible;
    }

    public void close() throws IOException {
        if (isDirty())
            getStream().close();
    }

    public void flush() throws IOException {
        if (isDirty())
            getStream().flush();
    }

    public void write(int b) throws IOException {
        getStream().write(b);
    }

    public void write(byte b[]) throws IOException {
        getStream().write(b);
    }

    public void write(byte b[], int off, int len) throws IOException {
        getStream().write(b, off, len);
    }

    protected ServletOutputStream getStream() throws IOException{
        if (myStream != null) return myStream;
        String type = myResponse.getContentType();

//        if (type == null){
//            logger.warning("Mime type was not set before first write!");
//        }

        if (type != null && isCompressible(type)){
            logger.log(Level.FINE, "Compressing output for mimetype: {0}", type);
            myStream = new GZIPResponseStream(myResponse);
        } else {
            logger.log(Level.FINE, "Not compressing output for mimetype: {0}", type);
            myStream = myResponse.getOutputStream();
        }

        return myStream;
    }

    protected boolean isDirty(){
        return myStream != null;
    }

    protected boolean isCompressible(String mimetype){
        String stripped = stripParams(mimetype);
        
        Iterator it = myCompressibleTypes.iterator();

        while (it.hasNext()){
            Pattern pat = (Pattern)it.next();
            Matcher matcher = pat.matcher(stripped);
            if (matcher.matches()) return true;
        }

        return false;
    }

    protected String stripParams(String mimetype){
        int firstSemicolon = mimetype.indexOf(";");

        if (firstSemicolon != -1){
            return mimetype.substring(0, firstSemicolon);
        }

        return mimetype;
    }
}
