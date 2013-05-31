/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


/**
 * Utility class performing operations related to http requests.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 * TODO: this class needs to be merged with org.vfny.geoserver.Requests.
 */
public class RequestUtils {
    

    /**
     * Pulls out the base url ( from the client point of view ), from the
     * given request object.
     *
     * @return A String of the form "<scheme>://<server>:<port>/<context>/"
     * @deprecated Use {@link ResponseUtils#baseURL(HttpServletRequest)} instead
     */
    public static String baseURL(HttpServletRequest req) {
        StringBuffer sb = new StringBuffer(req.getScheme());
        sb.append("://").append(req.getServerName()).append(":").append(req.getServerPort())
                .append(req.getContextPath()).append("/");
        return sb.toString();
    }
    
    
    /**
     * Given a list of provided versions, and a list of accepted versions, this method will
     * return the negotiated version to be used for response according to the pre OWS 1.1 specifications,
     * that is, WMS 1.1, WMS 1.3, WFS 1.0, WFS 1.1 and WCS 1.0
     * @param providedList a non null, non empty list of provided versions (in "x.y.z" format)
     * @param acceptedList a list of accepted versions, eventually null or empty (in "x.y.z" format)
     * @return the negotiated version to be used for response
     */
    public static String getVersionPreOws(List<String> providedList, List<String> acceptedList) {
        //first figure out which versions are provided
        TreeSet<Version> provided = new TreeSet<Version>();
        for (String v : providedList) {
            provided.add(new Version(v));
        }
        
        // if no accept list provided, we return the biggest
        if(acceptedList == null || acceptedList.isEmpty())
            return provided.last().toString();
    
        //next figure out what the client accepts (and check they are good version numbers)
        TreeSet<Version> accepted = new TreeSet<Version>();
        for (String v : acceptedList) {
            checkVersionNumber(v, null);
            
            accepted.add(new Version(v));
        }
    
        // prune out those not provided
        for (Iterator<Version> v = accepted.iterator(); v.hasNext();) {
            Version version = (Version) v.next();
    
            if (!provided.contains(version)) {
                v.remove();
            }
        }
    
        // lookup a matching version
        String version = null;
        if (!accepted.isEmpty()) {
            //return the highest version provided
            version = ((Version) accepted.last()).toString();
        } else {
            for (String v : acceptedList) {
                accepted.add(new Version(v));
            }
    
            //if highest accepted less then lowest provided, send lowest
            if ((accepted.last()).compareTo(provided.first()) < 0) {
                version = (provided.first()).toString();
            }
    
            //if lowest accepted is greater then highest provided, send highest
            if ((accepted.first()).compareTo(provided.last()) > 0) {
                version = (provided.last()).toString();
            }
    
            if (version == null) {
                //go through from lowest to highest, and return highest provided 
                // that is less than the highest accepted
                Iterator<Version> v = provided.iterator();
                Version last = v.next();
    
                for (; v.hasNext();) {
                    Version current = v.next();
    
                    if (current.compareTo(accepted.last()) > 0) {
                        break;
                    }
    
                    last = current;
                }
    
                version = last.toString();
            }
        }
        
        return version;
    }
    
    /**
     * Given a list of provided versions, and a list of accepted versions, this method will
     * return the negotiated version to be used for response according to the OWS 1.1 specification
     * (at the time of writing, only WCS 1.1.1 is using it)
     * @param providedList a non null, non empty list of provided versions (in "x.y.z" format)
     * @param acceptedList a list of accepted versions, eventually null or empty (in "x.y.z" format)
     * @return the negotiated version to be used for response
     */
    public static String getVersionOws11(List<String> providedList, List<String> acceptedList) {
        //first figure out which versions are provided
        TreeSet<Version> provided = new TreeSet<Version>();
        for (String v : providedList) {
            provided.add(new Version(v));
        }
        
        // if no accept list provided, we return the biggest supported version
        if(acceptedList == null || acceptedList.isEmpty())
            return provided.last().toString();
            
    
        // next figure out what the client accepts (and check they are good version numbers)
        List<Version> accepted = new ArrayList<Version>();
        for (String v : acceptedList) {
            checkVersionNumber(v, "AcceptVersions");
            
            accepted.add(new Version(v));
        }
    
        // from the specification "The server, upon receiving a GetCapabilities request, shall scan 
        // through this list and find the first version number that it supports"
        Version negotiated = null;
        for (Iterator<Version> v = accepted.iterator(); v.hasNext();) {
            Version version = (Version) v.next();
    
            if (provided.contains(version)) {
                negotiated = version;
                break;
            }
        }
        
        // from the spec: "If the list does not contain any version numbers that the server 
        // supports, the server shall return an Exception with 
        // exceptionCode="VersionNegotiationFailed"
        if(negotiated == null)
            throw new ServiceException("Could not find any matching version", "VersionNegotiationFailed");
        
        return negotiated.toString();
    }

    /**
     * Checks the validity of a version number (the specification version numbers, three dot
     * separated integers between 0 and 99). Throws a ServiceException if the version number
     * is not valid.
     * @param v the version number (in string format)
     * @param the locator for the service exception (may be null)
     */
    public static void checkVersionNumber(String v, String locator) throws ServiceException {
        if (!v.matches("[0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{1,2}")) {
            String msg = v + " is an invalid version number";
            throw new ServiceException(msg, "VersionNegotiationFailed", locator);
        }
    }
    
    /**
     * Wraps an xml input xstream in a buffered reader specifying a lookahead that can be used
     * to preparse some of the xml document, resetting it back to its original state for actual 
     * parsing.
     * 
     * @param stream The original xml stream.
     * @param xmlLookahead The number of bytes to support for parse. If more than this number of 
     *   bytes are preparsed the stream can not be properly reset.
     *     
     * @return The buffered reader.
     * @throws IOException
     */
    public static BufferedReader getBufferedXMLReader(InputStream stream, int xmlLookahead) 
        throws IOException {
        
        //create a buffer so we can reset the input stream
        BufferedInputStream input = new BufferedInputStream(stream);
        input.mark(xmlLookahead);

        //create object to hold encoding info
        EncodingInfo encoding = new EncodingInfo();

        //call this method to set the encoding info
        XmlCharsetDetector.getCharsetAwareReader(input, encoding);

        //call this method to create the reader
        Reader reader = XmlCharsetDetector.createReader(input, encoding);

        //rest the input
        input.reset();
        
        return getBufferedXMLReader(reader, xmlLookahead);
    }
    
    /**
     * Wraps an xml reader in a buffered reader specifying a lookahead that can be used
     * to preparse some of the xml document, resetting it back to its original state for actual 
     * parsing.
     * 
     * @param reader The original xml reader.
     * @param xmlLookahead The number of bytes to support for parse. If more than this number of 
     *   bytes are preparsed the stream can not be properly reset.
     *     
     * @return The buffered reader.
     * @throws IOException
     */
    public static BufferedReader getBufferedXMLReader(Reader reader, int xmlLookahead) 
        throws IOException {
        //ensure the reader is a buffered reader
        
        if (!(reader instanceof BufferedReader)) {
            reader = new BufferedReader(reader);
        }

        //mark the input stream
        reader.mark(xmlLookahead);
        
        return (BufferedReader) reader;
    }
}
