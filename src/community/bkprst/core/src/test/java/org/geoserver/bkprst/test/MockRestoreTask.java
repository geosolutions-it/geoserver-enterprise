package org.geoserver.bkprst.test;

import java.util.UUID;

import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.bkprst.ConfigurableDispatcherCallback;
import org.geoserver.config.GeoServerDataDirectory;

class MockRestoreTask extends MockBrTask {
    
    public MockRestoreTask(UUID id, String path, ConfigurableDispatcherCallback locker) {
        super(id, path, locker);
    }

    public void lock() {
        this.locker.setLockType(LockType.READ);
        this.locker.setEnabled(true);
    }
}

