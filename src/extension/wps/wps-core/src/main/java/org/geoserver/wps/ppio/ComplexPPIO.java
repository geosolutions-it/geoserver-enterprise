/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Process parameter input / output for arbitrary data on a specific mime type.
 * 
 * @author Lucas Reed, Refractions Research Inc
 * @author Justin Deoliveira, OpenGEO
 */
public abstract class ComplexPPIO extends ProcessParameterIO {

    /**
     * mime type of encoded content.
     */
    protected String mimeType;
    
    /**
     * Constructor.
     */
    protected ComplexPPIO(Class externalType, Class internalType, String mimeType) {
        super(externalType,internalType);
        this.mimeType = mimeType;
    }

    /**
     * The mime type of the parameter of the data in encoded form.
     */
    public final String getMimeType() {
        return mimeType;
    }
    
    /**
     * Decodes the parameter from an external source or input stream.
     * <p>
     * This method should parse the input stream into its "internal" representation.
     * </p>
     * @param input The input stream.
     * 
     * @return An object of type {@link #getType()}.
     */
    public abstract Object decode( InputStream input ) throws Exception;
    
    /**
     * Decodes the parameter from an extenral source that has been pre-parsed.
     * <p>
     * This method should transform the object from the external representation to 
     * the internal representation.
     * </p>
     * @param input An object of type {@link #getExternalType()}
     * 
     * @return An object of type {@link #getType()}.
     */
    public Object decode( Object input ) throws Exception {
        return input;
    }
    
    /**
     * Encodes the internal object representation of a parameter into an output strteam
     */
    public abstract void encode( Object value, OutputStream os) throws Exception;

    /**
     * Provides a suitable extension for the outut file
     * 
     * @return
     */
    public abstract String getFileExtension();

}
