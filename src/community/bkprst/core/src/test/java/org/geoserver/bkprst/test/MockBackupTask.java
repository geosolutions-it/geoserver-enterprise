package org.geoserver.bkprst.test;

import java.util.UUID;

import org.geoserver.bkprst.ConfigurableDispatcherCallback;
import org.geoserver.config.GeoServerDataDirectory;

class MockBackupTask extends MockBrTask {
    public MockBackupTask(UUID id, String path, ConfigurableDispatcherCallback locker) {
        super(id, path, locker);
    }
}

