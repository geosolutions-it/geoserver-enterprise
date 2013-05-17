/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst;

import it.geosolutions.tools.commons.listener.DefaultProgress;
import it.geosolutions.tools.commons.listener.Progress;
import it.geosolutions.tools.io.file.CopyTree;
import it.geosolutions.tools.io.file.Remove;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import org.geoserver.config.GeoServerDataDirectory;
import org.springframework.context.ApplicationContext;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author Luca Morandini lmorandini@ieee.org
 * 
 */
public class BackupTask extends BrTask {

    // Flag to asvoid backup data directory
    protected boolean includeData = false;

    // Flag to asvoid backup GeoWebCache directory
    protected boolean includeGwc = false;

    // Flag to asvoid backup logs directory
    protected boolean includeLog = false;

    protected BackupTransaction trans; 
    
    public BackupTask(UUID id, String path, ConfigurableDispatcherCallback locker,
            final GeoServerDataDirectory dataRoot) {
        super(id, path, locker, dataRoot);
    }

    public void setIncludeData(boolean includeData) {
        this.includeData = includeData;
    }

    public void setIncludeGwc(boolean includeGwc) {
        this.includeGwc = includeGwc;
    }

    public void setIncludeLog(boolean includeLog) {
        this.includeLog = includeLog;
    }

    /*
     * Backup execution
     * 
     * @see org.geoserver.br.BrTask#run()
     */
    @Override
    public void run() {

        // Sets up the filter to exclude some directories according to the previous backup info
        IOFileFilter excludeFilter = this.getExcludeFilter(this.includeData, this.includeGwc,
                this.includeLog);

        // Sets up source and destination
        File srcMount = this.dataRoot.root();
        File trgMount = new File(this.path);

        // Sets transaction
        this.trans = new BackupTransaction(this, srcMount, trgMount, excludeFilter);

        try {
            // Deletes dest directory if existing
            if (trgMount.exists()) {
                Remove.deleteDirectory(
                        trgMount,
                        FileFilterUtils.or(FileFilterUtils.directoryFileFilter(),
                                FileFilterUtils.fileFileFilter()), true, true);
            }

            // Starts transanction
            this.trans.start();

            // Sets up the copy task
            ExecutorService ex = Executors.newFixedThreadPool(2);
            if (ex == null || ex.isTerminated()) {
                throw new IllegalArgumentException(
                        "Unable to run asynchronously using a terminated or null ThreadPoolExecutor");
            }
            ExecutorCompletionService<File> cs = new ExecutorCompletionService<File>(ex);

            this.act = new CopyTree(excludeFilter, cs, srcMount, trgMount);
            this.act.addCopyListener(new DefaultProgress(this.id.toString()) {
                public void onUpdateProgress(float percent) {
                    super.onUpdateProgress(percent);
                    progress = percent;
                }
            });

            // Starts backup
            int workSize = this.act.copy();

            // This is to keep track af restore advancement
            while (workSize-- > 0) {
                Future<File> future;
                try {
                    future = cs.take();
                    LOGGER.info("copied file: " + future.get());
                } catch (Exception e) {
                    LOGGER.info(e.getMessage());
                }
            }

            // In case of test, pauses a while to let the test case unfold
            if (this.br.isTest() ) {
                Thread.sleep(BrManager.TESTTIME);    
            }
            
            // Writes info about backup
            if (!this.writeBackupInfo(this.path)) {
                LOGGER.severe("Backup data info were not written properly, a restore operation will fail on this data");
                this.state = BrTaskState.FAILED;
            }

            // Restore completed
            this.trans.commit();
            
        } catch (Exception e) {

            // In case of errors, rollbacks
            this.trans.rollback();
        } finally {
            this.trans= null;
        }
    }

    /*
     * Stops current backup
     */
    public void stop() {
        LOGGER.info("Backup " + this.id + " stopped");
        if (this.act != null) {
            this.act.setCancelled();
        }
        if (this.trans != null) {
            this.trans.rollback();
        }
        this.state = BrTaskState.STOPPED;
    }

}
