/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.opengis.wps10.DescribeProcessType;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.GetCapabilitiesType;
import net.opengis.wps10.ProcessDescriptionsType;
import net.opengis.wps10.WPSCapabilitiesType;

/**
 * @author Lucas Reed, Refractions Research Inc
 * @author Justin Deoliveira, OpenGEO
 */
public interface WebProcessingService {
    /**
     * Generates a XML object for the return of the getCapabilities request
     *
     */
    WPSCapabilitiesType getCapabilities(GetCapabilitiesType request) throws WPSException;

    /**
     * Generates a XML object for  the return of the describeProcess request
     */
    ProcessDescriptionsType describeProcess(DescribeProcessType request) throws WPSException;

    /**
     * Executes a execute request and writes output to the Servlet response
     */
    ExecuteResponseType execute(ExecuteType request) throws WPSException;
    
    /**
     * Returns the status of a given process execution, either as a {@link ExecuteResponseType} or
     * as a stored response File
     * @param request
     * @return
     * @throws WPSException
     */
    Object getExecutionStatus(GetExecutionStatusType request) throws WPSException;

    /**
     * Executes a get schema request and writes the output to the Servlet response
     *
     * @param request
     * @param response
     * @throws WPSException
     */
    void getSchema(HttpServletRequest request, HttpServletResponse response) throws WPSException;

    /**
     * Returns a file stored as a reference in the specified execution 
     * 
     * @param request
     * @return
     * @throws WPSException
     */
    File getExecutionResult(GetExecutionResultType request) throws WPSException;
}
