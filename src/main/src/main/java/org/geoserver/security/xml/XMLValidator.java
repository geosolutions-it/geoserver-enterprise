/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;


/**
 * Validating against the XML schema, depending on the version
 * 
 * 
 * @author christian
 *
 */
public class XMLValidator {

    public final static XMLValidator Singleton = new XMLValidator();
    protected  Map<String,Schema> versionMapUR,versionMapRR;
    private Object lockUR = new Object();
    private Object lockRR = new Object();
    
    /**
     * protected constructor, use the static Singleton instance
     */
    protected XMLValidator() {        
    }
    
    /**
     * Validates a User/Group DOM against the XMLSchema.
     * The schema is determined by the version of the 
     * User/Group DOM 
     * 
     * @param doc
     * @throws IOException
     */
    public void validateUserGroupRegistry(Document doc) throws IOException {
        if (versionMapUR==null)
            initializeSchemataUR();
        XPathExpression expr = XMLXpathFactory.Singleton.getVersionExpressionUR();
        String versionString=null;
        try {
            versionString = expr.evaluate(doc);
        } catch (XPathExpressionException e) {
            throw new IOException(e); // this should not happen
        }
        Schema schema = versionMapUR.get(versionString);
        Validator val = schema.newValidator();
        try {
            val.validate(new DOMSource(doc));
        } catch (SAXException e) {
            throw new IOException(e); // this should not happen
        }
        
    }

    /**
     * Validates a Role DOM against the XMLSchema.
     * The schema is determined by the version of the 
     * Role DOM 
     * 
     * @param doc
     * @throws IOException
     */

    public void validateRoleRegistry(Document doc) throws IOException{
        if (versionMapRR==null)
            initializeSchemataRR();
        XPathExpression expr = XMLXpathFactory.Singleton.getVersionExpressionRR();
        String versionString;
       try { 
       versionString = expr.evaluate(doc);
           } catch (XPathExpressionException e) {
               throw new IOException(e);
        }
        Schema schema = versionMapRR.get(versionString);
        Validator val = schema.newValidator();
        try {
            val.validate(new DOMSource(doc));
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }
    
    /**
     * Lazy initialization of  
     * User/Group schemata
     * 
     * @throws IOException
     */
    protected void  initializeSchemataUR() throws IOException {
        synchronized(lockUR) {
            if (versionMapUR!=null) return; // another tread was faster
            versionMapUR = new HashMap<String,Schema>();
            SchemaFactory factory = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        
            Schema schema=null;
            try {
                schema = factory.newSchema(this.getClass().getResource(XMLConstants.FILE_UR_SCHEMA));
            } catch (SAXException e) {
                throw new IOException(e); // this should not happen
            }        
            versionMapUR.put(XMLConstants.VERSION_UR_1_0, schema);
        }
                
    }
    
    /**
     * Lazy initialization of  
     * Role schemata
     * 
     * @throws IOException
     */    
    protected void initializeSchemataRR() throws IOException {
        
        synchronized(lockRR) {
            if (versionMapRR!=null) return; // another tread was faster
            
            versionMapRR = new HashMap<String,Schema>();
            SchemaFactory factory = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        
            Schema schema=null;
            try {
                schema = factory.newSchema(this.getClass().getResource(XMLConstants.FILE_RR_SCHEMA));
            } catch (SAXException e) {
                throw new IOException(e); // this should not happen
            }        
            versionMapRR.put(XMLConstants.VERSION_RR_1_0, schema);
        }                
    }



    
}
