/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import javax.xml.namespace.QName;

import net.opengis.wfs.WfsFactory;

import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;


/**
 * Binding object for the type http://www.opengis.net/wfs:WFS_CapabilitiesType.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;xsd:complexType name="WFS_CapabilitiesType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              XML encoded WFS GetCapabilities operation response. This
 *              document provides clients with service metadata about a
 *              specific service instance, including metadata about the
 *              tightly-coupled data served. If the server does not implement
 *              the updateSequence parameter, the server shall always return
 *              the complete Capabilities document, without the updateSequence
 *              parameter. When the server implements the updateSequence
 *              parameter and the GetCapabilities operation request included
 *              the updateSequence parameter with the current value, the server
 *              shall return this element with only the "version" and
 *              "updateSequence" attributes. Otherwise, all optional elements
 *              shall be included or not depending on the actual value of the
 *              Contents parameter in the GetCapabilities operation request.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base="ows:CapabilitiesBaseType"&gt;
 *              &lt;xsd:sequence&gt;
 *                  &lt;xsd:element minOccurs="0" ref="wfs:FeatureTypeList"/&gt;
 *                  &lt;xsd:element minOccurs="0" ref="wfs:ServesGMLObjectTypeList"/&gt;
 *                  &lt;xsd:element minOccurs="0" ref="wfs:SupportsGMLObjectTypeList"/&gt;
 *                  &lt;xsd:element ref="ogc:Filter_Capabilities"/&gt;
 *              &lt;/xsd:sequence&gt;
 *          &lt;/xsd:extension&gt;
 *      &lt;/xsd:complexContent&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 * </p>
 *
 * @generated
 */
public class WFS_CapabilitiesTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public WFS_CapabilitiesTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WFS.WFS_CAPABILITIESTYPE;
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
    public Object parse(ElementInstance instance, Node node, Object value)
        throws Exception {
        //TODO: implement
        return null;
    }
}
