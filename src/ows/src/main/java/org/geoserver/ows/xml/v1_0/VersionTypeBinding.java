/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.xml.v1_0;

import javax.xml.namespace.QName;

import net.opengis.ows10.Ows10Factory;

import org.geotools.xml.AbstractSimpleBinding;
import org.geotools.xml.InstanceComponent;


/**
 * Binding object for the type http://www.opengis.net/ows:VersionType.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;simpleType name="VersionType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;Specification version for OWS operation. The string value shall contain one x.y.z "version" value (e.g., "2.1.3"). A version number shall contain three non-negative integers separated by decimal points, in the form "x.y.z". The integers y and z shall not exceed 99. Each version shall be for the Implementation Specification (document) and the associated XML Schemas to which requested operations will conform. An Implementation Specification version normally specifies XML Schemas against which an XML encoded operation response must conform and should be validated. See Version negotiation subclause for more information. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;restriction base="string"/&gt;
 *  &lt;/simpleType&gt;
 *
 *          </code>
 *         </pre>
 * </p>
 *
 * @generated
 */
public class VersionTypeBinding extends AbstractSimpleBinding {
    Ows10Factory owsfactory;

    public VersionTypeBinding(Ows10Factory owsfactory) {
        this.owsfactory = owsfactory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return OWS.VERSIONTYPE;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return null;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(InstanceComponent instance, Object value)
        throws Exception {
        //TODO: implement
        return value;
    }
}
