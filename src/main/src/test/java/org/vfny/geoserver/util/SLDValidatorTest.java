/* Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import java.util.List;

import org.geoserver.test.GeoServerTestSupport;

public class SLDValidatorTest extends GeoServerTestSupport {

    public void testValid() throws Exception {
        SLDValidator validator = new SLDValidator();
        List errors = validator.validateSLD(getClass().getResourceAsStream("valid.sld"));
        
        //showErrors(errors);
        assertTrue(errors.isEmpty());
    }
    
    public void testInvalid() throws Exception {
        SLDValidator validator = new SLDValidator();
        List errors = validator.validateSLD(getClass().getResourceAsStream("invalid.sld"));
        
        showErrors(errors);
        assertFalse(errors.isEmpty());
    }
    
    void showErrors(List errors) {
        for (Exception err : (List<Exception>)errors) {
            System.out.println(err.getMessage());
        }
    }
}
