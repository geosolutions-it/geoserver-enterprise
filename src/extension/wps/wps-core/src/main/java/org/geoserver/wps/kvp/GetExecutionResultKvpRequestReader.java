/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.kvp;

import org.geoserver.ows.KvpRequestReader;
import org.geoserver.wps.GetExecutionStatusType;

/**
 * KVP reader for the ExecutionStatus request
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class GetExecutionResultKvpRequestReader extends KvpRequestReader {

    public GetExecutionResultKvpRequestReader() {
        super(GetExecutionStatusType.class);
    }

}
