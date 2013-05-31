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
 * Binding object for the type http://www.opengis.net/wfs:MetadataURLType.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;xsd:complexType name="MetadataURLType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              A Web Feature Server MAY use zero or more MetadataURL
 *              elements to offer detailed, standardized metadata about
 *              the data underneath a particular feature type.  The type
 *              attribute indicates the standard to which the metadata
 *              complies; the format attribute indicates how the metadata is
 *              structured.  Two types are defined at present:
 *              'TC211' or 'ISO19115' = ISO TC211 19115;
 *              'FGDC'                = FGDC CSDGM.
 *              'ISO19139'            = ISO 19139
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:simpleContent&gt;
 *          &lt;xsd:extension base="xsd:string"&gt;
 *              &lt;xsd:attribute name="type" use="required"&gt;
 *                  &lt;xsd:simpleType&gt;
 *                      &lt;xsd:restriction base="xsd:NMTOKEN"&gt;
 *                          &lt;xsd:enumeration value="TC211"/&gt;
 *                          &lt;xsd:enumeration value="FGDC"/&gt;
 *                          &lt;xsd:enumeration value="19115"/&gt;
 *                          &lt;xsd:enumeration value="19139"/&gt;
 *                      &lt;/xsd:restriction&gt;
 *                  &lt;/xsd:simpleType&gt;
 *              &lt;/xsd:attribute&gt;
 *              &lt;xsd:attribute name="format" use="required"&gt;
 *                  &lt;xsd:simpleType&gt;
 *                      &lt;xsd:restriction base="xsd:NMTOKEN"&gt;
 *                          &lt;xsd:enumeration value="text/xml"/&gt;
 *                          &lt;xsd:enumeration value="text/html"/&gt;
 *                          &lt;xsd:enumeration value="text/sgml"/&gt;
 *                          &lt;xsd:enumeration value="text/plain"/&gt;
 *                      &lt;/xsd:restriction&gt;
 *                  &lt;/xsd:simpleType&gt;
 *              &lt;/xsd:attribute&gt;
 *          &lt;/xsd:extension&gt;
 *      &lt;/xsd:simpleContent&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 * </p>
 *
 * @generated
 */
public class MetadataURLTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public MetadataURLTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WFS.METADATAURLTYPE;
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
