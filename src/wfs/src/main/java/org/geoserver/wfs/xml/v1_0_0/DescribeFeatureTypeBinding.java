/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import javax.xml.namespace.QName;

import net.opengis.wfs.WfsFactory;

import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;


/**
 * Binding object for the element http://www.opengis.net/wfs:DescribeFeatureType.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;xsd:element name="DescribeFeatureType"
 *      type="wfs:DescribeFeatureTypeType"&gt;       &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;             The DescribeFeatureType
 *              element is used to request that a Web             Feature
 *              Service generate a document describing one or more
 *              feature types.          &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;    &lt;/xsd:element&gt;
 *
 *          </code>
 *         </pre>
 * </p>
 *
 * @generated
 */
public class DescribeFeatureTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public DescribeFeatureTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WFS.DESCRIBEFEATURETYPE;
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
