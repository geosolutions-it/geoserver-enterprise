/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst;

import java.io.File;
import java.util.List;

import org.apache.commons.io.filefilter.IOFileFilter;

/**
 * Abstract class for backup/restore transactions
 * 
 * @author Luca Morandini lmorandini@ieee.org
 * 
 */
public abstract class BrTransaction {

    protected BrTask task;

    protected File srcMount;

    protected File trgMount;

    protected IOFileFilter filter;

    protected List<File> topFiles;

    BrTransaction(BrTask task, File srcMount, File trgMount, IOFileFilter filter) {
        this.task = task;
        this.srcMount = srcMount;
        this.trgMount = trgMount;
        this.filter = filter;
    }

    public abstract void start() throws Exception;

    public abstract void commit();

    public abstract void rollback();

}
