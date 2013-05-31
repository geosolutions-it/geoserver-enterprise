/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */package org.geoserver.catalog;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Represents a dimension, such as the standard TIME and ELEVATION ones, but could be a custom one 
 * 
 * @author Andrea Aime - GeoSolutions
 */
public interface DimensionInfo extends Serializable {

    /**
     * Whether this dimension is enabled or not
     */
    public boolean isEnabled();

    /**
     * Sets the dimension as enabled, or not
     * 
     * @param enabled
     */
    public void setEnabled(boolean enabled);

    /**
     * The attribute on which the dimension is based. Used only for vector data
     * 
     * @return
     */
    public String getAttribute();

    public void setAttribute(String attribute);
    
    /**
     * The attribute on which the end of the dimension is based. Used only for vector data.
     * This attribute is optional.
     * @return
     */
    public String getEndAttribute();
    
    public void setEndAttribute(String attribute);

    /**
     * The way the dimension is going to be presented in the capabilities documents
     * 
     * @return
     */
    public DimensionPresentation getPresentation();

    public void setPresentation(DimensionPresentation presentation);

    /**
     * The interval resolution in case {@link DimensionPresentation#DISCRETE_INTERVAL} presentation
     * has been chosen (it can be a representation of a elevation resolution or a time interval in
     * milliseconds)
     * 
     * @return
     */
    public BigDecimal getResolution();

    public void setResolution(BigDecimal resolution);

}
