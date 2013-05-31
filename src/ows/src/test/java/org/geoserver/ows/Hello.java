/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import java.net.URL;
import java.util.List;
import java.util.Map;

public class Hello implements OWS {
    public String getId() {
        return "hello";
    }

    public String getAbstract() {
        return null;
    }

    public String getAccessConstraints() {
        return null;
    }

    public Map getClientProperties() {
        return null;
    }

    public String getFees() {
        return null;
    }

    public List getKeywords() {
        return null;
    }

    public String getMaintainer() {
        return null;
    }

    public String getName() {
        return null;
    }

    public URL getOnlineResource() {
        return null;
    }

    public String getTitle() {
        return null;
    }

    public boolean isEnabled() {
        return false;
    }

    public void setAbstract(String serverAbstract) {
    }

    public void setAccessConstraints(String accessConstraints) {
    }

    public void setEnabled(boolean enabled) {
    }

    public void setFees(String fees) {
    }

    public void setMaintainer(String maintainer) {
    }

    public void setName(String name) {
    }

    public void setOnlineResource(URL onlineResource) {
    }

    public void setTitle(String title) {
    }

    public String getSchemaBaseURL() {
        return null;
    }

    public void setSchemaBaseURL(String schemaBaseURL) {
    }

    public boolean isVerbose() {
        return false;
    }

    public void setVerbose(boolean verbose) {
    }
}
