/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONException;
import net.sf.json.util.JSONBuilder;

import org.apache.commons.io.IOUtils;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;

/**
 * Enum to hold the MIME type for JSON and some useful related utils
 * <ul>
 * <li>JSON: application/json</li>
 * <li>JSONP: text/javascript</li>
 * </ul>
 * 
 * @author Carlo Cancellieri - GeoSolutions
 */
public enum JSONType {
    JSONP, JSON;

    /**
     * The key value into the optional FORMAT_OPTIONS map
     */
    public final static String CALLBACK_FUNCTION_KEY = "callback";

    /**
     * The default value of the callback function
     */
    public final static String CALLBACK_FUNCTION = "parseResponse";

    public final static String json = "application/json";

    public final static String simple_json = "json";

    public final static String jsonp = "text/javascript";

    /**
     * The key of the property to enable the JSonp responses This property is set default to false.
     */
    public final static String ENABLE_JSONP_KEY = "ENABLE_JSONP";

    private static boolean jsonpEnabled = isJsonpPropertyEnabled();

    /**
     * Check if the passed MimeType is a valid jsonp
     * 
     * @param type the MimeType string representation to check
     * @return true if type is equalsIgnoreCase to {@link #jsonp}
     */
    public static boolean isJsonpMimeType(String type) {
        return JSONType.jsonp.equalsIgnoreCase(type);
    }

    /**
     * Check if the passed MimeType is a valid jsonp and if jsonp is enabled
     * 
     * @param type the MimeType string representation to check
     * @return true if type is equalsIgnoreCase to {@link #jsonp} and jsonp is enabled
     * @see {@link JSONType#isJsonMimeType(String)}
     */
    public static boolean useJsonp(String type) {
        return JSONType.isJsonpEnabled() && JSONType.isJsonpMimeType(type);
    }

    private static ReadWriteLock lock = new ReentrantReadWriteLock(true);

