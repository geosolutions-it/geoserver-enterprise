/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.util;

import static org.geotools.coverage.grid.io.AbstractGridCoverage2DReader.ELEVATION_DOMAIN;
import static org.geotools.coverage.grid.io.AbstractGridCoverage2DReader.HAS_ELEVATION_DOMAIN;
import static org.geotools.coverage.grid.io.AbstractGridCoverage2DReader.HAS_TIME_DOMAIN;
import static org.geotools.coverage.grid.io.AbstractGridCoverage2DReader.TIME_DOMAIN;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.util.logging.Logging;

/**
 * Centralizes the metadata extraction and parsing used to read dimension informations out of a
 * coverage reader
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class ReaderDimensionsAccessor {

    private static final Logger LOGGER = Logging.getLogger(ReaderDimensionsAccessor.class);

    private static final String UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private AbstractGridCoverage2DReader reader;

    public ReaderDimensionsAccessor(AbstractGridCoverage2DReader reader) {
        this.reader = reader;
    }

    /**
     * True if the reader has a time dimension
     * 
     * @return
     */
    public boolean hasTime() {
        return "true".equalsIgnoreCase(reader.getMetadataValue(HAS_TIME_DOMAIN));
    }

    /**
     * Returns the full set of time values supported by the raster, sorted by time
     * 
     * @return
     */
    public TreeSet<Date> getTimeDomain() {
        final SimpleDateFormat df = getTimeFormat();
        String domain = reader.getMetadataValue(TIME_DOMAIN);
        String[] timeInstants = domain.split("\\s*,\\s*");
        TreeSet<Date> values = new TreeSet<Date>();
        for (String tp : timeInstants) {
            try {
                values.add(df.parse(tp));
            } catch (ParseException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }

        return values;
    }
    
    /**
     * Returns the max value for the time
     * 
     * @return
     */
    public Date getMaxTime() {
        final String currentTime = reader
                .getMetadataValue(AbstractGridCoverage2DReader.TIME_DOMAIN_MAXIMUM);
        if (currentTime == null) {
            return null;
        }
        try {
            return getTimeFormat().parse(currentTime);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to get CURRENT time from coverage reader", e);
        }
    }

    /**
     * Returns the min value for the time
     * 
     * @return
     */
    public Date getMinTime() {
        final String currentTime = reader
                .getMetadataValue(AbstractGridCoverage2DReader.TIME_DOMAIN_MINIMUM);
        if (currentTime == null) {
            return null;
        }
        try {
            return getTimeFormat().parse(currentTime);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to get minimum time from coverage reader", e);
        }
    }

    /**
     * Returns a {@link SimpleDateFormat} using the UTC_PATTERN and the UTC time zone
     * 
     * @return
     */
    public SimpleDateFormat getTimeFormat() {
        final SimpleDateFormat df = new SimpleDateFormat(UTC_PATTERN);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df;
    }

    /**
     * True if the reader has a elevation dimension
     * 
     * @return
     */
    public boolean hasElevation() {
        return "true".equalsIgnoreCase(reader.getMetadataValue(HAS_ELEVATION_DOMAIN));
    }

    /**
     * Returns the full set of elevation values, sorted from smaller to higher
     * 
     * @return
     */
    public TreeSet<Double> getElevationDomain() {
        // parse the values from the reader, they are exposed as strings...
        String[] elevationValues = reader.getMetadataValue(ELEVATION_DOMAIN).split(",");
        TreeSet<Double> elevations = new TreeSet<Double>();
        for (String val : elevationValues) {
            try {
                elevations.add(Double.parseDouble(val));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }

        return elevations;
    }
    
    /**
     * Returns the max value for the elevation
     * 
     * @return
     */
    public Double getMaxElevation() {
        final String elevation = reader
                .getMetadataValue(AbstractGridCoverage2DReader.ELEVATION_DOMAIN_MAXIMUM);
        if (elevation == null) {
            return null;
        }
        try {
            return Double.parseDouble(elevation);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to get maximum elevation from coverage reader", e);
        }
    }

    /**
     * Returns the min value for the elevation
     * 
     * @return
     */
    public Double getMinElevation() {
        final String elevation = reader
                .getMetadataValue(AbstractGridCoverage2DReader.ELEVATION_DOMAIN_MINIMUM);
        if (elevation == null) {
            return null;
        }
        try {
            return Double.parseDouble(elevation);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to get minimum elevation from coverage reader", e);
        }
    }

    

}
