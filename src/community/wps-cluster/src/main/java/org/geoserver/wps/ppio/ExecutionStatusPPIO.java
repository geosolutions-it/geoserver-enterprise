/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.ProcessException;
import org.geotools.util.logging.Logging;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * Handles input and output of feature collections as zipped files.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public class ExecutionStatusPPIO extends BinaryPPIO {

    /** The marshaller. */
    XStream marshaller = new XStream(new JettisonMappedXmlDriver());

    /** The geo server. */
    GeoServer geoServer;

    /** The catalog. */
    Catalog catalog;

    /** The resources. */
    WPSResourceManager resources;

    private final static Logger LOGGER = Logging.getLogger(ExecutionStatusPPIO.class);

    /**
     * Instantiates a new execution status list ppio.
     * 
     * @param geoServer the geo server
     * @param resources the resources
     */
    public ExecutionStatusPPIO(GeoServer geoServer, WPSResourceManager resources) {
        super(ExecutionStatus.class, ExecutionStatus.class, "application/json");
        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
        this.resources = resources;
    }

    /**
     * Encode.
     * 
     * @param output the output
     * @param os the os
     * @throws Exception the exception
     */
    @Override
    public void encode(final Object output, OutputStream os) throws Exception {
        try {
            ExecutionStatus status = (ExecutionStatus) output;
            marshaller.toXML(status, os);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest(marshaller.toXML(status));
            }
        } catch (Exception e) {
            throw new ProcessException(
                    "Could not encode output" + output != null ? (" :" + output.getClass())
                            : ": null", e);
        }
    }

    /**
     * Decode.
     * 
     * @param input the input
     * @return the object
     * @throws Exception the exception
     */
    @Override
    public Object decode(Object input) throws Exception {
        if (input instanceof String) {
            return marshaller.fromXML((String) input);
        }

        throw new ProcessException("Could not decode input:" + input.getClass());
    }

    /**
     * Decode.
     * 
     * @param input the input
     * @return the object
     * @throws Exception the exception
     */
    @Override
    public Object decode(InputStream input) throws Exception {
        try {
            Object object = marshaller.fromXML(input);
            if (object instanceof List) {
                return (ExecutionStatus) object;
            }
            // object of wrong type
            throw new IllegalArgumentException("Object of wrong type");
        } catch (Exception e) {
            throw new ProcessException("Could not decode the provided input", e);
        }

    }

    /**
     * Gets the file extension.
     * 
     * @return the file extension
     */
    @Override
    public String getFileExtension() {
        return "json";
    }
}
