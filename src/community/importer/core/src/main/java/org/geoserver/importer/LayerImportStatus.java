/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: LayerImportStatus.java 174 2012-01-23 15:11:17Z alessio $
 */
package org.geoserver.importer;

/**
 * The result of the import process for a certain layer
 * @author Andrea Aime, GeoSolutions SAS
 *
 */
public enum LayerImportStatus
{
    SUCCESS(true),
    DEFAULTED_SRS(true),
    DUPLICATE(false),
    MISSING_NATIVE_CRS(false),
    NO_SRS_MATCH(false),
    MISSING_BBOX(false),
    OTHER(false),
    UNSUPPORTED_FORMAT(false),
    IMPORT_ABORTED(false);

    boolean success;

    LayerImportStatus(boolean success)
    {
        this.success = success;
    }

    public boolean successful()
    {
        return success;
    }
}
