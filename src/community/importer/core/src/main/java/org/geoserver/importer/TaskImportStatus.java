/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: TaskImportStatus.java 174 2012-01-23 15:11:17Z alessio $
 */
package org.geoserver.importer;

/**
 * The result of an import task
 * @author Luca Morandini lmorandini@ieee.org
 */
public enum TaskImportStatus
{
    INPROGRESS,
    SUCCESS,
    CANCELED,
    ISSUES,
    ERROR;
}
