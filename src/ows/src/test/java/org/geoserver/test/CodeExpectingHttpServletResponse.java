package org.geoserver.test;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * HttpServletResponse wrapper to help in making assertions about expected status codes.
 * @author David Winslow, OpenGeo
 */
public class CodeExpectingHttpServletResponse extends HttpServletResponseWrapper{
    private int myErrorCode;

    public CodeExpectingHttpServletResponse (HttpServletResponse req){
        super(req);
        myErrorCode = 200;
    }

    public void setStatus(int sc){
        myErrorCode = sc;
        super.setStatus(sc);
    }

    public void setStatus(int sc, String sm){
        myErrorCode = sc;
        super.setStatus(sc, sm);
    }

    public void sendError(int sc) throws IOException {
        myErrorCode = sc;
        super.sendError(sc);
    }

    public void sendError(int sc, String sm) throws IOException {
        myErrorCode = sc;
        super.sendError(sc, sm);
    }

    public int getErrorCode(){
        return myErrorCode;
    }

    public int getStatusCode(){
        return myErrorCode;
    }
}


