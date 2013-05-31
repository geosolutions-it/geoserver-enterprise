package org.geoserver.config.impl;

import org.geoserver.config.LoggingInfo;

/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
public class LoggingInfoImpl implements LoggingInfo {

    String id;
    
    String level;

    String location;

    boolean stdOutLogging;

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isStdOutLogging() {
        return stdOutLogging;
    }

    public void setStdOutLogging(boolean stdOutLogging) {
        this.stdOutLogging = stdOutLogging;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((level == null) ? 0 : level.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + (stdOutLogging ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LoggingInfoImpl other = (LoggingInfoImpl) obj;
        if (level == null) {
            if (other.level != null)
                return false;
        } else if (!level.equals(other.level))
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        if (stdOutLogging != other.stdOutLogging)
            return false;
        return true;
    }
    
}
