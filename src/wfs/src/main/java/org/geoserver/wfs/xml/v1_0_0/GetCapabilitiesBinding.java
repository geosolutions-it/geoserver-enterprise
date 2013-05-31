/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import javax.xml.namespace.QName;

import net.opengis.wfs.WfsFactory;

import org.geotools.xml.AbstractComplexEMFBinding;


/**
 * Binding object for the element http://www.opengis.net/wfs:GetCapabilities.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;xsd:element name="GetCapabilities" type="wfs:GetCapabilitiesType"&gt;
 *          &lt;xsd:annotation&gt;          &lt;xsd:documentation&gt;             The
 *              GetCapapbilities element is used to request that a Web
 *              Feature             Service generate an XML document
 *              describing the organization             providing the
 *              service, the WFS operations that the service
 *              supports, a list of feature types that the service can
 *              operate             on and list of filtering capabilities
 *              that the service support.             Such an XML document
 *              is called a capabilities document.
 *          &lt;/xsd:documentation&gt;       &lt;/xsd:annotation&gt;    &lt;/xsd:element&gt;
 *
 *          </code>
 *         </pre>
 * </p>
 *
 * @generated
 */
public class GetCapabilitiesBinding extends AbstractComplexEMFBinding {
    public GetCapabilitiesBinding(WfsFactory wfsfactory) {
        super( wfsfactory );
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WFS.GETCAPABILITIES;
    }
}
