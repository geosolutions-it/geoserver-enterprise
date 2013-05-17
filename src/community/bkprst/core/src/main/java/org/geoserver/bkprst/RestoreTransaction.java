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
public class RestoreTransaction extends BrTransaction {

    RestoreTransaction(RestoreTask task, File srcMount, File trgMount, IOFileFilter filter) {
        super(task, srcMount, trgMount, filter);
    }

    /**
     * Starts the restore
     */
    public synchronized void start() throws Exception {
        task.setStartTime(new Date());
        task.setState(BrTaskState.STARTING);
        task.lock();
        BrTask.LOGGER.info("Started restore " + task.id + " to " + task.path + " from "
                + srcMount.getAbsolutePath() + " to" + this.trgMount.getAbsolutePath());

        // Puts in topFiles the top-level files of the target directoy using the filter
        Collector topColl = new Collector(this.filter, 1);
        this.topFiles = topColl.collect(this.trgMount);

        // Renames selected top-level files (deletes files with the sane name
        // if already present)
        for (File file : this.topFiles) {
            if (!file.equals(this.trgMount)) {
                File renamedFile= new File(file.getAbsolutePath() + BrTask.BACKUPEXT);
                try {
                    FileUtils.forceDelete(renamedFile);
                } catch(FileNotFoundException e) {
                }
                file.renameTo(renamedFile);
            }
        }
    }

    /**
     * Successfuly complete the restore
     */
    public synchronized void commit() {

        // Deletes selected top-level files who had their name changed 
        try {
            for (File file : this.topFiles) {
                if (!file.equals(this.trgMount)) {
                    File renamedFile= new File(file.getAbsolutePath() + BrTask.BACKUPEXT);
                    try {
                        FileUtils.forceDelete(renamedFile);
                    } catch(FileNotFoundException e) {
                    }
                }
            }
        } catch (Exception e) {
            BrTask.LOGGER.severe(e.getMessage());
            task.setState(BrTaskState.FAILED);
            task.setEndTime(new Date());
            BrTask.LOGGER.info("Restore " + task.getId() + " rolled back");
            task.unlock();
            return;
        }

        task.setState(BrTaskState.COMPLETED);
        task.setEndTime(new Date());
        BrTask.LOGGER.info("Restore " + task.getId() + " completed");
        task.unlock();
    }

    /**
     * Aborts the restore
     */
    public synchronized void rollback() {

        // Renames selected top-level files back the original name (deletes files with the sane name
        // if present)
        try {
            for (File file : this.topFiles) {
                if (!file.equals(this.trgMount)) {
                    File renamedFile= new File(file.getAbsolutePath() + BrTask.BACKUPEXT);
                    try {
                        FileUtils.forceDelete(file);
                    } catch(FileNotFoundException e) {
                    }
                    renamedFile.renameTo(file);
                }
            }
            File xmlFile= new File(this.trgMount.getAbsolutePath() + File.separatorChar + BrTask.INFOFILE);
            xmlFile.delete();
        } catch (Exception e) {
            BrTask.LOGGER.severe(e.getMessage());
        } finally {
            task.setState(BrTaskState.FAILED);
            task.setEndTime(new Date());
            BrTask.LOGGER.severe("Restore " + task.getId() + " failed");
            task.unlock();
        }
    }
}