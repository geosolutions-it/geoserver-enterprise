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
 * Binding object for the element http://www.opengis.net/wfs:Query.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;xsd:element name="Query" type="wfs:QueryType"&gt;       &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;             The Query element is used to
 *              describe a single query.             One or more Query
 *              elements can be specified inside a             GetFeature
 *              element so that multiple queries can be
 *              executed in one request.  The output from the various
 *              queries are combined in a wfs:FeatureCollection element
 *              to form the response to the request.
 *          &lt;/xsd:documentation&gt;       &lt;/xsd:annotation&gt;    &lt;/xsd:element&gt;
 *
 *          </code>
 *         </pre>
 * </p>
 *
 * @generated
 */
public class QueryBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public QueryBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WFS.QUERY;
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
