/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Class managing queud backuo/restore tasks
 * 
 * @author Luca Morandini lmorandini@ieee.org
 * 
 */
public class BrManager {

    private List<BrTask> tasks;

    private ExecutorService exec;

    private int taskRetentionTime; // Time (in ms) of completed tasks retention

    private BRLockDispatcherCallback writeLocker;

    private final static Logger LOGGER = Logging.getLogger(BrManager.class.toString());

    private final GeoServerDataDirectory dataRoot;

    private XStream xstream;

    // True if the task is undergoing testing
    private boolean test= false;

    // Forced duration (ms) of a task during testing
    public static long TESTTIME= 5000;
    
    // ReST paths
    public static final String REST_ID = "id";

    public static final String REST_MAINPATH = "bkprst";

    public static final String REST_TASKPATH = "/task/path";

    public static final String REST_INCLUDEDATAPATH = "/task/includedata";

    public static final String REST_INCLUDEGWCPATH = "/task/includegwc";

    public static final String REST_INCLUDELOGPATH = "/task/includelog";

    public BrManager(BRLockDispatcherCallback writeLocker, int taskRetentionTime,
            final GeoServerDataDirectory dataRoot) {
        this.taskRetentionTime = taskRetentionTime;
        this.exec = Executors.newSingleThreadExecutor();
        this.tasks = new ArrayList<BrTask>();
        this.writeLocker = writeLocker;
        this.dataRoot = dataRoot;

        // Register XML serialization using XStream
        this.xstream = new XStream();
        this.xstream.alias("bkprst", BrManager.class);
        this.xstream.omitField(BrManager.class, "exec");
        this.xstream.omitField(BrManager.class, "taskRetentionTime");
        this.xstream.omitField(BrManager.class, "writeLocker");
        this.xstream.omitField(BrManager.class, "LOGGER");
        this.xstream.omitField(BrManager.class, "dataRoot");
        this.xstream.omitField(BrManager.class, "xstream");
        this.xstream.omitField(BrManager.class, "test");

        this.xstream.aliasField("id", BrTask.class, "id");
        this.xstream.aliasField("state", BrTask.class, "state");
        this.xstream.aliasField("startTime", BrTask.class, "startTime");
        this.xstream.aliasField("endTime", BrTask.class, "endTime");
        this.xstream.aliasField("path", BrTask.class, "path");
        this.xstream.aliasField("progress", BrTask.class, "progress");

        this.xstream.omitField(BrTask.class, "br");
        this.xstream.omitField(BrTask.class, "locker");
        this.xstream.omitField(BrTask.class, "LOGGER");
        this.xstream.omitField(BrTask.class, "dataRoot");
        this.xstream.omitField(BrTask.class, "act");
        this.xstream.omitField(BrTask.class, "dataFilter");
        this.xstream.omitField(BrTask.class, "gwcFilter");
        this.xstream.omitField(BrTask.class, "logFilter");

        this.xstream.omitField(BackupTask.class, "trans");
        this.xstream.omitField(RestoreTask.class, "trans");
        
        this.xstream.alias("backup", BackupTask.class);
        this.xstream.alias("restore", RestoreTask.class);
    }

    /**
     * Selectors to set and check whether the task is undergoing testing
     */
    public void setTest() {
        this.test= true;
    }

    public boolean isTest() {
        return this.test;
    }
    
    /**
     * Retutns a new task UUID
     */
    public UUID generateId() {
        return UUID.randomUUID();
    }

    /**
     * Adds a given task to the execution queue after cleanuo
     */
    synchronized public UUID addTask(BrTask task) {
        this.cleanupTasks();
        task.setDataRoot(this.dataRoot);
        task.setBrManager(this);
        this.tasks.add(task);
        this.exec.execute(task);
        LOGGER.finest("Added backup task " + task.id.toString());
        return task.getId();
    }

    /**
     * Adds a backup task to the execution queue after cleanuo
     * 
     * @param path
     *            Path to backup configuraiton to
     */
    synchronized public UUID addBackupTask(String path, boolean includeData, boolean includeGwc,
            boolean includeLog) {
        BackupTask task = new BackupTask(this.generateId(), path, this.writeLocker, this.dataRoot);
        task.setBrManager(this);
        task.setIncludeData(includeData);
        task.setIncludeGwc(includeGwc);
        task.setIncludeLog(includeLog);
        return this.addTask(task);
    }

    /**
     * Adds a restore task to the execution queue after cleanup
     * 
     * @param path
     *            Path from which restor the configuraiton from
     */
    synchronized public UUID addRestoreTask(String path) {
        RestoreTask task = new RestoreTask(this.generateId(), path, this.writeLocker, this.dataRoot);
        task.setBrManager(this);
        return this.addTask(task);
    }

    /**
     * Attempts to stop a given task after cleanup
     * 
     * @param id
     *            ID of task to stop
     */
    public void stopBackupTask(UUID id) throws UnallowedOperationException, TaskNotFoundException {
        BrTask task = this.getTask(id);
        if (task == null) {
            throw new TaskNotFoundException("Task " + id + " was not found");
        }

        if (task.isBackup()) {
            this.cleanupTasks();
            ((BackupTask) (task)).stop();
        } else {
            throw new UnallowedOperationException("Stopping a restore task is not allowed");
        }
    }

    /**
     * Returns an tasks as a Collection
     */
    public Collection<BrTask> getAllTasks() {
        return this.tasks;
    }

    /**
     * Returns a given task after deleting stale ones
     * 
     * @param id
     *            ID of task to stop
     */
    public BrTask getTask(UUID id) {
        this.cleanupTasks();
        Iterator<BrTask> iter = this.tasks.iterator();
        while (iter.hasNext()) {
            BrTask nextTask = iter.next();
            if (nextTask.getId().equals(id)) {
                return nextTask;
            }
        }
        return null;
    }

    /**
     * Checks if a task has completed and its life after completion has surpassed the retention time
     * 
     * @param task
     *            Task to check
     */
    private boolean isStale(BrTask task) {
        if (task.getState().completed()
                && ((new Date()).getTime() - task.getEndTime().getTime()) > this.taskRetentionTime) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Deletes stale tasks
     */
    synchronized public void cleanupTasks() {
        Iterator<BrTask> iter = this.tasks.iterator();
        while (iter.hasNext()) {
            BrTask entry = iter.next();
            if (this.isStale(entry)) {
                LOGGER.finest("Removed task " + entry.getId().toString());
                iter.remove();
            }
        }
    }

    public int getTaskRetentionTime() {
        return taskRetentionTime;
    }

    public void setTaskRetentionTime(int taskRetentionTime) {
        this.taskRetentionTime = taskRetentionTime;
    }

    public BRLockDispatcherCallback getWriteLocker() {
        return writeLocker;
    }

    public void setWriteLocker(BRLockDispatcherCallback writeLocker) {
        this.writeLocker = writeLocker;
    }

    /**
     * Serializes an object to XML
     * 
     * @param obj
     *            Object to serialize
     */
    public String toXML(Object obj) {
        return this.xstream.toXML(obj);
    }

    /**
     * De-erializes an object from XML
     * 
     * @param xml
     *            XML to get the object from
     * 
     * @param obj
     *            Object to populate
     */
    public void fromXML(String xml, Object obj) {
        this.xstream.fromXML(xml, obj);
    }
}
