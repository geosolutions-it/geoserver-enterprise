package org.geoserver.bkprst.test;

import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.bkprst.BrTask;
import org.geoserver.bkprst.BrTaskState;
import org.geoserver.bkprst.ConfigurableDispatcherCallback;
import org.geotools.util.logging.Logging;

/**
 * Inner classes simulating tasks
 */
class MockBrTask extends BrTask {
    
    /** serialVersionUID */
    private static final long serialVersionUID = -2051509813693250760L;
    
    private final static Logger LOGGER = Logging.getLogger(MockBrTask.class.toString());

    public MockBrTask(UUID id, String path, ConfigurableDispatcherCallback locker) {
        super(id, path, locker);
    }

    /**
     * Simulates the run of a task
     */
    public void run () {
        this.state = BrTaskState.STARTING;
        this.lock(); 
        LOGGER.finest("Task " + this.id.toString() + " is started");
        this.startTime = new Date();
        this.progress = 0;
        try {
            int i;
            for (i = 0; i < 10; i++) {
                if (this.state == BrTaskState.STOPPED) {
                    break;
                }
                Thread.sleep(BrManagerTest.TASKDURATION / 10);
            }
            this.progress = (i + 1) * 10;
        } catch (InterruptedException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        if (this.state != BrTaskState.STOPPED) {
            this.endTime = new Date();
            this.state = BrTaskState.COMPLETED;
            LOGGER.finest("Task " + this.id.toString() + " is completed");
            this.progress = 100;
        }
        this.unlock(); 
    }

}
