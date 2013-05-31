package org.geoserver.web.demo;

import java.io.File;
import java.io.Serializable;

/**
 * Holds on the properties used as arguments for the TestWfsPost servlet
 * 
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 * @since 2.0.x
 */
public class DemoRequest implements Serializable {
    /**
     * The directory containing the demo files
     */
    private final File demoDir;

    private String requestFileName;

    private String requestUrl;

    private String requestBody;

    private String userName;

    private String password;

    public DemoRequest(final File demoDir) {
        this.demoDir = demoDir;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRequestFileName() {
        return requestFileName;
    }

    public void setRequestFileName(String requestFileName) {
        this.requestFileName = requestFileName;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public File getDemoDir() {
        return demoDir;
    }
}