/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst;

import it.geosolutions.tools.io.file.CopyTree;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.config.GeoServerDataDirectory;
import org.geotools.util.logging.Logging;

/**
 * Generic backup/restore task
 * 
 * @author Luca Morandini lmorandini@ieee.org
 */
public abstract class BrTask implements Runnable, Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = -6428867493005214884L;

    protected UUID id;

    protected BrTaskState state;

    protected Date startTime;

    protected Date endTime;

    protected String path;

    protected float progress;

    protected ConfigurableDispatcherCallback locker;

    private final static Logger LOGGER = Logging.getLogger(BrManager.class.toString());

    protected GeoServerDataDirectory dataRoot;

    protected CopyTree act;

    protected IOFileFilter dataFilter = FileFilterUtils.nameFileFilter("data", IOCase.INSENSITIVE);

    protected IOFileFilter gwcFilter = FileFilterUtils.nameFileFilter("gwc", IOCase.INSENSITIVE);

    protected IOFileFilter logFilter = FileFilterUtils.nameFileFilter("logs", IOCase.INSENSITIVE);

    protected BrManager br;

    protected static String INFOFILE= "backup.xml";
    
    protected static String BACKUPEXT= ".backup";
    
    protected volatile boolean haltRequested=false;
    
    protected final Semaphore haltSemaphore= new Semaphore(1);

    protected BrTransaction trans;
    
    public BrTask(UUID id, String path, ConfigurableDispatcherCallback locker) {
        this.id = id;
        this.state = BrTaskState.QUEUED;
        this.progress = 0;
        this.path = path;
        this.locker = locker;
    }

    public BrTask(UUID id, String path, ConfigurableDispatcherCallback locker,
            final GeoServerDataDirectory dataRoot) {
        this(id, path, locker);
        this.dataRoot = dataRoot;
    }

    public void setBrManager(BrManager br) {
        this.br= br;
    }
    
    public void setDataRoot(final GeoServerDataDirectory dataRoot) {
        this.dataRoot = dataRoot;
    }

    public UUID getId() {
        return this.id;
    }

    public BrTaskState getState() {
        return this.state;
    }

    public void setState(BrTaskState state) {
        this.state = state;
    }

    public Date getStartTime() {
        return this.startTime;
    }

    public Date getEndTime() {
        return this.endTime;
    }
    
    public void setStartTime(Date time) {
        this.startTime= time;
    }

    public void setEndTime(Date time) {
        this.endTime= time;
    }

    /*
     * Task execution (to be overriden by subclasses)
     * 
     * @see java.lang.Runnable#run()
     */
    public abstract void run();

    public void lock() {
        this.locker.setLockType(LockType.WRITE);
        this.locker.setEnabled(true);
    }

    public void unlock() {
        this.locker.setEnabled(false);
        // XXX BRLockDispatcherCallback.THREAD_LOCK.remove();
    }

    /*
     * Returns true if current task is a backup one, false if restore
     */
    public boolean isBackup() {
        if (this.getClass().getName().contains("BackupTask")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns an exclusion filter based on directories to avoid during backup based on parameters
     * 
     * @param includeData
     *            Should data directory be included ?
     * @param includeGwc
     *            Should GeoWebCache directory be included ?
     * @param includeLog
     *            Should logs directory be included ?
     */
    protected IOFileFilter getExcludeFilter(boolean includeData, boolean includeGwc,
            boolean includeLog) {
        List<IOFileFilter> filesToExclude = new ArrayList<IOFileFilter>();
        if (!includeData) {
            filesToExclude.add(this.dataFilter);
        }
        if (!includeGwc) {
            filesToExclude.add(this.gwcFilter);
        }
        if (!includeLog) {
            filesToExclude.add(this.logFilter);
        }

        OrFileFilter filesToExcludeFilter = new OrFileFilter();
        filesToExcludeFilter.setFileFilters(filesToExclude);
        return FileFilterUtils.notFileFilter(filesToExcludeFilter);
    }
    
    /**
     * Writes an XML file containing data about current backup
     *  
     * @path Directory to write the info in
     * 
     * @return true on success, false otherwise 
     */
    protected boolean writeBackupInfo(String path) {
    
        File xmlFile= new File(path + File.separatorChar + BrTask.INFOFILE);
        try {
            FileUtils.writeStringToFile(xmlFile, this.br.toXML(this));
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * Reads an XML file containing data about last backup
     *  
     * @path Directory get dasta from
     * 
     * @return a BackupTask object containing info about previous backup 
     */
    protected BackupTask readBackupInfo(String path) {
    
        File xmlFile= new File(path + File.separatorChar + BrTask.INFOFILE);
        BackupTask backupInfo= new BackupTask(null, "", null, null);
        try {
            String xml= FileUtils.readFileToString(xmlFile);
            this.br.fromXML(xml, backupInfo);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
            return null;
        }
        return backupInfo;
    }

    /**
     * @return the haltRequested
     */
    protected boolean isHaltRequested() {
        return haltRequested;
    }

    /**
     * @param haltRequested the haltRequested to set
     */
    protected void setHaltRequested() {
        this.haltRequested = true;
    }

    /**
     * Checks if an Halt was requested
     * 
     * @return
     */
    protected boolean checkForHalt() {
        if(isHaltRequested()){
            // cancel
            if (this.act != null) {
                this.act.setCancelled();
            }
            return true;
        }
        return false;
    }

    /*
     * Stops current backup
     */
    public void stop() {
        LOGGER.info("Backup " + this.id + " stopped");
        
        LOGGER.fine("stop:Halt requested " + this.id );
        // request halt
        setHaltRequested();
        try {
            // wait for stop
            haltSemaphore.acquire();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        LOGGER.fine("stop:Semaphore taken " + this.id);
        
        // closing all file handles...
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().runFinalization();
        System.gc();
        System.gc();
        System.gc();
        
        LOGGER.fine("stop:About to rollback " + this.id);
        if(trans!=null){
            this.trans.rollback();
        }
        this.state = BrTaskState.STOPPED;
        LOGGER.info("stop:STOPPED " + this.id);
    }
}
