package org.geoserver.bkprst.test;

import java.util.UUID;

import org.geoserver.bkprst.ConfigurableDispatcherCallback;

class MockBackupTask extends MockBrTask {
    
    /** serialVersionUID */
    private static final long serialVersionUID = -8495270705229732421L;

    public MockBackupTask(UUID id, String path, ConfigurableDispatcherCallback locker) {
        super(id, path, locker);
    }
}

