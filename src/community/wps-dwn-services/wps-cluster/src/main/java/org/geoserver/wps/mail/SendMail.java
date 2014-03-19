/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.global.GeoserverDataDirectory;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * The Class SendMail.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public class SendMail {

    /** The props. */
    private Properties props;

    /** The conf. */
    private final MailConfiguration conf = new MailConfiguration();

    private final static Logger LOGGER=Logging.getLogger(SendMail.class);

    /** FreeMarker TEMPLATES *. */
    static final Configuration TEMPLATES;

    static {
        TEMPLATES = new Configuration();
        // same package of this class
        try {
            File templatesPath = getSendMailTemplatesPath();

            if (templatesPath != null) {
                TEMPLATES.setDirectoryForTemplateLoading(templatesPath);
            } else {
                TEMPLATES.setClassForTemplateLoading(SendMail.class, "");
            }
        } catch (IOException e) {
            TEMPLATES.setClassForTemplateLoading(SendMail.class, "");

        }
        TEMPLATES.setObjectWrapper(new DefaultObjectWrapper());
    }

    /**
     * Instantiates a new send mail.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public SendMail() throws IOException {
        props = loadConfiguration();
    }

    /**
     * Send an EMail to a specified address.
     * 
     * @param address the to address
     * @param subject the email address
     * @param body message to send
     * @throws MessagingException the messaging exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void send(String address, String subject, String body){
        try {
            // Session session = Session.getDefaultInstance(props, null);
            Session session = Session.getDefaultInstance(props, (conf.getMailSmtpAuth()
                    .equalsIgnoreCase("true") ? new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(conf.getUserName(), conf.getPassword());
                }
            } : null));

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(conf.getFromAddress(), conf.getFromAddressname()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(address));
            message.setSubject(subject);
            message.setText(body.toString());

            Transport.send(message);

        } catch (Exception e) {
            if(LOGGER.isLoggable(Level.SEVERE)){
                LOGGER.log(Level.SEVERE,e.getLocalizedMessage(),e);
            }
        }

    }

    /**
     * Send a notification to the specified address.
     * 
     * @param toAddress the to address
     * @param executiondId the executiond id
     * @param expirationDelay
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MessagingException the messaging exception
     */
    public void sendFinishedNotification(String toAddress, String executiondId, String result,
            int expirationDelay){
        try{
            // load template for the password reset email
            Template mailTemplate = TEMPLATES.getTemplate("FinishedNotificationMail.ftl");
    
            StringWriter body = fillMailBody(toAddress, executiondId, result, expirationDelay,
                    mailTemplate);
    
            send(toAddress, conf.getSubjet(), body.toString());
        } catch (Exception e) {
            if(LOGGER.isLoggable(Level.SEVERE)){
                LOGGER.log(Level.SEVERE,e.getLocalizedMessage(),e);
            }
        }        
    }

    /**
     * Fill mail body.
     * 
     * @param toAddress the to address
     * @param executiondId the executiond id
     * @param result
     * @param expirationDelay
     * @param mailTemplate the mail template
     * @return the string writer
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private StringWriter fillMailBody(String toAddress, String executiondId, String result,
            int expirationDelay, Template mailTemplate) throws IOException {

        // create template context
        StringWriter body = new StringWriter();
        Map<String, Object> templateContext = new HashMap<String, Object>();
        templateContext.put("toAddress", toAddress);
        templateContext.put("executiondId", executiondId);

        String millis = String.format(
                "%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(expirationDelay),
                TimeUnit.MILLISECONDS.toSeconds(expirationDelay)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
                                .toMinutes(expirationDelay)));

        if (expirationDelay > 0) {
            templateContext.put("expirationDelay", millis);
        }
        if (result != null) {
            templateContext.put("result", result.toString());
        }

        // create message string
        try {
            mailTemplate.process(templateContext, body);
        } catch (TemplateException e) {
            throw new IOException(e);

        }
        return body;
    }

    /**
     * Send started notification.
     * 
     * @param toAddress the to address
     * @param executiondId the executiond id
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MessagingException the messaging exception
     */
    public void sendStartedNotification(String toAddress, String executiondId){
        try {
            // load template for the password reset email
            Template mailTemplate = TEMPLATES.getTemplate("StartedNotificationMail.ftl");

            StringWriter body = fillMailBody(toAddress, executiondId, null, 0, mailTemplate);

            send(toAddress, conf.getSubjet(), body.toString());
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }    
    }

    /**
     * Send started notification.
     * 
     * @param toAddress the to address
     * @param executiondId the executiond id
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MessagingException the messaging exception
     */
    public void sendFailedNotification(String toAddress, String executiondId, String reason){

        try {
            // load template for failed error
            Template mailTemplate = TEMPLATES.getTemplate("FailedNotificationMail.ftl");
    
            // create template context
            StringWriter body = new StringWriter();
            Map<String, Object> templateContext = new HashMap<String, Object>();
            templateContext.put("toAddress", toAddress);
            templateContext.put("executiondId", executiondId);
            templateContext.put("reason", reason);
    
            // create message string
            mailTemplate.process(templateContext, body);
            send(toAddress, conf.getSubjet(), body.toString());
        } catch (Exception e) {
            if(LOGGER.isLoggable(Level.SEVERE)){
                LOGGER.log(Level.SEVERE,e.getLocalizedMessage(),e);
            }
        }

    }

    /**
     * Load configuration.
     * 
     * @return the properties
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public Properties loadConfiguration() throws IOException {
    
        InputStream inputStream = null;
        // load the inputStream using the Properties
        try {
            inputStream = new FileInputStream(new File(SendMail.getSendMailTemplatesPath()
                    .getParentFile(), "mail.properties"));
    
            Properties properties = new Properties();
            properties.load(inputStream);
            conf.setMailSmtpHost(properties.getProperty("mail.smtp.host"));
            conf.setMailSmtpSocketFactoryPort(properties
                    .getProperty("mail.smtp.socketFactory.port"));
            conf.setMailSmtpFactoryClass(properties.getProperty("mail.smtp.socketFactory.class"));
            conf.setMailSmtpAuth(properties.getProperty("mail.smtp.auth"));
            conf.setMailSmtpPort(properties.getProperty("mail.smtp.port"));
            conf.setUserName(properties.getProperty("username"));
            conf.setPassword(properties.getProperty("password"));
            conf.setFromAddress(properties.getProperty("fromAddress"));
            conf.setFromAddressname(properties.getProperty("fromAddressname"));
            conf.setSubjet(properties.getProperty("subject"));
            conf.setBody(properties.getProperty("body"));
            // get the value of the property
            return properties;
        } finally {
            if (inputStream == null) {
                IOUtils.closeQuietly(inputStream);
            }
        }
    
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    public static File getSendMailTemplatesPath() throws IOException {
        // get the temporary storage for WPS
        try {
            File storage = GeoserverDataDirectory.findCreateConfigDir("wps-cluster/templates");
            return storage;
        } catch (Exception e) {
            throw new IOException("Could not find the data directory for WPS CLUSTER");
        }

    }
}
