/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import javax.xml.namespace.QName;

import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.WfsFactory;

import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;


/**
 * Binding object for the type http://www.opengis.net/wfs:GetCapabilitiesType.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;xsd:complexType name="GetCapabilitiesType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *            Request to a WFS to perform the GetCapabilities operation.
 *            This operation allows a client to retrieve a Capabilities
 *            XML document providing metadata for the specific WFS server.
 *
 *            The GetCapapbilities element is used to request that a Web Feature
 *            Service generate an XML document describing the organization
 *            providing the service, the WFS operations that the service
 *            supports, a list of feature types that the service can operate
 *            on and list of filtering capabilities that the service support.
 *            Such an XML document is called a capabilities document.
 *         &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base="ows:GetCapabilitiesType"&gt;
 *              &lt;xsd:attribute default="WFS" name="service"
 *                  type="ows:ServiceType" use="optional"/&gt;
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
public class GetCapabilitiesTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public GetCapabilitiesTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    public int getExecutionMode() {
        return AFTER;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WFS.GETCAPABILITIESTYPE;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return GetCapabilitiesType.class;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
        throws Exception {
        net.opengis.ows10.GetCapabilitiesType owsGetCapabilities = (net.opengis.ows10.GetCapabilitiesType) value;
        GetCapabilitiesType getCapabilities = wfsfactory.createGetCapabilitiesType();

        getCapabilities.setAcceptFormats(owsGetCapabilities.getAcceptFormats());
        getCapabilities.setAcceptVersions(owsGetCapabilities.getAcceptVersions());
        getCapabilities.setSections(owsGetCapabilities.getSections());
        getCapabilities.setUpdateSequence(owsGetCapabilities.getUpdateSequence());

        if (node.hasAttribute("service")) {
            getCapabilities.setService((String) node.getAttributeValue("service"));
        }

        return getCapabilities;
    }
}