    /**
     * @return The boolean returned represents the value of the jsonp toggle (if true jsonp is enabled)
     */
    public static boolean isJsonpEnabled() {
        lock.readLock().lock();
        try {
            return jsonpEnabled;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Enable disable the jsonp toggle overriding environment and properties.
     * 
     * @see {@link JSONType#isJsonpEnabledByEnv()} and {@link JSONType#isJsonpEnabledByProperty()}
     * @param jsonpEnabled true to enable jsonp
     */
    public static void setJsonpEnabled(boolean jsonpEnabled) {
        if (jsonpEnabled != JSONType.jsonpEnabled) {
            lock.writeLock().lock();
            try {
                JSONType.jsonpEnabled = jsonpEnabled;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * Parses the ENABLE_JSONP value as a boolean.
     * 
     * @return The boolean returned represents the value true if the string argument of the ENABLE_JSONP property is not null and is equal, ignoring
     *         case, to the string "true".
     */
    private static boolean isJsonpPropertyEnabled() {
        String jsonp = GeoServerExtensions.getProperty(ENABLE_JSONP_KEY);
        return Boolean.parseBoolean(jsonp);
    }

    /**
     * Check if the passed MimeType is a valid json
     * 
     * @param type the MimeType string representation to check
     * @return true if type is equalsIgnoreCase to {@link JSONType#json} or to {@link JSONType#simple_json}
     */
    public static boolean isJsonMimeType(String type) {
        return JSONType.json.equalsIgnoreCase(type) || JSONType.simple_json.equalsIgnoreCase(type);
    }

    /**
     * Return the JSNOType enum matching the passed MimeType or null (if no match)
     * 
     * @param mime the mimetype to check
     * @return the JSNOType enum matching the passed MimeType or null (if no match)
     */
    public static JSONType getJSONType(String mime) {
        if (json.equalsIgnoreCase(mime) || simple_json.equalsIgnoreCase(mime)) {
            return JSON;
        } else if (jsonp.equalsIgnoreCase(mime)) {
            return JSONP;
        } else {
            return null; // not valid representation
        }
    }

    /**
     * get the MimeType for this object
     * 
     * @return return a string representation of the MimeType
     */
    public String getMimeType() {
        switch (this) {
        case JSON:
            return json;
        case JSONP:
            return jsonp;
        default:
            return null;
        }
    }

    /**
     * get an array containing all the MimeType handled by this object
     * 
     * @return return a string array of handled MimeType
     * @return
     */
    public static String[] getSupportedTypes() {
        if (isJsonpEnabled())
            return new String[] { json, simple_json, jsonp };
        else
            return new String[] { json, simple_json };
    }

    /**
     * Can be used when {@link #jsonp} format is specified to resolve the callback parameter into the FORMAT_OPTIONS map
     * 
     * @param kvp the kay value pair map of the request
     * @return The string name of the callback function or the default {@link #CALLBACK_FUNCTION} if not found.
     */
    public static String getCallbackFunction(Map kvp) {
        if (!(kvp.get("FORMAT_OPTIONS") instanceof Map)) {
            return JSONType.CALLBACK_FUNCTION;
        } else {
            Map<String, String> map = (Map<String, String>) kvp.get("FORMAT_OPTIONS");
            String callback = map.get(CALLBACK_FUNCTION_KEY);
            if (callback != null) {
                return callback;
            } else {
                return JSONType.CALLBACK_FUNCTION;
            }
        }
    }

    /**
     * Handle Exception in JSON and JSONP format
     * 
     * @param LOGGER the logger to use (can be null)
     * @param exception the exception to write to the response outputStream
     * @param request the request generated the exception
     * @param charset the desired charset
     * @param verbose be verbose
     * @param isJsonp switch writing json (false) or jsonp (true)
     */
    public static void handleJsonException(Logger LOGGER, ServiceException exception,
            Request request, String charset, boolean verbose, boolean isJsonp) {

        final HttpServletResponse response = request.getHttpResponse();
        // TODO: server encoding options?
        response.setCharacterEncoding(charset);

        ServletOutputStream os = null;
        try {
            os = response.getOutputStream();
            if (isJsonp) {
                // jsonp
                response.setContentType(JSONType.jsonp);
                JSONType.writeJsonpException(exception, request, os, charset, verbose);
            } else {
                // json
                OutputStreamWriter outWriter = null;
                try {
                    outWriter = new OutputStreamWriter(os, charset);
                    response.setContentType(JSONType.json);
                    JSONType.writeJsonException(exception, request, outWriter, verbose);
                } finally {
                    if (outWriter != null) {
                        try {
                            outWriter.flush();
                        } catch (IOException ioe) {
                        }
                        IOUtils.closeQuietly(outWriter);
                    }
                }

            }
        } catch (Exception e) {
            if (LOGGER != null && LOGGER.isLoggable(Level.SEVERE))
                LOGGER.severe(e.getLocalizedMessage());
        } finally {
            if (os != null) {
                try {
                    os.flush();
                } catch (IOException ioe) {
                }
                IOUtils.closeQuietly(os);
            }
        }
    }

    private static void writeJsonpException(ServiceException exception, Request request,
            OutputStream out, String charset, boolean verbose) throws IOException {

        OutputStreamWriter outWriter = new OutputStreamWriter(out, charset);
        final String callback;
        if (request == null) {
            callback = JSONType.CALLBACK_FUNCTION;
        } else {
            callback = JSONType.getCallbackFunction(request.getKvp());
        }
        outWriter.write(callback + "(");

        writeJsonException(exception, request, outWriter, verbose);

        outWriter.write(")");
        outWriter.flush();
        IOUtils.closeQuietly(outWriter);
    }

    private static void writeJsonException(ServiceException exception, Request request,
            OutputStreamWriter outWriter, boolean verbose) throws IOException {
        try {
            JSONBuilder json = new JSONBuilder(outWriter);
            json.object().key("version").value(request.getVersion()).key("exceptions").array()
                    .object().key("code")
                    .value(exception.getCode() == null ? "noApplicableCode" : exception.getCode())
                    .key("locator")
                    .value(exception.getLocator() == null ? "noLocator" : exception.getLocator())
                    .key("text");
            // message
            if ((exception.getMessage() != null)) {
                StringBuffer sb = new StringBuffer(exception.getMessage().length());
                OwsUtils.dumpExceptionMessages(exception, sb, false);

                if (verbose) {
                    ByteArrayOutputStream stackTrace = null;
                    try {
                        stackTrace = new ByteArrayOutputStream();
                        exception.printStackTrace(new PrintStream(stackTrace));
                        sb.append("\nDetails:\n");
                        sb.append(new String(stackTrace.toByteArray()));
                    } finally {
                        IOUtils.closeQuietly(stackTrace);
                    }
                }
                json.value(sb.toString());

            }
            json.endObject().endArray().endObject();
        } catch (JSONException jsonException) {
            ServiceException serviceException = new ServiceException("Error: "
                    + jsonException.getMessage());
            serviceException.initCause(jsonException);
            throw serviceException;
        }
    }
}
