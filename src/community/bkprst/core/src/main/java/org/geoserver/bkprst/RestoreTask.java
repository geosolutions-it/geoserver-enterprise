/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst;

import it.geosolutions.tools.commons.listener.DefaultProgress;
import it.geosolutions.tools.io.file.Collector;
import it.geosolutions.tools.io.file.CopyTree;

import java.io.File;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.h2.util.FileUtils;
import org.springframework.context.ApplicationContext;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Restore task
 * 
 * @author Luca Morandini lmorandini@ieee.org
 */
@XStreamAlias("restore")
public class RestoreTask extends BrTask {

    protected RestoreTransaction trans;
    
    public RestoreTask(UUID id, String path, ConfigurableDispatcherCallback locker,
            final GeoServerDataDirectory dataRoot) {
        super(id, path, locker, dataRoot);
    }

    @Override
    public void lock() {
        this.locker.setLockType(LockType.READ);
        this.locker.setEnabled(true);
    }

    /*
     * Restore execution
     * 
     * @see org.geoserver.br.BrTask#run()
     */
    @Override
    public void run() {

        // If previous' backup info cannot be read, aborts the restore
        // Writes info about backup in a file
        BackupTask backupInfo = this.readBackupInfo(this.path);
        if (backupInfo == null) {
            LOGGER.severe("Backup data info were not written properly, the restore will not start");
            this.state = BrTaskState.FAILED;
            return;
        }

        // Sets up the filter to exclude some directories according to the previous backup info
        IOFileFilter excludeFilter = this.getExcludeFilter(backupInfo.includeData,
                backupInfo.includeGwc, backupInfo.includeLog);

        // Sets up source and destination
        File srcMount = new File(this.path);
        File trgMount = this.dataRoot.root();

        // Sets transaction
        this.trans = new RestoreTransaction(this, srcMount, trgMount, excludeFilter);

        try {
            // Start transanction
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

            // Starts restore
            int workSize = this.act.copy();
            LOGGER.info("Restore " + this.id + " has started");
            this.startTime = new Date();
            this.state = BrTaskState.RUNNING;

            // This is to keep track af restore advancement
            while (workSize-- > 0) {
                Future<File> future = cs.take();
                try {
                    LOGGER.info("copied file: " + future.get());
                } catch (ExecutionException e) {
                    LOGGER.info(e.getMessage());
                }
            }
            
            // In case of test, pauses a while to let the test case unfold
            if (this.br.isTest() ) {
                Thread.sleep(BrManager.TESTTIME);    
            }

            // Restore completed
            this.trans.commit();

            // reload the config from disk
            getGeoServer().reload();
        } catch (Exception e) {

            // In case of errors, rollback
            this.trans.rollback();
        } finally {
            this.trans= null;
        }
    }
    
    private GeoServer getGeoServer() {
        return (GeoServer) GeoServerExtensions.bean("geoServer");
    }

    /*
     * Stops current restore
     */
    public void stop() {
        LOGGER.info("Restore " + this.id + " stopped");
        if (this.act != null) {
            this.act.setCancelled();
        }
        if (this.trans != null) {
            this.trans.rollback();
        }
        this.state = BrTaskState.STOPPED;
    }

}
