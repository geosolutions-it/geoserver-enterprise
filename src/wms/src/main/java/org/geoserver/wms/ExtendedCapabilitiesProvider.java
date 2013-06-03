/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.List;

/**
 * Extension point that allows plugins to dynamically contribute extended properties
 * to the WMS capabilities document.
 *  
 * @author Justin Deoliveira, OpenGeo
 *
 */
public interface ExtendedCapabilitiesProvider extends org.geoserver.ExtendedCapabilitiesProvider<WMSInfo, GetCapabilitiesRequest>{

    /**
     * Returns the element names that are direct children of {@code VendorSpecificCapabilities}
     * contributed by this extended capabilities provider for WMS 1.1.1 DOCTYPE declaration.
     * <p>
     * This method returns only the element names that are direct children of
     * VendorSpecificCapabilities so that they can be aggregated in a single declaration like
     * {@code <!ELEMENT VendorSpecificCapabilities (ContributedElementOne*, ContributedElementTwo*) >}
     * . Implement {@link #getVendorSpecificCapabilitiesChildDecls()} to contribute the child
     * elements of these root ones.
     * </p>
     * 
     * @return the name of the elements to be declared as direct children of
     *         VendorSpecificCapabilities in a WMS 1.1.1 DOCTYPE internal DTD.
     */
    List<String> getVendorSpecificCapabilitiesRoots(GetCapabilitiesRequest request);
    
    /**
     * Returns the list of internal DTD element declarations contributed to WMS 1.1.1 DOCTYPE
     * GetCapabilities document.
     * <p>
     * Example DTD element declaration that could be a memeber of the returned list: "
     * {@code <!ELEMENT Resolutions (#PCDATA) >}"
     * </p>
     * 
     * @return the list of GetCapabilities internal DTD elements declarations, may be empty.
     */
    List<String> getVendorSpecificCapabilitiesChildDecls(GetCapabilitiesRequest request);
    
}
