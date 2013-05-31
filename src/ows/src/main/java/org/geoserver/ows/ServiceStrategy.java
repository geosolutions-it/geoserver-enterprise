/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;


/**
 * Strategy interface for writing output to an output stream.
 * <p>
 * This interface is used to provide different modes of output, an example would
 * be response buffering.
 * </p>
 *
 * @author Jody Garnett, Refractions Research
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface ServiceStrategy extends Cloneable {
    /**
     * @return A string used to identify the output strategy.
     */
    public String getId();

    /**
     * Get a OutputStream we can use to add content.
     * <p>
     * This output stream may be a wrapper around <code>response.getOutpuStream()</code>
     * or may not be.
     * </p>
     *
     * @param response The servlet response.
     *
     * @return An output stream to write to.
     *
     * @throws IOException Any I/O errors that occur.
     * @see DispatcherOutputStream
     */
    public DispatcherOutputStream getDestination(HttpServletResponse response)
        throws IOException;

    /**
     * Flushes the output, causing the response to be written to the actual
     * resposne output stream: <code>response.getOutputStrema()</code>
     * <p>
     * Any resources that the strategy holds on to should also be released at
     * this point.
     * </p>
     * @param response TODO
     *
     * @throws IOException Any I/O errors that occur.
     */
    public void flush(HttpServletResponse response) throws IOException;

    /**
     * Complete opperation in the negative.
     *
     * <p>
     * Gives ServiceConfig a chance to clean up resources
     * </p>
     */
    public void abort();

    /**
     * Clones the service strategy.
     *
     */
    public Object clone() throws CloneNotSupportedException;
}
