/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.mail;


/**
 * The Class MailConfiguration.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public class MailConfiguration {

    /** The body. */
    private String body;

    /** The from address. */
    private String fromAddress;

    /** The from address name. */
    private String fromAddressname;

    /** The mail smtp auth. */
    private String mailSmtpAuth;

    /** The mail smtp factory class. */
    private String mailSmtpFactoryClass;

    /** The mail smtp host. */
    private String mailSmtpHost;

    /** The mail smtp port. */
    private String mailSmtpPort;

    /** The mail smtp socket factory port. */
    private String mailSmtpSocketFactoryPort;

    /** The password. */
    private String password;

    /** The subjet. */
    private String subjet;

    /** The user name. */
    private String userName;

    /**
     * Instantiates a new mail configuration.
     */
    public MailConfiguration() {

    }

    /**
     * Gets the body.
     * 
     * @return the body
     */
    public String getBody() {
        return body;
    }

    /**
     * Gets the from address.
     * 
     * @return the from address
     */
    public String getFromAddress() {
        return fromAddress;
    }

    /**
     * Sets the from addressname.
     * 
     * @param fromAddressname the new from addressname
     */
    public void setFromAddressname(String fromAddressname) {
        this.fromAddressname = fromAddressname;
    }

    /**
     * Gets the from addressname.
     * 
     * @return the from addressname
     */
    public String getFromAddressname() {
        return fromAddressname;
    }

    /**
     * Gets the mail smtp auth.
     * 
     * @return the mail smtp auth
     */
    public String getMailSmtpAuth() {
        return mailSmtpAuth;
    }

    /**
     * Gets the mail smtp factory class.
     * 
     * @return the mail smtp factory class
     */
    public String getMailSmtpFactoryClass() {
        return mailSmtpFactoryClass;
    }

    /**
     * Gets the mail smtp host.
     * 
     * @return the mail smtp host
     */
    public String getMailSmtpHost() {
        return mailSmtpHost;
    }

    /**
     * Gets the mail smtp port.
     * 
     * @return the mail smtp port
     */
    public String getMailSmtpPort() {
        return mailSmtpPort;
    }

    /**
     * Gets the mail smtp socket factory port.
     * 
     * @return the mail smtp socket factory port
     */
    public String getMailSmtpSocketFactoryPort() {
        return mailSmtpSocketFactoryPort;
    }

    /**
     * Gets the password.
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the subjet.
     * 
     * @return the subjet
     */
    public String getSubjet() {
        return subjet;
    }

    /**
     * Gets the user name.
     * 
     * @return the user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the body.
     * 
     * @param body the new body
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Sets the from address.
     * 
     * @param fromAddress the new from address
     */
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    /**
     * Sets the mail smtp auth.
     * 
     * @param mailSmtpAuth the new mail smtp auth
     */
    public void setMailSmtpAuth(String mailSmtpAuth) {
        this.mailSmtpAuth = mailSmtpAuth;
    }

    /**
     * Sets the mail smtp factory class.
     * 
     * @param mailSmtpFactoryClass the new mail smtp factory class
     */
    public void setMailSmtpFactoryClass(String mailSmtpFactoryClass) {
        this.mailSmtpFactoryClass = mailSmtpFactoryClass;
    }

    /**
     * Sets the mail smtp host.
     * 
     * @param mailSmtpHost the new mail smtp host
     */
    public void setMailSmtpHost(String mailSmtpHost) {
        this.mailSmtpHost = mailSmtpHost;
    }

    /**
     * Sets the mail smtp port.
     * 
     * @param mailSmtpPort the new mail smtp port
     */
    public void setMailSmtpPort(String mailSmtpPort) {
        this.mailSmtpPort = mailSmtpPort;
    }

    /**
     * Sets the mail smtp socket factory port.
     * 
     * @param mailSmtpSocketFactoryPort the new mail smtp socket factory port
     */
    public void setMailSmtpSocketFactoryPort(String mailSmtpSocketFactoryPort) {
        this.mailSmtpSocketFactoryPort = mailSmtpSocketFactoryPort;
    }

    /**
     * Sets the password.
     * 
     * @param password the new password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the subjet.
     * 
     * @param subjet the new subjet
     */
    public void setSubjet(String subjet) {
        this.subjet = subjet;
    }

    /**
     * Sets the user name.
     * 
     * @param userName the new user name
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

}
