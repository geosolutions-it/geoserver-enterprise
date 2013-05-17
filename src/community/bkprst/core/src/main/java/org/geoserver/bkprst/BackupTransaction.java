/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst;

import it.geosolutions.tools.io.file.Collector;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

/**
 * Class implementing a basic transanction management for BackupTask 
 * 
 * @author Luca Morandini lmorandini@ieee.org
 *
 */
public class BackupTransaction extends BrTransaction{

    BackupTransaction(BackupTask task, File srcMount, File trgMount, IOFileFilter filter) {
        super(task, srcMount, trgMount, filter);
    }

    /**
     * Starts the backup 
     */
    public synchronized void start() {
        this.task.lock();
        this.task.setState(BrTaskState.STARTING);
        this.task.setStartTime(new Date());
        BrTask.LOGGER.info("Started backup " + this.task.getId() + " from " + this.srcMount.getAbsolutePath()
                + " to " + this.trgMount.getAbsolutePath());
    }
    
    /**
     * Successfuly complete the backup 
     */
    public synchronized void commit() {
        this.task.setState(BrTaskState.COMPLETED);
        BrTask.LOGGER.info("Backup " + this.task.getId() + " completed");
        this.task.setEndTime(new Date());
        this.task.unlock();
    }
    
    /**
     * Aborts the backup 
     */
    public synchronized void rollback() {
        try {
            FileUtils.deleteDirectory(this.trgMount);
        } catch(Exception e) {
            BrTask.LOGGER.severe(e.getMessage());
        } finally {
            this.task.setState(BrTaskState.FAILED);
            BrTask.LOGGER.severe("Backup " + this.task.getId() + " rolled back");
            this.task.setEndTime(new Date());
            this.task.unlock();
        }
    }
}
