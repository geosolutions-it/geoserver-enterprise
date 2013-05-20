package org.geoserver.bkprst.test;

import java.util.UUID;

import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.bkprst.ConfigurableDispatcherCallback;

class MockRestoreTask extends MockBrTask {
    
    /** serialVersionUID */
    private static final long serialVersionUID = -5654408222059689673L;

    public MockRestoreTask(UUID id, String path, ConfigurableDispatcherCallback locker) {
        super(id, path, locker);
    }

    public void lock() {
        this.locker.setLockType(LockType.READ);
        this.locker.setEnabled(true);
    }
}

