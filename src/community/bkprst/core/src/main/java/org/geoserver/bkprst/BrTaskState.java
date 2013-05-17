/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst;

/*
 * @author Luca Morandini lmorandini@ieee.org
 * 
 * State of backup/restore tasks
 * 
 * TODO: add i18n messages
 */
public enum BrTaskState {
    QUEUED(false), // Task in quue, waiting for oter tasks to be completed 
    STARTING(false), // Task started, but not yet acquired locks
    RUNNING(false), // Task started, actually copying
    STOPPED(true), // Completed because external user/process stopped it
    FAILED(true), // Completed for internal failure
    COMPLETED(true); // Succesfully completed

    boolean completed;

    BrTaskState(boolean completed) {
        this.completed = completed;
    }

    public boolean completed() {
        return completed;
    }
}
